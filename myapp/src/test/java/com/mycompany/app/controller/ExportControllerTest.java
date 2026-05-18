package com.mycompany.app.controller;

import com.mycompany.app.dto.BalancePersonaDTO;
import com.mycompany.app.dto.ResumenGrupoDTO;
import com.mycompany.app.dto.TransferenciaDTO;
import com.mycompany.app.entity.Gasto;
import com.mycompany.app.entity.Pago;
import com.mycompany.app.entity.Usuario;
import com.mycompany.app.entity.Grupo;
import com.mycompany.app.entity.Moneda;
import com.mycompany.app.entity.CategoriaGasto;
import com.mycompany.app.repository.GastoRepository;
import com.mycompany.app.repository.PagoRepository;
import com.mycompany.app.service.GastoService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExportControllerTest {

    @Mock
    private GastoService gastoService;

    @Mock
    private GastoRepository gastoRepository;

    @Mock
    private PagoRepository pagoRepository;

    @InjectMocks
    private ExportController exportController;

    private Usuario usuario1;
    private Usuario usuario2;
    private Grupo grupo;
    private Gasto gasto;
    private Pago pago;
    private ResumenGrupoDTO resumen;

    @BeforeEach
    void setUp() {
        usuario1 = new Usuario();
        usuario1.setUsername("Adrien");
        usuario1.setEmail("adrien@test.com");

        usuario2 = new Usuario();
        usuario2.setUsername("Prueba");
        usuario2.setEmail("prueba@test.com");

        grupo = new Grupo("Viaje", Moneda.EURO);

        gasto = new Gasto("Cena", 50.0, usuario1, grupo);
        gasto.setCategoria(CategoriaGasto.COMIDA);
        gasto.setRepartoGeneral(true);

        pago = new Pago(25.0, usuario2, usuario1, grupo);

        BalancePersonaDTO balance1 = new BalancePersonaDTO(1L, "Adrien", 25.0, "positivo");
        BalancePersonaDTO balance2 = new BalancePersonaDTO(2L, "Prueba", -25.0, "debe");
        TransferenciaDTO solucion = new TransferenciaDTO(2L, "Prueba", 1L, "Adrien", 25.0);

        resumen = new ResumenGrupoDTO(50.0, List.of(balance1, balance2), List.of(solucion));
    }

    // ==================== TESTS PDF ====================

    @Test
    void testExportarPDF_devuelveOkConContenido() throws Exception {
        when(gastoService.obtenerResumenGrupo(1L)).thenReturn(resumen);
        when(gastoRepository.findByGrupoId(1L)).thenReturn(List.of(gasto));
        when(pagoRepository.findByGrupoId(1L)).thenReturn(List.of(pago));

        ResponseEntity<byte[]> response = exportController.exportarPDF(1L);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().length > 0);
        assertTrue(response.getHeaders().getContentDisposition().toString().contains("resumen_grupo_1.pdf"));
        assertEquals("application/pdf", response.getHeaders().getContentType().toString());
    }

    @Test
    void testExportarPDF_sinGastosNiPagos() throws Exception {
        when(gastoService.obtenerResumenGrupo(1L)).thenReturn(resumen);
        when(gastoRepository.findByGrupoId(1L)).thenReturn(Collections.emptyList());
        when(pagoRepository.findByGrupoId(1L)).thenReturn(Collections.emptyList());

        ResponseEntity<byte[]> response = exportController.exportarPDF(1L);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().length > 0);
    }

    @Test
    void testExportarPDF_errorDevuelveBadRequest() throws Exception {
        when(gastoService.obtenerResumenGrupo(99L)).thenThrow(new RuntimeException("Grupo no encontrado"));

        ResponseEntity<byte[]> response = exportController.exportarPDF(99L);

        assertEquals(400, response.getStatusCode().value());
    }

    // ==================== TESTS CSV ====================

    @Test
    void testExportarCSV_devuelveOkConContenido() throws Exception {
        when(gastoRepository.findByGrupoId(1L)).thenReturn(List.of(gasto));
        when(pagoRepository.findByGrupoId(1L)).thenReturn(List.of(pago));

        ResponseEntity<byte[]> response = exportController.exportarCSV(1L);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());

        String csv = new String(response.getBody());
        assertTrue(csv.contains("GASTOS"));
        assertTrue(csv.contains("Cena"));
        assertTrue(csv.contains("Adrien"));
        assertTrue(csv.contains("PAGOS"));
        assertTrue(csv.contains("Prueba"));
        assertTrue(csv.contains("25"));
    }

    @Test
    void testExportarCSV_sinGastosNiPagos() throws Exception {
        when(gastoRepository.findByGrupoId(1L)).thenReturn(Collections.emptyList());
        when(pagoRepository.findByGrupoId(1L)).thenReturn(Collections.emptyList());

        ResponseEntity<byte[]> response = exportController.exportarCSV(1L);

        assertEquals(200, response.getStatusCode().value());
        String csv = new String(response.getBody());
        assertTrue(csv.contains("GASTOS"));
        assertTrue(csv.contains("PAGOS"));
    }

    @Test
    void testExportarCSV_headerDescargaCorrecto() throws Exception {
        when(gastoRepository.findByGrupoId(1L)).thenReturn(List.of(gasto));
        when(pagoRepository.findByGrupoId(1L)).thenReturn(List.of(pago));

        ResponseEntity<byte[]> response = exportController.exportarCSV(1L);

        assertTrue(response.getHeaders().getContentDisposition().toString().contains("resumen_grupo_1.csv"));
        assertEquals("text/csv", response.getHeaders().getContentType().toString());
    }

    @Test
    void testExportarCSV_gastoConParticipantesEspecificos() throws Exception {
        gasto.setRepartoGeneral(false);
        gasto.setParticipantes(new ArrayList<>(List.of(usuario1, usuario2)));

        when(gastoRepository.findByGrupoId(1L)).thenReturn(List.of(gasto));
        when(pagoRepository.findByGrupoId(1L)).thenReturn(Collections.emptyList());

        ResponseEntity<byte[]> response = exportController.exportarCSV(1L);

        assertEquals(200, response.getStatusCode().value());
        String csv = new String(response.getBody());
        assertTrue(csv.contains("Adrien"));
        assertTrue(csv.contains("Prueba"));
    }

    @Test
    void testExportarPDF_verificaLlamadasARepositorios() throws Exception {
        when(gastoService.obtenerResumenGrupo(1L)).thenReturn(resumen);
        when(gastoRepository.findByGrupoId(1L)).thenReturn(List.of(gasto));
        when(pagoRepository.findByGrupoId(1L)).thenReturn(List.of(pago));

        exportController.exportarPDF(1L);

        verify(gastoService, times(1)).obtenerResumenGrupo(1L);
        verify(gastoRepository, times(1)).findByGrupoId(1L);
        verify(pagoRepository, times(1)).findByGrupoId(1L);
    }
}