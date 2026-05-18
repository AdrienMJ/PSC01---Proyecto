package com.mycompany.app.service;

import com.mycompany.app.entity.Comentario;
import com.mycompany.app.entity.Gasto;
import com.mycompany.app.entity.Grupo;
import com.mycompany.app.entity.Usuario;
import com.mycompany.app.repository.ComentarioRepository;
import com.mycompany.app.repository.GastoRepository;
import com.mycompany.app.repository.GrupoRepository;
import com.mycompany.app.repository.UsuarioRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ComentarioService {

    @Autowired
    private ComentarioRepository comentarioRepository;

    @Autowired
    private GastoRepository gastoRepository;

    @Autowired
    private GrupoRepository grupoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Añadir un comentario a un gasto.
     * Solo miembros del grupo pueden comentar.
     */
    public Comentario agregarComentario(Long gastoId, Long usuarioId, String texto) throws Exception {
        if (texto == null || texto.trim().isEmpty()) {
            throw new Exception("El comentario no puede estar vacío");
        }

        if (texto.length() > 500) {
            throw new Exception("El comentario no puede superar los 500 caracteres");
        }

        Gasto gasto = gastoRepository.findById(gastoId)
                .orElseThrow(() -> new Exception("Gasto no encontrado"));

        Usuario autor = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new Exception("Usuario no encontrado"));

        // Verificar que el usuario pertenece al grupo del gasto
        Long grupoId = gasto.getGrupo().getId();
        Grupo grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new Exception("Grupo no encontrado"));

        boolean esMiembro = grupo.getMiembros().stream()
                .anyMatch(m -> m.getId().equals(usuarioId));

        if (!esMiembro) {
            throw new Exception("Solo los miembros del grupo pueden comentar");
        }

        Comentario comentario = new Comentario(texto.trim(), autor, gasto);
        return comentarioRepository.save(comentario);
    }

    /**
     * Obtener todos los comentarios de un gasto ordenados por fecha.
     */
    public List<Comentario> obtenerComentarios(Long gastoId) throws Exception {
        if (!gastoRepository.existsById(gastoId)) {
            throw new Exception("Gasto no encontrado");
        }
        return comentarioRepository.findByGastoIdOrderByFechaAsc(gastoId);
    }

    /**
     * Eliminar un comentario. Solo el autor puede eliminarlo.
     */
    public void eliminarComentario(Long comentarioId, Long usuarioId) throws Exception {
        Comentario comentario = comentarioRepository.findById(comentarioId)
                .orElseThrow(() -> new Exception("Comentario no encontrado"));

        if (!comentario.getAutor().getId().equals(usuarioId)) {
            throw new Exception("Solo el autor puede eliminar su comentario");
        }

        comentarioRepository.delete(comentario);
    }
}