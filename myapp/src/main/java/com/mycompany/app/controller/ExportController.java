package com.mycompany.app.controller;

import com.mycompany.app.dto.ResumenGrupoDTO;
import com.mycompany.app.entity.Gasto;
import com.mycompany.app.entity.Pago;
import com.mycompany.app.service.GastoService;
import com.mycompany.app.repository.GastoRepository;
import com.mycompany.app.repository.PagoRepository;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import com.opencsv.CSVWriter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/export")
public class ExportController {

    @Autowired
    private GastoService gastoService;

    @Autowired
    private GastoRepository gastoRepository;

    @Autowired
    private PagoRepository pagoRepository;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @GetMapping("/grupo/{grupoId}/pdf")
    public ResponseEntity<byte[]> exportarPDF(@PathVariable("grupoId") Long grupoId) {
        try {
            ResumenGrupoDTO resumen = gastoService.obtenerResumenGrupo(grupoId);
            List<Gasto> gastos = gastoRepository.findByGrupoId(grupoId);
            List<Pago> pagos = pagoRepository.findByGrupoId(grupoId);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document doc = new Document(pdfDoc);

            // Título
            doc.add(new Paragraph("Resumen del Grupo")
                    .setFontSize(20).setBold().setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("Gasto total: " + resumen.getTotalGastado() + "€")
                    .setFontSize(12).setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph(" "));

            // Balances
            doc.add(new Paragraph("Balances").setFontSize(16).setBold());
            Table balanceTable = new Table(UnitValue.createPercentArray(new float[]{3, 2, 2}))
                    .useAllAvailableWidth();
            balanceTable.addHeaderCell(new Cell().add(new Paragraph("Miembro").setBold()));
            balanceTable.addHeaderCell(new Cell().add(new Paragraph("Balance").setBold()));
            balanceTable.addHeaderCell(new Cell().add(new Paragraph("Estado").setBold()));

            resumen.getBalances().forEach(b -> {
                balanceTable.addCell(b.getUsername());
                balanceTable.addCell(String.format("%.2f€", b.getBalance()));
                balanceTable.addCell(b.getEstado());
            });
            doc.add(balanceTable);
            doc.add(new Paragraph(" "));

            // Transferencias recomendadas
            if (resumen.getSoluciones() != null && !resumen.getSoluciones().isEmpty()) {
                doc.add(new Paragraph("Transferencias recomendadas").setFontSize(16).setBold());
                Table solTable = new Table(UnitValue.createPercentArray(new float[]{3, 3, 2}))
                        .useAllAvailableWidth();
                solTable.addHeaderCell(new Cell().add(new Paragraph("De").setBold()));
                solTable.addHeaderCell(new Cell().add(new Paragraph("A").setBold()));
                solTable.addHeaderCell(new Cell().add(new Paragraph("Monto").setBold()));

                resumen.getSoluciones().forEach(s -> {
                    solTable.addCell(s.getDeUsername());
                    solTable.addCell(s.getAUsername());
                    solTable.addCell(String.format("%.2f€", s.getMonto()));
                });
                doc.add(solTable);
                doc.add(new Paragraph(" "));
            }

            // Gastos
            doc.add(new Paragraph("Detalle de gastos").setFontSize(16).setBold());
            Table gastoTable = new Table(UnitValue.createPercentArray(new float[]{3, 2, 2, 2, 2}))
                    .useAllAvailableWidth();
            gastoTable.addHeaderCell(new Cell().add(new Paragraph("Concepto").setBold()));
            gastoTable.addHeaderCell(new Cell().add(new Paragraph("Monto").setBold()));
            gastoTable.addHeaderCell(new Cell().add(new Paragraph("Pagador").setBold()));
            gastoTable.addHeaderCell(new Cell().add(new Paragraph("Categoría").setBold()));
            gastoTable.addHeaderCell(new Cell().add(new Paragraph("Fecha").setBold()));

            gastos.forEach(g -> {
                gastoTable.addCell(g.getConcepto() != null ? g.getConcepto() : "");
                gastoTable.addCell(String.format("%.2f€", g.getMonto()));
                gastoTable.addCell(g.getPagador() != null ? g.getPagador().getUsername() : "");
                gastoTable.addCell(g.getCategoria() != null ? g.getCategoria().name() : "OTROS");
                gastoTable.addCell(g.getFecha() != null ? g.getFecha().format(FMT) : "");
            });
            doc.add(gastoTable);
            doc.add(new Paragraph(" "));

            // Pagos
            if (!pagos.isEmpty()) {
                doc.add(new Paragraph("Historial de pagos").setFontSize(16).setBold());
                Table pagoTable = new Table(UnitValue.createPercentArray(new float[]{3, 3, 2, 2}))
                        .useAllAvailableWidth();
                pagoTable.addHeaderCell(new Cell().add(new Paragraph("Pagador").setBold()));
                pagoTable.addHeaderCell(new Cell().add(new Paragraph("Receptor").setBold()));
                pagoTable.addHeaderCell(new Cell().add(new Paragraph("Monto").setBold()));
                pagoTable.addHeaderCell(new Cell().add(new Paragraph("Fecha").setBold()));

                pagos.forEach(p -> {
                    pagoTable.addCell(p.getPagador() != null ? p.getPagador().getUsername() : "");
                    pagoTable.addCell(p.getReceptor() != null ? p.getReceptor().getUsername() : "");
                    pagoTable.addCell(String.format("%.2f€", p.getMonto()));
                    pagoTable.addCell(p.getFecha() != null ? p.getFecha().format(FMT) : "");
                });
                doc.add(pagoTable);
            }

            doc.close();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=resumen_grupo_" + grupoId + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(baos.toByteArray());

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/grupo/{grupoId}/csv")
    public ResponseEntity<byte[]> exportarCSV(@PathVariable("grupoId") Long grupoId) {
        try {
            List<Gasto> gastos = gastoRepository.findByGrupoId(grupoId);
            List<Pago> pagos = pagoRepository.findByGrupoId(grupoId);

            StringWriter sw = new StringWriter();
            CSVWriter csvWriter = new CSVWriter(sw);

            // Cabecera gastos
            csvWriter.writeNext(new String[]{"GASTOS"});
            csvWriter.writeNext(new String[]{"Concepto", "Monto", "Pagador", "Categoría", "Participantes", "Fecha"});

            gastos.forEach(g -> {
                String participantes = (g.isRepartoGeneral() || g.getParticipantes() == null || g.getParticipantes().isEmpty())
                        ? "General (todos)"
                        : g.getParticipantes().stream().map(u -> u.getUsername()).collect(java.util.stream.Collectors.joining(", "));
                csvWriter.writeNext(new String[]{
                        g.getConcepto() != null ? g.getConcepto() : "",
                        String.format("%.2f", g.getMonto()),
                        g.getPagador() != null ? g.getPagador().getUsername() : "",
                        g.getCategoria() != null ? g.getCategoria().name() : "OTROS",
                        participantes,
                        g.getFecha() != null ? g.getFecha().format(FMT) : ""
                });
            });

            // Separador
            csvWriter.writeNext(new String[]{});

            // Cabecera pagos
            csvWriter.writeNext(new String[]{"PAGOS"});
            csvWriter.writeNext(new String[]{"Pagador", "Receptor", "Monto", "Fecha"});

            pagos.forEach(p -> {
                csvWriter.writeNext(new String[]{
                        p.getPagador() != null ? p.getPagador().getUsername() : "",
                        p.getReceptor() != null ? p.getReceptor().getUsername() : "",
                        String.format("%.2f", p.getMonto()),
                        p.getFecha() != null ? p.getFecha().format(FMT) : ""
                });
            });

            csvWriter.close();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=resumen_grupo_" + grupoId + ".csv")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(sw.toString().getBytes("UTF-8"));

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
