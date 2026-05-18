package com.mycompany.app.controller;

import com.mycompany.app.entity.Moneda;
import com.mycompany.app.entity.Usuario;
import com.mycompany.app.service.UsuarioService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@RestController // Ruta base para usuarios
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Value("${app.uploads.dir}")
    private String uploadsDir;

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

    @GetMapping("/{id}/notificaciones-deudas")
    public ResponseEntity<?> obtenerNotificacionesDeudas(@PathVariable("id") Long id) {
        try {
            return ResponseEntity.ok(usuarioService.obtenerNotificacionesDeudas(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/perfil")
    public ResponseEntity<?> obtenerPerfil(@PathVariable("id") Long id) {
        try {
            return ResponseEntity.ok(usuarioService.obtenerPorId(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PutMapping(value = "/{id}/perfil", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> actualizarPerfil(
            @PathVariable("id") Long id,
            @RequestPart(value = "nombreVisible", required = false) String nombreVisible,
            @RequestPart(value = "foto", required = false) MultipartFile foto) {
        try {
            String fotoUrl = null;

            if (foto != null && !foto.isEmpty()) {
                String extension = "";
                String originalName = foto.getOriginalFilename();
                if (originalName != null && originalName.contains(".")) {
                    extension = originalName.substring(originalName.lastIndexOf("."));
                }

                Path carpetaPerfiles = Paths.get(uploadsDir, "perfiles")
                        .toAbsolutePath()
                        .normalize();
                Files.createDirectories(carpetaPerfiles);

                String nombreArchivo = UUID.randomUUID() + extension;
                Path destino = carpetaPerfiles.resolve(nombreArchivo);
                Files.copy(foto.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
                fotoUrl = "/perfiles/" + nombreArchivo;
            }

            Usuario actualizado = usuarioService.actualizarPerfil(id, nombreVisible, fotoUrl);
            return ResponseEntity.ok(actualizado);
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

    @PutMapping("/{id}/moneda-predeterminada")
    public ResponseEntity<?> actualizarMonedaPredeterminada(
            @PathVariable("id") Long id,
            @RequestBody java.util.Map<String, String> body) {
        try {
            Moneda moneda = Moneda.valueOf(body.get("moneda"));
            Usuario usuario = usuarioService.actualizarMonedaPredeterminada(id, moneda);
            return ResponseEntity.ok(usuario);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Moneda no válida");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarCuenta(@PathVariable("id") Long id) {
        try {
            usuarioService.eliminarCuentaYDatos(id);
            return ResponseEntity.ok("Cuenta y datos eliminados correctamente");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}
