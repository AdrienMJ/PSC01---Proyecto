package com.mycompany.app.service;

import static org.junit.jupiter.api.Assertions.*;
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
}
