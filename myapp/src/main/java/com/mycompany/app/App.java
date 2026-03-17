package com.mycompany.app;

import com.mycompany.app.entity.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
        System.out.println("¡App de Tricount funcionando!");
        
        // Ejemplo de creación de objetos compatible con las nuevas Entidades
        Usuario adrien = new Usuario("Adrien", "adrien@mail.com", "password123");
        Grupo viaje = new Grupo("Viaje a Madrid");
        
        // Ahora el constructor de Gasto solo pide lo necesario
        Gasto cena = new Gasto("Cena japonesa", 45.50, adrien, viaje);
        
        System.out.println("Gasto creado: " + cena.getConcepto() + " por " + cena.getMonto() + "€");
    }
}