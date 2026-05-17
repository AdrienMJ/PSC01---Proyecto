package com.mycompany.app.service;

import com.mycompany.app.dto.ContactoDTO;
import com.mycompany.app.entity.Usuario;
import com.mycompany.app.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ContactoService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public List<ContactoDTO> obtenerContactos(Long usuarioId) throws Exception {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new Exception("Usuario no encontrado"));
        return usuario.getContactos().stream()
                .map(c -> new ContactoDTO(c.getId(), c.getUsername(), c.getEmail()))
                .toList();
    }

    @Transactional
    public void agregarContacto(Long usuarioId, String emailContacto) throws Exception {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new Exception("Usuario no encontrado"));
        Usuario contacto = usuarioRepository.findByEmail(emailContacto)
                .orElseThrow(() -> new Exception("No existe ningún usuario con ese email"));
        if (contacto.getId().equals(usuarioId)) {
            throw new Exception("No puedes añadirte a ti mismo como contacto");
        }
        boolean yaExiste = usuario.getContactos().stream()
                .anyMatch(c -> c.getId().equals(contacto.getId()));
        if (yaExiste) {
            throw new Exception("Este usuario ya está en tus contactos");
        }
        usuario.getContactos().add(contacto);
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void eliminarContacto(Long usuarioId, Long contactoId) throws Exception {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new Exception("Usuario no encontrado"));
        usuario.getContactos().removeIf(c -> c.getId().equals(contactoId));
        usuarioRepository.save(usuario);
    }
}
