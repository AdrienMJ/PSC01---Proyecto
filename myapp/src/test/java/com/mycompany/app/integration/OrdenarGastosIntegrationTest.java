package com.mycompany.app.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.mycompany.app.entity.CategoriaGasto;
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
        "spring.datasource.url=jdbc:h2:mem:ordenar-gastos-it;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "app.uploads.dir=./build/test-uploads"
})
class OrdenarGastosIntegrationTest {

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
    private Grupo grupo;

    /**
     * Crea 3 gastos con montos distintos y categorías distintas, con 50ms de
     * separación entre cada construcción para que sus fechas (LocalDateTime.now())
     * sean diferentes y los tests de ordenación por fecha sean deterministas.
     *
     * Orden de creación y montos:
     *   gasto1 (más antiguo):  monto=30,  categoría=COMIDA
     *   gasto2 (intermedio):   monto=100, categoría=TRANSPORTE
     *   gasto3 (más reciente): monto=60,  categoría=COMIDA
     */
    @BeforeEach
    void limpiarEntorno() throws InterruptedException {
        limpiarBaseDatos();

        pagador = usuarioRepository.save(new Usuario("Ana", "ana@test.com", "1234"));
        Usuario otro = usuarioRepository.save(new Usuario("Luis", "luis@test.com", "1234"));

        grupo = new Grupo("Grupo Test", Moneda.EURO);
        grupo.setIdCreador(pagador.getId());
        grupo.getMiembros().add(pagador);
        grupo.getMiembros().add(otro);
        grupo = grupoRepository.save(grupo);

        Gasto g1 = new Gasto();
        g1.setConcepto("Desayuno");
        g1.setMonto(30.0);
        g1.setMoneda(Moneda.EURO);
        g1.setPagador(pagador);
        g1.setGrupo(grupo);
        g1.setParticipantes(List.of(pagador, otro));
        g1.setRepartoGeneral(false);
        g1.setCategoria(CategoriaGasto.COMIDA);
        gastoRepository.save(g1);

        Thread.sleep(50);

        Gasto g2 = new Gasto();
        g2.setConcepto("Vuelo");
        g2.setMonto(100.0);
        g2.setMoneda(Moneda.EURO);
        g2.setPagador(pagador);
        g2.setGrupo(grupo);
        g2.setParticipantes(List.of(pagador, otro));
        g2.setRepartoGeneral(false);
        g2.setCategoria(CategoriaGasto.TRANSPORTE);
        gastoRepository.save(g2);

        Thread.sleep(50);

        Gasto g3 = new Gasto();
        g3.setConcepto("Cena");
        g3.setMonto(60.0);
        g3.setMoneda(Moneda.EURO);
        g3.setPagador(pagador);
        g3.setGrupo(grupo);
        g3.setParticipantes(List.of(pagador, otro));
        g3.setRepartoGeneral(false);
        g3.setCategoria(CategoriaGasto.COMIDA);
        gastoRepository.save(g3);
    }

    @Test
    void testListarGastos_ordenPorFechaDescEsElDefault() throws Exception {
        // Sin parámetros → fecha DESC: más reciente primero (Cena=60, Vuelo=100, Desayuno=30)
        mockMvc.perform(get("/api/gastos/grupo/{grupoId}", grupo.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].monto").value(60.0))
                .andExpect(jsonPath("$[1].monto").value(100.0))
                .andExpect(jsonPath("$[2].monto").value(30.0));
    }

    @Test
    void testListarGastos_ordenPorFechaAsc() throws Exception {
        // fecha ASC → más antiguo primero (Desayuno=30, Vuelo=100, Cena=60)
        mockMvc.perform(get("/api/gastos/grupo/{grupoId}", grupo.getId())
                .param("ordenar", "fecha")
                .param("direccion", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].monto").value(30.0))
                .andExpect(jsonPath("$[1].monto").value(100.0))
                .andExpect(jsonPath("$[2].monto").value(60.0));
    }

    @Test
    void testListarGastos_ordenPorMontoAsc() throws Exception {
        // monto ASC → menor a mayor (30, 60, 100)
        mockMvc.perform(get("/api/gastos/grupo/{grupoId}", grupo.getId())
                .param("ordenar", "monto")
                .param("direccion", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].monto").value(30.0))
                .andExpect(jsonPath("$[1].monto").value(60.0))
                .andExpect(jsonPath("$[2].monto").value(100.0));
    }

    @Test
    void testListarGastos_ordenPorMontoDesc() throws Exception {
        // monto DESC → mayor a menor (100, 60, 30)
        mockMvc.perform(get("/api/gastos/grupo/{grupoId}", grupo.getId())
                .param("ordenar", "monto")
                .param("direccion", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].monto").value(100.0))
                .andExpect(jsonPath("$[1].monto").value(60.0))
                .andExpect(jsonPath("$[2].monto").value(30.0));
    }

    @Test
    void testListarGastos_filtrarPorCategoriaComida() throws Exception {
        // COMIDA: Desayuno(30) y Cena(60) → 2 resultados
        mockMvc.perform(get("/api/gastos/grupo/{grupoId}", grupo.getId())
                .param("categoria", "COMIDA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void testListarGastos_filtrarPorCategoriaTransporte() throws Exception {
        // TRANSPORTE: solo Vuelo(100) → 1 resultado
        mockMvc.perform(get("/api/gastos/grupo/{grupoId}", grupo.getId())
                .param("categoria", "TRANSPORTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].monto").value(100.0));
    }

    @Test
    void testListarGastos_filtrarPorCategoriaTodasDevuelveTodos() throws Exception {
        mockMvc.perform(get("/api/gastos/grupo/{grupoId}", grupo.getId())
                .param("categoria", "TODAS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void testListarGastos_grupoSinGastos() throws Exception {
        Grupo grupoVacio = new Grupo("Vacío", Moneda.EURO);
        grupoVacio.setIdCreador(pagador.getId());
        grupoVacio.getMiembros().add(pagador);
        grupoVacio = grupoRepository.save(grupoVacio);

        mockMvc.perform(get("/api/gastos/grupo/{grupoId}", grupoVacio.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
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
