package com.mycompany.app.controller;

import com.mycompany.app.entity.Grupo;
import com.mycompany.app.entity.Moneda;
import com.mycompany.app.dto.GrupoRequest;
import com.mycompany.app.dto.InvitarUsuarioRequest;
import com.mycompany.app.dto.RenombrarGrupoRequest;
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
    public ResponseEntity<List<Grupo>> listarGrupos(@PathVariable("userId") Long userId) {
        return ResponseEntity.ok(grupoService.listarGruposPorUsuario(userId));
    }

    @GetMapping("/monedas") //Hace que se puedan mostrar todos los tipos de moneda que estan en el Enum
    public Moneda[] getMonedas() {
        return Moneda.values(); // Devuelve EURO, DOLAR, LIBRA... automáticamente
    }

    @PutMapping("/{grupoId}/nombre")
    public ResponseEntity<?> renombrarGrupo(@PathVariable("grupoId") Long grupoId, @RequestBody RenombrarGrupoRequest request) {
        try {
            Grupo grupoActualizado = grupoService.renombrarGrupo(grupoId, request.idUsuario, request.nombre);
            return ResponseEntity.ok(grupoActualizado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/{grupoId}/usuarios")
    public ResponseEntity<?> obtenerUsuariosDelGrupo(@PathVariable("grupoId") Long grupoId) {
        try {
            Grupo grupo = grupoService.obtenerGrupoPorId(grupoId);
            return ResponseEntity.ok(grupo.getMiembros());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Invitar a un usuario al grupo por email.
     * POST /api/grupos/{grupoId}/invitar
     */
    @PostMapping("/{grupoId}/invitar")
    public ResponseEntity<?> invitarUsuario(@PathVariable("grupoId") Long grupoId, @RequestBody InvitarUsuarioRequest request) {
        try {
            Grupo grupoActualizado = grupoService.invitarUsuario(grupoId, request.email, request.idUsuarioInvitador);
            return ResponseEntity.ok(grupoActualizado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/{grupoId}/creador")
    public ResponseEntity<?> obtenerCreador(@PathVariable("grupoId") Long grupoId) {
        try {
            Grupo grupo = grupoService.obtenerGrupoPorId(grupoId);
            return ResponseEntity.ok(java.util.Map.of("idCreador", grupo.getIdCreador() != null ? grupo.getIdCreador() : -1L));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @DeleteMapping("/{grupoId}/miembros/{miembroId}")
    public ResponseEntity<?> expulsarMiembro(
            @PathVariable("grupoId") Long grupoId,
            @PathVariable("miembroId") Long miembroId,
            @RequestParam("idAdmin") Long idAdmin) {
        try {
            grupoService.expulsarMiembro(grupoId, idAdmin, miembroId);
            return ResponseEntity.ok("Miembro expulsado correctamente");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}