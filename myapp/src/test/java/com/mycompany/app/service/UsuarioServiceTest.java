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
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

public class UsuarioServiceTest {
    @Mock
    private UsuarioRepository usuarioRepository; // para simular el respositorio

    @Mock
    private JdbcTemplate jdbcTemplate;

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

    @Test
    public void testEliminarCuentaYDatosBorraRelacionadosYUsuario() throws Exception {
    Long idUsuario = 1L;
    Long grupoId = 10L;
    Long gastoUsuario = 100L;
    Long gastoGrupo = 200L;

    when(usuarioRepository.existsById(idUsuario)).thenReturn(true);

    when(jdbcTemplate.queryForList(
        "SELECT grupo_id FROM grupo_usuarios WHERE usuario_id = ?",
        Long.class,
        idUsuario
    )).thenReturn(Collections.singletonList(grupoId));

    when(jdbcTemplate.queryForList(
        "SELECT id FROM gastos WHERE usuario_id = ?",
        Long.class,
        idUsuario
    )).thenReturn(Collections.singletonList(gastoUsuario));

    when(jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM grupo_usuarios WHERE grupo_id = ?",
        Integer.class,
        grupoId
    )).thenReturn(0);

    when(jdbcTemplate.queryForList(
        "SELECT id FROM gastos WHERE grupo_id = ?",
        Long.class,
        grupoId
    )).thenReturn(Arrays.asList(gastoUsuario, gastoGrupo));

    usuarioService.eliminarCuentaYDatos(idUsuario);

    verify(jdbcTemplate).update("DELETE FROM pagos WHERE pagador_id = ? OR receptor_id = ?", idUsuario, idUsuario);
    verify(jdbcTemplate).update("DELETE FROM gasto_participantes WHERE gasto_id IN (?)", gastoUsuario);
    verify(jdbcTemplate).update("DELETE FROM gastos WHERE usuario_id = ?", idUsuario);
    verify(jdbcTemplate).update("DELETE FROM gasto_participantes WHERE usuario_id = ?", idUsuario);
    verify(jdbcTemplate).update("DELETE FROM grupo_usuarios WHERE usuario_id = ?", idUsuario);
    verify(jdbcTemplate).update("DELETE FROM gasto_participantes WHERE gasto_id IN (?,?)", gastoUsuario, gastoGrupo);
    verify(jdbcTemplate).update("DELETE FROM pagos WHERE grupo_id = ?", grupoId);
    verify(jdbcTemplate).update("DELETE FROM gastos WHERE grupo_id = ?", grupoId);
    verify(jdbcTemplate).update("DELETE FROM grupos WHERE id = ?", grupoId);
    verify(usuarioRepository).deleteById(idUsuario);
    }
}
