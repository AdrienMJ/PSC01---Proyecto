package com.mycompany.app.service;

import com.mycompany.app.entity.Usuario;
import com.mycompany.app.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    public Usuario registrar(Usuario usuario) throws Exception {
        // Validación: ¿Ya existe el email?
        if (usuarioRepository.findByEmail(usuario.getEmail()).isPresent()) {
            throw new Exception("El email ya está registrado");
        }
        
        // Aquí en el futuro cifrarás la contraseña (BCrypt)
        // Por ahora, lo guardamos tal cual para la demo
        return usuarioRepository.save(usuario);
    }
}
