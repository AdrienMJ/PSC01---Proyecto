package com.mycompany.app.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.app.dto.NotificacionDeudaDTO;
import com.mycompany.app.entity.Moneda;
import com.mycompany.app.entity.Usuario;
import com.mycompany.app.service.UsuarioService;

@WebMvcTest(UsuarioController.class)
public class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsuarioService usuarioService;

    @Autowired
    private ObjectMapper objectMapper;

    private Usuario usuarioBase;

    @BeforeEach
    void setUp() {
        usuarioBase = new Usuario();
        usuarioBase.setId(1L);
        usuarioBase.setUsername("TestUser");
        usuarioBase.setEmail("test@mail.com");
        usuarioBase.setPassword("password123");
    }

    // ==========================================
    // GET /api/usuarios (Listar todos)
    // ==========================================
    @Test
    void testListarUsuariosExito() throws Exception {
        List<Usuario> usuarios = Arrays.asList(usuarioBase);
        when(usuarioService.listarTodos()).thenReturn(usuarios);

        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("TestUser"))
                .andExpect(jsonPath("$[0].email").value("test@mail.com"));
    }

    @Test
    void testListarUsuariosError() throws Exception {
        when(usuarioService.listarTodos()).thenThrow(new RuntimeException("Fallo en la base de datos"));

        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: Fallo en la base de datos"));
    }

    // ==========================================
    // POST /api/usuarios/register (Registrar)
    // ==========================================
    @Test
    void testRegistrarUsuarioExito() throws Exception {
        when(usuarioService.registrar(any(Usuario.class))).thenReturn(usuarioBase);

        mockMvc.perform(post("/api/usuarios/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(usuarioBase)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("TestUser"));
    }

    @Test
    void testRegistrarUsuarioError() throws Exception {
        // En tu controlador, este catch no concatena "Error: ", devuelve el mensaje directo
        when(usuarioService.registrar(any(Usuario.class))).thenThrow(new RuntimeException("El email ya está en uso"));

        mockMvc.perform(post("/api/usuarios/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(usuarioBase)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("El email ya está en uso"));
    }

    // ==========================================
    // POST /api/usuarios/login (Autenticar)
    // ==========================================
    @Test
    void testLoginExito() throws Exception {
        // Usamos any() en lugar de anyString() para evitar problemas de compatibilidad de tipos o nulos
        when(usuarioService.login(any(), any())).thenReturn(usuarioBase);

        Usuario loginRequest = new Usuario();
        loginRequest.setEmail("test@mail.com");
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/api/usuarios/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("TestUser"));
    }

    @Test
    void testLoginError() throws Exception {
        // Usamos any() de nuevo para garantizar que salte la excepción
        when(usuarioService.login(any(), any())).thenThrow(new RuntimeException("Credenciales incorrectas"));

        Usuario loginRequest = new Usuario();
        loginRequest.setEmail("test@mail.com");
        loginRequest.setPassword("wrongpassword");

        mockMvc.perform(post("/api/usuarios/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized()) // Verifica el HTTP 401
                .andExpect(content().string("Credenciales incorrectas"));
    }

    // ==========================================
    // DELETE /api/usuarios/{id} (Eliminar cuenta)
    // ==========================================
    @Test
    void testEliminarCuentaExito() throws Exception {
        doNothing().when(usuarioService).eliminarCuentaYDatos(anyLong());

        mockMvc.perform(delete("/api/usuarios/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Cuenta y datos eliminados correctamente"));
    }

    @Test
    void testEliminarCuentaError() throws Exception {
        doThrow(new RuntimeException("Usuario no encontrado")).when(usuarioService).eliminarCuentaYDatos(anyLong());

        mockMvc.perform(delete("/api/usuarios/99"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: Usuario no encontrado"));
    }

    // ==========================================
    // PUT /api/usuarios/{id}/moneda-predeterminada
    // ==========================================

    @Test
    void testActualizarMonedaPredeterminadaExito() throws Exception {
        usuarioBase.setMonedaPredeterminada(Moneda.DOLAR);
        when(usuarioService.actualizarMonedaPredeterminada(anyLong(), any(Moneda.class)))
                .thenReturn(usuarioBase);

        mockMvc.perform(put("/api/usuarios/1/moneda-predeterminada")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"moneda\": \"DOLAR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("TestUser"))
                .andExpect(jsonPath("$.monedaPredeterminada").value("DOLAR"));
    }

    @Test
    void testActualizarMonedaPredeterminadaMonedaInvalida() throws Exception {
        mockMvc.perform(put("/api/usuarios/1/moneda-predeterminada")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"moneda\": \"INVALIDA\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Moneda no válida"));
    }

    @Test
    void testActualizarMonedaPredeterminadaUsuarioNoEncontrado() throws Exception {
        when(usuarioService.actualizarMonedaPredeterminada(anyLong(), any(Moneda.class)))
                .thenThrow(new Exception("Usuario no encontrado"));

        mockMvc.perform(put("/api/usuarios/99/moneda-predeterminada")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"moneda\": \"EURO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: Usuario no encontrado"));
    }

    @Test
    void testObtenerPerfilExito() throws Exception {
        usuarioBase.setFotoPerfilUrl("/perfiles/test.png");
        when(usuarioService.obtenerPorId(1L)).thenReturn(usuarioBase);

        mockMvc.perform(get("/api/usuarios/1/perfil"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("TestUser"))
                .andExpect(jsonPath("$.fotoPerfilUrl").value("/perfiles/test.png"));
    }

    @Test
    void testActualizarPerfilSinFotoExito() throws Exception {
        usuarioBase.setUsername("NuevoNombre");
        when(usuarioService.actualizarPerfil(1L, "NuevoNombre", null)).thenReturn(usuarioBase);

        MockMultipartFile nombreVisible = new MockMultipartFile(
                "nombreVisible", "", "text/plain", "NuevoNombre".getBytes());

        mockMvc.perform(multipart("/api/usuarios/1/perfil")
                        .file(nombreVisible)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("NuevoNombre"));
    }

    @Test
    void testActualizarPerfilConFotoExito() throws Exception {
        Usuario actualizado = new Usuario();
        actualizado.setId(1L);
        actualizado.setUsername("TestUser");
        actualizado.setFotoPerfilUrl("/perfiles/nueva-foto.png");

        when(usuarioService.actualizarPerfil(anyLong(), anyString(), anyString())).thenReturn(actualizado);

        MockMultipartFile nombreVisible = new MockMultipartFile(
                "nombreVisible", "", "text/plain", "TestUser".getBytes());
        MockMultipartFile foto = new MockMultipartFile(
                "foto", "avatar.png", "image/png", "img-content".getBytes());

        mockMvc.perform(multipart("/api/usuarios/1/perfil")
                        .file(nombreVisible)
                        .file(foto)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fotoPerfilUrl").value("/perfiles/nueva-foto.png"));
    }

        @Test
        void testObtenerNotificacionesDeudasExito() throws Exception {
        List<NotificacionDeudaDTO> notificaciones = Arrays.asList(
            new NotificacionDeudaDTO(7L, "Viaje a Roma", "EURO", 2L, "Ana", 24.5)
        );
        when(usuarioService.obtenerNotificacionesDeudas(1L)).thenReturn(notificaciones);

        mockMvc.perform(get("/api/usuarios/1/notificaciones-deudas"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].grupoNombre").value("Viaje a Roma"))
            .andExpect(jsonPath("$[0].acreedorUsername").value("Ana"))
            .andExpect(jsonPath("$[0].monto").value(24.5));
        }

        @Test
        void testObtenerNotificacionesDeudasError() throws Exception {
        when(usuarioService.obtenerNotificacionesDeudas(99L)).thenThrow(new Exception("Usuario no encontrado"));

        mockMvc.perform(get("/api/usuarios/99/notificaciones-deudas"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("Error: Usuario no encontrado"));
        }
}
