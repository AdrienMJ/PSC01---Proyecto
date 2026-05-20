package com.mycompany.app.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.mycompany.app.entity.Gasto;
import com.mycompany.app.entity.Grupo;
import com.mycompany.app.entity.Moneda;
import com.mycompany.app.entity.Usuario;
import com.mycompany.app.repository.GastoRepository;
import com.mycompany.app.repository.GrupoRepository;
import com.mycompany.app.repository.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:editar-gasto-it;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "app.uploads.dir=./build/test-uploads"
})
class EliminarEditarGastoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private GrupoRepository grupoRepository;

    @Autowired
    private GastoRepository gastoRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Usuario admin;
    private Usuario miembro;
    private Grupo grupo;
    private Gasto gasto;

    @BeforeEach
    void limpiarEntorno() {
        limpiarBaseDatos();

        admin = usuarioRepository.save(new Usuario("Admin", "admin@test.com", "1234"));
        miembro = usuarioRepository.save(new Usuario("Luis", "luis@test.com", "1234"));

        grupo = new Grupo("Grupo Test", Moneda.EURO);
        grupo.setIdCreador(admin.getId());
        grupo.getMiembros().add(admin);
        grupo.getMiembros().add(miembro);
        grupo = grupoRepository.save(grupo);

        gasto = new Gasto();
        gasto.setConcepto("Cena original");
        gasto.setMonto(120.0);
        gasto.setMoneda(Moneda.EURO);
        gasto.setPagador(admin);
        gasto.setGrupo(grupo);
        gasto.setParticipantes(List.of(admin, miembro));
        gasto.setRepartoGeneral(false);
        gasto = gastoRepository.save(gasto);
    }

    // ── ELIMINAR ──────────────────────────────────────────────────────────────

    @Test
    void testEliminarGasto_adminExitoso() throws Exception {
        mockMvc.perform(delete("/api/gastos/{id}", gasto.getId())
                .param("usuarioId", admin.getId().toString()))
                .andExpect(status().isOk());

        assertFalse(gastoRepository.existsById(gasto.getId()));
    }

    @Test
    void testEliminarGasto_miembroNoAdminFalla() throws Exception {
        mockMvc.perform(delete("/api/gastos/{id}", gasto.getId())
                .param("usuarioId", miembro.getId().toString()))
                .andExpect(status().isBadRequest());

        assertTrue(gastoRepository.existsById(gasto.getId()));
    }

    @Test
    void testEliminarGasto_gastoInexistenteFalla() throws Exception {
        mockMvc.perform(delete("/api/gastos/{id}", 9999L)
                .param("usuarioId", admin.getId().toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testEliminarGasto_listaDeGastosSeActualiza() throws Exception {
        mockMvc.perform(delete("/api/gastos/{id}", gasto.getId())
                .param("usuarioId", admin.getId().toString()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/gastos/grupo/{grupoId}", grupo.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ── EDITAR ────────────────────────────────────────────────────────────────

    @Test
    void testEditarGasto_adminCambiaConcepto() throws Exception {
        String body = "{\"concepto\": \"Cena corregida\"}";

        mockMvc.perform(put("/api/gastos/{id}/editar", gasto.getId())
                .param("usuarioId", admin.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.concepto").value("Cena corregida"));

        assertEquals("Cena corregida", gastoRepository.findById(gasto.getId()).orElseThrow().getConcepto());
    }

    @Test
    void testEditarGasto_adminCambiaMonto() throws Exception {
        String body = "{\"monto\": 200.0}";

        mockMvc.perform(put("/api/gastos/{id}/editar", gasto.getId())
                .param("usuarioId", admin.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monto").value(200.0));

        assertEquals(200.0, gastoRepository.findById(gasto.getId()).orElseThrow().getMonto());
    }

    @Test
    void testEditarGasto_adminCambiaCategoria() throws Exception {
        String body = "{\"categoria\": \"COMIDA\"}";

        mockMvc.perform(put("/api/gastos/{id}/editar", gasto.getId())
                .param("usuarioId", admin.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoria").value("COMIDA"));
    }

    @Test
    void testEditarGasto_adminCambiaVariosCamposALaVez() throws Exception {
        String body = "{\"concepto\": \"Hotel corregido\", \"monto\": 90.0, \"categoria\": \"VIVIENDA\"}";

        mockMvc.perform(put("/api/gastos/{id}/editar", gasto.getId())
                .param("usuarioId", admin.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.concepto").value("Hotel corregido"))
                .andExpect(jsonPath("$.monto").value(90.0))
                .andExpect(jsonPath("$.categoria").value("VIVIENDA"));
    }

    @Test
    void testEditarGasto_montoNegativoEsIgnoradoYMontoNoCambia() throws Exception {
        String body = "{\"monto\": -50.0}";

        mockMvc.perform(put("/api/gastos/{id}/editar", gasto.getId())
                .param("usuarioId", admin.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk());

        assertEquals(120.0, gastoRepository.findById(gasto.getId()).orElseThrow().getMonto());
    }

    @Test
    void testEditarGasto_miembroNoAdminFalla() throws Exception {
        String body = "{\"concepto\": \"Intento no autorizado\"}";

        mockMvc.perform(put("/api/gastos/{id}/editar", gasto.getId())
                .param("usuarioId", miembro.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());

        assertEquals("Cena original", gastoRepository.findById(gasto.getId()).orElseThrow().getConcepto());
    }

    @Test
    void testEditarGasto_gastoInexistenteFalla() throws Exception {
        String body = "{\"concepto\": \"No existe\"}";

        mockMvc.perform(put("/api/gastos/{id}/editar", 9999L)
                .param("usuarioId", admin.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());
    }

    private void limpiarBaseDatos() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        List<String> tablas = jdbcTemplate.queryForList(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_TYPE = 'BASE TABLE'",
                String.class);
        for (String tabla : tablas) {
            jdbcTemplate.execute("TRUNCATE TABLE " + tabla);
        }
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }
}
