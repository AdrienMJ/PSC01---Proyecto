package com.mycompany.app.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.mycompany.app.entity.Gasto;
import com.mycompany.app.entity.GastoCuota;
import com.mycompany.app.entity.Grupo;
import com.mycompany.app.entity.Moneda;
import com.mycompany.app.entity.TipoReparto;
import com.mycompany.app.entity.Usuario;
import com.mycompany.app.repository.GastoCuotaRepository;
import com.mycompany.app.repository.GastoRepository;
import com.mycompany.app.repository.GrupoRepository;
import com.mycompany.app.repository.PagoRepository;
import com.mycompany.app.repository.UsuarioRepository;

public class RepartoDesigualServiceTest {

    @Mock private GastoRepository gastoRepository;
    @Mock private GrupoRepository grupoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PagoRepository pagoRepository;
    @Mock private GastoCuotaRepository gastoCuotaRepository;

    @InjectMocks
    private GastoService gastoService;

    private Usuario user1;
    private Usuario user2;
    private Usuario user3;
    private Grupo grupo;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        user1 = new Usuario(); user1.setId(1L); user1.setUsername("Adrien");
        user2 = new Usuario(); user2.setId(2L); user2.setUsername("Prueba");
        user3 = new Usuario(); user3.setId(3L); user3.setUsername("Carlos");

