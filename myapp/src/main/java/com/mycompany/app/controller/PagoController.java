package com.mycompany.app.controller;

import com.mycompany.app.entity.Pago;
import com.mycompany.app.service.PagoService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    @Autowired
    private PagoService pagoService;

    @PostMapping("/registrar")
    public ResponseEntity<?> registrarPago(@RequestBody Pago pago) {
        try {
            Pago nuevo = pagoService.registrarPago(pago);
            return ResponseEntity.ok(nuevo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/grupo/{grupoId}")
    public ResponseEntity<?> historialPorGrupo(@PathVariable Long grupoId) {
        try {
            List<Pago> pagos = pagoService.obtenerHistorialPorGrupo(grupoId);
            return ResponseEntity.ok(pagos);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<?> pagosPorUsuario(@PathVariable Long usuarioId) {
        try {
            List<Pago> pagos = pagoService.obtenerPagosPorUsuario(usuarioId);
            return ResponseEntity.ok(pagos);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}