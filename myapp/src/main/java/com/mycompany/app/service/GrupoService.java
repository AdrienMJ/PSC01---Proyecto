package com.mycompany.app.service;

import com.mycompany.app.entity.Grupo;
import com.mycompany.app.entity.Moneda;
import com.mycompany.app.entity.Usuario;
import com.mycompany.app.repository.GrupoRepository;
import com.mycompany.app.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GrupoService {

    @Autowired
    private GrupoRepository grupoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

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
}