package com.mycompany.app.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
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
        "spring.datasource.url=jdbc:h2:mem:usuarios-it;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "app.uploads.dir=./build/test-uploads"
})
class UsuarioIntegrationTest {

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

    @BeforeEach
    void limpiarEntorno() throws IOException {
        limpiarBaseDatos();
        limpiarUploads();
    }

    @Test
    void debeObtenerNotificacionesDeudasDelUsuario() throws Exception {
        Usuario acreedor = usuarioRepository.save(new Usuario("Ana", "ana@test.com", "1234"));
        Usuario deudor = usuarioRepository.save(new Usuario("Luis", "luis@test.com", "1234"));

        Grupo grupo = new Grupo("Viaje a Roma", Moneda.EURO);
        grupo.getMiembros().add(acreedor);
        grupo.getMiembros().add(deudor);
        grupo = grupoRepository.save(grupo);

        Gasto gasto = new Gasto();
        gasto.setConcepto("Hotel");
        gasto.setMonto(100.0);
        gasto.setMoneda(Moneda.EURO);
        gasto.setPagador(acreedor);
        gasto.setGrupo(grupo);
        gasto.setParticipantes(List.of(acreedor, deudor));
        gasto.setRepartoGeneral(false);
        gastoRepository.save(gasto);

        mockMvc.perform(get("/api/usuarios/{id}/notificaciones-deudas", deudor.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].grupoNombre").value("Viaje a Roma"))
                .andExpect(jsonPath("$[0].acreedorUsername").value("Ana"))
                .andExpect(jsonPath("$[0].monto").value(50.0));
    }

    @Test
    void debePermitirActualizarNombreVisibleYFotoPerfil() throws Exception {
        Usuario usuario = usuarioRepository.save(new Usuario("NombreViejo", "perfil@test.com", "1234"));

        MockMultipartFile nombreVisible = new MockMultipartFile(
                "nombreVisible",
                "",
                "text/plain",
                "NombreNuevo".getBytes());
        MockMultipartFile foto = new MockMultipartFile(
                "foto",
                "avatar.png",
                "image/png",
                "contenido-imagen".getBytes());

        String respuesta = mockMvc.perform(multipart("/api/usuarios/{id}/perfil", usuario.getId())
                        .file(nombreVisible)
                        .file(foto)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("NombreNuevo"))
                .andExpect(jsonPath("$.fotoPerfilUrl", startsWith("/perfiles/")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Usuario actualizado = usuarioRepository.findById(usuario.getId()).orElseThrow();
        assertEquals("NombreNuevo", actualizado.getUsername());
        assertTrue(actualizado.getFotoPerfilUrl() != null && actualizado.getFotoPerfilUrl().startsWith("/perfiles/"));

        String rutaRelativa = actualizado.getFotoPerfilUrl().replaceFirst("^/", "");
        Path rutaArchivo = Paths.get("build", "test-uploads", rutaRelativa).normalize();
        assertTrue(Files.exists(rutaArchivo), "La foto de perfil debe guardarse en disco");
        assertTrue(respuesta.contains("NombreNuevo"));
    }

    @Test
    void debeDevolverListaVaciaCuandoNoTieneDeudas() throws Exception {
        Usuario usuario = usuarioRepository.save(new Usuario("SinDeudas", "sin@deudas.com", "1234"));

        mockMvc.perform(get("/api/usuarios/{id}/notificaciones-deudas", usuario.getId()))
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

    private void limpiarUploads() throws IOException {
        Path uploads = Paths.get("build", "test-uploads");
        if (!Files.exists(uploads)) {
            return;
        }

        try (var stream = Files.walk(uploads)) {
            stream.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            throw new RuntimeException("No se pudo limpiar uploads de test", e);
                        }
                    });
        }
    }
}
