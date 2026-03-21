package com.mycompany.app.controller;

import com.mycompany.app.entity.Gasto;
import com.mycompany.app.service.GastoService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/gastos")
public class GastoController {

    @Autowired
    private GastoService gastoService;

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
    public ResponseEntity<?> listarPorGrupo(@PathVariable Long grupoId) {
        try {
            List<Gasto> gastos = gastoService.listarPorGrupo(grupoId);
            return ResponseEntity.ok(gastos);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}