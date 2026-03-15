package com.mycompany.app;

import com.mycompany.app.entity.Grupo;
import com.mycompany.app.entity.Usuario;
import com.mycompany.app.entity.Gasto;
import com.mycompany.app.entity.Moneda;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class App {
    public static void main(String[] args) {
        // Crear grupo
        Grupo grupo = new Grupo("1", "Amigos Vacaciones", "VAC2023", Moneda.EURO);

        // Crear usuarios
        Usuario usuario1 = new Usuario("Alice", "alice@email.com", "pass1", grupo);
        Usuario usuario2 = new Usuario("Bob", "bob@email.com", "pass2", grupo);
        Usuario usuario3 = new Usuario("Charlie", "charlie@email.com", "pass3", grupo);

        // Agregar usuarios al grupo
        grupo.addUsuario(usuario1);
        grupo.addUsuario(usuario2);
        grupo.addUsuario(usuario3);

        // Crear gastos
        Gasto gasto1 = new Gasto("Cena", 60.0, "2023-07-01", usuario1, grupo);
        Gasto gasto2 = new Gasto("Hotel", 120.0, "2023-07-02", usuario2, grupo);
        Gasto gasto3 = new Gasto("Transporte", 30.0, "2023-07-03", usuario3, grupo);

        List<Gasto> gastos = new ArrayList<>();
        gastos.add(gasto1);
        gastos.add(gasto2);
        gastos.add(gasto3);

        // Calcular balances
        Map<Usuario, Double> balances = calcularBalances(gastos);

        // Mostrar resultados
        System.out.println("Balances del grupo " + grupo.getNombre() + ":");
        for (Map.Entry<Usuario, Double> entry : balances.entrySet()) {
            Usuario usuario = entry.getKey();
            double balance = entry.getValue();
            if (balance > 0) {
                System.out.println(usuario.getNombreUsuario() + " debe recibir " + balance + " " + grupo.getMoneda());
            } else if (balance < 0) {
                System.out.println(usuario.getNombreUsuario() + " debe pagar " + (-balance) + " " + grupo.getMoneda());
            } else {
                System.out.println(usuario.getNombreUsuario() + " está al día");
            }
        }
    }

    private static Map<Usuario, Double> calcularBalances(List<Gasto> gastos) {
        Map<Usuario, Double> balances = new HashMap<>();

        for (Gasto gasto : gastos) {
            Usuario pagador = gasto.getPagadoPor();
            Grupo grupo = gasto.getGrupo();
            List<Usuario> miembros = grupo.getUsuarios();
            int numMiembros = miembros.size();
            double cantidadPorPersona = gasto.getGasto() / numMiembros;

            // El pagador recibe el total, luego paga su parte
            balances.put(pagador, balances.getOrDefault(pagador, 0.0) + gasto.getGasto() - cantidadPorPersona);

            // Los otros miembros deben cantidadPorPersona al pagador
            for (Usuario miembro : miembros) {
                if (!miembro.equals(pagador)) {
                    balances.put(miembro, balances.getOrDefault(miembro, 0.0) - cantidadPorPersona);
                }
            }
        }

        return balances;
    }
}