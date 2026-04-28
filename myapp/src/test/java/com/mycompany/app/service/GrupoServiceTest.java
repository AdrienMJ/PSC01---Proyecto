package com.mycompany.app.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.mycompany.app.dto.BalancePersonaDTO;
import com.mycompany.app.dto.ResumenGrupoDTO;
import com.mycompany.app.entity.Grupo;
import com.mycompany.app.entity.Moneda;
import com.mycompany.app.entity.Usuario;
import com.mycompany.app.repository.GrupoRepository;
import com.mycompany.app.repository.UsuarioRepository;

import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

/**
 * Historia de usuario: Como administrador, quiero poder expulsar a un miembro
 * del grupo una vez sus deudas estén saldadas.
 */
public class GrupoServiceTest {

    @Mock
    private GrupoRepository grupoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private GastoService gastoService;

    @InjectMocks
    private GrupoService grupoService;

    private Usuario admin;
    private Usuario miembro;
    private Grupo grupo;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        admin = new Usuario("Admin", "admin@mail.com", "admin123");
        admin.setId(1L);

        miembro = new Usuario("Miembro", "miembro@mail.com", "pass");
        miembro.setId(2L);

        grupo = new Grupo("Viaje a París", Moneda.EURO);
        grupo.setId(10L);
        grupo.setIdCreador(1L);
        grupo.addMiembro(admin);
        grupo.addMiembro(miembro);
    }

    // 1. Éxito: el admin expulsa a un miembro con deudas saldadas (balance = 0)
    @Test
    public void testExpulsarMiembroConDeudaSaldadaExitoso() throws Exception {
        BalancePersonaDTO balanceSaldado = new BalancePersonaDTO(2L, "Miembro", 0.0, "Saldado");
        ResumenGrupoDTO resumen = new ResumenGrupoDTO(100.0, Arrays.asList(balanceSaldado), Collections.emptyList());

        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        when(gastoService.obtenerResumenGrupo(10L)).thenReturn(resumen);

        grupoService.expulsarMiembro(10L, 1L, 2L);

        // El miembro debe haber sido eliminado de la lista
        assertFalse(grupo.getMiembros().stream().anyMatch(m -> m.getId().equals(2L)));
        verify(grupoRepository).save(grupo);
    }

    // 2. Error: el miembro aún tiene deuda pendiente (balance != 0)
    @Test
    public void testExpulsarMiembroConDeudaPendienteLanzaExcepcion() throws Exception {
        BalancePersonaDTO balanceDeudor = new BalancePersonaDTO(2L, "Miembro", -30.0, "Deudor");
        ResumenGrupoDTO resumen = new ResumenGrupoDTO(100.0, Arrays.asList(balanceDeudor), Collections.emptyList());

        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        when(gastoService.obtenerResumenGrupo(10L)).thenReturn(resumen);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                grupoService.expulsarMiembro(10L, 1L, 2L));

        assertEquals("No se puede expulsar a este usuario hasta que salde sus deudas", ex.getMessage());
        verify(grupoRepository, never()).save(any());
    }

    // 3. Error: quien intenta expulsar no es el creador/administrador del grupo
    @Test
    public void testExpulsarMiembroSiendoNoAdminLanzaExcepcion() {
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                grupoService.expulsarMiembro(10L, 2L, 1L)); // El miembro intenta expulsar al admin

        assertEquals("Solo el creador del grupo puede expulsar miembros", ex.getMessage());
        verify(grupoRepository, never()).save(any());
    }

    // 4. Error: el administrador intenta expulsarse a sí mismo
    @Test
    public void testAdminNoPuedeExpulsarseASiMismoLanzaExcepcion() {
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                grupoService.expulsarMiembro(10L, 1L, 1L));

        assertEquals("El creador no puede expulsarse a sí mismo", ex.getMessage());
        verify(grupoRepository, never()).save(any());
    }

    // 5. Error: el usuario a expulsar no pertenece al grupo
    @Test
    public void testExpulsarUsuarioQueNoEsMiembroLanzaExcepcion() {
        Usuario extraño = new Usuario("Extraño", "extra@mail.com", "pass");
        extraño.setId(99L);

        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                grupoService.expulsarMiembro(10L, 1L, 99L));

        assertEquals("El usuario no es miembro de este grupo", ex.getMessage());
        verify(grupoRepository, never()).save(any());
    }

    @Test
    public void testCrearGrupoExito() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(grupoRepository.save(any(Grupo.class))).thenAnswer(inv -> inv.getArgument(0));

        Grupo resultado = grupoService.crearGrupo("Nuevo Grupo", Moneda.DOLAR, 1L);

        assertEquals("Nuevo Grupo", resultado.getNombre());
        assertEquals(Moneda.DOLAR, resultado.getMoneda());
        assertEquals(1L, resultado.getIdCreador());
        assertTrue(resultado.getMiembros().contains(admin));
    }

    @Test
    public void testCrearGrupoUsuarioNoEncontrado() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());
        
        RuntimeException ex = assertThrows(RuntimeException.class, () -> 
            grupoService.crearGrupo("Grupo Fantasma", Moneda.EURO, 99L)
        );
        assertEquals("Usuario creador no encontrado con ID: 99", ex.getMessage());
    }
    @Test
    public void testListarGruposExito() {
        when(usuarioRepository.existsById(1L)).thenReturn(true);
        when(grupoRepository.findByMiembros_Id(1L)).thenReturn(Arrays.asList(grupo));
        
        List<Grupo> lista = grupoService.listarGruposPorUsuario(1L);
        
        assertEquals(1, lista.size());
        assertEquals("Viaje a París", lista.get(0).getNombre());
    }

    @Test
    public void testListarGruposUsuarioNoEncontrado() {
        when(usuarioRepository.existsById(99L)).thenReturn(false);
        
        RuntimeException ex = assertThrows(RuntimeException.class, () -> 
            grupoService.listarGruposPorUsuario(99L)
        );
        assertEquals("Usuario no encontrado", ex.getMessage());
    }
    @Test
    public void testRenombrarGrupoExito() {
        when(usuarioRepository.existsById(1L)).thenReturn(true);
        when(grupoRepository.existsByIdAndMiembros_Id(10L, 1L)).thenReturn(true);
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        when(grupoRepository.save(any(Grupo.class))).thenAnswer(inv -> inv.getArgument(0));

        Grupo resultado = grupoService.renombrarGrupo(10L, 1L, "  Viaje a Roma  "); // Con espacios para probar el trim()

        assertEquals("Viaje a Roma", resultado.getNombre());
    }

    @Test
    public void testRenombrarGrupoNombreInvalido() {
        RuntimeException ex1 = assertThrows(RuntimeException.class, () -> 
            grupoService.renombrarGrupo(10L, 1L, null)
        );
        assertEquals("El nombre del grupo no puede estar vacío", ex1.getMessage());

        RuntimeException ex2 = assertThrows(RuntimeException.class, () -> 
            grupoService.renombrarGrupo(10L, 1L, "   ")
        );
        assertEquals("El nombre del grupo no puede estar vacío", ex2.getMessage());
    }

    @Test
    public void testRenombrarGrupoUsuarioInvalido() {
        // ID nulo
        assertThrows(RuntimeException.class, () -> grupoService.renombrarGrupo(10L, null, "Valido"));
        
        // Usuario no existe
        when(usuarioRepository.existsById(99L)).thenReturn(false);
        assertThrows(RuntimeException.class, () -> grupoService.renombrarGrupo(10L, 99L, "Valido"));
    }

    @Test
    public void testRenombrarGrupoSinPermisos() {
        when(usuarioRepository.existsById(2L)).thenReturn(true);
        when(grupoRepository.existsByIdAndMiembros_Id(10L, 2L)).thenReturn(false); // Miembro 2 no está en el grupo
        
        RuntimeException ex = assertThrows(RuntimeException.class, () -> 
            grupoService.renombrarGrupo(10L, 2L, "Valido")
        );
        assertEquals("No tienes permiso para renombrar este grupo", ex.getMessage());
    }
    @Test
    public void testObtenerGrupoPorIdExito() {
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        Grupo resultado = grupoService.obtenerGrupoPorId(10L);
        assertNotNull(resultado);
    }

    @Test
    public void testObtenerGrupoPorIdNoEncontrado() {
        when(grupoRepository.findById(99L)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> grupoService.obtenerGrupoPorId(99L));
        assertEquals("Grupo no encontrado", ex.getMessage());
    }
    @Test
    public void testInvitarUsuarioExito() {
        Usuario nuevoInvitado = new Usuario("Nuevo", "nuevo@mail.com", "pass");
        nuevoInvitado.setId(3L);

        when(usuarioRepository.existsById(1L)).thenReturn(true);
        when(grupoRepository.existsByIdAndMiembros_Id(10L, 1L)).thenReturn(true);
        when(usuarioRepository.findByEmail("nuevo@mail.com")).thenReturn(Optional.of(nuevoInvitado));
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        when(grupoRepository.save(any(Grupo.class))).thenAnswer(inv -> inv.getArgument(0));

        Grupo resultado = grupoService.invitarUsuario(10L, "nuevo@mail.com", 1L);

        assertTrue(resultado.getMiembros().contains(nuevoInvitado));
        assertEquals(3, resultado.getMiembros().size()); // admin + miembro + nuevoInvitado
    }

    @Test
    public void testInvitarUsuarioInvitadorInvalido() {
        assertThrows(RuntimeException.class, () -> grupoService.invitarUsuario(10L, "mail@mail.com", null));
        
        when(usuarioRepository.existsById(99L)).thenReturn(false);
        assertThrows(RuntimeException.class, () -> grupoService.invitarUsuario(10L, "mail@mail.com", 99L));
    }

    @Test
    public void testInvitarUsuarioEmailNoEncontrado() {
        when(usuarioRepository.existsById(1L)).thenReturn(true);
        when(grupoRepository.existsByIdAndMiembros_Id(10L, 1L)).thenReturn(true);
        when(usuarioRepository.findByEmail("fake@mail.com")).thenReturn(Optional.empty());
        
        RuntimeException ex = assertThrows(RuntimeException.class, () -> 
            grupoService.invitarUsuario(10L, "fake@mail.com", 1L)
        );
        assertEquals("No existe ningún usuario con el email: fake@mail.com", ex.getMessage());
    }

    @Test
    public void testInvitarUsuarioYaEsMiembro() {
        when(usuarioRepository.existsById(1L)).thenReturn(true);
        when(grupoRepository.existsByIdAndMiembros_Id(10L, 1L)).thenReturn(true);
        
        // Simulamos buscar a "miembro@mail.com", quien ya está instanciado en el @BeforeEach
        when(usuarioRepository.findByEmail("miembro@mail.com")).thenReturn(Optional.of(miembro));
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> 
            grupoService.invitarUsuario(10L, "miembro@mail.com", 1L)
        );
        assertEquals("El usuario ya es miembro de este grupo", ex.getMessage());
    }
    @Test
    public void testExpulsarMiembroGrupoNoEncontrado() {
        when(grupoRepository.findById(99L)).thenReturn(Optional.empty());
        
        RuntimeException ex = assertThrows(RuntimeException.class, () -> 
            grupoService.expulsarMiembro(99L, 1L, 2L)
        );
        assertEquals("Grupo no encontrado", ex.getMessage());
    }
}
