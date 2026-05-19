package com.mycompany.app.controller;

import com.mycompany.app.dto.NotificacionPagoDTO;
import com.mycompany.app.entity.Pago;
import com.mycompany.app.service.PagoService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    public ResponseEntity<?> historialPorGrupo(@PathVariable("grupoId") Long grupoId) {
        try {
            List<Pago> pagos = pagoService.obtenerHistorialPorGrupo(grupoId);
            return ResponseEntity.ok(pagos);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<?> pagosPorUsuario(@PathVariable("usuarioId") Long usuarioId) {
        try {
            List<Pago> pagos = pagoService.obtenerPagosPorUsuario(usuarioId);
            return ResponseEntity.ok(pagos);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /** Returns pending (unconfirmed) payments that the given user must confirm or reject. */
    @GetMapping("/receptor/{userId}/pendientes")
    public ResponseEntity<?> pendientesConfirmacion(@PathVariable("userId") Long userId) {
        try {
            List<NotificacionPagoDTO> pendientes = pagoService.obtenerPendientesConfirmacion(userId);
            return ResponseEntity.ok(pendientes);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /** Confirms receipt of a payment. Only the receptor can call this. */
    @PostMapping("/{id}/confirmar")
    public ResponseEntity<?> confirmarPago(
            @PathVariable("id") Long id,
            @RequestParam("receptorId") Long receptorId) {
        try {
            Pago pago = pagoService.confirmarPago(id, receptorId);
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("id", pago.getId());
            result.put("confirmado", pago.isConfirmado());
            return ResponseEntity.ok(result);
        } catch (Throwable t) {
            System.err.println("[ERROR confirmarPago] " + t.getClass().getName() + ": " + t.getMessage());
            t.printStackTrace();
            return ResponseEntity.badRequest().body("Error: " + t.getMessage());
        }
    }

    /** Rejects (cancels) a pending payment. Only the receptor can call this. */
    @PostMapping("/{id}/rechazar")
    public ResponseEntity<?> rechazarPago(
            @PathVariable("id") Long id,
            @RequestParam("receptorId") Long receptorId) {
        try {
            pagoService.rechazarPago(id, receptorId);
            return ResponseEntity.ok("Pago rechazado correctamente");
        } catch (Throwable t) {
            System.err.println("[ERROR rechazarPago] " + t.getClass().getName() + ": " + t.getMessage());
            t.printStackTrace();
            return ResponseEntity.badRequest().body("Error: " + t.getMessage());
        }
    }
}