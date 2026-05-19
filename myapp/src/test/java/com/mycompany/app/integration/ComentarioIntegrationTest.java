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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.app.entity.*;
import com.mycompany.app.repository.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:comentario-it;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "app.uploads.dir=./build/test-uploads"
})
class ComentarioIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private GrupoRepository grupoRepository;

    @Autowired
    private GastoRepository gastoRepository;

    @Autowired
    private ComentarioRepository comentarioRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private Usuario usuario;
    private Usuario otroUsuario;
    private Gasto gasto;

    @BeforeEach
    void limpiarEntorno() {
        limpiarBaseDatos();

        usuario = usuarioRepository.save(new Usuario("Adrien", "adrien@test.com", "1234"));
        otroUsuario = usuarioRepository.save(new Usuario("Prueba", "prueba@test.com", "1234"));

        Grupo grupo = new Grupo("Viaje Test", Moneda.EURO);
        grupo.setIdCreador(usuario.getId());
        grupo.getMiembros().add(usuario);
        grupo.getMiembros().add(otroUsuario);
        grupo = grupoRepository.save(grupo);

        gasto = new Gasto();
        gasto.setConcepto("Cena");
        gasto.setMonto(50.0);
        gasto.setMoneda(Moneda.EURO);
        gasto.setPagador(usuario);
        gasto.setGrupo(grupo);
        gasto.setParticipantes(List.of(usuario, otroUsuario));
        gasto.setRepartoGeneral(false);
        gasto = gastoRepository.save(gasto);
    }

    @Test
    void testAgregarComentario_exitoso() throws Exception {
        String json = "{\"usuarioId\": " + usuario.getId() + ", \"texto\": \"Buena cena!\"}";

        mockMvc.perform(post("/api/comentarios/gasto/{id}", gasto.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.texto").value("Buena cena!"))
                .andExpect(jsonPath("$.autor.username").value("Adrien"));
    }

    @Test
    void testAgregarComentario_textoVacio() throws Exception {
        String json = "{\"usuarioId\": " + usuario.getId() + ", \"texto\": \"\"}";

        mockMvc.perform(post("/api/comentarios/gasto/{id}", gasto.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testListarComentarios_exitoso() throws Exception {
        // Crear comentario primero
        String json = "{\"usuarioId\": " + usuario.getId() + ", \"texto\": \"Primer comentario\"}";
        mockMvc.perform(post("/api/comentarios/gasto/{id}", gasto.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));

        mockMvc.perform(get("/api/comentarios/gasto/{id}", gasto.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].texto").value("Primer comentario"));
    }

    @Test
    void testListarComentarios_sinComentarios() throws Exception {
        mockMvc.perform(get("/api/comentarios/gasto/{id}", gasto.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testEliminarComentario_exitoso() throws Exception {
        // Crear comentario
        String json = "{\"usuarioId\": " + usuario.getId() + ", \"texto\": \"A eliminar\"}";
        String response = mockMvc.perform(post("/api/comentarios/gasto/{id}", gasto.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andReturn().getResponse().getContentAsString();

        JsonNode node = objectMapper.readTree(response);
        Long comentarioId = node.get("id").asLong();

        // Eliminar
        mockMvc.perform(delete("/api/comentarios/{id}", comentarioId)
                .param("usuarioId", usuario.getId().toString()))
                .andExpect(status().isOk());

        // Verificar que ya no existe
        mockMvc.perform(get("/api/comentarios/gasto/{id}", gasto.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testEliminarComentario_otroUsuarioNoPuede() throws Exception {
        // Crear comentario con usuario 1
        String json = "{\"usuarioId\": " + usuario.getId() + ", \"texto\": \"Mi comentario\"}";
        String response = mockMvc.perform(post("/api/comentarios/gasto/{id}", gasto.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andReturn().getResponse().getContentAsString();

        JsonNode node = objectMapper.readTree(response);
        Long comentarioId = node.get("id").asLong();

        // Intentar eliminar con usuario 2
        mockMvc.perform(delete("/api/comentarios/{id}", comentarioId)
                .param("usuarioId", otroUsuario.getId().toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUsuarioNoMiembroNoPuedeComentar() throws Exception {
        Usuario externo = usuarioRepository.save(new Usuario("Externo", "externo@test.com", "1234"));

        String json = "{\"usuarioId\": " + externo.getId() + ", \"texto\": \"No debería poder\"}";

        mockMvc.perform(post("/api/comentarios/gasto/{id}", gasto.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
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