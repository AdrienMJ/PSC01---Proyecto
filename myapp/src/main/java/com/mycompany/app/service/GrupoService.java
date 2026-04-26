package com.mycompany.app.service;

import com.mycompany.app.dto.ResumenGrupoDTO;
import com.mycompany.app.entity.Grupo;
import com.mycompany.app.entity.Moneda;
import com.mycompany.app.entity.Usuario;
import com.mycompany.app.repository.GrupoRepository;
import com.mycompany.app.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GrupoService {

    @Autowired
    private GrupoRepository grupoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    @Lazy
    private GastoService gastoService;

    /**
     * Crea un nuevo grupo y asigna al creador como el primer miembro.
     */
    @Transactional // Asegura que si algo falla, no se guarde nada a medias
    public Grupo crearGrupo(String nombre, Moneda moneda, Long idCreador) {
        // 1. Validar que el usuario creador existe
        Usuario creador = usuarioRepository.findById(idCreador)
                .orElseThrow(() -> new RuntimeException("Usuario creador no encontrado con ID: " + idCreador));

        // 2. Instanciar el nuevo grupo
        Grupo nuevoGrupo = new Grupo(nombre, moneda);
        nuevoGrupo.setIdCreador(idCreador);

        // 3. Vincular al creador usando el método de conveniencia
        // Esto llena la tabla intermedia 'grupo_usuarios' automáticamente
        nuevoGrupo.addMiembro(creador);

        // 4. Persistir en la base de datos
        return grupoRepository.save(nuevoGrupo);
    }

    /**
     * Recupera todos los grupos donde un usuario es miembro.
     * Útil para mostrar la lista en el Dashboard.
     */
    public List<Grupo> listarGruposPorUsuario(Long idUsuario) {
        if (!usuarioRepository.existsById(idUsuario)) {
            throw new RuntimeException("Usuario no encontrado");
        }

        return grupoRepository.findByMiembros_Id(idUsuario);
    }

    @Transactional
    public Grupo renombrarGrupo(Long idGrupo, Long idUsuario, String nuevoNombre) {
        if (nuevoNombre == null || nuevoNombre.trim().isEmpty()) {
            throw new RuntimeException("El nombre del grupo no puede estar vacío");
        }

        if (idUsuario == null || !usuarioRepository.existsById(idUsuario)) {
            throw new RuntimeException("Usuario no encontrado");
        }

        if (!grupoRepository.existsByIdAndMiembros_Id(idGrupo, idUsuario)) {
            throw new RuntimeException("No tienes permiso para renombrar este grupo");
        }

        Grupo grupo = grupoRepository.findById(idGrupo)
                .orElseThrow(() -> new RuntimeException("Grupo no encontrado con ID: " + idGrupo));

        grupo.setNombre(nuevoNombre.trim());
        return grupoRepository.save(grupo);
    }
    public Grupo obtenerGrupoPorId(Long id) {
        return grupoRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));
}

    /**
     * Invita a un usuario al grupo buscándolo por email.
     * Solo un miembro existente del grupo puede invitar.
     */
    @Transactional
    public Grupo invitarUsuario(Long idGrupo, String emailInvitado, Long idUsuarioInvitador) {
        // 1. Validar que el invitador existe
        if (idUsuarioInvitador == null || !usuarioRepository.existsById(idUsuarioInvitador)) {
            throw new RuntimeException("Usuario invitador no encontrado");
        }

        // 2. Validar que el invitador pertenece al grupo
        if (!grupoRepository.existsByIdAndMiembros_Id(idGrupo, idUsuarioInvitador)) {
            throw new RuntimeException("No tienes permiso para invitar a este grupo");
        }

        // 3. Buscar al usuario invitado por email
        Usuario invitado = usuarioRepository.findByEmail(emailInvitado)
                .orElseThrow(() -> new RuntimeException("No existe ningún usuario con el email: " + emailInvitado));

        // 4. Obtener el grupo
        Grupo grupo = grupoRepository.findById(idGrupo)
                .orElseThrow(() -> new RuntimeException("Grupo no encontrado con ID: " + idGrupo));

        // 5. Comprobar que no sea ya miembro
        boolean yaEsMiembro = grupo.getMiembros().stream()
                .anyMatch(m -> m.getId().equals(invitado.getId()));
        if (yaEsMiembro) {
            throw new RuntimeException("El usuario ya es miembro de este grupo");
        }

        // 6. Añadir al grupo
        grupo.addMiembro(invitado);

        return grupoRepository.save(grupo);
    }

    @Transactional
    public void expulsarMiembro(Long grupoId, Long idAdmin, Long idMiembro) throws Exception {
        Grupo grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));

        if (grupo.getIdCreador() == null || !grupo.getIdCreador().equals(idAdmin)) {
            throw new RuntimeException("Solo el creador del grupo puede expulsar miembros");
        }

        if (idAdmin.equals(idMiembro)) {
            throw new RuntimeException("El creador no puede expulsarse a sí mismo");
        }

        boolean esMiembro = grupo.getMiembros().stream()
                .anyMatch(m -> m.getId().equals(idMiembro));
        if (!esMiembro) {
            throw new RuntimeException("El usuario no es miembro de este grupo");
        }

        // Comprobar que el usuario no tiene deudas pendientes en el grupo
        ResumenGrupoDTO resumen = gastoService.obtenerResumenGrupo(grupoId);
        resumen.getBalances().stream()
                .filter(b -> b.getUsuarioId().equals(idMiembro))
                .findFirst()
                .ifPresent(b -> {
                    if (Math.abs(b.getBalance()) > 0.009) {
                        throw new RuntimeException(
                            "No se puede expulsar a este usuario hasta que salde sus deudas");
                    }
                });

        grupo.getMiembros().removeIf(m -> m.getId().equals(idMiembro));
        grupoRepository.save(grupo);
    }
}