        grupo = new Grupo();
        grupo.setId(10L);
        grupo.setNombre("Viaje");
        grupo.setMoneda(Moneda.EURO);
        grupo.setIdCreador(1L);
        grupo.setMiembros(new ArrayList<>(Arrays.asList(user1, user2, user3)));

        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(gastoRepository.save(any(Gasto.class))).thenAnswer(i -> {
            Gasto g = (Gasto) i.getArguments()[0];
            if (g.getId() == null) {
                try {
                    var field = Gasto.class.getDeclaredField("id");
                    field.setAccessible(true);
                    field.set(g, 99L);
                } catch (Exception ignored) {}
            }
            return g;
        });
        when(gastoCuotaRepository.save(any(GastoCuota.class))).thenAnswer(i -> i.getArguments()[0]);
    }

    // ==========================================
    // TipoReparto.IGUAL — comportamiento por defecto
    // ==========================================

    @Test
    public void testCrear_TipoRepartoIgualNoGuardaCuotas() throws Exception {
        Gasto gasto = gastoBase();
        gasto.setTipoReparto(TipoReparto.IGUAL);

        gastoService.crear(gasto, null);

        verify(gastoCuotaRepository, never()).save(any(GastoCuota.class));
    }

    @Test
    public void testCrear_SinTipoRepartoAsignaIgualYNoGuardaCuotas() throws Exception {
        Gasto gasto = gastoBase();
        gasto.setTipoReparto(null);

        Gasto resultado = gastoService.crear(gasto, null);

        assertEquals(TipoReparto.IGUAL, resultado.getTipoReparto());
        verify(gastoCuotaRepository, never()).save(any(GastoCuota.class));
    }

    // ==========================================
    // TipoReparto.CUOTA_FIJA — camino feliz
    // ==========================================

    @Test
    public void testCrear_CuotaFijaGuardaCuotasCorrectas() throws Exception {
        Gasto gasto = gastoBase();
        gasto.setTipoReparto(TipoReparto.CUOTA_FIJA);
        gasto.setParticipantes(new ArrayList<>(Arrays.asList(user1, user2)));
        gasto.setRepartoGeneral(false);

        Map<Long, Double> cuotas = new HashMap<>();
        cuotas.put(1L, 30.0);
        cuotas.put(2L, 70.0);
        gasto.setCuotasMap(cuotas);

        gastoService.crear(gasto, null);

        // Debe guardar una GastoCuota por cada participante
        ArgumentCaptor<GastoCuota> captor = ArgumentCaptor.forClass(GastoCuota.class);
        verify(gastoCuotaRepository, times(2)).save(captor.capture());

        List<GastoCuota> guardadas = captor.getAllValues();
        double suma = guardadas.stream().mapToDouble(GastoCuota::getMonto).sum();
        assertEquals(100.0, suma, 0.01);
    }

    @Test
    public void testCrear_CuotaFijaConTresParticipantes() throws Exception {
        Gasto gasto = gastoBase();
        gasto.setMonto(90.0);
        gasto.setTipoReparto(TipoReparto.CUOTA_FIJA);
        gasto.setParticipantes(new ArrayList<>(Arrays.asList(user1, user2, user3)));
        gasto.setRepartoGeneral(false);
        when(usuarioRepository.findAllById(any())).thenReturn(Arrays.asList(user1, user2, user3));

        Map<Long, Double> cuotas = new HashMap<>();
        cuotas.put(1L, 10.0);
        cuotas.put(2L, 40.0);
        cuotas.put(3L, 40.0);
        gasto.setCuotasMap(cuotas);

        gastoService.crear(gasto, null);

        verify(gastoCuotaRepository, times(3)).save(any(GastoCuota.class));
    }

    @Test
    public void testCrear_CuotaFijaSumaIncorrectaLanzaExcepcion() {
        Gasto gasto = gastoBase(); // monto = 100€
        gasto.setTipoReparto(TipoReparto.CUOTA_FIJA);
        gasto.setParticipantes(new ArrayList<>(Arrays.asList(user1, user2)));
        gasto.setRepartoGeneral(false);

        Map<Long, Double> cuotas = new HashMap<>();
        cuotas.put(1L, 40.0);
        cuotas.put(2L, 40.0); // solo 80, faltan 20
        gasto.setCuotasMap(cuotas);

        Exception ex = assertThrows(Exception.class, () -> gastoService.crear(gasto, null));
        assertTrue(ex.getMessage().contains("cuotas deben sumar"));
    }

    @Test
    public void testCrear_CuotaFijaSinCuotasMapLanzaExcepcion() {
        Gasto gasto = gastoBase();
        gasto.setTipoReparto(TipoReparto.CUOTA_FIJA);
        gasto.setCuotasMap(null);

        Exception ex = assertThrows(Exception.class, () -> gastoService.crear(gasto, null));
        assertTrue(ex.getMessage().contains("cuotas de cada participante"));
    }

    @Test
    public void testCrear_CuotaFijaMapVacioLanzaExcepcion() {
        Gasto gasto = gastoBase();
        gasto.setTipoReparto(TipoReparto.CUOTA_FIJA);
        gasto.setCuotasMap(new HashMap<>());

        Exception ex = assertThrows(Exception.class, () -> gastoService.crear(gasto, null));
        assertTrue(ex.getMessage().contains("cuotas de cada participante"));
    }

    // ==========================================
    // TipoReparto.PORCENTAJE — camino feliz
    // ==========================================

    @Test
    public void testCrear_PorcentajeConvierteAMontos() throws Exception {
        Gasto gasto = gastoBase(); // monto = 100€
        gasto.setTipoReparto(TipoReparto.PORCENTAJE);
        gasto.setParticipantes(new ArrayList<>(Arrays.asList(user1, user2)));
        gasto.setRepartoGeneral(false);

        Map<Long, Double> cuotas = new HashMap<>();
        cuotas.put(1L, 30.0); // 30% → 30€
        cuotas.put(2L, 70.0); // 70% → 70€
        gasto.setCuotasMap(cuotas);

        gastoService.crear(gasto, null);

        ArgumentCaptor<GastoCuota> captor = ArgumentCaptor.forClass(GastoCuota.class);
        verify(gastoCuotaRepository, times(2)).save(captor.capture());

        List<GastoCuota> guardadas = captor.getAllValues();
        // Los montos guardados deben ser ya en € (no en %)
        double suma = guardadas.stream().mapToDouble(GastoCuota::getMonto).sum();
        assertEquals(100.0, suma, 0.01);
    }

    @Test
    public void testCrear_PorcentajeSumaIncorrectaLanzaExcepcion() {
        Gasto gasto = gastoBase();
        gasto.setTipoReparto(TipoReparto.PORCENTAJE);
        gasto.setParticipantes(new ArrayList<>(Arrays.asList(user1, user2)));
        gasto.setRepartoGeneral(false);

        Map<Long, Double> cuotas = new HashMap<>();
        cuotas.put(1L, 40.0);
        cuotas.put(2L, 40.0); // solo 80%, faltan 20%
        gasto.setCuotasMap(cuotas);

        Exception ex = assertThrows(Exception.class, () -> gastoService.crear(gasto, null));
        assertTrue(ex.getMessage().contains("porcentajes deben sumar 100"));
    }

    @Test
    public void testCrear_PorcentajeSumaExacta100Exito() throws Exception {
        Gasto gasto = gastoBase();
        gasto.setTipoReparto(TipoReparto.PORCENTAJE);
        gasto.setParticipantes(new ArrayList<>(Arrays.asList(user1, user2, user3)));
        gasto.setRepartoGeneral(false);
        when(usuarioRepository.findAllById(any())).thenReturn(Arrays.asList(user1, user2, user3));

        Map<Long, Double> cuotas = new HashMap<>();
        cuotas.put(1L, 50.0);
        cuotas.put(2L, 25.0);
        cuotas.put(3L, 25.0); // exacto 100%
        gasto.setCuotasMap(cuotas);

        assertDoesNotThrow(() -> gastoService.crear(gasto, null));
        verify(gastoCuotaRepository, times(3)).save(any(GastoCuota.class));
    }

    @Test
    public void testCrear_PorcentajeSinCuotasMapLanzaExcepcion() {
        Gasto gasto = gastoBase();
        gasto.setTipoReparto(TipoReparto.PORCENTAJE);
        gasto.setCuotasMap(null);

        Exception ex = assertThrows(Exception.class, () -> gastoService.crear(gasto, null));
        assertTrue(ex.getMessage().contains("cuotas de cada participante"));
    }

    // ==========================================
    // Resumen — usa cuotas guardadas en BBDD
    // ==========================================

    @Test
    public void testResumen_UsaCuotasGuardadasEnLugarDeRepartoIgual() throws Exception {
        // user1 paga 100€ pero user2 solo debe 30 y user1 se debe 70 a sí mismo
        Gasto gasto = new Gasto();
        gasto.setMonto(100.0);
        gasto.setPagador(user1);
        gasto.setRepartoGeneral(false);
        gasto.setParticipantes(new ArrayList<>(Arrays.asList(user1, user2)));
        gasto.setTipoReparto(TipoReparto.CUOTA_FIJA);

        GastoCuota cuota1 = new GastoCuota(gasto, user1, 70.0);
        GastoCuota cuota2 = new GastoCuota(gasto, user2, 30.0);

        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        when(gastoRepository.findByGrupoId(10L)).thenReturn(List.of(gasto));
        when(gastoCuotaRepository.findByGastoId(any())).thenReturn(Arrays.asList(cuota1, cuota2));
        when(pagoRepository.findByGrupoId(10L)).thenReturn(new ArrayList<>());

        var resumen = gastoService.obtenerResumenGrupo(10L);

        // user2 solo debe 30, no 50 (que sería reparto igual)
        var balanceUser2 = resumen.getBalances().stream()
                .filter(b -> b.getUsuarioId().equals(2L))
                .findFirst().orElseThrow();
        assertEquals(-30.0, balanceUser2.getBalance(), 0.01);
    }

    @Test
    public void testResumen_SinCuotasUsaRepartoIgualComoFallback() throws Exception {
        // Gasto IGUAL sin cuotas en BBDD → debe repartir monto/n
        Gasto gasto = new Gasto();
        gasto.setMonto(60.0);
        gasto.setPagador(user1);
        gasto.setRepartoGeneral(true);
        gasto.setTipoReparto(TipoReparto.IGUAL);

        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        when(gastoRepository.findByGrupoId(10L)).thenReturn(List.of(gasto));
        when(gastoCuotaRepository.findByGastoId(any())).thenReturn(new ArrayList<>());
        when(pagoRepository.findByGrupoId(10L)).thenReturn(new ArrayList<>());

        var resumen = gastoService.obtenerResumenGrupo(10L);

        // 60€ / 3 miembros = 20€ por persona
        var balanceUser2 = resumen.getBalances().stream()
                .filter(b -> b.getUsuarioId().equals(2L))
                .findFirst().orElseThrow();
        assertEquals(-20.0, balanceUser2.getBalance(), 0.01);
    }

    // ==========================================
    // GastoCuota — entidad directamente
    // ==========================================

    @Test
    public void testGastoCuota_ConstructorYGetters() {
        Gasto gasto = new Gasto();
        GastoCuota cuota = new GastoCuota(gasto, user1, 45.5);

        assertEquals(gasto, cuota.getGasto());
        assertEquals(user1, cuota.getUsuario());
        assertEquals(45.5, cuota.getMonto());
    }

    @Test
    public void testGastoCuota_SettersActualizan() {
        GastoCuota cuota = new GastoCuota();
        cuota.setGasto(new Gasto());
        cuota.setUsuario(user2);
        cuota.setMonto(99.0);

        assertEquals(user2, cuota.getUsuario());
        assertEquals(99.0, cuota.getMonto());
    }

    // ==========================================
    // TipoReparto — enum valores
    // ==========================================

    @Test
    public void testTipoReparto_TieneLosTresValores() {
        TipoReparto[] valores = TipoReparto.values();
        assertEquals(3, valores.length);
        assertNotNull(TipoReparto.valueOf("IGUAL"));
        assertNotNull(TipoReparto.valueOf("PORCENTAJE"));
        assertNotNull(TipoReparto.valueOf("CUOTA_FIJA"));
    }

    @Test
    public void testGasto_TipoRepartoDefaultEsIgual() {
        Gasto gasto = new Gasto();
        assertEquals(TipoReparto.IGUAL, gasto.getTipoReparto());
    }

    @Test
    public void testGasto_SetTipoRepartoFunciona() {
        Gasto gasto = new Gasto();
        gasto.setTipoReparto(TipoReparto.PORCENTAJE);
        assertEquals(TipoReparto.PORCENTAJE, gasto.getTipoReparto());

        gasto.setTipoReparto(TipoReparto.CUOTA_FIJA);
        assertEquals(TipoReparto.CUOTA_FIJA, gasto.getTipoReparto());
    }

    @Test
    public void testGasto_CuotasMapTransiente() {
        Gasto gasto = new Gasto();
        assertNull(gasto.getCuotasMap());

        Map<Long, Double> map = new HashMap<>();
        map.put(1L, 60.0);
        map.put(2L, 40.0);
        gasto.setCuotasMap(map);

        assertEquals(2, gasto.getCuotasMap().size());
        assertEquals(60.0, gasto.getCuotasMap().get(1L));
    }

    // ==========================================
    // Helper
    // ==========================================

    private Gasto gastoBase() {
        Gasto g = new Gasto();
        g.setConcepto("Cena");
        g.setMonto(100.0);
        g.setMoneda(Moneda.EURO);
        g.setPagador(user1);
        g.setGrupo(grupo);
        g.setRepartoGeneral(true);
        return g;
    }
}