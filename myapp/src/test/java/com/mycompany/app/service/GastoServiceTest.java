package com.mycompany.app.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
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
        
        user1 = new Usuario(); user1.setId(1L); user1.setUsername("Adrien");
        user2 = new Usuario(); user2.setId(2L); user2.setUsername("Prueba");
        
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
            
            Gasto resultado = gastoService.crear(gasto);
            assertEquals(90.0, resultado.getMonto());
            assertEquals(Moneda.EURO, resultado.getMoneda());
        }
    }

    @Test
    public void testCrearGasto_ValidacionesFallidas() {
        Gasto g = new Gasto();
        // Caso: Monto nulo
        assertThrows(Exception.class, () -> gastoService.crear(g));
        
        g.setMonto(10.0);
        // Caso: Grupo nulo
        assertThrows(Exception.class, () -> gastoService.crear(g));
        
        g.setGrupo(grupo);
        // Caso: Pagador nulo
        assertThrows(Exception.class, () -> gastoService.crear(g));
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

        Gasto result = gastoService.crear(gasto);
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

    // --- PRUEBAS DE RESUMEN Y BALANCES (La parte más compleja) ---

    @Test
    public void testObtenerResumenGrupo_TransferenciasComplejas() throws Exception {
        // User1 paga 30€ (Reparto general: cada uno debe 15€)
        Gasto g1 = new Gasto();
        g1.setMonto(30.0); 
        g1.setPagador(user1); 
        g1.setRepartoGeneral(true);
        
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        when(gastoRepository.findByGrupoId(10L)).thenReturn(List.of(g1));
        when(pagoRepository.findByGrupoId(10L)).thenReturn(new ArrayList<>());

        ResumenGrupoDTO resumen = gastoService.obtenerResumenGrupo(10L);

        // Cambiamos getTransferencias() por getSoluciones() que es el nombre real en tu DTO
        assertNotNull(resumen.getSoluciones());
        assertEquals(1, resumen.getSoluciones().size());
        assertEquals(15.0, resumen.getSoluciones().get(0).getMonto());
    }

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
        when(gastoRepository.findByGrupoId(10L)).thenReturn(List.of(g));

        var resultado = gastoService.obtenerResumenPorGrupoParaUsuario(1L);
        
        assertFalse(resultado.isEmpty());
        assertEquals(10L, resultado.get(0).get("grupoId"));
        assertEquals(100.0, resultado.get(0).get("totalPagado"));
        assertEquals(50.0, resultado.get(0).get("totalParte")); // 100 / 2 miembros
    }
    
    @Test
    public void testObtenerTodasLasTasas_RamasDefault() {
        // Probar el switch de monedas (obtenerCodigoIso)
        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, context) -> when(mock.getForObject(anyString(), eq(Map.class))).thenReturn(null))) {
            
            // Probar default y null
            gastoService.obtenerTodasLasTasas(null);
            gastoService.obtenerTodasLasTasas(Moneda.REAL);
            
            // El mapa vuelve vacío porque el response es null
            assertTrue(gastoService.obtenerTodasLasTasas(Moneda.EURO).isEmpty());
        }
    }

    // --- TESTS EXTRA PARA COBERTURA ALTA EN GASTOSERVICE ---

    @Test
    public void testCrear_SinConversionYAsignacionValoresPorDefecto() throws Exception {
        Gasto gasto = new Gasto();
        gasto.setMonto(50.0);
        gasto.setMoneda(Moneda.EURO); // Misma moneda que el grupo, salta la conversión
        gasto.setPagador(user1);
        gasto.setGrupo(grupo);
        gasto.setCategoria(null); // Forzará a asignar OTROS
        gasto.setEmote("   "); // Forzará a asignar null
        gasto.setParticipantes(null); // Forzará a asignar reparto general
        
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(gastoRepository.save(any(Gasto.class))).thenAnswer(i -> i.getArguments()[0]);

        Gasto result = gastoService.crear(gasto);
        
        assertEquals(Moneda.EURO, result.getMoneda());
        assertEquals(CategoriaGasto.OTROS, result.getCategoria());
        assertNull(result.getEmote());
        assertTrue(result.isRepartoGeneral());
    }

    @Test
    public void testCrear_ValidacionesExtraDeIntegridad() {
        Gasto gasto = new Gasto();
        // 1. Monto negativo
        gasto.setMonto(-10.0);
        assertThrows(Exception.class, () -> gastoService.crear(gasto));
        
        // 2. Grupo sin ID
        gasto.setMonto(10.0);
        gasto.setGrupo(new Grupo()); 
        assertThrows(Exception.class, () -> gastoService.crear(gasto));
        
        // 3. Pagador sin ID
        gasto.setGrupo(grupo);
        gasto.setPagador(new Usuario());
        assertThrows(Exception.class, () -> gastoService.crear(gasto));
        
        // 4. Pagador que no está en el grupo
        Usuario externo = new Usuario(); externo.setId(99L);
        gasto.setPagador(externo);
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        when(usuarioRepository.findById(99L)).thenReturn(Optional.of(externo));
        assertThrows(Exception.class, () -> gastoService.crear(gasto));
    }

    @Test
    public void testListarPorGrupo_OpcionesAlternativas() {
        when(gastoRepository.findByGrupoId(eq(10L), any(Sort.class))).thenReturn(new ArrayList<>());
        when(gastoRepository.findByGrupoIdAndCategoria(eq(10L), any(), any(Sort.class))).thenReturn(new ArrayList<>());

        // Prueba orden por fecha descendente y sin categoría
        gastoService.listarPorGrupo(10L, "fecha", "desc", null);
        verify(gastoRepository).findByGrupoId(eq(10L), any(Sort.class));

        // Prueba búsqueda con categoría "OTROS"
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
        
        // Falla porque ya está pagado
        assertThrows(Exception.class, () -> gastoService.marcarComoPagado(1L, 2L));

        // Falla porque no tiene grupo
        Gasto gSinGrupo = new Gasto(); gSinGrupo.setPagado(false);
        when(gastoRepository.findById(2L)).thenReturn(Optional.of(gSinGrupo));
        assertThrows(Exception.class, () -> gastoService.marcarComoPagado(2L, 2L));
        
        // Falla porque el grupo no tiene miembros
        Gasto gConGrupoVacio = new Gasto(); gConGrupoVacio.setGrupo(new Grupo()); gConGrupoVacio.getGrupo().setId(10L);
        when(gastoRepository.findById(3L)).thenReturn(Optional.of(gConGrupoVacio));
        Grupo grupoVacio = new Grupo(); grupoVacio.setMiembros(new ArrayList<>());
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupoVacio));
        assertThrows(Exception.class, () -> gastoService.marcarComoPagado(3L, 2L));
    }

    @Test
    public void testObtenerResumenGrupo_ConPagosYGastosIgnorados() throws Exception {
        // Gasto ya pagado (debería ignorarse en el balance)
        Gasto gPagado = new Gasto(); 
        gPagado.setPagado(true); 
        gPagado.setMonto(100.0); 
        gPagado.setPagador(user1);
        
        // Pago directo de user2 a user1
        Pago pago = new Pago(); 
        pago.setMonto(15.0); 
        pago.setPagador(user2); 
        pago.setReceptor(user1);
        
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        when(gastoRepository.findByGrupoId(10L)).thenReturn(List.of(gPagado)); // El totalGastado lo suma igualmente
        when(pagoRepository.findByGrupoId(10L)).thenReturn(List.of(pago));
        
        ResumenGrupoDTO resumen = gastoService.obtenerResumenGrupo(10L);
        
        // El total gastado en el grupo suma 100
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
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo)); // user1 es admin
        
        // Falla porque user2 no es admin
        assertThrows(Exception.class, () -> gastoService.editarGasto(1L, 2L, new Gasto()));
        
        // Editamos campos permitidos como admin (emote en blanco pasa a null)
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
        gasto.setRepartoGeneral(false); // IMPRESCINDIBLE para que no lo ignore
        
        Usuario externo = new Usuario(); 
        externo.setId(99L);
        gasto.setParticipantes(List.of(externo));
        
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(user1));
        
        Exception ex = assertThrows(Exception.class, () -> gastoService.crear(gasto));
        assertEquals("Todos los participantes deben pertenecer al grupo", ex.getMessage());
    }

    @Test
    public void testListarPorGrupo_CategoriaInvalida() {
        // Al pasar "INVENTADA", el enum lanza IllegalArgumentException. Lo capturamos.
        assertThrows(IllegalArgumentException.class, () -> {
            gastoService.listarPorGrupo(10L, "monto", "asc", "INVENTADA");
        });
    }

    @Test
    public void testObtenerResumenGrupo_ConRepartoEspecifico() throws Exception {
        // Cubre: obtenerResumenGrupo -> la rama 'else' (cuando NO es reparto general)
        Gasto g = new Gasto();
        g.setMonto(20.0);
        g.setPagador(user1);
        g.setRepartoGeneral(false);
        g.setParticipantes(List.of(user2)); // user1 paga, pero el gasto es solo para user2
        
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        when(gastoRepository.findByGrupoId(10L)).thenReturn(List.of(g));
        when(pagoRepository.findByGrupoId(10L)).thenReturn(new ArrayList<>());
        
        ResumenGrupoDTO resumen = gastoService.obtenerResumenGrupo(10L);
        
        // user2 le debe 20 a user1
        assertEquals(1, resumen.getSoluciones().size());
        assertEquals(20.0, resumen.getSoluciones().get(0).getMonto());
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
        actualizado.setConcepto(null);  // Debe ignorarse porque es null
        actualizado.setMonto(-5.0);     // Debe ignorarse porque es negativo
        actualizado.setCategoria(null); // Debe ignorarse porque es null
        
        Gasto result = gastoService.editarGasto(1L, 1L, actualizado);
        
        // Comprobamos que mantuvo los valores originales
        assertEquals("Comida", result.getConcepto());
        assertEquals(50.0, result.getMonto());
    }

    @Test
    public void testEditarGasto_ParticipanteNoEnGrupoLanzaExcepcion() throws Exception {
        // Cubre: editarGasto -> la validación de participantes nuevos dentro del grupo
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
        // Cubre: obtenerResumenPorGrupoParaUsuario -> el 'if (!grupo.getMiembros().contains(usuario)) continue;'
        Usuario externo = new Usuario(); 
        externo.setId(99L);
        when(usuarioRepository.findById(99L)).thenReturn(Optional.of(externo));
        when(grupoRepository.findAll()).thenReturn(List.of(grupo)); // 'grupo' solo tiene a user1 y user2
        
        var resultado = gastoService.obtenerResumenPorGrupoParaUsuario(99L);
        
        // Debe ignorar el grupo y devolver una lista vacía
        assertTrue(resultado.isEmpty());
    }

    @Test
    public void testObtenerTodasLasTasas_LanzaExcepcion() {
        // Cubre: obtenerTodasLasTasas -> el bloque catch cuando el RestTemplate falla
        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, context) -> when(mock.getForObject(anyString(), eq(Map.class)))
                        .thenThrow(new RuntimeException("API caída")))) {
            
            Map<String, Object> tasas = gastoService.obtenerTodasLasTasas(Moneda.EURO);
            
            // Si salta la excepción, el código devuelve un mapa vacío
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
        
        // Rama 1: Participantes es null
        gasto.setParticipantes(null);
        Gasto g1 = gastoService.crear(gasto);
        assertEquals(2, g1.getParticipantes().size()); // Coge los del grupo
        
        // Rama 2: Participantes está vacío
        gasto.setParticipantes(new ArrayList<>());
        Gasto g2 = gastoService.crear(gasto);
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
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo)); // admin es user1
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
        actualizado.setConcepto(""); // isBlank() -> true
        actualizado.setMonto(null); // monto null -> falla el != null
        actualizado.setEmote("   "); // isBlank() -> true, guarda null
        actualizado.setParticipantes(new ArrayList<>()); // isEmpty() -> true
        
        when(gastoRepository.findById(1L)).thenReturn(Optional.of(original));
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        when(gastoRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        
        Gasto result = gastoService.editarGasto(1L, 1L, actualizado);
        
        assertEquals(50.0, result.getMonto()); // Se mantiene el original
        assertNull(result.getEmote()); // Se pone a null por ser espacios
    }

    @Test
    public void testListarPorGrupo_CategoriaVaciaOBlanco() {
        when(gastoRepository.findByGrupoId(anyLong(), any(Sort.class))).thenReturn(new ArrayList<>());
        // Le pasamos espacios en blanco para cubrir la rama !categoria.trim().isEmpty()
        gastoService.listarPorGrupo(10L, "monto", "asc", "   ");
        verify(gastoRepository).findByGrupoId(eq(10L), any(Sort.class));
    }

    @Test
    public void testObtenerTodasLasTasas_ResultadoNoSuccess() {
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("result", "error"); // Falla el if ("success".equals(...))
        
        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, context) -> when(mock.getForObject(anyString(), eq(Map.class))).thenReturn(mockResponse))) {
            Map<String, Object> tasas = gastoService.obtenerTodasLasTasas(Moneda.EURO);
            assertTrue(tasas.isEmpty());
        }
    }

    @Test
    public void testObtenerTodasLasTasas_Exito() {
        // Preparamos la respuesta simulada que espera tu Service
        Map<String, Object> mockResponse = new HashMap<>();
        
        // Creamos las tasas ficticias
        Map<String, Object> rates = new HashMap<>();
        rates.put("USD", 1.1);
        
        // ¡Importante! La clave debe ser "rates", que es lo que busca tu GastoService
        mockResponse.put("rates", rates); 
        
        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, context) -> when(mock.getForObject(anyString(), eq(Map.class))).thenReturn(mockResponse))) {
            
            Map<String, Object> tasas = gastoService.obtenerTodasLasTasas(Moneda.EURO);
            
            assertFalse(tasas.isEmpty());
            assertEquals(1.1, tasas.get("USD"));
        }
    }
}