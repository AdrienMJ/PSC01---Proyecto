package com.mycompany.app.controller;

import com.mycompany.app.dto.ResumenGrupoDTO;
import com.mycompany.app.entity.Gasto;
import com.mycompany.app.entity.Grupo;
import com.mycompany.app.entity.CategoriaGasto;
import com.mycompany.app.service.GastoService;
import com.mycompany.app.service.GrupoService;
import com.mycompany.app.repository.GastoRepository;

import java.util.List;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController // Ruta base para gastos
@RequestMapping("/api/gastos") 
public class GastoController {

    @Autowired
    private GastoService gastoService;

    @Autowired
    private GrupoService grupoService;

    @Autowired
    private GastoRepository gastoRepository;

    /**
     * Crear un gasto
     */
    @PostMapping("/crear")
    public ResponseEntity<?> crear(@RequestBody Gasto gasto) {
        try {
            Gasto nuevo = gastoService.crear(gasto);
            return ResponseEntity.ok(nuevo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Listar gastos de un grupo
     */
    @GetMapping("/grupo/{grupoId}")
    public ResponseEntity<?> listarPorGrupo(
            @PathVariable("grupoId") Long grupoId,
            @RequestParam(value = "ordenar", required = false, defaultValue = "fecha") String ordenar,
            @RequestParam(value = "direccion", required = false, defaultValue = "desc") String direccion,
            @RequestParam(value = "categoria", required = false, defaultValue = "TODAS") String categoria) {
        try {
            List<Gasto> gastos = gastoService.listarPorGrupo(grupoId, ordenar, direccion, categoria);
            return ResponseEntity.ok(gastos);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarMonto(@PathVariable Long id, @RequestBody Gasto gasto) {
        try {
            Gasto existente = gastoService.obtenerPorId(id);
            Double nuevoMonto = gasto.getMonto();
            if (nuevoMonto == null || nuevoMonto <= 0) {
                return ResponseEntity.badRequest().body("El monto debe ser mayor que 0");
            }
            existente.setMonto(nuevoMonto);
            Gasto actualizado = gastoRepository.save(existente);
            return ResponseEntity.ok(actualizado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/pagado/{usuarioId}")
    public ResponseEntity<?> marcarComoPagado(@PathVariable Long id, @PathVariable Long usuarioId) {
        try {
            Gasto actualizado = gastoService.marcarComoPagado(id, usuarioId);
            return ResponseEntity.ok(actualizado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/{grupoId}/usuarios")
    public ResponseEntity<?> obtenerUsuarios(@PathVariable("grupoId") Long grupoId) {
    try {
        Grupo grupo = grupoService.obtenerGrupoPorId(grupoId);
        return ResponseEntity.ok(grupo.getMiembros());
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}

    @GetMapping("/categorias")
    public ResponseEntity<?> listarCategorias() {
        return ResponseEntity.ok(Arrays.stream(CategoriaGasto.values()).map(Enum::name).toList());
    }

    @GetMapping("/grupo/{grupoId}/resumen")
    public ResponseEntity<?> obtenerResumen(@PathVariable("grupoId") Long grupoId) {
        try {
            ResumenGrupoDTO resumen = gastoService.obtenerResumenGrupo(grupoId);
            return ResponseEntity.ok(resumen);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}