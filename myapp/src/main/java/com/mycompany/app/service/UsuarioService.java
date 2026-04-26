package com.mycompany.app.service;

import com.mycompany.app.entity.Usuario;
import com.mycompany.app.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public Usuario registrar(Usuario usuario) throws Exception {
        // Validación: ¿Ya existe el email?
        if (usuarioRepository.findByEmail(usuario.getEmail()).isPresent()) {
            throw new Exception("El email ya está registrado");
        }    
        //igual habria que poner un encriptador de contraseñas o algo (opcional)
        return usuarioRepository.save(usuario);
    }
    public Usuario login(String email, String password) throws Exception {
    //Buscar usuario por email
    java.util.Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
    
    //si no existe el email lanzamos execepción generica
    if (usuarioOpt.isEmpty()) {
        throw new Exception("Email o contraseña incorrectos");
    }
    //si el email existe comprobamos contraseña
    Usuario usuario = usuarioOpt.get();
    if (!usuario.getPassword().equals(password)) {
        throw new Exception("Email o contraseña incorrectos");
    }
    //si todo esta bien devolvemos el usuario
    return usuario;
}

    public java.util.List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    @Transactional
    public void eliminarCuentaYDatos(Long idUsuario) throws Exception {
        if (idUsuario == null || !usuarioRepository.existsById(idUsuario)) {
            throw new Exception("Usuario no encontrado");
        }

        List<Long> gruposUsuario = jdbcTemplate.queryForList(
                "SELECT grupo_id FROM grupo_usuarios WHERE usuario_id = ?",
                Long.class,
                idUsuario
        );

        // Pagos donde participa como pagador o receptor.
        jdbcTemplate.update(
                "DELETE FROM pagos WHERE pagador_id = ? OR receptor_id = ?",
                idUsuario,
                idUsuario
        );

        // Gastos creados por el usuario (y sus participaciones).
        List<Long> gastosUsuario = jdbcTemplate.queryForList(
                "SELECT id FROM gastos WHERE usuario_id = ?",
                Long.class,
                idUsuario
        );
        borrarParticipantesPorGastos(gastosUsuario);
        jdbcTemplate.update("DELETE FROM gastos WHERE usuario_id = ?", idUsuario);

        // Participaciones del usuario en gastos de terceros.
        jdbcTemplate.update("DELETE FROM gasto_participantes WHERE usuario_id = ?", idUsuario);

        // Quitar usuario de todos los grupos.
        jdbcTemplate.update("DELETE FROM grupo_usuarios WHERE usuario_id = ?", idUsuario);

        // Eliminar grupos que hayan quedado vacíos tras salir este usuario.
        for (Long grupoId : gruposUsuario) {
            Integer miembros = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM grupo_usuarios WHERE grupo_id = ?",
                    Integer.class,
                    grupoId
            );

            if (miembros != null && miembros == 0) {
                List<Long> gastosGrupo = jdbcTemplate.queryForList(
                        "SELECT id FROM gastos WHERE grupo_id = ?",
                        Long.class,
                        grupoId
                );

                borrarParticipantesPorGastos(gastosGrupo);
                jdbcTemplate.update("DELETE FROM pagos WHERE grupo_id = ?", grupoId);
                jdbcTemplate.update("DELETE FROM gastos WHERE grupo_id = ?", grupoId);
                jdbcTemplate.update("DELETE FROM grupos WHERE id = ?", grupoId);
            }
        }

        usuarioRepository.deleteById(idUsuario);
    }

    private void borrarParticipantesPorGastos(List<Long> idsGasto) {
        if (idsGasto == null || idsGasto.isEmpty()) {
            return;
        }

        String placeholders = String.join(",", Collections.nCopies(idsGasto.size(), "?"));
        jdbcTemplate.update(
                "DELETE FROM gasto_participantes WHERE gasto_id IN (" + placeholders + ")",
                idsGasto.toArray()
        );
    }
}
