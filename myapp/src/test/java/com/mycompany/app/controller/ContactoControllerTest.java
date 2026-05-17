package com.mycompany.app.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.mycompany.app.dto.ContactoDTO;
import com.mycompany.app.service.ContactoService;

@WebMvcTest(ContactoController.class)
public class ContactoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ContactoService contactoService;

    // --- SECCIÓN: GET /{id}/contactos ---

    @Test
    void testListarContactosExito() throws Exception {
        List<ContactoDTO> contactos = Arrays.asList(new ContactoDTO(2L, "Luis", "luis@mail.com"));

        when(contactoService.obtenerContactos(1L)).thenReturn(contactos);

        mockMvc.perform(get("/api/usuarios/1/contactos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].username").value("Luis"));
    }

    @Test
    void testListarContactosUsuarioNoEncontrado() throws Exception {
        when(contactoService.obtenerContactos(99L)).thenThrow(new Exception("Usuario no encontrado"));

        mockMvc.perform(get("/api/usuarios/99/contactos"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: Usuario no encontrado"));
    }

    @Test
    void testListarContactosVacio() throws Exception {
        when(contactoService.obtenerContactos(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/usuarios/1/contactos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // --- SECCIÓN: POST /{id}/contactos ---

    @Test
    void testAgregarContactoExito() throws Exception {
        doNothing().when(contactoService).agregarContacto(anyLong(), anyString());

        mockMvc.perform(post("/api/usuarios/1/contactos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"luis@mail.com\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Contacto añadido correctamente"));
    }

    @Test
    void testAgregarContactoEmailNoExiste() throws Exception {
        doThrow(new Exception("No existe ningún usuario con ese email"))
                .when(contactoService).agregarContacto(anyLong(), anyString());

        mockMvc.perform(post("/api/usuarios/1/contactos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"nadie@mail.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("No existe ningún usuario con ese email"));
    }

    @Test
    void testAgregarContactoUnoMismo() throws Exception {
        doThrow(new Exception("No puedes añadirte a ti mismo como contacto"))
                .when(contactoService).agregarContacto(anyLong(), anyString());

        mockMvc.perform(post("/api/usuarios/1/contactos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"ana@mail.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("No puedes añadirte a ti mismo como contacto"));
    }

    @Test
    void testAgregarContactoYaExiste() throws Exception {
        doThrow(new Exception("Este usuario ya está en tus contactos"))
                .when(contactoService).agregarContacto(anyLong(), anyString());

        mockMvc.perform(post("/api/usuarios/1/contactos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"luis@mail.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Este usuario ya está en tus contactos"));
    }

    // --- SECCIÓN: DELETE /{id}/contactos/{contactoId} ---

    @Test
    void testEliminarContactoExito() throws Exception {
        doNothing().when(contactoService).eliminarContacto(anyLong(), anyLong());

        mockMvc.perform(delete("/api/usuarios/1/contactos/2"))
                .andExpect(status().isOk())
                .andExpect(content().string("Contacto eliminado correctamente"));
    }

    @Test
    void testEliminarContactoUsuarioNoEncontrado() throws Exception {
        doThrow(new Exception("Usuario no encontrado"))
                .when(contactoService).eliminarContacto(anyLong(), anyLong());

        mockMvc.perform(delete("/api/usuarios/99/contactos/2"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: Usuario no encontrado"));
    }
}
