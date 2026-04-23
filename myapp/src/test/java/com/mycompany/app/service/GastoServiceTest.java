package com.mycompany.app.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import com.mycompany.app.entity.Gasto;
import com.mycompany.app.repository.GastoRepository;
import java.util.Arrays;
import java.util.List;

public class GastoServiceTest {

    @Mock
    private GastoRepository gastoRepository; 

    @InjectMocks
    private GastoService gastoService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

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
}
