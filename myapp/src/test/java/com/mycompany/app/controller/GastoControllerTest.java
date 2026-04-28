package com.mycompany.app.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.app.dto.ResumenGrupoDTO;
import com.mycompany.app.entity.Gasto;
import com.mycompany.app.entity.Grupo;
import com.mycompany.app.entity.Moneda;
import com.mycompany.app.entity.Usuario;
import com.mycompany.app.repository.GastoRepository;
import com.mycompany.app.service.GastoService;
import com.mycompany.app.service.GrupoService;

@WebMvcTest(GastoController.class)
public class GastoControllerTest {

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
        gastoBase.setMonto(150.0);
        gastoBase.setConcepto("Cena de prueba");
    }

    // ==========================================
    // POST /api/gastos/crear
    // ==========================================
    @Test
    void testCrearExito() throws Exception {
        when(gastoService.crear(any(Gasto.class))).thenReturn(gastoBase);

        mockMvc.perform(post("/api/gastos/crear")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(gastoBase)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monto").value(150.0));
    }

    @Test
    void testCrearError() throws Exception {
        when(gastoService.crear(any(Gasto.class))).thenThrow(new Exception("Monto invalido"));

        mockMvc.perform(post("/api/gastos/crear")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(gastoBase)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: Monto invalido"));
    }

    // ==========================================
    // GET /api/gastos/grupo/{grupoId}
    // ==========================================
    @Test
    void testListarPorGrupoExito() throws Exception {
        List<Gasto> listaGastos = Arrays.asList(gastoBase);
        when(gastoService.listarPorGrupo(eq(1L), anyString(), anyString(), anyString())).thenReturn(listaGastos);

        mockMvc.perform(get("/api/gastos/grupo/1")
                .param("ordenar", "monto")
                .param("direccion", "asc")
                .param("categoria", "COMIDA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].monto").value(150.0));
    }

    @Test
    void testListarPorGrupoError() throws Exception {
        when(gastoService.listarPorGrupo(anyLong(), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Grupo no existe"));

        mockMvc.perform(get("/api/gastos/grupo/99"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: Grupo no existe"));
    }

    // ==========================================
    // PUT /api/gastos/{id} (Actualizar monto)
    // ==========================================
    @Test
    void testActualizarMontoExito() throws Exception {
        Gasto gastoExistente = new Gasto();
        gastoExistente.setMonto(50.0);

        Gasto gastoActualizado = new Gasto();
        gastoActualizado.setMonto(200.0); // Monto > 0

        when(gastoService.obtenerPorId(1L)).thenReturn(gastoExistente);
        when(gastoRepository.save(any(Gasto.class))).thenReturn(gastoActualizado);

        mockMvc.perform(put("/api/gastos/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(gastoActualizado)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monto").value(200.0));
    }

    @Test
    void testActualizarMontoInvalido() throws Exception {
        Gasto gastoMontoCero = new Gasto();
        gastoMontoCero.setMonto(0.0); // Provoca el retorno badRequest del IF

        when(gastoService.obtenerPorId(1L)).thenReturn(gastoBase);

        mockMvc.perform(put("/api/gastos/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(gastoMontoCero)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("El monto debe ser mayor que 0"));
    }

    @Test
    void testActualizarMontoErrorServicio() throws Exception {
        Gasto gasto = new Gasto(); gasto.setMonto(100.0);
        when(gastoService.obtenerPorId(99L)).thenThrow(new Exception("Gasto no encontrado"));

        mockMvc.perform(put("/api/gastos/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(gasto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: Gasto no encontrado"));
    }

    // ==========================================
    // PUT /api/gastos/{id}/pagado/{usuarioId}
    // ==========================================
    @Test
    void testMarcarComoPagadoExito() throws Exception {
        Gasto gastoPagado = new Gasto();
        gastoPagado.setPagado(true);

        when(gastoService.marcarComoPagado(100L, 1L)).thenReturn(gastoPagado);

        mockMvc.perform(put("/api/gastos/100/pagado/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagado").value(true));
    }

    @Test
    void testMarcarComoPagadoError() throws Exception {
        when(gastoService.marcarComoPagado(100L, 1L)).thenThrow(new Exception("No tienes permisos"));

        mockMvc.perform(put("/api/gastos/100/pagado/1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: No tienes permisos"));
    }

    // ==========================================
    // GET /api/gastos/{grupoId}/usuarios
    // ==========================================
    @Test
    void testObtenerUsuariosExito() throws Exception {
        Usuario user = new Usuario(); user.setUsername("TestUser");
        Grupo grupo = new Grupo(); grupo.getMiembros().add(user);

        when(grupoService.obtenerGrupoPorId(1L)).thenReturn(grupo);

        mockMvc.perform(get("/api/gastos/1/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("TestUser"));
    }

    @Test
    void testObtenerUsuariosError() throws Exception {
        when(grupoService.obtenerGrupoPorId(99L)).thenThrow(new RuntimeException("Grupo fantasma"));

        mockMvc.perform(get("/api/gastos/99/usuarios"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Grupo fantasma"));
    }

    // ==========================================
    // GET /api/gastos/categorias
    // ==========================================
    @Test
    void testListarCategorias() throws Exception {
        mockMvc.perform(get("/api/gastos/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").isNotEmpty());
    }

    // ==========================================
    // GET /api/gastos/grupo/{grupoId}/resumen
    // ==========================================
    @Test
    void testObtenerResumenExito() throws Exception {
        ResumenGrupoDTO resumen = new ResumenGrupoDTO(500.0, Collections.emptyList(), Collections.emptyList());
        when(gastoService.obtenerResumenGrupo(1L)).thenReturn(resumen);

        mockMvc.perform(get("/api/gastos/grupo/1/resumen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalGastado").value(500.0));
    }

    @Test
    void testObtenerResumenError() throws Exception {
        when(gastoService.obtenerResumenGrupo(99L)).thenThrow(new Exception("Fallo al calcular"));

        mockMvc.perform(get("/api/gastos/grupo/99/resumen"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: Fallo al calcular"));
    }

    // ==========================================
    // GET /api/gastos/tasas/{monedaBase}
    // ==========================================
    @Test
    void testObtenerTasasExito() throws Exception {
        Map<String, Object> tasasMock = new HashMap<>();
        tasasMock.put("USD", 1.08);
        when(gastoService.obtenerTodasLasTasas(Moneda.EURO)).thenReturn(tasasMock);

        mockMvc.perform(get("/api/gastos/tasas/EURO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.USD").value(1.08));
    }

    @Test
    void testObtenerTasasMapaVacioDevuelve500() throws Exception {
        when(gastoService.obtenerTodasLasTasas(Moneda.EURO)).thenReturn(new HashMap<>());

        mockMvc.perform(get("/api/gastos/tasas/EURO"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testObtenerTasasMonedaInvalidaDevuelve400() throws Exception {
        // Al intentar convertir "INVENTO" a Moneda.valueOf(), lanzará IllegalArgumentException
        mockMvc.perform(get("/api/gastos/tasas/INVENTO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testObtenerTasasErrorGenericoDevuelve500() throws Exception {
        when(gastoService.obtenerTodasLasTasas(any())).thenThrow(new RuntimeException("API caída"));

        mockMvc.perform(get("/api/gastos/tasas/EURO"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").exists());
    }

    // ==========================================
    // GET /api/gastos/usuario/{userId}/resumen-por-grupo
    // ==========================================
    @Test
    void testResumenPorGrupoUsuarioExito() throws Exception {
        Map<String, Object> mapResumen = new HashMap<>();
        mapResumen.put("grupoNombre", "Amigos");
        List<Map<String, Object>> listResumen = Arrays.asList(mapResumen);

        when(gastoService.obtenerResumenPorGrupoParaUsuario(1L)).thenReturn(listResumen);

        mockMvc.perform(get("/api/gastos/usuario/1/resumen-por-grupo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].grupoNombre").value("Amigos"));
    }

    @Test
    void testResumenPorGrupoUsuarioError() throws Exception {
        when(gastoService.obtenerResumenPorGrupoParaUsuario(99L)).thenThrow(new RuntimeException("User no existe"));

        mockMvc.perform(get("/api/gastos/usuario/99/resumen-por-grupo"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: User no existe"));
    }

    // ==========================================
    // DELETE /api/gastos/{id}?usuarioId={usuarioId}
    // ==========================================
    @Test
    void testEliminarGastoExito() throws Exception {
        doNothing().when(gastoService).eliminarGasto(100L, 1L);

        mockMvc.perform(delete("/api/gastos/100")
                .param("usuarioId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Gasto eliminado correctamente"));
    }

    @Test
    void testEliminarGastoError() throws Exception {
        doThrow(new Exception("No es el admin")).when(gastoService).eliminarGasto(100L, 2L);

        mockMvc.perform(delete("/api/gastos/100")
                .param("usuarioId", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: No es el admin"));
    }

    @Test
    void testEliminarGastoErrorGraveThrowable() throws Exception {
        // Simulamos un error grave (hijo de Throwable, no Exception)
        doThrow(new OutOfMemoryError("Memoria llena")).when(gastoService).eliminarGasto(100L, 1L);

        mockMvc.perform(delete("/api/gastos/100")
                .param("usuarioId", "1"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error interno: Memoria llena"));
    }

    // ==========================================
    // PUT /api/gastos/{id}/editar?usuarioId={usuarioId}
    // ==========================================
    @Test
    void testEditarGastoExito() throws Exception {
        Gasto gastoEditado = new Gasto();
        gastoEditado.setConcepto("Nuevo Concepto");

        when(gastoService.editarGasto(eq(100L), eq(1L), any(Gasto.class))).thenReturn(gastoEditado);

        mockMvc.perform(put("/api/gastos/100/editar")
                .param("usuarioId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(gastoBase)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.concepto").value("Nuevo Concepto"));
    }

    @Test
    void testEditarGastoError() throws Exception {
        when(gastoService.editarGasto(eq(100L), eq(2L), any(Gasto.class)))
                .thenThrow(new Exception("Fallo al editar"));

        mockMvc.perform(put("/api/gastos/100/editar")
                .param("usuarioId", "2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(gastoBase)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: Fallo al editar"));
    }
}