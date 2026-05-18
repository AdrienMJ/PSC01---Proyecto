package com.mycompany.app.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.app.entity.Gasto;
import com.mycompany.app.repository.GastoRepository;
import com.mycompany.app.service.GastoService;
import com.mycompany.app.service.GrupoService;

@WebMvcTest(GastoController.class)
@TestPropertySource(properties = "app.uploads.dir=${java.io.tmpdir}/tickets-test")
public class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GastoService gastoService;

    @MockBean
    private GrupoService grupoService;

    @MockBean
    private GastoRepository gastoRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Gasto gastoBase;
    private byte[] gastoJson;

    @BeforeEach
    void setUp() throws Exception {
        gastoBase = new Gasto();
        gastoBase.setConcepto("Cena");
        gastoBase.setMonto(50.0);

        gastoJson = objectMapper.writeValueAsBytes(gastoBase);
    }

    // ==========================================
    // POST /api/gastos/crear — sin foto
    // ==========================================

    @Test
    void testCrearSinTicketDevuelveGasto() throws Exception {
        when(gastoService.crear(any(Gasto.class), isNull())).thenReturn(gastoBase);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/gastos/crear")
                .file("gasto", gastoJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.concepto").value("Cena"))
                .andExpect(jsonPath("$.monto").value(50.0));
    }

    // ==========================================
    // POST /api/gastos/crear — con foto adjunta
    // ==========================================

    @Test
    void testCrearConTicketJpgLlamaServicioConUrl() throws Exception {
        Gasto gastoConTicket = new Gasto();
        gastoConTicket.setConcepto("Cena");
        gastoConTicket.setMonto(50.0);
        gastoConTicket.setTicketUrl("/tickets/alguna-imagen.jpg");

        // El controller guarda la foto y llama a crear con una URL no nula
        when(gastoService.crear(any(Gasto.class), isNotNull())).thenReturn(gastoConTicket);

        MockMultipartFile foto = new MockMultipartFile(
                "ticket", "foto.jpg", "image/jpeg", "contenido-falso".getBytes());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/gastos/crear")
                .file("gasto", gastoJson)
                .file(foto))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketUrl").value("/tickets/alguna-imagen.jpg"));
    }

    @Test
    void testCrearConTicketPngTambienFunciona() throws Exception {
        Gasto gastoConTicket = new Gasto();
        gastoConTicket.setConcepto("Cena");
        gastoConTicket.setMonto(50.0);
        gastoConTicket.setTicketUrl("/tickets/alguna-imagen.png");

        when(gastoService.crear(any(Gasto.class), isNotNull())).thenReturn(gastoConTicket);

        MockMultipartFile foto = new MockMultipartFile(
                "ticket", "recibo.png", "image/png", "bytes-png".getBytes());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/gastos/crear")
                .file("gasto", gastoJson)
                .file(foto))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketUrl").value("/tickets/alguna-imagen.png"));
    }

    // ==========================================
    // POST /api/gastos/crear — errores de servicio
    // ==========================================

    @Test
    void testCrearSinTicketErrorServicioDevuelve400() throws Exception {
        when(gastoService.crear(any(Gasto.class), isNull()))
                .thenThrow(new Exception("El monto debe ser mayor que 0"));

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/gastos/crear")
                .file("gasto", gastoJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCrearConTicketErrorServicioDevuelve400() throws Exception {
        when(gastoService.crear(any(Gasto.class), isNotNull()))
                .thenThrow(new Exception("Pagador no encontrado"));

        MockMultipartFile foto = new MockMultipartFile(
                "ticket", "foto.jpg", "image/jpeg", "bytes".getBytes());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/gastos/crear")
                .file("gasto", gastoJson)
                .file(foto))
                .andExpect(status().isBadRequest());
    }
}