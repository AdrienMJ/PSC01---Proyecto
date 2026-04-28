package com.mycompany;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.mycompany.app.DataInitializer;
import com.mycompany.app.entity.Usuario;
import com.mycompany.app.repository.UsuarioRepository;

public class DataInitializerTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private DataInitializer dataInitializer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testRunGuardaUsuariosSiNoExisten() throws Exception {
        // Simulamos que la base de datos está vacía y no encuentra los emails
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // Ejecutamos el método
        dataInitializer.run();

        // Verificamos que el método save se llamó exactamente 3 veces (una por cada if)
        verify(usuarioRepository, times(3)).save(any(Usuario.class));
    }

    @Test
    void testRunNoGuardaUsuariosSiYaExisten() throws Exception {
        // Simulamos que los usuarios ya están en la base de datos
        Usuario usuarioExistente = new Usuario("Ya Existo", "mail@mail.com", "pass");
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuarioExistente));

        // Ejecutamos el método
        dataInitializer.run();

        // Verificamos que el método save NO se llamó ninguna vez porque no entró a los if
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }
}
