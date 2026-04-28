package com.mycompany.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.mycompany.app.entity.CategoriaGasto;
import com.mycompany.app.entity.Gasto;
import com.mycompany.app.entity.Grupo;
import com.mycompany.app.entity.Moneda;
import com.mycompany.app.entity.Usuario;
import com.mycompany.app.repository.GastoRepository;
import com.mycompany.app.repository.GrupoRepository;
import com.mycompany.app.repository.PagoRepository;
import com.mycompany.app.repository.UsuarioRepository;

public class CategoriaGastoCreacionTest {

    @Mock
    private GastoRepository gastoRepository;

    @Mock
    private GrupoRepository grupoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PagoRepository pagoRepository;

    @InjectMocks
    private GastoService gastoService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testCrearGastoConCategoriaComida() throws Exception {
        Usuario pagador = crearUsuario(1L, "Aimar");
        Grupo grupo = crearGrupo(10L, pagador);

        Gasto gasto = new Gasto();
        gasto.setConcepto("Cena");
        gasto.setMonto(30.0);
        gasto.setMoneda(Moneda.EURO);
        gasto.setGrupo(grupo);
        gasto.setPagador(pagador);
        gasto.setCategoria(CategoriaGasto.COMIDA);

        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(pagador));
        when(gastoRepository.save(any(Gasto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Gasto creado = gastoService.crear(gasto);

        assertNotNull(creado);
        assertEquals(CategoriaGasto.COMIDA, creado.getCategoria());
    }

    @Test
    public void testCrearVariosGastosConCategoriasDistintas() throws Exception {
        Usuario pagador = crearUsuario(1L, "Aimar");
        Grupo grupo = crearGrupo(20L, pagador);

        Gasto gastoOcio = new Gasto();
        gastoOcio.setConcepto("Cine");
        gastoOcio.setMonto(20.0);
        gastoOcio.setMoneda(Moneda.EURO);
        gastoOcio.setGrupo(grupo);
        gastoOcio.setPagador(pagador);
        gastoOcio.setCategoria(CategoriaGasto.OCIO);

        Gasto gastoTransporte = new Gasto();
        gastoTransporte.setConcepto("Metro");
        gastoTransporte.setMonto(10.0);
        gastoTransporte.setMoneda(Moneda.EURO);
        gastoTransporte.setGrupo(grupo);
        gastoTransporte.setPagador(pagador);
        gastoTransporte.setCategoria(CategoriaGasto.TRANSPORTE);

        when(grupoRepository.findById(20L)).thenReturn(Optional.of(grupo));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(pagador));
        when(gastoRepository.save(any(Gasto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Gasto creadoOcio = gastoService.crear(gastoOcio);
        Gasto creadoTransporte = gastoService.crear(gastoTransporte);

        assertEquals(CategoriaGasto.OCIO, creadoOcio.getCategoria());
        assertEquals(CategoriaGasto.TRANSPORTE, creadoTransporte.getCategoria());
    }

    @Test
    public void testCrearGastoSinCategoriaAsignaOtrosPorDefecto() throws Exception {
        Usuario pagador = crearUsuario(1L, "Aimar");
        Grupo grupo = crearGrupo(30L, pagador);

        Gasto gasto = new Gasto();
        gasto.setConcepto("Compra");
        gasto.setMonto(15.0);
        gasto.setMoneda(Moneda.EURO);
        gasto.setGrupo(grupo);
        gasto.setPagador(pagador);
        gasto.setCategoria(null);

        when(grupoRepository.findById(30L)).thenReturn(Optional.of(grupo));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(pagador));
        when(gastoRepository.save(any(Gasto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Gasto creado = gastoService.crear(gasto);

        assertEquals(CategoriaGasto.OTROS, creado.getCategoria());
    }

    private Usuario crearUsuario(Long id, String username) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setUsername(username);
        return usuario;
    }

    private Grupo crearGrupo(Long id, Usuario miembro) {
        Grupo grupo = new Grupo();
        grupo.setId(id);
        grupo.setMoneda(Moneda.EURO);
        grupo.getMiembros().add(miembro);
        return grupo;
    }
}
