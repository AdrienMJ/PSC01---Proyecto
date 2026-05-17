package com.mycompany.app.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.mycompany.app.dto.ContactoDTO;
import com.mycompany.app.entity.Usuario;
import com.mycompany.app.repository.UsuarioRepository;

public class ContactoServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private ContactoService contactoService;

    private Usuario usuario;
    private Usuario contacto;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        usuario = new Usuario("Ana", "ana@mail.com", "123");
        usuario.setId(1L);
        contacto = new Usuario("Luis", "luis@mail.com", "456");
        contacto.setId(2L);
    }

    // --- SECCIÓN: Obtener contactos ---

    @Test
    void testObtenerContactosExito() throws Exception {
        usuario.getContactos().add(contacto);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        List<ContactoDTO> resultado = contactoService.obtenerContactos(1L);

        assertEquals(1, resultado.size());
        assertEquals(2L, resultado.get(0).getId());
        assertEquals("Luis", resultado.get(0).getUsername());
        assertEquals("luis@mail.com", resultado.get(0).getEmail());
    }

    @Test
    void testObtenerContactosUsuarioNoEncontrado() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class, () -> contactoService.obtenerContactos(99L));
        assertEquals("Usuario no encontrado", ex.getMessage());
    }

    @Test
    void testObtenerContactosListaVacia() throws Exception {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        List<ContactoDTO> resultado = contactoService.obtenerContactos(1L);

        assertTrue(resultado.isEmpty());
    }

    // --- SECCIÓN: Agregar contacto ---

    @Test
    void testAgregarContactoExito() throws Exception {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.findByEmail("luis@mail.com")).thenReturn(Optional.of(contacto));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        contactoService.agregarContacto(1L, "luis@mail.com");

        assertTrue(usuario.getContactos().contains(contacto));
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void testAgregarContactoUsuarioNoEncontrado() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class, () ->
            contactoService.agregarContacto(99L, "luis@mail.com")
        );
        assertEquals("Usuario no encontrado", ex.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void testAgregarContactoEmailNoExiste() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.findByEmail("fantasma@mail.com")).thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class, () ->
            contactoService.agregarContacto(1L, "fantasma@mail.com")
        );
        assertEquals("No existe ningún usuario con ese email", ex.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void testAgregarContactoUnoMismoError() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.findByEmail("ana@mail.com")).thenReturn(Optional.of(usuario));

        Exception ex = assertThrows(Exception.class, () ->
            contactoService.agregarContacto(1L, "ana@mail.com")
        );
        assertEquals("No puedes añadirte a ti mismo como contacto", ex.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void testAgregarContactoYaExiste() {
        usuario.getContactos().add(contacto);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.findByEmail("luis@mail.com")).thenReturn(Optional.of(contacto));

        Exception ex = assertThrows(Exception.class, () ->
            contactoService.agregarContacto(1L, "luis@mail.com")
        );
        assertEquals("Este usuario ya está en tus contactos", ex.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    // --- SECCIÓN: Eliminar contacto ---

    @Test
    void testEliminarContactoExito() throws Exception {
        usuario.getContactos().add(contacto);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        contactoService.eliminarContacto(1L, 2L);

        assertTrue(usuario.getContactos().isEmpty());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void testEliminarContactoUsuarioNoEncontrado() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class, () ->
            contactoService.eliminarContacto(99L, 2L)
        );
        assertEquals("Usuario no encontrado", ex.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void testEliminarContactoQueNoExisteNoFalla() throws Exception {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        assertDoesNotThrow(() -> contactoService.eliminarContacto(1L, 999L));
        verify(usuarioRepository).save(usuario);
    }
}
