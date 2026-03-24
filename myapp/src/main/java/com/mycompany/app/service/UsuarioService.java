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
}
