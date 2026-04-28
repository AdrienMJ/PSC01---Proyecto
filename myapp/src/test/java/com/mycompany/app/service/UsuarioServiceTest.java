package com.mycompany.app.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;

import com.mycompany.app.entity.Usuario;
import com.mycompany.app.repository.UsuarioRepository;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private UsuarioService usuarioService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    // --- SECCIÓN: Registro ---

    @Test
    public void testRegistrarExitoso() throws Exception {
        Usuario nuevo = new Usuario("Pepe", "pepe@mail.com", "123");
        when(usuarioRepository.findByEmail("pepe@mail.com")).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(nuevo);

        Usuario resultado = usuarioService.registrar(nuevo);

        assertNotNull(resultado);
        verify(usuarioRepository).save(nuevo);
    }

    @Test
    public void testRegistrarEmailYaExiste() {
        Usuario existente = new Usuario("Pepe", "pepe@mail.com", "123");
        when(usuarioRepository.findByEmail("pepe@mail.com")).thenReturn(Optional.of(existente));

        Exception ex = assertThrows(Exception.class, () -> usuarioService.registrar(existente));
        assertEquals("El email ya está registrado", ex.getMessage());
    }

    // --- SECCIÓN: Login ---

    @Test
    public void testLoginExitoso() throws Exception {
        Usuario usuarioFicticio = new Usuario("Adrien", "adrien@mail.com", "123");
        when(usuarioRepository.findByEmail("adrien@mail.com")).thenReturn(Optional.of(usuarioFicticio));

        Usuario resultado = usuarioService.login("adrien@mail.com", "123");

        assertNotNull(resultado);
        assertEquals("Adrien", resultado.getUsername()); 
    }

    @Test
    public void testLoginContrasenaIncorrecta() {
        Usuario usuarioFicticio = new Usuario("Adrien", "adrien@mail.com", "123");
        when(usuarioRepository.findByEmail("adrien@mail.com")).thenReturn(Optional.of(usuarioFicticio));

        Exception exception = assertThrows(Exception.class, () -> 
            usuarioService.login("adrien@mail.com", "password_falsa")
        );
        assertEquals("Email o contraseña incorrectos", exception.getMessage());
    }

    @Test
    public void testLoginUsuarioNoEncontrado() {
        when(usuarioRepository.findByEmail("fantasma@mail.com")).thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> usuarioService.login("fantasma@mail.com", "123"));
    }

    // --- SECCIÓN: Listado ---

    @Test
    public void testListarTodos() {
        when(usuarioRepository.findAll()).thenReturn(Arrays.asList(new Usuario(), new Usuario()));
        List<Usuario> lista = usuarioService.listarTodos();
        assertEquals(2, lista.size());
    }

    // --- SECCIÓN: Eliminar Cuenta (Lógica Compleja) ---

    @Test
    public void testEliminarCuentaYDatos_UsuarioNoExiste() {
        when(usuarioRepository.existsById(1L)).thenReturn(false);
        assertThrows(Exception.class, () -> usuarioService.eliminarCuentaYDatos(1L));
    }

    @Test
    public void testEliminarCuentaYDatos_GrupoQuedaVacio() throws Exception {
        Long idUsuario = 1L;
        Long grupoId = 10L;
        Long gastoUsuario = 100L;

        when(usuarioRepository.existsById(idUsuario)).thenReturn(true);
        
        // Mock de grupos donde está el usuario
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), eq(idUsuario)))
            .thenReturn(Collections.singletonList(grupoId));

        // Mock de gastos del usuario
        when(jdbcTemplate.queryForList("SELECT id FROM gastos WHERE usuario_id = ?", Long.class, idUsuario))
            .thenReturn(Collections.singletonList(gastoUsuario));

        // Simulamos que el grupo se queda con 0 miembros
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(grupoId))).thenReturn(0);

        usuarioService.eliminarCuentaYDatos(idUsuario);

        // Verificamos que se ejecutan los borrados de grupo (porque miembros == 0)
        verify(jdbcTemplate).update(contains("DELETE FROM grupos WHERE id = ?"), eq(grupoId));
        verify(usuarioRepository).deleteById(idUsuario);
    }

    @Test
    public void testEliminarCuentaYDatos_GrupoAunTieneMiembros() throws Exception {
        Long idUsuario = 1L;
        Long grupoId = 10L;

        when(usuarioRepository.existsById(idUsuario)).thenReturn(true);
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), eq(idUsuario)))
            .thenReturn(Collections.singletonList(grupoId));

        // Simulamos que el grupo aún tiene 2 miembros
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(grupoId))).thenReturn(2);

        usuarioService.eliminarCuentaYDatos(idUsuario);

        // Verificamos que NO se intenta borrar el grupo
        verify(jdbcTemplate, never()).update(contains("DELETE FROM grupos WHERE id = ?"), anyLong());
        verify(usuarioRepository).deleteById(idUsuario);
    }

    @Test
    public void testBorrarParticipantesPorGastos_ListaVacia() throws Exception {
        // Este test sirve para cubrir la rama "if (idsGasto == null || idsGasto.isEmpty())"
        // Invocamos eliminarCuenta para un usuario que no tiene gastos creados
        when(usuarioRepository.existsById(1L)).thenReturn(true);
        when(jdbcTemplate.queryForList(contains("SELECT id FROM gastos"), eq(Long.class), anyLong()))
            .thenReturn(Collections.emptyList());

        usuarioService.eliminarCuentaYDatos(1L);

        // Si la lista es vacía, no debería llamar al update del IN (?,?)
        verify(jdbcTemplate, never()).update(
        contains("DELETE FROM gasto_participantes WHERE gasto_id IN"), 
        any(Object[].class) 
        );
    }
}