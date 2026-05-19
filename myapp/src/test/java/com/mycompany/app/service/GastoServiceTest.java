package com.mycompany.app.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.mycompany.app.dto.BalancePersonaDTO;
import com.mycompany.app.dto.ResumenGrupoDTO;
import com.mycompany.app.entity.*;
import com.mycompany.app.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Sort;
import org.springframework.web.client.RestTemplate;

import java.util.*;

public class GastoServiceTest {

    @Mock private GastoRepository gastoRepository;
    @Mock private GrupoRepository grupoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PagoRepository pagoRepository;

    @InjectMocks
    private GastoService gastoService;

    private Usuario user1;
    private Usuario user2;
    private Grupo grupo;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        
        user1 = new Usuario(); 
        user1.setId(1L); 
        user1.setUsername("Adrien");
        user1.setEmail("adrien@test.com");
        
        user2 = new Usuario(); 
        user2.setId(2L); 
        user2.setUsername("Prueba");
        user2.setEmail("prueba@test.com");
        
        grupo = new Grupo();
        grupo.setId(10L);
        grupo.setNombre("Viaje");
        grupo.setMoneda(Moneda.EURO);
        grupo.setIdCreador(1L); // User1 es admin
        grupo.setMiembros(new ArrayList<>(Arrays.asList(user1, user2)));
    }

    // --- PRUEBAS DE CREAR (Validaciones y Ramas) ---

    @Test
    public void testCrearGasto_ExitoConConversion() throws Exception {
        Gasto gasto = new Gasto();
        gasto.setMonto(100.0);
        gasto.setMoneda(Moneda.DOLAR); // Diferente a la del grupo (EURO)
        gasto.setPagador(user1);
        gasto.setGrupo(grupo);
        gasto.setRepartoGeneral(true);

        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(gastoRepository.save(any(Gasto.class))).thenAnswer(i -> i.getArguments()[0]);

        // Mock RestTemplate para la conversión (100 USD -> 90 EUR)
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("rates", Map.of("EUR", 0.9));

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, context) -> when(mock.getForObject(anyString(), eq(Map.class))).thenReturn(mockResponse))) {
            
            Gasto resultado = gastoService.crear(gasto, null);
            assertEquals(90.0, resultado.getMonto());
            assertEquals(Moneda.EURO, resultado.getMoneda());
        }
    }

    @Test
    public void testCrearGasto_ValidacionesFallidas() {
        Gasto g = new Gasto();
        // Caso: Monto nulo
        assertThrows(Exception.class, () -> gastoService.crear(g, null));
        
        g.setMonto(10.0);
        // Caso: Grupo nulo
        assertThrows(Exception.class, () -> gastoService.crear(g, null));
        
        g.setGrupo(grupo);
        // Caso: Pagador nulo
        assertThrows(Exception.class, () -> gastoService.crear(g, null));
    }

    @Test
    public void testCrearGasto_ParticipantesEspecificos() throws Exception {
        Gasto gasto = new Gasto();
        gasto.setMonto(100.0);
        gasto.setPagador(user1);
        gasto.setGrupo(grupo);
        gasto.setRepartoGeneral(false);
        gasto.setParticipantes(List.of(user1)); // Solo participa user1

        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(usuarioRepository.findAllById(any())).thenReturn(List.of(user1));
        when(gastoRepository.save(any())).thenReturn(gasto);

        Gasto result = gastoService.crear(gasto, null);
        assertFalse(result.isRepartoGeneral());
        assertEquals(1, result.getParticipantes().size());
    }

    // --- PRUEBAS DE LISTAR Y OBTENER ---

    @Test
    public void testObtenerPorId_InexistenteLanzaExcepcion() {
        when(gastoRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(Exception.class, () -> gastoService.obtenerPorId(99L));
    }

    // --- PRUEBAS DE MARCAR COMO PAGADO ---

    @Test
    public void testMarcarComoPagado_Validaciones() throws Exception {
        Gasto gasto = new Gasto();
        gasto.setGrupo(grupo);
        gasto.setPagador(user1);
        gasto.setPagado(false);

        when(gastoRepository.findById(1L)).thenReturn(Optional.of(gasto));
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));

        // No puede marcar su propio gasto
        assertThrows(Exception.class, () -> gastoService.marcarComoPagado(1L, 1L));
        
        // Usuario no pertenece al grupo
        assertThrows(Exception.class, () -> gastoService.marcarComoPagado(1L, 99L));

        // Caso éxito: User2 marca el gasto de User1
        when(gastoRepository.save(any())).thenReturn(gasto);
        Gasto pagado = gastoService.marcarComoPagado(1L, 2L);
        assertTrue(pagado.isPagado());
    }

    // --- PRUEBAS DE RESUMEN Y BALANCES ---

    

    // --- PRUEBAS DE ADMIN (Eliminar y Editar) ---

    @Test
    public void testEliminarGasto_FallaPorNoSerAdmin() throws Exception {
        Gasto gasto = new Gasto();
        gasto.setGrupo(grupo); // el admin es user1 (ID 1L)
        gasto.setPagador(user2); // el pagador es user2 (ID 2L)
        
        when(gastoRepository.findById(1L)).thenReturn(Optional.of(gasto));
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        
        // user2 intenta borrar. Como no es el admin, debe saltar excepción
        Exception ex = assertThrows(Exception.class, () -> gastoService.eliminarGasto(1L, 2L));
        assertEquals("Solo el administrador del grupo puede eliminar gastos", ex.getMessage());
    }

    @Test
    public void testEditarGasto_Exito() throws Exception {
        Gasto original = new Gasto();
        original.setGrupo(grupo);
        original.setConcepto("Viejo");

        Gasto actualizado = new Gasto();
        actualizado.setConcepto("Nuevo");
        actualizado.setMonto(50.0);

        when(gastoRepository.findById(1L)).thenReturn(Optional.of(original));
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        when(gastoRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        Gasto result = gastoService.editarGasto(1L, 1L, actualizado);
        
        assertEquals("Nuevo", result.getConcepto());
        assertEquals(50.0, result.getMonto());
    }

    @Test
    public void testObtenerResumenPorGrupoParaUsuario() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(grupoRepository.findAll()).thenReturn(List.of(grupo));
        
        Gasto g = new Gasto();
        g.setMonto(100.0); g.setPagador(user1); g.setRepartoGeneral(true);
        g.setParticipantes(new ArrayList<>(Arrays.asList(user1, user2)));
        when(gastoRepository.findByGrupoId(10L)).thenReturn(List.of(g));

        var resultado = gastoService.obtenerResumenPorGrupoParaUsuario(1L);
        
        assertFalse(resultado.isEmpty());
        assertEquals(10L, resultado.get(0).get("grupoId"));
        assertEquals(100.0, resultado.get(0).get("totalPagado"));
        assertEquals(50.0, resultado.get(0).get("totalParte")); 
    }
    
    @Test
    public void testObtenerTodasLasTasas_RamasDefault() {
        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, context) -> when(mock.getForObject(anyString(), eq(Map.class))).thenReturn(null))) {
            
            gastoService.obtenerTodasLasTasas(null);
            gastoService.obtenerTodasLasTasas(Moneda.REAL);
            
            assertTrue(gastoService.obtenerTodasLasTasas(Moneda.EURO).isEmpty());
        }
    }

    // --- TESTS EXTRA PARA COBERTURA ---

    @Test
    public void testCrear_SinConversionYAsignacionValoresPorDefecto() throws Exception {
        Gasto gasto = new Gasto();
        gasto.setMonto(50.0);
        gasto.setMoneda(Moneda.EURO); 
        gasto.setPagador(user1);
        gasto.setGrupo(grupo);
        gasto.setCategoria(null); 
        gasto.setEmote("   "); 
        gasto.setParticipantes(null); 
        
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(gastoRepository.save(any(Gasto.class))).thenAnswer(i -> i.getArguments()[0]);

        Gasto result = gastoService.crear(gasto, null);
        
        assertEquals(Moneda.EURO, result.getMoneda());
        assertEquals(CategoriaGasto.OTROS, result.getCategoria());
        assertNull(result.getEmote());
        assertTrue(result.isRepartoGeneral());
    }

    @Test
    public void testCrear_ValidacionesExtraDeIntegridad() {
        Gasto gasto = new Gasto();
        gasto.setMonto(-10.0);
        assertThrows(Exception.class, () -> gastoService.crear(gasto, null));
        
        gasto.setMonto(10.0);
        gasto.setGrupo(new Grupo()); 
        assertThrows(Exception.class, () -> gastoService.crear(gasto, null));
        
        gasto.setGrupo(grupo);
        gasto.setPagador(new Usuario());
        assertThrows(Exception.class, () -> gastoService.crear(gasto, null));
        
        Usuario externo = new Usuario(); externo.setId(99L);
        gasto.setPagador(externo);
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        when(usuarioRepository.findById(99L)).thenReturn(Optional.of(externo));
        assertThrows(Exception.class, () -> gastoService.crear(gasto, null));
    }

    @Test
    public void testListarPorGrupo_OpcionesAlternativas() {
        when(gastoRepository.findByGrupoId(eq(10L), any(Sort.class))).thenReturn(new ArrayList<>());
        when(gastoRepository.findByGrupoIdAndCategoria(eq(10L), any(), any(Sort.class))).thenReturn(new ArrayList<>());

        gastoService.listarPorGrupo(10L, "fecha", "desc", null);
        verify(gastoRepository).findByGrupoId(eq(10L), any(Sort.class));

        gastoService.listarPorGrupo(10L, "monto", "asc", "OTROS"); 
        verify(gastoRepository).findByGrupoIdAndCategoria(eq(10L), eq(CategoriaGasto.OTROS), any(Sort.class));
    }

    @Test
    public void testObtenerPorId_Exito() throws Exception {
        Gasto gasto = new Gasto();
        when(gastoRepository.findById(1L)).thenReturn(Optional.of(gasto));
        assertEquals(gasto, gastoService.obtenerPorId(1L));
    }

    @Test
    public void testMarcarComoPagado_YaPagadoOGrupoInvalido() {
        Gasto g = new Gasto();
        g.setPagado(true);
        when(gastoRepository.findById(1L)).thenReturn(Optional.of(g));
        
        assertThrows(Exception.class, () -> gastoService.marcarComoPagado(1L, 2L));

        Gasto gSinGrupo = new Gasto(); gSinGrupo.setPagado(false);
        when(gastoRepository.findById(2L)).thenReturn(Optional.of(gSinGrupo));
        assertThrows(Exception.class, () -> gastoService.marcarComoPagado(2L, 2L));
        
        Gasto gConGrupoVacio = new Gasto(); gConGrupoVacio.setGrupo(new Grupo()); gConGrupoVacio.getGrupo().setId(10L);
        when(gastoRepository.findById(3L)).thenReturn(Optional.of(gConGrupoVacio));
        Grupo grupoVacio = new Grupo(); grupoVacio.setMiembros(new ArrayList<>());
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupoVacio));
        assertThrows(Exception.class, () -> gastoService.marcarComoPagado(3L, 2L));
    }

    @Test
    public void testObtenerResumenGrupo_ConPagosYGastosIgnorados() throws Exception {
        Gasto gPagado = new Gasto(); 
        gPagado.setPagado(true); 
        gPagado.setMonto(100.0); 
        gPagado.setPagador(user1);
        gPagado.setRepartoGeneral(true);
        gPagado.setParticipantes(new ArrayList<>(Arrays.asList(user1, user2)));
        
        Pago pago = new Pago(); 
        pago.setMonto(15.0); 
        pago.setPagador(user2); 
        pago.setReceptor(user1);
        
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        when(gastoRepository.findByGrupoId(10L)).thenReturn(List.of(gPagado)); 
        when(pagoRepository.findByGrupoId(10L)).thenReturn(List.of(pago));
        
        ResumenGrupoDTO resumen = gastoService.obtenerResumenGrupo(10L);
        assertEquals(100.0, resumen.getTotalGastado());
    }
    
    @Test
    public void testCalcularTotalGrupo() {
        Gasto g1 = new Gasto(); g1.setMonto(10.0);
        Gasto g2 = new Gasto(); g2.setMonto(25.5);
        when(gastoRepository.findByGrupoId(10L)).thenReturn(List.of(g1, g2));
        
        assertEquals(35.5, gastoService.calcularTotalGrupo(10L));
    }

    @Test
    public void testEditarGasto_NoAdminYActualizacionParcial() throws Exception {
        Gasto g = new Gasto(); g.setGrupo(grupo);
        when(gastoRepository.findById(1L)).thenReturn(Optional.of(g));
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo)); 
        
        assertThrows(Exception.class, () -> gastoService.editarGasto(1L, 2L, new Gasto()));
        
        Gasto gActualizado = new Gasto();
        gActualizado.setEmote("  "); 
        gActualizado.setCategoria(CategoriaGasto.OTROS);
        when(gastoRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        
        Gasto result = gastoService.editarGasto(1L, 1L, gActualizado);
        assertNull(result.getEmote());
        assertEquals(CategoriaGasto.OTROS, result.getCategoria());
    }

    @Test
    public void testCrear_ParticipanteNoEnGrupo() throws Exception {
        Gasto gasto = new Gasto();
        gasto.setMonto(10.0);
        gasto.setPagador(user1);
        gasto.setGrupo(grupo);
        gasto.setRepartoGeneral(false); 
        
        Usuario externo = new Usuario(); 
        externo.setId(99L);
        gasto.setParticipantes(List.of(externo));
        
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(user1));
        
        Exception ex = assertThrows(Exception.class, () -> gastoService.crear(gasto, null));
        assertEquals("Todos los participantes deben pertenecer al grupo", ex.getMessage());
    }

    @Test
    public void testListarPorGrupo_CategoriaInvalida() {
        assertThrows(IllegalArgumentException.class, () -> {
            gastoService.listarPorGrupo(10L, "monto", "asc", "INVENTADA");
        });
    }

    @Test
    public void testEditarGasto_SaltarActualizacionesNulasOInvalidas() throws Exception {
        Gasto original = new Gasto();
        original.setConcepto("Comida");
        original.setMonto(50.0);
        original.setGrupo(grupo);
        original.setPagador(user1);
        
        when(gastoRepository.findById(1L)).thenReturn(Optional.of(original));
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        when(gastoRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        
        Gasto actualizado = new Gasto();
        actualizado.setConcepto(null);  
        actualizado.setMonto(-5.0);    
        actualizado.setCategoria(null); 
        
        Gasto result = gastoService.editarGasto(1L, 1L, actualizado);
        
        assertEquals("Comida", result.getConcepto());
        assertEquals(50.0, result.getMonto());
    }

    @Test
    public void testEditarGasto_ParticipanteNoEnGrupoLanzaExcepcion() throws Exception {
        Gasto original = new Gasto();
        original.setGrupo(grupo);
        original.setPagador(user1);
        
        when(gastoRepository.findById(1L)).thenReturn(Optional.of(original));
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        
        Gasto actualizado = new Gasto();
        Usuario externo = new Usuario(); 
        externo.setId(99L);
        actualizado.setParticipantes(List.of(externo));
        
        assertThrows(Exception.class, () -> gastoService.editarGasto(1L, 1L, actualizado));
    }

    @Test
    public void testObtenerResumenPorGrupoParaUsuario_UsuarioNoEnGrupo() {
        Usuario externo = new Usuario(); 
        externo.setId(99L);
        when(usuarioRepository.findById(99L)).thenReturn(Optional.of(externo));
        when(grupoRepository.findAll()).thenReturn(List.of(grupo)); 
        
        var resultado = gastoService.obtenerResumenPorGrupoParaUsuario(99L);
        assertTrue(resultado.isEmpty());
    }

    @Test
    public void testObtenerTodasLasTasas_LanzaExcepcion() {
        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, context) -> when(mock.getForObject(anyString(), eq(Map.class)))
                        .thenThrow(new RuntimeException("API caída")))) {
            
            Map<String, Object> tasas = gastoService.obtenerTodasLasTasas(Moneda.EURO);
            assertTrue(tasas.isEmpty());
        }
    }

    @Test
    public void testCrear_ParticipantesNulosOVacios() throws Exception {
        Gasto gasto = new Gasto();
        gasto.setMonto(10.0);
        gasto.setPagador(user1);
        gasto.setGrupo(grupo);
        
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(gastoRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        
        gasto.setParticipantes(null);
        Gasto g1 = gastoService.crear(gasto, null);
        assertEquals(2, g1.getParticipantes().size()); 
        
        gasto.setParticipantes(new ArrayList<>());
        Gasto g2 = gastoService.crear(gasto, null);
        assertEquals(2, g2.getParticipantes().size());
    }

    @Test
    public void testEditarGasto_ActualizacionCompletaValida() throws Exception {
        Gasto original = new Gasto();
        original.setGrupo(grupo);
        original.setPagador(user1);
        
        Gasto actualizado = new Gasto();
        actualizado.setConcepto("Nuevo Concepto"); 
        actualizado.setMonto(100.0); 
        actualizado.setCategoria(CategoriaGasto.OCIO); 
        actualizado.setEmote("👍"); 
        actualizado.setParticipantes(List.of(user2)); 
        
        when(gastoRepository.findById(1L)).thenReturn(Optional.of(original));
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo)); 
        when(usuarioRepository.findAllById(any())).thenReturn(List.of(user2));
        when(gastoRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        
        Gasto result = gastoService.editarGasto(1L, 1L, actualizado);
        
        assertEquals("Nuevo Concepto", result.getConcepto());
        assertEquals(100.0, result.getMonto());
        assertEquals(CategoriaGasto.OCIO, result.getCategoria());
        assertEquals("👍", result.getEmote());
        assertEquals(1, result.getParticipantes().size());
    }

    @Test
    public void testEditarGasto_MontoNuloYEmoteVacio() throws Exception {
        Gasto original = new Gasto();
        original.setGrupo(grupo);
        original.setMonto(50.0);
        
        Gasto actualizado = new Gasto();
        actualizado.setConcepto(""); 
        actualizado.setMonto(null); 
        actualizado.setEmote("   "); 
        actualizado.setParticipantes(new ArrayList<>()); 
        
        when(gastoRepository.findById(1L)).thenReturn(Optional.of(original));
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        when(gastoRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        
        Gasto result = gastoService.editarGasto(1L, 1L, actualizado);
        
        assertEquals(50.0, result.getMonto()); 
        assertNull(result.getEmote()); 
    }

    @Test
    public void testListarPorGrupo_CategoriaVaciaOBlanco() {
        when(gastoRepository.findByGrupoId(anyLong(), any(Sort.class))).thenReturn(new ArrayList<>());
        gastoService.listarPorGrupo(10L, "monto", "asc", "   ");
        verify(gastoRepository).findByGrupoId(eq(10L), any(Sort.class));
    }

    @Test
    public void testObtenerTodasLasTasas_ResultadoNoSuccess() {
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("result", "error"); 
        
        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, context) -> when(mock.getForObject(anyString(), eq(Map.class))).thenReturn(mockResponse))) {
            Map<String, Object> tasas = gastoService.obtenerTodasLasTasas(Moneda.EURO);
            assertTrue(tasas.isEmpty());
        }
    }

    @Test
    public void testObtenerTodasLasTasas_Exito() {
        Map<String, Object> mockResponse = new HashMap<>();
        Map<String, Object> rates = new HashMap<>();
        rates.put("USD", 1.1);
        mockResponse.put("rates", rates); 
        
        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, context) -> when(mock.getForObject(anyString(), eq(Map.class))).thenReturn(mockResponse))) {
            
            Map<String, Object> tasas = gastoService.obtenerTodasLasTasas(Moneda.EURO);
            
            assertFalse(tasas.isEmpty());
            assertEquals(1.1, tasas.get("USD"));
        }
    }

    @Test
    public void testObtenerTotalesPorCategoria_Exito() {
        Gasto g1 = new Gasto();
        g1.setMonto(40.0);
        g1.setCategoria(CategoriaGasto.COMIDA);
        
        Gasto g2 = new Gasto();
        g2.setMonto(25.0);
        g2.setCategoria(CategoriaGasto.OCIO);

        Gasto g3 = new Gasto();
        g3.setMonto(15.0);
        g3.setCategoria(CategoriaGasto.COMIDA); 

        when(gastoRepository.findByGrupoId(10L)).thenReturn(List.of(g1, g2, g3));

        Map<String, Double> resultado = gastoService.obtenerTotalesPorCategoria(10L);

        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        assertEquals(55.0, resultado.get("COMIDA")); 
        assertEquals(25.0, resultado.get("OCIO"));
    }

}