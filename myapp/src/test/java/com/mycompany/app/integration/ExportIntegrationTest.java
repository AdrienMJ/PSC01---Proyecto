package com.mycompany.app.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

import com.mycompany.app.entity.*;
import com.mycompany.app.repository.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:export-it;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "app.uploads.dir=./build/test-uploads"
})
class ExportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private GrupoRepository grupoRepository;

    @Autowired
    private GastoRepository gastoRepository;

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Grupo grupo;

    @BeforeEach
    void limpiarEntorno() {
        limpiarBaseDatos();

        Usuario u1 = usuarioRepository.save(new Usuario("Adrien", "adrien@test.com", "1234"));
        Usuario u2 = usuarioRepository.save(new Usuario("Prueba", "prueba@test.com", "1234"));

        grupo = new Grupo("Viaje Test", Moneda.EURO);
        grupo.setIdCreador(u1.getId());
        grupo.getMiembros().add(u1);
        grupo.getMiembros().add(u2);
        grupo = grupoRepository.save(grupo);

        Gasto gasto = new Gasto();
        gasto.setConcepto("Cena");
        gasto.setMonto(50.0);
        gasto.setMoneda(Moneda.EURO);
        gasto.setPagador(u1);
        gasto.setGrupo(grupo);
        gasto.setParticipantes(List.of(u1, u2));
        gasto.setRepartoGeneral(false);
        gastoRepository.save(gasto);

        Pago pago = new Pago(25.0, u2, u1, grupo);
        pagoRepository.save(pago);
    }

    @Test
    void testExportarPDF_devuelvePDFValido() throws Exception {
        mockMvc.perform(get("/api/export/grupo/{id}/pdf", grupo.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("resumen_grupo_" + grupo.getId() + ".pdf")));
    }

    @Test
    void testExportarCSV_devuelveCSVValido() throws Exception {
        mockMvc.perform(get("/api/export/grupo/{id}/csv", grupo.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv"))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("resumen_grupo_" + grupo.getId() + ".csv")));
    }

    @Test
    void testExportarCSV_contieneGastosYPagos() throws Exception {
        String csv = mockMvc.perform(get("/api/export/grupo/{id}/csv", grupo.getId()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assert csv.contains("GASTOS");
        assert csv.contains("Cena");
        assert csv.contains("Adrien");
        assert csv.contains("PAGOS");
        assert csv.contains("Prueba");
    }

    @Test
    void testExportarPDF_grupoInexistente() throws Exception {
        mockMvc.perform(get("/api/export/grupo/99999/pdf"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testExportarCSV_grupoSinGastos() throws Exception {
        Usuario u3 = usuarioRepository.save(new Usuario("Solo", "solo@test.com", "1234"));
        Grupo grupoVacio = new Grupo("Vacío", Moneda.EURO);
        grupoVacio.setIdCreador(u3.getId());
        grupoVacio.getMiembros().add(u3);
        grupoVacio = grupoRepository.save(grupoVacio);

        mockMvc.perform(get("/api/export/grupo/{id}/csv", grupoVacio.getId()))
                .andExpect(status().isOk());
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