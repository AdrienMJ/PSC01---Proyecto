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

import com.mycompany.app.entity.Grupo;
import com.mycompany.app.entity.Moneda;
import com.mycompany.app.entity.Usuario;
import com.mycompany.app.repository.GrupoRepository;
import com.mycompany.app.repository.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:contactos-grupo-it;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "app.uploads.dir=./build/test-uploads"
})
class AgregarMiembroDesdeContactosIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private GrupoRepository grupoRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Usuario admin;
    private Usuario contacto;
    private Grupo grupo;

    @BeforeEach
    void limpiarEntorno() {
        limpiarBaseDatos();

        admin = usuarioRepository.save(new Usuario("Admin", "admin@test.com", "1234"));
        contacto = usuarioRepository.save(new Usuario("Contacto", "contacto@test.com", "1234"));

        grupo = new Grupo("Grupo Test", Moneda.EURO);
        grupo.setIdCreador(admin.getId());
        grupo.getMiembros().add(admin);
        grupo = grupoRepository.save(grupo);
    }

    @Test
    void testAgregarContactoYInvitarAlGrupo_exitoso() throws Exception {
        String bodyContacto = "{\"email\": \"" + contacto.getEmail() + "\"}";
        mockMvc.perform(post("/api/usuarios/{id}/contactos", admin.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyContacto))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/usuarios/{id}/contactos", admin.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].email").value("contacto@test.com"))
                .andExpect(jsonPath("$[0].username").value("Contacto"));

        String bodyInvitar = "{\"email\": \"" + contacto.getEmail() + "\", \"idUsuarioInvitador\": " + admin.getId() + "}";
        mockMvc.perform(post("/api/grupos/{grupoId}/invitar", grupo.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyInvitar))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/grupos/{grupoId}/usuarios", grupo.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].email", hasItem("contacto@test.com")));
    }

    @Test
    void testListarContactos_devuelveListaVaciaSinContactos() throws Exception {
        mockMvc.perform(get("/api/usuarios/{id}/contactos", admin.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testAgregarContacto_emailInexistenteFalla() throws Exception {
        String body = "{\"email\": \"noexiste@test.com\"}";
        mockMvc.perform(post("/api/usuarios/{id}/contactos", admin.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAgregarContacto_aSiMismoFalla() throws Exception {
        String body = "{\"email\": \"" + admin.getEmail() + "\"}";
        mockMvc.perform(post("/api/usuarios/{id}/contactos", admin.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAgregarMismoContactoDosVeces_falla() throws Exception {
        String body = "{\"email\": \"" + contacto.getEmail() + "\"}";

        mockMvc.perform(post("/api/usuarios/{id}/contactos", admin.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/usuarios/{id}/contactos", admin.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testInvitarContactoQueYaEsMiembro_falla() throws Exception {
        String bodyContacto = "{\"email\": \"" + contacto.getEmail() + "\"}";
        mockMvc.perform(post("/api/usuarios/{id}/contactos", admin.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyContacto));

        String bodyInvitar = "{\"email\": \"" + contacto.getEmail() + "\", \"idUsuarioInvitador\": " + admin.getId() + "}";
        mockMvc.perform(post("/api/grupos/{grupoId}/invitar", grupo.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyInvitar))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/grupos/{grupoId}/invitar", grupo.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyInvitar))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testInvitarDesdeUsuarioNoMiembroDelGrupo_falla() throws Exception {
        Usuario externo = usuarioRepository.save(new Usuario("Externo", "externo@test.com", "1234"));

        String body = "{\"email\": \"" + contacto.getEmail() + "\", \"idUsuarioInvitador\": " + externo.getId() + "}";
        mockMvc.perform(post("/api/grupos/{grupoId}/invitar", grupo.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testEliminarContacto_exitoso() throws Exception {
        String body = "{\"email\": \"" + contacto.getEmail() + "\"}";
        mockMvc.perform(post("/api/usuarios/{id}/contactos", admin.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/usuarios/{id}/contactos/{contactoId}", admin.getId(), contacto.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/usuarios/{id}/contactos", admin.getId()))
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
