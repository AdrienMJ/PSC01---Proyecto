package com.mycompany.app.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
        "spring.datasource.url=jdbc:h2:mem:pagado-it;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "app.uploads.dir=./build/test-uploads"
})
class MarcarGastoPagadoIntegrationTest {

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

    private Usuario pagador;
    private Usuario deudor;
    private Grupo grupo;
    private Gasto gasto;

    @BeforeEach
    void limpiarEntorno() {
        limpiarBaseDatos();

        pagador = usuarioRepository.save(new Usuario("Ana", "ana@test.com", "1234"));
        deudor = usuarioRepository.save(new Usuario("Luis", "luis@test.com", "1234"));

        grupo = new Grupo("Grupo Test", Moneda.EURO);
        grupo.setIdCreador(pagador.getId());
        grupo.getMiembros().add(pagador);
        grupo.getMiembros().add(deudor);
        grupo = grupoRepository.save(grupo);

        gasto = new Gasto();
        gasto.setConcepto("Hotel");
        gasto.setMonto(100.0);
        gasto.setMoneda(Moneda.EURO);
        gasto.setPagador(pagador);
        gasto.setGrupo(grupo);
        gasto.setParticipantes(List.of(pagador, deudor));
        gasto.setRepartoGeneral(false);
        gasto = gastoRepository.save(gasto);
    }

    @Test
    void testMarcarGastoPagado_exitoso() throws Exception {
        mockMvc.perform(put("/api/gastos/{id}/pagado/{usuarioId}", gasto.getId(), deudor.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagado").value(true));

        assertTrue(gastoRepository.findById(gasto.getId()).orElseThrow().isPagado());
    }

    @Test
    void testMarcarGastoPagado_actualizaBalanceDeudorAEquilibrado() throws Exception {
        // Antes: Ana (index 0) acreedor, Luis (index 1) deudor (orden alfabético)
        mockMvc.perform(get("/api/gastos/grupo/{grupoId}/resumen", grupo.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balances[1].username").value("Luis"))
                .andExpect(jsonPath("$.balances[1].estado").value("debe"));

        mockMvc.perform(put("/api/gastos/{id}/pagado/{usuarioId}", gasto.getId(), deudor.getId()))
                .andExpect(status().isOk());

        // Después: el gasto pagado se excluye del cálculo, ambos en equilibrio
        mockMvc.perform(get("/api/gastos/grupo/{grupoId}/resumen", grupo.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balances[1].username").value("Luis"))
                .andExpect(jsonPath("$.balances[1].estado").value("equilibrado"))
                .andExpect(jsonPath("$.balances[1].balance").value(0.0));
    }

    @Test
    void testMarcarGastoPagado_pagadorNoPuedeMarcarSuPropio() throws Exception {
        mockMvc.perform(put("/api/gastos/{id}/pagado/{usuarioId}", gasto.getId(), pagador.getId()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testMarcarGastoPagado_gastoYaPagadoFalla() throws Exception {
        mockMvc.perform(put("/api/gastos/{id}/pagado/{usuarioId}", gasto.getId(), deudor.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/gastos/{id}/pagado/{usuarioId}", gasto.getId(), deudor.getId()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testMarcarGastoPagado_usuarioNoMiembroFalla() throws Exception {
        Usuario externo = usuarioRepository.save(new Usuario("Externo", "externo@test.com", "1234"));

        mockMvc.perform(put("/api/gastos/{id}/pagado/{usuarioId}", gasto.getId(), externo.getId()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testMarcarGastoPagado_gastoInexistenteFalla() throws Exception {
        mockMvc.perform(put("/api/gastos/{id}/pagado/{usuarioId}", 9999L, deudor.getId()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGastoPagadoNoApareceTotalGastado() throws Exception {
        // Antes: totalGastado incluye el gasto
        mockMvc.perform(get("/api/gastos/grupo/{grupoId}/resumen", grupo.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalGastado").value(100.0));

        mockMvc.perform(put("/api/gastos/{id}/pagado/{usuarioId}", gasto.getId(), deudor.getId()))
                .andExpect(status().isOk());

        // Después: totalGastado sigue contando (solo los balances cambian)
        mockMvc.perform(get("/api/gastos/grupo/{grupoId}/resumen", grupo.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balances[0].estado").value("equilibrado"))
                .andExpect(jsonPath("$.balances[1].estado").value("equilibrado"));
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
