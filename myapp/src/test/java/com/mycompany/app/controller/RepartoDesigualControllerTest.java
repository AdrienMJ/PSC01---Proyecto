package com.mycompany.app.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import java.util.HashMap;
import java.util.Map;

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
import com.mycompany.app.entity.TipoReparto;
import com.mycompany.app.repository.GastoRepository;
import com.mycompany.app.service.GastoService;
import com.mycompany.app.service.GrupoService;

@WebMvcTest(GastoController.class)
@TestPropertySource(properties = "app.uploads.dir=${java.io.tmpdir}/tickets-test")
public class RepartoDesigualControllerTest {

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

    @BeforeEach
    void setUp() {
        gastoBase = new Gasto();
        gastoBase.setConcepto("Cena");
        gastoBase.setMonto(100.0);
    }

    // ==========================================
    // POST /api/gastos/crear con tipoReparto IGUAL
    // ==========================================

    @Test
    void testCrearConTipoRepartoIgualExito() throws Exception {
        gastoBase.setTipoReparto(TipoReparto.IGUAL);
        when(gastoService.crear(any(Gasto.class), isNull())).thenReturn(gastoBase);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/gastos/crear")
                .file("gasto", objectMapper.writeValueAsBytes(gastoBase)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoReparto").value("IGUAL"));
    }

    // ==========================================
    // POST /api/gastos/crear con tipoReparto PORCENTAJE
    // ==========================================

    @Test
    void testCrearConTipoRepartoPorcentajeExito() throws Exception {
        Gasto gastoPorc = new Gasto();
        gastoPorc.setConcepto("Comida");
        gastoPorc.setMonto(100.0);
        gastoPorc.setTipoReparto(TipoReparto.PORCENTAJE);

        Map<Long, Double> cuotasMap = new HashMap<>();
        cuotasMap.put(1L, 60.0);
        cuotasMap.put(2L, 40.0);
        gastoPorc.setCuotasMap(cuotasMap);

        when(gastoService.crear(any(Gasto.class), isNull())).thenReturn(gastoPorc);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/gastos/crear")
                .file("gasto", objectMapper.writeValueAsBytes(gastoPorc)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoReparto").value("PORCENTAJE"));
    }

    @Test
    void testCrearConTipoRepartoPorcentajeSumaIncorrectaDevuelve400() throws Exception {
        Gasto gastoPorc = new Gasto();
        gastoPorc.setConcepto("Taxi");
        gastoPorc.setMonto(100.0);
        gastoPorc.setTipoReparto(TipoReparto.PORCENTAJE);

        Map<Long, Double> cuotasMap = new HashMap<>();
        cuotasMap.put(1L, 40.0);
        cuotasMap.put(2L, 40.0); // solo 80%
        gastoPorc.setCuotasMap(cuotasMap);

        when(gastoService.crear(any(Gasto.class), isNull()))
                .thenThrow(new Exception("Los porcentajes deben sumar 100. Suma actual: 80"));

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/gastos/crear")
                .file("gasto", objectMapper.writeValueAsBytes(gastoPorc)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: Los porcentajes deben sumar 100. Suma actual: 80"));
    }

    // ==========================================
    // POST /api/gastos/crear con tipoReparto CUOTA_FIJA
    // ==========================================

    @Test
    void testCrearConTipoRepartoCuotaFijaExito() throws Exception {
        Gasto gastoCuota = new Gasto();
        gastoCuota.setConcepto("Hotel");
        gastoCuota.setMonto(100.0);
        gastoCuota.setTipoReparto(TipoReparto.CUOTA_FIJA);

        Map<Long, Double> cuotasMap = new HashMap<>();
        cuotasMap.put(1L, 30.0);
        cuotasMap.put(2L, 70.0);
        gastoCuota.setCuotasMap(cuotasMap);

        when(gastoService.crear(any(Gasto.class), isNull())).thenReturn(gastoCuota);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/gastos/crear")
                .file("gasto", objectMapper.writeValueAsBytes(gastoCuota)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoReparto").value("CUOTA_FIJA"));
    }

    @Test
    void testCrearConTipoRepartoCuotaFijaSumaIncorrectaDevuelve400() throws Exception {
        Gasto gastoCuota = new Gasto();
        gastoCuota.setConcepto("Cena");
        gastoCuota.setMonto(100.0);
        gastoCuota.setTipoReparto(TipoReparto.CUOTA_FIJA);

        Map<Long, Double> cuotasMap = new HashMap<>();
        cuotasMap.put(1L, 30.0);
        cuotasMap.put(2L, 50.0); // 80€, faltan 20€
        gastoCuota.setCuotasMap(cuotasMap);

        when(gastoService.crear(any(Gasto.class), isNull()))
                .thenThrow(new Exception("Las cuotas deben sumar el monto total (100.0). Suma actual: 80.0"));

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/gastos/crear")
                .file("gasto", objectMapper.writeValueAsBytes(gastoCuota)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: Las cuotas deben sumar el monto total (100.0). Suma actual: 80.0"));
    }

    @Test
    void testCrearConTipoRepartoCuotaFijaSinCuotasMapDevuelve400() throws Exception {
        Gasto gastoCuota = new Gasto();
        gastoCuota.setConcepto("Cena");
        gastoCuota.setMonto(100.0);
        gastoCuota.setTipoReparto(TipoReparto.CUOTA_FIJA);
        gastoCuota.setCuotasMap(null);

        when(gastoService.crear(any(Gasto.class), isNull()))
                .thenThrow(new Exception("Debes indicar las cuotas de cada participante"));

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/gastos/crear")
                .file("gasto", objectMapper.writeValueAsBytes(gastoCuota)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: Debes indicar las cuotas de cada participante"));
    }

    // ==========================================
    // POST con foto + tipoReparto desigual
    // ==========================================

    @Test
    void testCrearConTicketYCuotaFijaExito() throws Exception {
        Gasto gastoCuota = new Gasto();
        gastoCuota.setConcepto("Cena con ticket");
        gastoCuota.setMonto(100.0);
        gastoCuota.setTipoReparto(TipoReparto.CUOTA_FIJA);
        gastoCuota.setTicketUrl("/tickets/factura.jpg");

        Map<Long, Double> cuotasMap = new HashMap<>();
        cuotasMap.put(1L, 60.0);
        cuotasMap.put(2L, 40.0);
        gastoCuota.setCuotasMap(cuotasMap);

        when(gastoService.crear(any(Gasto.class), any())).thenReturn(gastoCuota);

        MockMultipartFile foto = new MockMultipartFile(
                "ticket", "factura.jpg", "image/jpeg", "bytes".getBytes());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/gastos/crear")
                .file("gasto", objectMapper.writeValueAsBytes(gastoCuota))
                .file(foto))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoReparto").value("CUOTA_FIJA"))
                .andExpect(jsonPath("$.ticketUrl").value("/tickets/factura.jpg"));
    }
}