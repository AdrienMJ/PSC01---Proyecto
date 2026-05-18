package com.mycompany.app.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.mycompany.app.entity.Gasto;
import com.mycompany.app.entity.Grupo;
import com.mycompany.app.entity.Moneda;
import com.mycompany.app.entity.Usuario;
import com.mycompany.app.repository.GastoRepository;
import com.mycompany.app.repository.GrupoRepository;
import com.mycompany.app.repository.PagoRepository;
import com.mycompany.app.repository.UsuarioRepository;

public class TicketServiceTest {

    @Mock private GastoRepository gastoRepository;
    @Mock private GrupoRepository grupoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PagoRepository pagoRepository;

    @InjectMocks
    private GastoService gastoService;

    private Usuario pagador;
    private Grupo grupo;
    private Gasto gasto;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        pagador = new Usuario();
        pagador.setId(1L);
        pagador.setUsername("Adrien");

        grupo = new Grupo();
        grupo.setId(10L);
        grupo.setMoneda(Moneda.EURO);
        grupo.getMiembros().add(pagador);

        gasto = new Gasto();
        gasto.setConcepto("Cena");
        gasto.setMonto(50.0);
        gasto.setMoneda(Moneda.EURO);
        gasto.setPagador(pagador);
        gasto.setGrupo(grupo);

        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(pagador));
        when(gastoRepository.save(any(Gasto.class))).thenAnswer(i -> i.getArguments()[0]);
    }

    // ==========================================
    // Ticket URL se guarda correctamente
    // ==========================================

    @Test
    public void testCrearGastoConTicketUrlSeAsigna() throws Exception {
        String ticketUrl = "/tickets/abc123.jpg";

        Gasto creado = gastoService.crear(gasto, ticketUrl);

        assertNotNull(creado);
        assertEquals("/tickets/abc123.jpg", creado.getTicketUrl());
    }

    @Test
    public void testCrearGastoSinTicketUrlEsNull() throws Exception {
        Gasto creado = gastoService.crear(gasto, null);

        assertNotNull(creado);
        assertNull(creado.getTicketUrl());
    }

    @Test
    public void testCrearGastoConTicketUrlVaciaSeAsigna() throws Exception {
        // Una cadena vacía no es null, se asigna igualmente (la validación es en el controller)
        Gasto creado = gastoService.crear(gasto, "");

        assertNotNull(creado);
        assertEquals("", creado.getTicketUrl());
    }

    @Test
    public void testCrearGastoConTicketUrlNoAfectaOtrosCampos() throws Exception {
        gasto.setConcepto("Comida de trabajo");
        gasto.setMonto(120.0);
        String ticketUrl = "/tickets/factura-comida.png";

        Gasto creado = gastoService.crear(gasto, ticketUrl);

        // El ticket no debe alterar ningún otro campo
        assertEquals("Comida de trabajo", creado.getConcepto());
        assertEquals(120.0, creado.getMonto());
        assertEquals(ticketUrl, creado.getTicketUrl());
    }

    @Test
    public void testCrearVariosGastosTicketUrlIndependiente() throws Exception {
        // Primer gasto con ticket
        Gasto creado1 = gastoService.crear(gasto, "/tickets/ticket1.jpg");

        // Segundo gasto sin ticket (mismo objeto reseteado)
        Gasto gasto2 = new Gasto();
        gasto2.setConcepto("Taxi");
        gasto2.setMonto(15.0);
        gasto2.setMoneda(Moneda.EURO);
        gasto2.setPagador(pagador);
        gasto2.setGrupo(grupo);
        Gasto creado2 = gastoService.crear(gasto2, null);

        assertEquals("/tickets/ticket1.jpg", creado1.getTicketUrl());
        assertNull(creado2.getTicketUrl());
    }
}