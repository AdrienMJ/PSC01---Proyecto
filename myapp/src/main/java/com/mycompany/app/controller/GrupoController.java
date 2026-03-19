package com.mycompany.app.controller;

import com.mycompany.app.entity.Grupo;
import com.mycompany.app.entity.Moneda;
import com.mycompany.app.dto.GrupoRequest;
import com.mycompany.app.service.GrupoService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/grupos")
public class GrupoController {

    @Autowired
    private GrupoService grupoService;

    @PostMapping("/crear")
    public ResponseEntity<?> crear(@RequestBody GrupoRequest request) {
        try {
            // Convertimos el String de la moneda al Enum
            Moneda monedaEnum = Moneda.valueOf(request.moneda);
            
            Grupo nuevo = grupoService.crearGrupo(request.nombre, monedaEnum, request.idCreador);
            return ResponseEntity.ok(nuevo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage()); //Si el nombre ya existe o la moneda es inválida, devolvemos un error 400
        }
    }

    @GetMapping("/usuario/{userId}") //Hace que pueda ver mis grupos una vez este en el "Dashboard"
    public ResponseEntity<List<Grupo>> listarGrupos(@PathVariable Long userId) {
        return ResponseEntity.ok(grupoService.listarGruposPorUsuario(userId));
    }

    @GetMapping("/monedas") //Hace que se puedan mostrar todos los tipos de moneda que estan en el Enum
    public Moneda[] getMonedas() {
        return Moneda.values(); // Devuelve EURO, DOLAR, LIBRA... automáticamente
    }

}