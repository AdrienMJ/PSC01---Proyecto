package com.mycompany.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.mycompany.app.dto.ResumenGrupoDTO;
import com.mycompany.app.dto.TransferenciaDTO;
import com.mycompany.app.entity.Gasto;
import com.mycompany.app.entity.Grupo;
import com.mycompany.app.entity.Moneda;
import com.mycompany.app.entity.Usuario;
import com.mycompany.app.repository.GastoRepository;
import com.mycompany.app.repository.GrupoRepository;
import com.mycompany.app.repository.PagoRepository;
import com.mycompany.app.repository.UsuarioRepository;

public class ResumenDeudasUsuarioTest {

    @Mock
    private GastoRepository gastoRepository;

    @Mock
    private GrupoRepository grupoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PagoRepository pagoRepository;

    @InjectMocks
    private GastoService gastoService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testResumenMuestraQuienDebeAQuienEnGrupoSimple() throws Exception {
        Usuario ana = crearUsuario(1L, "Ana");
        Usuario luis = crearUsuario(2L, "Luis");

        Grupo grupo = new Grupo();
        grupo.setId(10L);
        grupo.setMoneda(Moneda.EURO);
        grupo.getMiembros().add(ana);
        grupo.getMiembros().add(luis);

        Gasto cena = new Gasto();
        cena.setMonto(100.0);
        cena.setPagador(ana);
        cena.setGrupo(grupo);
        cena.setRepartoGeneral(true);

        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        when(gastoRepository.findByGrupoId(10L)).thenReturn(Arrays.asList(cena));
        when(pagoRepository.findByGrupoId(10L)).thenReturn(Collections.emptyList());

        ResumenGrupoDTO resumen = gastoService.obtenerResumenGrupo(10L);

        assertNotNull(resumen);
        assertEquals(1, resumen.getSoluciones().size());

        TransferenciaDTO transferencia = resumen.getSoluciones().get(0);
        assertEquals(2L, transferencia.getDeUsuarioId());
        assertEquals("Luis", transferencia.getDeUsername());
        assertEquals(1L, transferencia.getAUsuarioId());
        assertEquals("Ana", transferencia.getAUsername());
        assertEquals(50.0, transferencia.getMonto(), 0.01);
    }

    @Test
    public void testResumenMuestraMultiplesDeudoresHaciaUnAcreedor() throws Exception {
        Usuario ana = crearUsuario(1L, "Ana");
        Usuario luis = crearUsuario(2L, "Luis");
        Usuario marta = crearUsuario(3L, "Marta");

        Grupo grupo = new Grupo();
        grupo.setId(20L);
        grupo.setMoneda(Moneda.EURO);
        grupo.getMiembros().add(ana);
        grupo.getMiembros().add(luis);
        grupo.getMiembros().add(marta);

        Gasto hotel = new Gasto();
        hotel.setMonto(120.0);
        hotel.setPagador(ana);
        hotel.setGrupo(grupo);
        hotel.setRepartoGeneral(true);

        Gasto taxi = new Gasto();
        taxi.setMonto(60.0);
        taxi.setPagador(luis);
        taxi.setGrupo(grupo);
        taxi.setRepartoGeneral(false);
        taxi.setParticipantes(Arrays.asList(luis, marta));

        when(grupoRepository.findById(20L)).thenReturn(Optional.of(grupo));
        when(gastoRepository.findByGrupoId(20L)).thenReturn(Arrays.asList(hotel, taxi));
        when(pagoRepository.findByGrupoId(20L)).thenReturn(Collections.emptyList());

        ResumenGrupoDTO resumen = gastoService.obtenerResumenGrupo(20L);

        assertNotNull(resumen);
        assertEquals(2, resumen.getSoluciones().size());

        TransferenciaDTO primera = resumen.getSoluciones().get(0);
        assertEquals("Luis", primera.getDeUsername());
        assertEquals("Ana", primera.getAUsername());
        assertEquals(10.0, primera.getMonto(), 0.01);

        TransferenciaDTO segunda = resumen.getSoluciones().get(1);
        assertEquals("Marta", segunda.getDeUsername());
        assertEquals("Ana", segunda.getAUsername());
        assertEquals(70.0, segunda.getMonto(), 0.01);
    }

    private Usuario crearUsuario(Long id, String username) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setUsername(username);
        return usuario;
    }
}
