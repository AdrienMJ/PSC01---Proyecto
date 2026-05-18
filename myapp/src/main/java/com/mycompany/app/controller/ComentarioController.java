package com.mycompany.app.controller;

import com.mycompany.app.entity.Comentario;
import com.mycompany.app.service.ComentarioService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comentarios")
public class ComentarioController {

    @Autowired
    private ComentarioService comentarioService;

    /**
     * Añadir comentario a un gasto
     * POST /api/comentarios/gasto/{gastoId}
     */
    @PostMapping("/gasto/{gastoId}")
    public ResponseEntity<?> agregar(
            @PathVariable("gastoId") Long gastoId,
            @RequestBody Map<String, Object> body) {
        try {
            Long usuarioId = Long.valueOf(body.get("usuarioId").toString());
            String texto = body.get("texto").toString();
            Comentario nuevo = comentarioService.agregarComentario(gastoId, usuarioId, texto);
            return ResponseEntity.ok(nuevo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Obtener comentarios de un gasto
     * GET /api/comentarios/gasto/{gastoId}
     */
    @GetMapping("/gasto/{gastoId}")
    public ResponseEntity<?> listar(@PathVariable("gastoId") Long gastoId) {
        try {
            List<Comentario> comentarios = comentarioService.obtenerComentarios(gastoId);
            return ResponseEntity.ok(comentarios);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Eliminar un comentario
     * DELETE /api/comentarios/{comentarioId}?usuarioId=X
     */
    @DeleteMapping("/{comentarioId}")
    public ResponseEntity<?> eliminar(
            @PathVariable("comentarioId") Long comentarioId,
            @RequestParam("usuarioId") Long usuarioId) {
        try {
            comentarioService.eliminarComentario(comentarioId, usuarioId);
            return ResponseEntity.ok("{\"mensaje\": \"Comentario eliminado\"}");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}