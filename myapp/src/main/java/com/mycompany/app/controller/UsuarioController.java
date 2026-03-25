package com.mycompany.app.controller;

import com.mycompany.app.entity.Usuario;
import com.mycompany.app.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController // Ruta base para usuarios
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<?> listarUsuarios() {
        try {
            return ResponseEntity.ok(usuarioService.listarTodos());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registrarUsuario(@RequestBody Usuario usuario) {
        try {
            Usuario nuevoUsuario = usuarioService.registrar(usuario);
            return ResponseEntity.ok(nuevoUsuario);
        } catch (Exception e) {
            // Si el email ya existe, devolvemos un error 400 (Bad Request)
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Usuario loginRequest) {
        try {
            // Usamos el servicio de login
            Usuario usuarioLogueado = usuarioService.login(loginRequest.getEmail(), loginRequest.getPassword());
            
            // Devolvemos el usuario
            return ResponseEntity.ok(usuarioLogueado);
        } catch (Exception e) {
            // 401 Unauthorized (No autorizado)
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }
}
