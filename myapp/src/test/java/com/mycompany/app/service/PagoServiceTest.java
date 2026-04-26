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
import com.mycompany.app.entity.Moneda;
import com.mycompany.app.entity.Pago;
import com.mycompany.app.entity.Usuario;
import com.mycompany.app.repository.GrupoRepository;
import com.mycompany.app.repository.PagoRepository;
import com.mycompany.app.repository.UsuarioRepository;

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

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    // 1. Test de registro exitoso
    @Test
    public void testRegistrarPagoExitoso() throws Exception {
        Usuario pagador = new Usuario();
        pagador.setId(1L);
        Usuario receptor = new Usuario();
        receptor.setId(2L);

        Grupo grupo = new Grupo();
        grupo.setId(10L);
        grupo.getMiembros().add(pagador);
        grupo.getMiembros().add(receptor);

        Pago pago = new Pago();
        pago.setMonto(50.0);
        pago.setPagador(pagador);
        pago.setReceptor(receptor);
        pago.setGrupo(grupo);

        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(pagador));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(receptor));
        when(pagoRepository.save(any(Pago.class))).thenReturn(pago);

        Pago resultado = pagoService.registrarPago(pago);

        assertNotNull(resultado);
        assertEquals(50.0, resultado.getMonto());
        verify(pagoRepository).save(pago);
    }

    // 2. Test de validación: monto cero
    @Test
    public void testRegistrarPagoMontoCeroLanzaExcepcion() {
        Pago pago = new Pago();
        pago.setMonto(0.0);

        Exception ex = assertThrows(Exception.class, () -> {
            pagoService.registrarPago(pago);
        });

        assertEquals("El monto debe ser mayor que 0", ex.getMessage());
    }

    // 3. Test de validación: monto negativo
    @Test
    public void testRegistrarPagoMontoNegativoLanzaExcepcion() {
        Pago pago = new Pago();
        pago.setMonto(-10.0);

        Exception ex = assertThrows(Exception.class, () -> {
            pagoService.registrarPago(pago);
        });

        assertEquals("El monto debe ser mayor que 0", ex.getMessage());
    }

    // 4. Test de validación: pagador y receptor son la misma persona
    @Test
    public void testRegistrarPagoMismaPersonaLanzaExcepcion() {
        Usuario mismo = new Usuario();
        mismo.setId(1L);

        Pago pago = new Pago();
        pago.setMonto(30.0);
        pago.setPagador(mismo);
        pago.setReceptor(mismo);
        pago.setGrupo(new Grupo());
        pago.getGrupo().setId(10L);

        Exception ex = assertThrows(Exception.class, () -> {
            pagoService.registrarPago(pago);
        });

        assertEquals("El pagador y el receptor no pueden ser la misma persona", ex.getMessage());
    }

    // 5. Test de validación: pagador no pertenece al grupo
    @Test
    public void testRegistrarPagoPagadorNoPerteneceAlGrupo() {
        Usuario pagador = new Usuario();
        pagador.setId(1L);
        Usuario receptor = new Usuario();
        receptor.setId(2L);

        Grupo grupo = new Grupo();
        grupo.setId(10L);
        grupo.getMiembros().add(receptor); // solo receptor es miembro

        Pago pago = new Pago();
        pago.setMonto(25.0);
        pago.setPagador(pagador);
        pago.setReceptor(receptor);
        pago.setGrupo(grupo);

        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(pagador));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(receptor));

        Exception ex = assertThrows(Exception.class, () -> {
            pagoService.registrarPago(pago);
        });

        assertEquals("El pagador no pertenece al grupo", ex.getMessage());
    }

    // 6. Test de validación: receptor no pertenece al grupo
    @Test
    public void testRegistrarPagoReceptorNoPerteneceAlGrupo() {
        Usuario pagador = new Usuario();
        pagador.setId(1L);
        Usuario receptor = new Usuario();
        receptor.setId(2L);

        Grupo grupo = new Grupo();
        grupo.setId(10L);
        grupo.getMiembros().add(pagador); // solo pagador es miembro

        Pago pago = new Pago();
        pago.setMonto(25.0);
        pago.setPagador(pagador);
        pago.setReceptor(receptor);
        pago.setGrupo(grupo);

        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(pagador));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(receptor));

        Exception ex = assertThrows(Exception.class, () -> {
            pagoService.registrarPago(pago);
        });

        assertEquals("El receptor no pertenece al grupo", ex.getMessage());
    }

    // 7. Test de obtener historial por grupo
    @Test
    public void testObtenerHistorialPorGrupo() throws Exception {
        Long grupoId = 10L;
        Grupo grupo = new Grupo();
        grupo.setId(grupoId);

        Pago pago1 = new Pago();
        pago1.setMonto(50.0);
        Pago pago2 = new Pago();
        pago2.setMonto(30.0);

        when(grupoRepository.findById(grupoId)).thenReturn(Optional.of(grupo));
        when(pagoRepository.findByGrupoIdOrderByFechaDesc(grupoId))
                .thenReturn(Arrays.asList(pago1, pago2));

        List<Pago> resultado = pagoService.obtenerHistorialPorGrupo(grupoId);

        assertNotNull(resultado);
        assertEquals(2, resultado.size());
    }

    // 8. Test de historial con grupo inexistente
    @Test
    public void testObtenerHistorialGrupoNoExisteLanzaExcepcion() {
        when(grupoRepository.findById(99L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class, () -> {
            pagoService.obtenerHistorialPorGrupo(99L);
        });

        assertEquals("Grupo no encontrado", ex.getMessage());
    }
}