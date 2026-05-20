package com.mycompany.app.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import com.mycompany.app.entity.Moneda;
import com.mycompany.app.entity.Usuario;
import com.mycompany.app.repository.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:moneda-it;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "app.uploads.dir=./build/test-uploads"
})
class MonedaPredeterminadaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Usuario usuario;

    @BeforeEach
    void limpiarEntorno() {
        limpiarBaseDatos();
        usuario = usuarioRepository.save(new Usuario("Carlos", "carlos@test.com", "1234"));
    }

    @Test
    void testEstablecerMonedaEuro_exitoso() throws Exception {
        mockMvc.perform(put("/api/usuarios/{id}/moneda-predeterminada", usuario.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"moneda\": \"EURO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monedaPredeterminada").value("EURO"));

        Usuario actualizado = usuarioRepository.findById(usuario.getId()).orElseThrow();
        assertEquals(Moneda.EURO, actualizado.getMonedaPredeterminada());
    }

    @Test
    void testEstablecerMonedaDolar_exitoso() throws Exception {
        mockMvc.perform(put("/api/usuarios/{id}/moneda-predeterminada", usuario.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"moneda\": \"DOLAR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monedaPredeterminada").value("DOLAR"));

        Usuario actualizado = usuarioRepository.findById(usuario.getId()).orElseThrow();
        assertEquals(Moneda.DOLAR, actualizado.getMonedaPredeterminada());
    }

    @Test
    void testCambiarMonedaVariasVeces_persisteUltimoCambio() throws Exception {
        mockMvc.perform(put("/api/usuarios/{id}/moneda-predeterminada", usuario.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"moneda\": \"DOLAR\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/usuarios/{id}/moneda-predeterminada", usuario.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"moneda\": \"LIBRA\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/usuarios/{id}/moneda-predeterminada", usuario.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"moneda\": \"EURO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monedaPredeterminada").value("EURO"));

        Usuario actualizado = usuarioRepository.findById(usuario.getId()).orElseThrow();
        assertEquals(Moneda.EURO, actualizado.getMonedaPredeterminada());
    }

    @Test
    void testEstablecerMonedaInvalida_falla() throws Exception {
        mockMvc.perform(put("/api/usuarios/{id}/moneda-predeterminada", usuario.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"moneda\": \"BITCOIN\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testEstablecerMonedaUsuarioInexistente_falla() throws Exception {
        mockMvc.perform(put("/api/usuarios/{id}/moneda-predeterminada", 9999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"moneda\": \"EURO\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testMonedaPredeterminadaPorDefecto_esEuro() throws Exception {
        mockMvc.perform(get("/api/usuarios/{id}/perfil", usuario.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monedaPredeterminada").value("EURO"));
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
