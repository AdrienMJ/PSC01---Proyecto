package com.mycompany.app.controller;

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
import static org.mockito.ArgumentMatchers.any;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;


import com.mycompany.app.entity.Grupo;
import com.mycompany.app.entity.Moneda;
import com.mycompany.app.entity.Usuario;
import com.mycompany.app.service.GrupoService;

@WebMvcTest(GrupoController.class)
public class GrupoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GrupoService grupoService;

    private Grupo grupoBase;
    private Usuario admin;

    @BeforeEach
    void setUp() {
        admin = new Usuario("Admin", "admin@mail.com", "pass");
        admin.setId(1L);

        grupoBase = new Grupo("Viaje a Japón", Moneda.YEN);
        grupoBase.setId(10L);
        grupoBase.setIdCreador(1L);
        grupoBase.addMiembro(admin);
    }

    // ==========================================
    // POST /api/grupos/crear
    // ==========================================
    @Test
    void testCrearExito() throws Exception {
        when(grupoService.crearGrupo(eq("Viaje a Japón"), eq(Moneda.YEN), eq(1L))).thenReturn(grupoBase);

        String jsonRequest = "{\"nombre\":\"Viaje a Japón\",\"moneda\":\"YEN\",\"idCreador\":1}";

        mockMvc.perform(post("/api/grupos/crear")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Viaje a Japón"))
                .andExpect(jsonPath("$.moneda").value("YEN"));
    }

    @Test
    void testCrearError() throws Exception {
        when(grupoService.crearGrupo(anyString(), any(Moneda.class), anyLong()))
                .thenThrow(new RuntimeException("Usuario no encontrado"));

        String jsonRequest = "{\"nombre\":\"Viaje a Japón\",\"moneda\":\"YEN\",\"idCreador\":99}";

        mockMvc.perform(post("/api/grupos/crear")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: Usuario no encontrado"));
    }

    // ==========================================
    // GET /api/grupos/usuario/{userId}
    // ==========================================
    @Test
    void testListarGrupos() throws Exception {
        List<Grupo> grupos = Arrays.asList(grupoBase);
        when(grupoService.listarGruposPorUsuario(1L)).thenReturn(grupos);

        mockMvc.perform(get("/api/grupos/usuario/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Viaje a Japón"));
    }

    // ==========================================
    // GET /api/grupos/{grupoId}
    // ==========================================
    @Test
    void testObtenerGrupoExito() throws Exception {
        when(grupoService.obtenerGrupoPorId(10L)).thenReturn(grupoBase);

        mockMvc.perform(get("/api/grupos/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.nombre").value("Viaje a Japón"))
                .andExpect(jsonPath("$.moneda").value("YEN"))
                .andExpect(jsonPath("$.idCreador").value(1));
    }

    @Test
    void testObtenerGrupoError() throws Exception {
        when(grupoService.obtenerGrupoPorId(99L)).thenThrow(new RuntimeException("Grupo no encontrado"));

        mockMvc.perform(get("/api/grupos/99"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: Grupo no encontrado"));
    }

    // ==========================================
    // GET /api/grupos/monedas
    // ==========================================
    @Test
    void testGetMonedas() throws Exception {
        mockMvc.perform(get("/api/grupos/monedas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.[0]").exists()); // Verifica que hay elementos
    }

    // ==========================================
    // PUT /api/grupos/{grupoId}/nombre
    // ==========================================
    @Test
    void testRenombrarGrupoExito() throws Exception {
        Grupo grupoEditado = new Grupo("Nuevo Nombre", Moneda.YEN);
        grupoEditado.setId(10L);

        when(grupoService.renombrarGrupo(eq(10L), eq(1L), eq("Nuevo Nombre"))).thenReturn(grupoEditado);

        String jsonRequest = "{\"idUsuario\":1,\"nombre\":\"Nuevo Nombre\"}";

        mockMvc.perform(put("/api/grupos/10/nombre")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Nuevo Nombre"));
    }

    @Test
    void testRenombrarGrupoError() throws Exception {
        when(grupoService.renombrarGrupo(eq(10L), eq(1L), eq("")))
                .thenThrow(new RuntimeException("El nombre no puede estar vacío"));

        String jsonRequest = "{\"idUsuario\":1,\"nombre\":\"\"}";

        mockMvc.perform(put("/api/grupos/10/nombre")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: El nombre no puede estar vacío"));
    }

    // ==========================================
    // GET /api/grupos/{grupoId}/usuarios
    // ==========================================
    @Test
    void testObtenerUsuariosDelGrupoExito() throws Exception {
        when(grupoService.obtenerGrupoPorId(10L)).thenReturn(grupoBase);

        mockMvc.perform(get("/api/grupos/10/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("Admin"));
    }

    @Test
    void testObtenerUsuariosDelGrupoError() throws Exception {
        when(grupoService.obtenerGrupoPorId(99L)).thenThrow(new RuntimeException("Grupo no existe"));

        mockMvc.perform(get("/api/grupos/99/usuarios"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: Grupo no existe"));
    }

    // ==========================================
    // POST /api/grupos/{grupoId}/invitar
    // ==========================================
    @Test
    void testInvitarUsuarioExito() throws Exception {
        Usuario invitado = new Usuario("Invitado", "invitado@mail.com", "pass");
        grupoBase.addMiembro(invitado); // Simulamos que el grupo resultante ya tiene al invitado

        when(grupoService.invitarUsuario(eq(10L), eq("invitado@mail.com"), eq(1L))).thenReturn(grupoBase);

        String jsonRequest = "{\"email\":\"invitado@mail.com\",\"idUsuarioInvitador\":1}";

        mockMvc.perform(post("/api/grupos/10/invitar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.miembros.length()").value(2));
    }

    @Test
    void testInvitarUsuarioError() throws Exception {
        when(grupoService.invitarUsuario(anyLong(), anyString(), anyLong()))
                .thenThrow(new RuntimeException("El usuario ya es miembro"));

        String jsonRequest = "{\"email\":\"admin@mail.com\",\"idUsuarioInvitador\":1}";

        mockMvc.perform(post("/api/grupos/10/invitar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: El usuario ya es miembro"));
    }

    // ==========================================
    // GET /api/grupos/{grupoId}/creador
    // ==========================================
    @Test
    void testObtenerCreadorExito() throws Exception {
        when(grupoService.obtenerGrupoPorId(10L)).thenReturn(grupoBase);

        mockMvc.perform(get("/api/grupos/10/creador"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idCreador").value(1));
    }
    
    @Test
    void testObtenerCreadorNull() throws Exception {
        Grupo grupoSinCreador = new Grupo("Test", Moneda.EURO);
        grupoSinCreador.setIdCreador(null); // Caso borde

        when(grupoService.obtenerGrupoPorId(10L)).thenReturn(grupoSinCreador);

        mockMvc.perform(get("/api/grupos/10/creador"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idCreador").value(-1));
    }

    @Test
    void testObtenerCreadorError() throws Exception {
        when(grupoService.obtenerGrupoPorId(99L)).thenThrow(new RuntimeException("Fallo"));

        mockMvc.perform(get("/api/grupos/99/creador"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: Fallo"));
    }

    // ==========================================
    // DELETE /api/grupos/{grupoId}/miembros/{miembroId}
    // ==========================================
    @Test
    void testExpulsarMiembroExito() throws Exception {
        doNothing().when(grupoService).expulsarMiembro(10L, 1L, 2L);

        mockMvc.perform(delete("/api/grupos/10/miembros/2")
                .param("idAdmin", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Miembro expulsado correctamente"));
    }

    @Test
    void testExpulsarMiembroError() throws Exception {
        doThrow(new RuntimeException("No tiene permisos")).when(grupoService).expulsarMiembro(10L, 2L, 1L);

        mockMvc.perform(delete("/api/grupos/10/miembros/1")
                .param("idAdmin", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("No tiene permisos"));
    }
}