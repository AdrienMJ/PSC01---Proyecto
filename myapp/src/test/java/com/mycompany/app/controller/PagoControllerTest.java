package com.mycompany.app.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.app.dto.NotificacionPagoDTO;
import com.mycompany.app.entity.Pago;
import com.mycompany.app.service.PagoService;

@WebMvcTest(PagoController.class)
public class PagoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PagoService pagoService;

    @Autowired
    private ObjectMapper objectMapper;

    private Pago pagoBase;

    @BeforeEach
    void setUp() {
        pagoBase = new Pago();
        pagoBase.setMonto(50.0);
    }

    // ==========================================
    // POST /api/pagos/registrar
    // ==========================================
    @Test
    void testRegistrarPagoExito() throws Exception {
        when(pagoService.registrarPago(any(Pago.class))).thenReturn(pagoBase);

        mockMvc.perform(post("/api/pagos/registrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pagoBase)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monto").value(50.0));
    }

    @Test
    void testRegistrarPagoError() throws Exception {
        when(pagoService.registrarPago(any(Pago.class)))
                .thenThrow(new Exception("Monto inválido o pagador nulo"));

        mockMvc.perform(post("/api/pagos/registrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pagoBase)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: Monto inválido o pagador nulo"));
    }

    // ==========================================
    // GET /api/pagos/grupo/{grupoId}
    // ==========================================
    @Test
    void testHistorialPorGrupoExito() throws Exception {
        List<Pago> listaPagos = Arrays.asList(pagoBase);
        when(pagoService.obtenerHistorialPorGrupo(10L)).thenReturn(listaPagos);

        mockMvc.perform(get("/api/pagos/grupo/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].monto").value(50.0));
    }

    @Test
    void testHistorialPorGrupoError() throws Exception {
        when(pagoService.obtenerHistorialPorGrupo(anyLong()))
                .thenThrow(new Exception("El grupo no existe"));

        mockMvc.perform(get("/api/pagos/grupo/99"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: El grupo no existe"));
    }

    // ==========================================
    // GET /api/pagos/usuario/{usuarioId}
    // ==========================================
    @Test
    void testPagosPorUsuarioExito() throws Exception {
        List<Pago> listaPagos = Arrays.asList(pagoBase);
        when(pagoService.obtenerPagosPorUsuario(1L)).thenReturn(listaPagos);

        mockMvc.perform(get("/api/pagos/usuario/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].monto").value(50.0));
    }

    @Test
    void testPagosPorUsuarioError() throws Exception {
        when(pagoService.obtenerPagosPorUsuario(any(Long.class)))
                .thenThrow(new RuntimeException("Usuario no encontrado"));

        mockMvc.perform(get("/api/pagos/usuario/99"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: Usuario no encontrado"));
    }

    // ==========================================
    // GET /api/pagos/receptor/{userId}/pendientes
    // ==========================================
    @Test
    void testPendientesConfirmacionExito() throws Exception {
        NotificacionPagoDTO dto = new NotificacionPagoDTO(1L, 10L, "Alice", 5L, "Grupo Test", "EURO", 30.0, null);
        when(pagoService.obtenerPendientesConfirmacion(2L)).thenReturn(Arrays.asList(dto));

        mockMvc.perform(get("/api/pagos/receptor/2/pendientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].monto").value(30.0))
                .andExpect(jsonPath("$[0].pagadorUsername").value("Alice"));
    }

    @Test
    void testPendientesConfirmacionError() throws Exception {
        when(pagoService.obtenerPendientesConfirmacion(anyLong()))
                .thenThrow(new RuntimeException("Error al obtener pendientes"));

        mockMvc.perform(get("/api/pagos/receptor/99/pendientes"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: Error al obtener pendientes"));
    }

    // ==========================================
    // POST /api/pagos/{id}/confirmar?receptorId=X
    // ==========================================
    @Test
    void testConfirmarPagoExito() throws Exception {
        Pago pagoConfirmado = new Pago();
        pagoConfirmado.setConfirmado(true);
        when(pagoService.confirmarPago(eq(1L), eq(2L))).thenReturn(pagoConfirmado);

        mockMvc.perform(post("/api/pagos/1/confirmar")
                .param("receptorId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmado").value(true));
    }

    @Test
    void testConfirmarPagoReceptorIncorrecto() throws Exception {
        when(pagoService.confirmarPago(anyLong(), anyLong()))
                .thenThrow(new Exception("Solo el receptor puede confirmar este pago"));

        mockMvc.perform(post("/api/pagos/1/confirmar")
                .param("receptorId", "99"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: Solo el receptor puede confirmar este pago"));
    }

    @Test
    void testConfirmarPagoNoEncontrado() throws Exception {
        when(pagoService.confirmarPago(anyLong(), anyLong()))
                .thenThrow(new Exception("Pago no encontrado"));

        mockMvc.perform(post("/api/pagos/99/confirmar")
                .param("receptorId", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: Pago no encontrado"));
    }

    // ==========================================
    // POST /api/pagos/{id}/rechazar?receptorId=X
    // ==========================================
    @Test
    void testRechazarPagoExito() throws Exception {
        mockMvc.perform(post("/api/pagos/1/rechazar")
                .param("receptorId", "2"))
                .andExpect(status().isOk())
                .andExpect(content().string("Pago rechazado correctamente"));
    }

    @Test
    void testRechazarPagoYaConfirmado() throws Exception {
        doThrow(new Exception("No se puede rechazar un pago ya confirmado"))
                .when(pagoService).rechazarPago(eq(1L), eq(2L));

        mockMvc.perform(post("/api/pagos/1/rechazar")
                .param("receptorId", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: No se puede rechazar un pago ya confirmado"));
    }
}
