package com.mycompany.app.controller;

import com.mycompany.app.dto.ContactoDTO;
import com.mycompany.app.service.ContactoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
public class ContactoController {

    @Autowired
    private ContactoService contactoService;

    @GetMapping("/{id}/contactos")
    public ResponseEntity<?> listarContactos(@PathVariable("id") Long id) {
        try {
            List<ContactoDTO> contactos = contactoService.obtenerContactos(id);
            return ResponseEntity.ok(contactos);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        } catch (Throwable e) {
            return ResponseEntity.status(500).body("Error interno: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    @PostMapping("/{id}/contactos")
    public ResponseEntity<?> agregarContacto(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> body) {
        try {
            contactoService.agregarContacto(id, body.get("email"));
            return ResponseEntity.ok("Contacto añadido correctamente");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Throwable e) {
            return ResponseEntity.status(500).body("Error interno: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}/contactos/{contactoId}")
    public ResponseEntity<?> eliminarContacto(
            @PathVariable("id") Long id,
            @PathVariable("contactoId") Long contactoId) {
        try {
            contactoService.eliminarContacto(id, contactoId);
            return ResponseEntity.ok("Contacto eliminado correctamente");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        } catch (Throwable e) {
            return ResponseEntity.status(500).body("Error interno: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
