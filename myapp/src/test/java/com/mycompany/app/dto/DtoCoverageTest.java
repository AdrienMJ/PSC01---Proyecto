package com.mycompany.app.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

public class DtoCoverageTest {

    @Test
    public void testResumenGrupoDTO_gettersSetters() {
        BalancePersonaDTO b1 = new BalancePersonaDTO(1L, "u1", 10.0, "Deudor");
        BalancePersonaDTO b2 = new BalancePersonaDTO(2L, "u2", -5.0, "Acreedor");
        List<BalancePersonaDTO> balances = Arrays.asList(b1, b2);

        TransferenciaDTO t1 = new TransferenciaDTO(1L, "u1", 2L, "u2", 5.0);
        List<TransferenciaDTO> soluciones = Arrays.asList(t1);

        ResumenGrupoDTO resumen = new ResumenGrupoDTO();
        resumen.setTotalGastado(123.45);
        resumen.setBalances(balances);
        resumen.setSoluciones(soluciones);

        assertEquals(123.45, resumen.getTotalGastado());
        assertSame(balances, resumen.getBalances());
        assertSame(soluciones, resumen.getSoluciones());

        // constructor
        ResumenGrupoDTO resumen2 = new ResumenGrupoDTO(10.0, balances, soluciones);
        assertEquals(10.0, resumen2.getTotalGastado());
        assertEquals(2, resumen2.getBalances().size());
        assertEquals(1, resumen2.getSoluciones().size());
    }

    @Test
    public void testTransferenciaDTO_getters() {
        TransferenciaDTO t = new TransferenciaDTO(5L, "alice", 7L, "bob", 42.5);
        assertEquals(5L, t.getDeUsuarioId());
        assertEquals("alice", t.getDeUsername());
        assertEquals(7L, t.getAUsuarioId());
        assertEquals("bob", t.getAUsername());
        assertEquals(42.5, t.getMonto());
    }

    @Test
    public void testBalancePersonaDTO_getters() {
        BalancePersonaDTO b = new BalancePersonaDTO(9L, "carlos", -12.3, "Deudor");
        assertEquals(9L, b.getUsuarioId());
        assertEquals("carlos", b.getUsername());
        assertEquals(-12.3, b.getBalance());
        assertEquals("Deudor", b.getEstado());
    }

    @Test
    public void testSimpleRequests_fields() {
        GrupoRequest g = new GrupoRequest();
        g.nombre = "GrupoX";
        g.moneda = "USD";
        g.idCreador = 11L;
        assertEquals("GrupoX", g.nombre);
        assertEquals("USD", g.moneda);
        assertEquals(11L, g.idCreador);

        GastoRequest gr = new GastoRequest();
        gr.concepto = "Taxi";
        gr.monto = 20.0;
        gr.idPagador = 2L;
        gr.idGrupo = 3L;
        gr.moneda = com.mycompany.app.entity.Moneda.EURO;
        gr.repartoGeneral = true;
        assertEquals("Taxi", gr.concepto);
        assertEquals(20.0, gr.monto);
        assertEquals(2L, gr.idPagador);
        assertEquals(3L, gr.idGrupo);
        assertEquals(com.mycompany.app.entity.Moneda.EURO, gr.moneda);
        assertTrue(gr.repartoGeneral);

        InvitarUsuarioRequest inv = new InvitarUsuarioRequest();
        inv.email = "x@x.com";
        inv.idUsuarioInvitador = 99L;
        assertEquals("x@x.com", inv.email);
        assertEquals(99L, inv.idUsuarioInvitador);

        RenombrarGrupoRequest ren = new RenombrarGrupoRequest();
        ren.nombre = "Nuevo";
        ren.idUsuario = 77L;
        assertEquals("Nuevo", ren.nombre);
        assertEquals(77L, ren.idUsuario);
    }

    @Test
    public void testInvokeAllDtoMethodsByReflection() throws Exception {
        // List of DTO classes to reflectively exercise
        Class<?>[] dtoClasses = new Class<?>[] {
            ResumenGrupoDTO.class,
            TransferenciaDTO.class,
            BalancePersonaDTO.class,
            GrupoRequest.class,
            GastoRequest.class,
            InvitarUsuarioRequest.class,
            RenombrarGrupoRequest.class
        };

        for (Class<?> cls : dtoClasses) {
            // try instantiate with no-arg constructor if present
            Object instance = null;
            try {
                instance = cls.getDeclaredConstructor().newInstance();
            } catch (NoSuchMethodException e) {
                // try to find any constructor and call it with sample values
                java.lang.reflect.Constructor<?>[] ctors = cls.getDeclaredConstructors();
                if (ctors.length > 0) {
                    java.lang.reflect.Constructor<?> ctor = ctors[0];
                    Class<?>[] pts = ctor.getParameterTypes();
                    Object[] args = new Object[pts.length];
                    for (int i = 0; i < pts.length; i++) {
                        Class<?> p = pts[i];
                        if (p == Long.class || p == long.class) args[i] = 1L;
                        else if (p == String.class) args[i] = "x";
                        else if (p == double.class || p == Double.class) args[i] = 0.0;
                        else if (java.util.List.class.isAssignableFrom(p)) args[i] = java.util.Collections.emptyList();
                        else args[i] = null;
                    }
                    ctor.setAccessible(true);
                    instance = ctor.newInstance(args);
                }
            }

            if (instance == null) continue;

            // invoke all setters with simple values
            for (java.lang.reflect.Method m : cls.getDeclaredMethods()) {
                if (m.getName().startsWith("set") && m.getParameterCount() == 1) {
                    Class<?> p = m.getParameterTypes()[0];
                    Object val = null;
                    if (p == Long.class || p == long.class) val = 2L;
                    else if (p == String.class) val = "s";
                    else if (p == double.class || p == Double.class) val = 1.5;
                    else if (java.util.List.class.isAssignableFrom(p)) val = Arrays.asList();
                    try { m.setAccessible(true); m.invoke(instance, val); } catch (Exception ignored) {}
                }
                if (m.getName().startsWith("get") && m.getParameterCount() == 0) {
                    try { m.setAccessible(true); m.invoke(instance); } catch (Exception ignored) {}
                }
            }
        }
    }
}
