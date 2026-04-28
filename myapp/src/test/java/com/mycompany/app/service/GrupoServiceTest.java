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

    // --- TESTS: crearGrupo ---

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

    // --- TESTS: listarGruposPorUsuario ---

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

    // --- TESTS: renombrarGrupo ---

    @Test
    public void testRenombrarGrupoExito() {
        when(usuarioRepository.existsById(1L)).thenReturn(true);
        when(grupoRepository.existsByIdAndMiembros_Id(10L, 1L)).thenReturn(true);
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        when(grupoRepository.save(any(Grupo.class))).thenAnswer(inv -> inv.getArgument(0));

        Grupo resultado = grupoService.renombrarGrupo(10L, 1L, "  Viaje a Roma  ");

        assertEquals("Viaje a Roma", resultado.getNombre());
    }

    @Test
    public void testRenombrarGrupoNombreInvalido() {
        assertThrows(RuntimeException.class, () -> grupoService.renombrarGrupo(10L, 1L, null));
        assertThrows(RuntimeException.class, () -> grupoService.renombrarGrupo(10L, 1L, "   "));
    }

    @Test
    public void testRenombrarGrupo_GrupoNoEncontrado() {
        when(usuarioRepository.existsById(1L)).thenReturn(true);
        when(grupoRepository.existsByIdAndMiembros_Id(10L, 1L)).thenReturn(true);
        when(grupoRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> 
            grupoService.renombrarGrupo(10L, 1L, "Nuevo Nombre")
        );
    }

    // --- TESTS: invitarUsuario ---

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
    }

    @Test
    public void testInvitarUsuario_InvitadorSinPermisos() {
        when(usuarioRepository.existsById(1L)).thenReturn(true);
        when(grupoRepository.existsByIdAndMiembros_Id(10L, 1L)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> 
            grupoService.invitarUsuario(10L, "invitado@mail.com", 1L)
        );
        assertEquals("No tienes permiso para invitar a este grupo", ex.getMessage());
    }

    @Test
    public void testInvitarUsuarioYaEsMiembro() {
        when(usuarioRepository.existsById(1L)).thenReturn(true);
        when(grupoRepository.existsByIdAndMiembros_Id(10L, 1L)).thenReturn(true);
        when(usuarioRepository.findByEmail("miembro@mail.com")).thenReturn(Optional.of(miembro));
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));

        assertThrows(RuntimeException.class, () -> 
            grupoService.invitarUsuario(10L, "miembro@mail.com", 1L)
        );
    }

    // --- TESTS: expulsarMiembro ---

    @Test
    public void testExpulsarMiembroExitoso() throws Exception {
        BalancePersonaDTO balanceSaldado = new BalancePersonaDTO(2L, "Miembro", 0.0, "Saldado");
        ResumenGrupoDTO resumen = new ResumenGrupoDTO(100.0, Arrays.asList(balanceSaldado), Collections.emptyList());

        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        when(gastoService.obtenerResumenGrupo(10L)).thenReturn(resumen);

        grupoService.expulsarMiembro(10L, 1L, 2L);

        assertFalse(grupo.getMiembros().stream().anyMatch(m -> m.getId().equals(2L)));
        verify(grupoRepository).save(grupo);
    }

    @Test
    public void testExpulsarMiembroConDeudaPendiente() throws Exception {
        BalancePersonaDTO balanceDeudor = new BalancePersonaDTO(2L, "Miembro", -30.0, "Deudor");
        ResumenGrupoDTO resumen = new ResumenGrupoDTO(100.0, Arrays.asList(balanceDeudor), Collections.emptyList());

        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        when(gastoService.obtenerResumenGrupo(10L)).thenReturn(resumen);

        assertThrows(RuntimeException.class, () -> grupoService.expulsarMiembro(10L, 1L, 2L));
    }

    @Test
    public void testExpulsarMiembro_NoAdmin() {
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        assertThrows(RuntimeException.class, () -> grupoService.expulsarMiembro(10L, 2L, 1L));
    }

    @Test
    public void testExpulsarMiembro_AutoExpulsion() {
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        assertThrows(RuntimeException.class, () -> grupoService.expulsarMiembro(10L, 1L, 1L));
    }

    @Test
    public void testExpulsarMiembro_CreadorEsNulo() {
        grupo.setIdCreador(null);
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));

        assertThrows(RuntimeException.class, () -> grupoService.expulsarMiembro(10L, 1L, 2L));
    }

    @Test
    public void testExpulsarMiembro_SinBalanceRegistrado() throws Exception {
        // Al no haber balance en la lista, el ifPresent no se ejecuta y permite borrar
        ResumenGrupoDTO resumenVacio = new ResumenGrupoDTO(0.0, Collections.emptyList(), Collections.emptyList());

        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        when(gastoService.obtenerResumenGrupo(10L)).thenReturn(resumenVacio);

        assertDoesNotThrow(() -> grupoService.expulsarMiembro(10L, 1L, 2L));
        verify(grupoRepository).save(grupo);
    }

    @Test
    public void testObtenerGrupoPorIdExito() {
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        Grupo resultado = grupoService.obtenerGrupoPorId(10L);
        assertNotNull(resultado);
    }

    // --- TESTS EXTRA PARA COBERTURA ALTA EN GRUPOSERVICE ---

    @Test
    public void testRenombrarGrupo_UsuarioNuloONoExiste() {
        // ID de usuario nulo
        assertThrows(RuntimeException.class, () -> 
            grupoService.renombrarGrupo(10L, null, "Nuevo Nombre")
        );
        // Usuario no existe en DB
        when(usuarioRepository.existsById(99L)).thenReturn(false);
        assertThrows(RuntimeException.class, () -> 
            grupoService.renombrarGrupo(10L, 99L, "Nuevo Nombre")
        );
    }

    @Test
    public void testObtenerGrupoPorId_NoEncontrado() {
        when(grupoRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> grupoService.obtenerGrupoPorId(99L));
    }

    @Test
    public void testInvitarUsuario_InvitadorNuloONoExiste() {
        assertThrows(RuntimeException.class, () -> 
            grupoService.invitarUsuario(10L, "test@mail.com", null)
        );
        
        when(usuarioRepository.existsById(99L)).thenReturn(false);
        assertThrows(RuntimeException.class, () -> 
            grupoService.invitarUsuario(10L, "test@mail.com", 99L)
        );
    }

    @Test
    public void testInvitarUsuario_EmailNoExiste() {
        when(usuarioRepository.existsById(1L)).thenReturn(true);
        when(grupoRepository.existsByIdAndMiembros_Id(10L, 1L)).thenReturn(true);
        when(usuarioRepository.findByEmail("noexiste@mail.com")).thenReturn(Optional.empty());
        
        assertThrows(RuntimeException.class, () -> 
            grupoService.invitarUsuario(10L, "noexiste@mail.com", 1L)
        );
    }

    @Test
    public void testExpulsarMiembro_GrupoNoEncontrado() {
        when(grupoRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(Exception.class, () -> grupoService.expulsarMiembro(99L, 1L, 2L));
    }
}