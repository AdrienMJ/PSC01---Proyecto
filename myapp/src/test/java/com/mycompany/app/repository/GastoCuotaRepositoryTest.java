package com.mycompany.app.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.mycompany.app.entity.Gasto;
import com.mycompany.app.entity.GastoCuota;
import com.mycompany.app.entity.Grupo;
import com.mycompany.app.entity.Moneda;
import com.mycompany.app.entity.TipoReparto;
import com.mycompany.app.entity.Usuario;

@DataJpaTest
public class GastoCuotaRepositoryTest {

    @Autowired
    private GastoCuotaRepository gastoCuotaRepository;

    @Autowired
    private GastoRepository gastoRepository;

    @Autowired
    private GrupoRepository grupoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Gasto gastoGuardado;
    private Usuario user1;
    private Usuario user2;

    @BeforeEach
    void setUp() {
        // Crear y persistir usuarios
        user1 = new Usuario();
        user1.setUsername("Adrien");
        user1.setEmail("adrien@test.com");
        user1.setPassword("pass");
        user1 = usuarioRepository.save(user1);

        user2 = new Usuario();
        user2.setUsername("Prueba");
        user2.setEmail("prueba@test.com");
        user2.setPassword("pass");
        user2 = usuarioRepository.save(user2);

        // Crear y persistir grupo
        Grupo grupo = new Grupo();
        grupo.setNombre("TestGrupo");
        grupo.setMoneda(Moneda.EURO);
        grupo.setIdCreador(user1.getId());
        grupo.getMiembros().add(user1);
        grupo.getMiembros().add(user2);
        grupo = grupoRepository.save(grupo);

        // Crear y persistir gasto
        Gasto gasto = new Gasto();
        gasto.setConcepto("Cena test");
        gasto.setMonto(100.0);
        gasto.setMoneda(Moneda.EURO);
        gasto.setPagador(user1);
        gasto.setGrupo(grupo);
        gasto.setTipoReparto(TipoReparto.CUOTA_FIJA);
        gastoGuardado = gastoRepository.save(gasto);
    }

    // ==========================================
    // findByGastoId
    // ==========================================

    @Test
    void testFindByGastoId_DevuelveCuotasDelGasto() {
        gastoCuotaRepository.save(new GastoCuota(gastoGuardado, user1, 60.0));
        gastoCuotaRepository.save(new GastoCuota(gastoGuardado, user2, 40.0));

        List<GastoCuota> cuotas = gastoCuotaRepository.findByGastoId(gastoGuardado.getId());

        assertEquals(2, cuotas.size());
        double suma = cuotas.stream().mapToDouble(GastoCuota::getMonto).sum();
        assertEquals(100.0, suma, 0.01);
    }

    @Test
    void testFindByGastoId_DevuelveVacioSiNoHayCuotas() {
        List<GastoCuota> cuotas = gastoCuotaRepository.findByGastoId(gastoGuardado.getId());
        assertTrue(cuotas.isEmpty());
    }

    @Test
    void testFindByGastoId_NoDevuelveCuotasDeOtroGasto() {
        // Guardar cuota para el gasto creado en setUp
        gastoCuotaRepository.save(new GastoCuota(gastoGuardado, user1, 100.0));

        // Consultar con un ID inexistente
        List<GastoCuota> cuotas = gastoCuotaRepository.findByGastoId(9999L);
        assertTrue(cuotas.isEmpty());
    }

    // ==========================================
    // deleteByGastoId
    // ==========================================

    @Test
    void testDeleteByGastoId_EliminaSoloCuotasDelGasto() {
        gastoCuotaRepository.save(new GastoCuota(gastoGuardado, user1, 60.0));
        gastoCuotaRepository.save(new GastoCuota(gastoGuardado, user2, 40.0));

        gastoCuotaRepository.deleteByGastoId(gastoGuardado.getId());

        List<GastoCuota> cuotas = gastoCuotaRepository.findByGastoId(gastoGuardado.getId());
        assertTrue(cuotas.isEmpty());
    }

    @Test
    void testDeleteByGastoId_IdInexistenteNoLanzaError() {
        // No debe lanzar excepción
        assertDoesNotThrow(() -> gastoCuotaRepository.deleteByGastoId(9999L));
    }

    // ==========================================
    // Persistencia de montos
    // ==========================================

    @Test
    void testGuardarCuota_MontoSePersisteBien() {
        GastoCuota cuota = new GastoCuota(gastoGuardado, user1, 37.50);
        GastoCuota guardada = gastoCuotaRepository.save(cuota);

        assertNotNull(guardada.getId());
        assertEquals(37.50, guardada.getMonto(), 0.001);
        assertEquals(user1.getId(), guardada.getUsuario().getId());
        assertEquals(gastoGuardado.getId(), guardada.getGasto().getId());
    }

    @Test
    void testGuardarVariasCuotas_SumanElTotalDelGasto() {
        gastoCuotaRepository.save(new GastoCuota(gastoGuardado, user1, 33.33));
        gastoCuotaRepository.save(new GastoCuota(gastoGuardado, user2, 66.67));

        List<GastoCuota> cuotas = gastoCuotaRepository.findByGastoId(gastoGuardado.getId());
        double suma = cuotas.stream().mapToDouble(GastoCuota::getMonto).sum();

        assertEquals(100.0, suma, 0.01);
    }
}