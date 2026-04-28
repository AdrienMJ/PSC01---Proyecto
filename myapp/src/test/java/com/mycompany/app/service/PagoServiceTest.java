package com.mycompany.app.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.mycompany.app.entity.Grupo;
import com.mycompany.app.entity.Pago;
import com.mycompany.app.entity.Usuario;
import com.mycompany.app.repository.GrupoRepository;
import com.mycompany.app.repository.PagoRepository;
import com.mycompany.app.repository.UsuarioRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class PagoServiceTest {

    @Mock
    private PagoRepository pagoRepository;

    @Mock
    private GrupoRepository grupoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private PagoService pagoService;

    private Usuario pagador;
    private Usuario receptor;
    private Grupo grupo;
    private Pago pagoBase;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        pagador = new Usuario();
        pagador.setId(1L);
        pagador.setUsername("Pagador");

        receptor = new Usuario();
        receptor.setId(2L);
        receptor.setUsername("Receptor");

        grupo = new Grupo();
        grupo.setId(10L);
        grupo.getMiembros().add(pagador);
        grupo.getMiembros().add(receptor);

        pagoBase = new Pago();
        pagoBase.setMonto(50.0);
        pagoBase.setPagador(pagador);
        pagoBase.setReceptor(receptor);
        pagoBase.setGrupo(grupo);
    }

    // --- SECCIÓN: registrarPago (Éxito y Validaciones) ---

    @Test
    public void testRegistrarPagoExitoso() throws Exception {
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(pagador));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(receptor));
        when(pagoRepository.save(any(Pago.class))).thenReturn(pagoBase);

        Pago resultado = pagoService.registrarPago(pagoBase);

        assertNotNull(resultado);
        assertEquals(50.0, resultado.getMonto());
        verify(pagoRepository).save(pagoBase);
    }

    @Test
    public void testRegistrarPago_ValidacionesNulos() {
        // Monto nulo
        pagoBase.setMonto(null);
        assertThrows(Exception.class, () -> pagoService.registrarPago(pagoBase));

        // Pagador nulo
        pagoBase.setMonto(50.0);
        pagoBase.setPagador(null);
        assertThrows(Exception.class, () -> pagoService.registrarPago(pagoBase));

        // Receptor nulo
        pagoBase.setPagador(pagador);
        pagoBase.setReceptor(null);
        assertThrows(Exception.class, () -> pagoService.registrarPago(pagoBase));

        // Grupo nulo
        pagoBase.setReceptor(receptor);
        pagoBase.setGrupo(null);
        assertThrows(Exception.class, () -> pagoService.registrarPago(pagoBase));
    }

    @Test
    public void testRegistrarPago_EntidadesNoEncontradas() {
        when(grupoRepository.findById(10L)).thenReturn(Optional.empty());
        assertThrows(Exception.class, () -> pagoService.registrarPago(pagoBase), "Grupo no encontrado");

        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(Exception.class, () -> pagoService.registrarPago(pagoBase), "Pagador no encontrado");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(pagador));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(Exception.class, () -> pagoService.registrarPago(pagoBase), "Receptor no encontrado");
    }

    @Test
    public void testRegistrarPagoMontoCeroONegativo() {
        pagoBase.setMonto(0.0);
        assertEquals("El monto debe ser mayor que 0", 
            assertThrows(Exception.class, () -> pagoService.registrarPago(pagoBase)).getMessage());

        pagoBase.setMonto(-10.0);
        assertEquals("El monto debe ser mayor que 0", 
            assertThrows(Exception.class, () -> pagoService.registrarPago(pagoBase)).getMessage());
    }

    @Test
    public void testRegistrarPagoMismaPersona() {
        pagoBase.setReceptor(pagador);
        assertEquals("El pagador y el receptor no pueden ser la misma persona", 
            assertThrows(Exception.class, () -> pagoService.registrarPago(pagoBase)).getMessage());
    }

    @Test
    public void testRegistrarPago_NoPertenecenAlGrupo() {
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(pagador));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(receptor));

        // Pagador no está en el grupo
        grupo.getMiembros().clear();
        grupo.getMiembros().add(receptor);
        assertEquals("El pagador no pertenece al grupo", 
            assertThrows(Exception.class, () -> pagoService.registrarPago(pagoBase)).getMessage());

        // Receptor no está en el grupo
        grupo.getMiembros().clear();
        grupo.getMiembros().add(pagador);
        assertEquals("El receptor no pertenece al grupo", 
            assertThrows(Exception.class, () -> pagoService.registrarPago(pagoBase)).getMessage());
    }

    // --- SECCIÓN: obtenerHistorialPorGrupo ---

    @Test
    public void testObtenerHistorialPorGrupoExito() throws Exception {
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        when(pagoRepository.findByGrupoIdOrderByFechaDesc(10L)).thenReturn(Arrays.asList(pagoBase));

        List<Pago> resultado = pagoService.obtenerHistorialPorGrupo(10L);
        assertEquals(1, resultado.size());
    }

    @Test
    public void testObtenerHistorialGrupoInexistente() {
        when(grupoRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(Exception.class, () -> pagoService.obtenerHistorialPorGrupo(99L));
    }

    // --- SECCIÓN: obtenerPagosPorUsuario ---

    @Test
    public void testObtenerPagosPorUsuario() {
        Pago pagoEnviado = new Pago();
        pagoEnviado.setMonto(100.0);
        Pago pagoRecibido = new Pago();
        pagoRecibido.setMonto(200.0);

        // Importante usar ArrayList para que el .addAll() del Service no falle
        when(pagoRepository.findByPagadorId(1L)).thenReturn(new ArrayList<>(List.of(pagoEnviado)));
        when(pagoRepository.findByReceptorId(1L)).thenReturn(new ArrayList<>(List.of(pagoRecibido)));

        List<Pago> resultado = pagoService.obtenerPagosPorUsuario(1L);

        assertEquals(2, resultado.size());
        assertTrue(resultado.contains(pagoEnviado));
        assertTrue(resultado.contains(pagoRecibido));
    }
}