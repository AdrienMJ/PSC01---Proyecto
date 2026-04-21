package com.mycompany.app.service;

import org.junit.jupiter.api.Test; 
import org.junit.jupiter.api.BeforeEach; 
import static org.junit.jupiter.api.Assertions.*; 

// Imports de Mockito (estos suelen ser iguales)
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.mycompany.app.entity.Usuario;
import com.mycompany.app.repository.UsuarioRepository;

import static org.mockito.Mockito.*;

import java.util.Optional;

public class UsuarioServiceTest {
    @Mock
    private UsuarioRepository usuarioRepository; // para simular el respositorio

    @InjectMocks
    private UsuarioService usuarioService;// esta es la clase que vamos a probar, inyectamos el mock del repositorio

    @BeforeEach
    public void setup() {
        //inicializa los objetos mock antes de cada test 
        MockitoAnnotations.openMocks(this); 
    }

    @Test
    public void testLoginExitoso() throws Exception {
        //creamos usuario inventado
        Usuario usuarioFicticio = new Usuario("Adrien", "adrien@mail.com", "123");

        // el servicio busca el mail
        when(usuarioRepository.findByEmail("adrien@mail.com")).thenReturn(Optional.of(usuarioFicticio));

        // probamos el metodo login
        Usuario resultado = usuarioService.login("adrien@mail.com", "123");


        //comprobamos que no sea nulo y que sea el mismo usuario que se ha creado
        assertNotNull(resultado);
        assertEquals("Adrien", resultado.getUsername()); 
    }

    @Test
    public void testLoginContraseñaIncorrecta() {
        // preparamos los datos de prueba
        Usuario usuarioFicticio = new Usuario("Adrien", "adrien@mail.com", "123");
        when(usuarioRepository.findByEmail("adrien@mail.com")).thenReturn(Optional.of(usuarioFicticio));

        //vemos si lanza excepción al poner la contraseña mal
        Exception exception = assertThrows(Exception.class, () -> {
            usuarioService.login("adrien@mail.com", "password_falsa");
        });

        // comprobamos que el mensaje de error es correcto
        assertEquals("Email o contraseña incorrectos", exception.getMessage());
    }
    
    @Test
    public void testLoginUsuarioNoEncontrado() {
        when(usuarioRepository.findByEmail("fantasma@mail.com")).thenReturn(Optional.empty());

        Exception exception = assertThrows(Exception.class, () -> {
            usuarioService.login("fantasma@mail.com", "123");
        });

        assertEquals("Email o contraseña incorrectos", exception.getMessage());
    }
}
