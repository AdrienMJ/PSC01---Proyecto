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

import com.mycompany.app.dto.BalancePersonaDTO;
import com.mycompany.app.dto.ResumenGrupoDTO;
import com.mycompany.app.dto.TransferenciaDTO;
import com.mycompany.app.entity.Grupo;
import com.mycompany.app.entity.Moneda;
import com.mycompany.app.entity.Usuario;
import com.mycompany.app.repository.GrupoRepository;
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

    @Mock
    private GrupoRepository grupoRepository;

    @Mock
    private GastoService gastoService;

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

    // --- SECCIÓN: Moneda Predeterminada ---

    @Test
    public void testActualizarMonedaPredeterminadaExito() throws Exception {
        Usuario usuario = new Usuario("Ana", "ana@mail.com", "123");
        usuario.setId(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        Usuario resultado = usuarioService.actualizarMonedaPredeterminada(1L, Moneda.DOLAR);

        assertNotNull(resultado);
        assertEquals(Moneda.DOLAR, resultado.getMonedaPredeterminada());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    public void testActualizarMonedaPredeterminadaUsuarioNoEncontrado() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class, () ->
            usuarioService.actualizarMonedaPredeterminada(99L, Moneda.EURO)
        );
        assertEquals("Usuario no encontrado", ex.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    public void testMonedaPredeterminadaPorDefectoEsEuro() {
        Usuario usuario = new Usuario("Luis", "luis@mail.com", "abc");
        assertEquals(Moneda.EURO, usuario.getMonedaPredeterminada());
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

    @Test
    public void testObtenerNotificacionesDeudasFiltraSoloLasDelUsuario() throws Exception {
        Long idUsuario = 1L;
        Usuario u1 = new Usuario("Luis", "luis@mail.com", "123");
        u1.setId(idUsuario);
        Usuario u2 = new Usuario("Ana", "ana@mail.com", "123");
        u2.setId(2L);

        Grupo grupo = new Grupo("Viaje", Moneda.EURO);
        grupo.setId(10L);
        grupo.getMiembros().add(u1);
        grupo.getMiembros().add(u2);

        ResumenGrupoDTO resumen = new ResumenGrupoDTO(
                100.0,
                Arrays.asList(
                        new BalancePersonaDTO(1L, "Luis", -30.0, "debe"),
                        new BalancePersonaDTO(2L, "Ana", 30.0, "positivo")
                ),
                Arrays.asList(
                        new TransferenciaDTO(1L, "Luis", 2L, "Ana", 30.0),
                        new TransferenciaDTO(3L, "Marta", 2L, "Ana", 10.0)
                )
        );

        when(usuarioRepository.existsById(idUsuario)).thenReturn(true);
        when(grupoRepository.findByMiembros_Id(idUsuario)).thenReturn(Arrays.asList(grupo));
        when(gastoService.obtenerResumenGrupo(10L)).thenReturn(resumen);

        var notificaciones = usuarioService.obtenerNotificacionesDeudas(idUsuario);

        assertEquals(1, notificaciones.size());
        assertEquals("Viaje", notificaciones.get(0).getGrupoNombre());
        assertEquals("Ana", notificaciones.get(0).getAcreedorUsername());
        assertEquals(30.0, notificaciones.get(0).getMonto(), 0.01);
    }

    @Test
    public void testObtenerNotificacionesDeudasUsuarioNoExiste() {
        when(usuarioRepository.existsById(99L)).thenReturn(false);

        Exception ex = assertThrows(Exception.class, () -> usuarioService.obtenerNotificacionesDeudas(99L));
        assertEquals("Usuario no encontrado", ex.getMessage());
    }
}