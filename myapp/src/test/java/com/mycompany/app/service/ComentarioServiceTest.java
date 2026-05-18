package com.mycompany.app.service;

import com.mycompany.app.entity.Comentario;
import com.mycompany.app.entity.Gasto;
import com.mycompany.app.entity.Grupo;
import com.mycompany.app.entity.Moneda;
import com.mycompany.app.entity.Usuario;
import com.mycompany.app.repository.ComentarioRepository;
import com.mycompany.app.repository.GastoRepository;
import com.mycompany.app.repository.GrupoRepository;
import com.mycompany.app.repository.UsuarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ComentarioServiceTest {

    @Mock
    private ComentarioRepository comentarioRepository;

    @Mock
    private GastoRepository gastoRepository;

    @Mock
    private GrupoRepository grupoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private ComentarioService comentarioService;

    private Usuario usuario;
    private Usuario otroUsuario;
    private Grupo grupo;
    private Gasto gasto;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setUsername("Adrien");
        usuario.setEmail("adrien@test.com");
        // Usamos reflection para setear el id ya que no hay setter
        try {
            var field = Usuario.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(usuario, 1L);
        } catch (Exception ignored) {}

        otroUsuario = new Usuario();
        otroUsuario.setUsername("Prueba");
        otroUsuario.setEmail("prueba@test.com");
        try {
            var field = Usuario.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(otroUsuario, 2L);
        } catch (Exception ignored) {}

        grupo = new Grupo("Viaje", Moneda.EURO);
        try {
            var field = Grupo.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(grupo, 1L);
        } catch (Exception ignored) {}
        grupo.addMiembro(usuario);
        grupo.addMiembro(otroUsuario);

        gasto = new Gasto("Cena", 50.0, usuario, grupo);
        try {
            var field = Gasto.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(gasto, 1L);
        } catch (Exception ignored) {}
    }

    @Test
    void testAgregarComentarioExitoso() throws Exception {
        when(gastoRepository.findById(1L)).thenReturn(Optional.of(gasto));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(grupoRepository.findById(1L)).thenReturn(Optional.of(grupo));
        when(comentarioRepository.save(any(Comentario.class))).thenAnswer(inv -> inv.getArgument(0));

        Comentario resultado = comentarioService.agregarComentario(1L, 1L, "Buena cena!");

        assertNotNull(resultado);
        assertEquals("Buena cena!", resultado.getTexto());
        assertEquals("Adrien", resultado.getAutor().getUsername());
        verify(comentarioRepository, times(1)).save(any(Comentario.class));
    }

    @Test
    void testAgregarComentarioVacioLanzaExcepcion() {
        Exception ex = assertThrows(Exception.class, () -> {
            comentarioService.agregarComentario(1L, 1L, "");
        });
        assertEquals("El comentario no puede estar vacío", ex.getMessage());
    }

    @Test
    void testAgregarComentarioNuloLanzaExcepcion() {
        Exception ex = assertThrows(Exception.class, () -> {
            comentarioService.agregarComentario(1L, 1L, null);
        });
        assertEquals("El comentario no puede estar vacío", ex.getMessage());
    }

    @Test
    void testAgregarComentarioDemasiadoLargoLanzaExcepcion() {
        String textoLargo = "a".repeat(501);

        Exception ex = assertThrows(Exception.class, () -> {
            comentarioService.agregarComentario(1L, 1L, textoLargo);
        });
        assertEquals("El comentario no puede superar los 500 caracteres", ex.getMessage());
    }

    @Test
    void testAgregarComentarioGastoNoExisteLanzaExcepcion() {
        when(gastoRepository.findById(99L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class, () -> {
            comentarioService.agregarComentario(99L, 1L, "Hola");
        });
        assertEquals("Gasto no encontrado", ex.getMessage());
    }

    @Test
    void testAgregarComentarioUsuarioNoExisteLanzaExcepcion() {
        when(gastoRepository.findById(1L)).thenReturn(Optional.of(gasto));
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class, () -> {
            comentarioService.agregarComentario(1L, 99L, "Hola");
        });
        assertEquals("Usuario no encontrado", ex.getMessage());
    }

    @Test
    void testAgregarComentarioUsuarioNoEsMiembroLanzaExcepcion() {
        Usuario externo = new Usuario();
        externo.setUsername("Externo");
        externo.setEmail("externo@test.com");
        try {
            var field = Usuario.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(externo, 99L);
        } catch (Exception ignored) {}

        when(gastoRepository.findById(1L)).thenReturn(Optional.of(gasto));
        when(usuarioRepository.findById(99L)).thenReturn(Optional.of(externo));
        when(grupoRepository.findById(1L)).thenReturn(Optional.of(grupo));

        Exception ex = assertThrows(Exception.class, () -> {
            comentarioService.agregarComentario(1L, 99L, "No debería poder");
        });
        assertEquals("Solo los miembros del grupo pueden comentar", ex.getMessage());
    }

    @Test
    void testObtenerComentariosExitoso() throws Exception {
        Comentario c1 = new Comentario("Primer comentario", usuario, gasto);
        Comentario c2 = new Comentario("Segundo comentario", otroUsuario, gasto);

        when(gastoRepository.existsById(1L)).thenReturn(true);
        when(comentarioRepository.findByGastoIdOrderByFechaAsc(1L)).thenReturn(List.of(c1, c2));

        List<Comentario> resultado = comentarioService.obtenerComentarios(1L);

        assertEquals(2, resultado.size());
        assertEquals("Primer comentario", resultado.get(0).getTexto());
        assertEquals("Segundo comentario", resultado.get(1).getTexto());
    }

    @Test
    void testObtenerComentariosGastoNoExisteLanzaExcepcion() {
        when(gastoRepository.existsById(99L)).thenReturn(false);

        Exception ex = assertThrows(Exception.class, () -> {
            comentarioService.obtenerComentarios(99L);
        });
        assertEquals("Gasto no encontrado", ex.getMessage());
    }

    @Test
    void testEliminarComentarioExitoso() throws Exception {
        Comentario comentario = new Comentario("Mi comentario", usuario, gasto);
        try {
            var field = Comentario.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(comentario, 1L);
        } catch (Exception ignored) {}

        when(comentarioRepository.findById(1L)).thenReturn(Optional.of(comentario));

        comentarioService.eliminarComentario(1L, 1L);

        verify(comentarioRepository, times(1)).delete(comentario);
    }

    @Test
    void testEliminarComentarioOtroUsuarioLanzaExcepcion() {
        Comentario comentario = new Comentario("Mi comentario", usuario, gasto);
        try {
            var field = Comentario.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(comentario, 1L);
        } catch (Exception ignored) {}

        when(comentarioRepository.findById(1L)).thenReturn(Optional.of(comentario));

        Exception ex = assertThrows(Exception.class, () -> {
            comentarioService.eliminarComentario(1L, 2L);
        });
        assertEquals("Solo el autor puede eliminar su comentario", ex.getMessage());
    }

    @Test
    void testEliminarComentarioNoExisteLanzaExcepcion() {
        when(comentarioRepository.findById(99L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class, () -> {
            comentarioService.eliminarComentario(99L, 1L);
        });
        assertEquals("Comentario no encontrado", ex.getMessage());
    }
}