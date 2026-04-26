package com.mycompany.app.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;

import com.mycompany.app.entity.Gasto;
import com.mycompany.app.entity.Grupo;
import com.mycompany.app.entity.Moneda;
import com.mycompany.app.entity.Usuario;
import com.mycompany.app.repository.GastoRepository;
import com.mycompany.app.repository.GrupoRepository;
import com.mycompany.app.repository.UsuarioRepository;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GastoServiceTest {

    @Mock
    private GastoRepository gastoRepository;

    @Mock
    private GrupoRepository grupoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private GastoService gastoService;
    
    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }
    // 1. Test del método calcularTotalGrupo
    @Test
    public void testCalcularTotalGrupo() {
        Long grupoId = 1L;
        Gasto g1 = new Gasto();
        g1.setMonto(100.0);
        
        Gasto g2 = new Gasto();
        g2.setMonto(50.50);
        List<Gasto> listaGastos = Arrays.asList(g1, g2);

        when(gastoRepository.findByGrupoId(grupoId)).thenReturn(listaGastos);

        Double total = gastoService.calcularTotalGrupo(grupoId);

        assertEquals(150.50, total, 0.001); 
    }

    // 2. Test de creación exitosa con las nuevas validaciones
    @Test
    public void testCrearGastoExitoso() throws Exception {
        // Preparar datos
        Usuario juan = new Usuario();
        juan.setId(1L);
        juan.setUsername("Juan");

        Grupo viaje = new Grupo();
        viaje.setId(10L);
        viaje.setMoneda(Moneda.EURO);
        viaje.getMiembros().add(juan); // Aseguramos que Juan es miembro

        Gasto nuevoGasto = new Gasto();
        nuevoGasto.setMonto(100.0);
        nuevoGasto.setMoneda(Moneda.DOLAR);
        nuevoGasto.setPagador(juan);
        nuevoGasto.setGrupo(viaje);

        // Configurar Mocks
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(viaje));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(juan));
        when(gastoRepository.save(any(Gasto.class))).thenReturn(nuevoGasto);

        // Ejecutar
        Gasto resultado = gastoService.crear(nuevoGasto);

        // Verificar
        assertNotNull(resultado);
        assertEquals(100.0, resultado.getMonto());
        verify(gastoRepository).save(nuevoGasto);
    }

    // 3. Test de validación: Monto incorrecto
    @Test
    public void testCrearGastoMontoCeroLanzaExcepcion() {
        Gasto gastoMal = new Gasto();
        gastoMal.setMonto(0.0);

        Exception ex = assertThrows(Exception.class, () -> {
            gastoService.crear(gastoMal);
        });

        assertEquals("El monto debe ser mayor que 0", ex.getMessage());
    }

    // 4. Test de validación: Pagador no pertenece al grupo
    @Test
    public void testCrearGastoPagadorNoPerteneceAlGrupo() {
        Usuario juan = new Usuario(); juan.setId(1L);
        Usuario intruso = new Usuario(); intruso.setId(2L);

        Grupo viaje = new Grupo();
        viaje.setId(10L);
        viaje.getMiembros().add(juan); // Aseguramos que Juan es miembro

        Gasto gasto = new Gasto();
        gasto.setMonto(50.0);
        gasto.setGrupo(viaje);
        gasto.setPagador(intruso); // Intruso intenta pagar

        when(grupoRepository.findById(10L)).thenReturn(Optional.of(viaje));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(intruso));

        Exception ex = assertThrows(Exception.class, () -> {
            gastoService.crear(gasto);
        });

        assertEquals("El pagador no pertenece al grupo", ex.getMessage());
    }

    // 5. Test de conversión: La API externa responde correctamente sin modificar GastoService
    @Test
    public void testObtenerTodasLasTasasExito() {
        Moneda base = Moneda.EURO;
        String urlEsperada = "https://open.er-api.com/v6/latest/EUR";

        // Preparamos el JSON simulado que devolverá nuestra API falsa
        Map<String, Object> respuestaFalsaApi = new HashMap<>();
        Map<String, Object> ratesFalsos = new HashMap<>();
        ratesFalsos.put("USD", 1.08); 
        ratesFalsos.put("GBP", 0.85); 
        respuestaFalsaApi.put("rates", ratesFalsos);

        // Usamos MockedConstruction para interceptar "new RestTemplate()" dentro de tu método
        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, context) -> {
                    // Le decimos al mock qué devolver cuando se llame a getForObject
                    when(mock.getForObject(urlEsperada, Map.class)).thenReturn(respuestaFalsaApi);
                })) {
            
            // Ejecutamos tu código original intacto
            Map<String, Object> tasasObtenidas = gastoService.obtenerTodasLasTasas(base);

            // Verificamos que todo ha ido bien
            assertNotNull(tasasObtenidas);
            assertFalse(tasasObtenidas.isEmpty());
            assertEquals(1.08, tasasObtenidas.get("USD"));
            assertEquals(0.85, tasasObtenidas.get("GBP"));
        }
    }

    // 6. Test de conversión: Fallo en la API (ej. sin internet) devolviendo mapa vacío
    @Test
    public void testObtenerTodasLasTasasFalloApiDevuelveMapaVacio() {
        Moneda base = Moneda.DOLAR;
        String urlEsperada = "https://open.er-api.com/v6/latest/USD";

        // Interceptamos de nuevo para simular un fallo de conexión
        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, context) -> {
                    // Simulamos que al intentar conectarse salta una excepción
                    when(mock.getForObject(urlEsperada, Map.class))
                        .thenThrow(new RuntimeException("Error de conexión a internet"));
                })) {
            
            
            Map<String, Object> tasasObtenidas = gastoService.obtenerTodasLasTasas(base);

            //Verificamos que tu bloque catch actuó y devolvió el mapa vacío sin explotar
            assertNotNull(tasasObtenidas);
            assertTrue(tasasObtenidas.isEmpty());
        }
    }

    // ================== Tests para marcarComoPagado ==================

    // Método auxiliar para crear Gasto con ID usando reflexión
    private Gasto crearGastoConId(Long id, Double monto, boolean pagado, Usuario pagador, Grupo grupo) throws Exception {
        Gasto gasto = new Gasto();
        gasto.setConcepto("Test");
        gasto.setMonto(monto);
        gasto.setPagado(pagado);
        gasto.setPagador(pagador);
        gasto.setGrupo(grupo);
        // Usar reflexión para establecer el ID
        var idField = Gasto.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(gasto, id);
        return gasto;
    }

    // 7. Test: Marcar gasto como pagado exitosamente
    @Test
    public void testMarcarGastoPagadoExitoso() throws Exception {
        // Preparar datos
        Usuario pagador = new Usuario();
        pagador.setId(1L);
        pagador.setUsername("Juan");

        Usuario pagador2 = new Usuario();
        pagador2.setId(2L);
        pagador2.setUsername("Maria");

        Grupo grupo = new Grupo();
        grupo.setId(10L);
        grupo.getMiembros().add(pagador);
        grupo.getMiembros().add(pagador2);

        Gasto gasto = crearGastoConId(100L, 50.0, false, pagador, grupo);

        // Configurar Mocks
        when(gastoRepository.findById(100L)).thenReturn(Optional.of(gasto));
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        when(gastoRepository.save(any(Gasto.class))).thenReturn(gasto);

        // Ejecutar
        Gasto resultado = gastoService.marcarComoPagado(100L, 2L); // Maria marca como pagado

        // Verificar
        assertNotNull(resultado);
        assertTrue(resultado.isPagado());
        verify(gastoRepository).save(gasto);
    }

    // 8. Test: Error - Gasto no encontrado
    @Test
    public void testMarcarGastoPagadoGastoNoEncontrado() {
        when(gastoRepository.findById(999L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class, () -> {
            gastoService.marcarComoPagado(999L, 1L);
        });

        assertEquals("Gasto no encontrado", ex.getMessage());
    }

    // 9. Test: Error - Gasto ya está marcado como pagado
    @Test
    public void testMarcarGastoPagadoYaPagado() throws Exception {
        Usuario pagador = new Usuario();
        pagador.setId(1L);

        Grupo grupo = new Grupo();
        grupo.setId(10L);
        grupo.getMiembros().add(pagador);

        Gasto gasto = crearGastoConId(100L, 50.0, true, pagador, grupo);

        when(gastoRepository.findById(100L)).thenReturn(Optional.of(gasto));
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));

        Exception ex = assertThrows(Exception.class, () -> {
            gastoService.marcarComoPagado(100L, 2L);
        });

        assertEquals("El gasto ya está marcado como pagado", ex.getMessage());
    }

    // 10. Test: Error - Usuario no pertenece al grupo
    @Test
    public void testMarcarGastoPagadoUsuarioNoPerteneceAlGrupo() throws Exception {
        Usuario pagador = new Usuario();
        pagador.setId(1L);

        Grupo grupo = new Grupo();
        grupo.setId(10L);
        grupo.getMiembros().add(pagador); // Solo Juan es miembro

        Gasto gasto = crearGastoConId(100L, 50.0, false, pagador, grupo);

        when(gastoRepository.findById(100L)).thenReturn(Optional.of(gasto));
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));

        // Maria (id=2) no pertenece al grupo
        Exception ex = assertThrows(Exception.class, () -> {
            gastoService.marcarComoPagado(100L, 2L);
        });

        assertEquals("Usuario no pertenece al grupo", ex.getMessage());
    }

    // 11. Test: Error - El pagador no puede marcar su propio gasto
    @Test
    public void testMarcarGastoPagadoNoPuedeMarcarSuPropioGasto() throws Exception {
        Usuario juan = new Usuario();
        juan.setId(1L);
        juan.setUsername("Juan");

        Grupo grupo = new Grupo();
        grupo.setId(10L);
        grupo.getMiembros().add(juan);

        Gasto gasto = crearGastoConId(100L, 50.0, false, juan, grupo);

        when(gastoRepository.findById(100L)).thenReturn(Optional.of(gasto));
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));

        // Juan intenta marcar su propio gasto como pagado
        Exception ex = assertThrows(Exception.class, () -> {
            gastoService.marcarComoPagado(100L, 1L);
        });

        assertEquals("No puedes marcar tu propio gasto como pagado. Otro miembro del grupo debe confirmarlo.", ex.getMessage());
    }

    // 12. Test: Error - El gasto no tiene grupo asociado
    @Test
    public void testMarcarGastoPagadoSinGrupo() throws Exception {
        Gasto gasto = crearGastoConId(100L, 50.0, false, null, null);

        when(gastoRepository.findById(100L)).thenReturn(Optional.of(gasto));

        Exception ex = assertThrows(Exception.class, () -> {
            gastoService.marcarComoPagado(100L, 1L);
        });

        assertEquals("El gasto no tiene grupo asociado", ex.getMessage());
    }

    // 13. Test: Error - El grupo no tiene miembros
    @Test
    public void testMarcarGastoPagadoGrupoSinMiembros() throws Exception {
        Usuario pagador = new Usuario();
        pagador.setId(1L);

        Grupo grupo = new Grupo();
        grupo.setId(10L);
        // Grupo sin miembros

        Gasto gasto = crearGastoConId(100L, 50.0, false, pagador, grupo);

        when(gastoRepository.findById(100L)).thenReturn(Optional.of(gasto));
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));

        Exception ex = assertThrows(Exception.class, () -> {
            gastoService.marcarComoPagado(100L, 1L);
        });

        assertEquals("El grupo no tiene miembros", ex.getMessage());
    }
}
