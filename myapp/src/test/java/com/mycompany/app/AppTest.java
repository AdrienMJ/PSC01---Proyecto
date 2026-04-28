package com.mycompany.app;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AppTest {

    @Test
    void contextLoads() {
        // Esta prueba simplemente verifica que la aplicación 
        // de Spring Boot puede arrancar sin errores.
    }

    @Test
    void mainMethodTest() {
        // Llamamos explícitamente al método main para cubrir sus líneas en JaCoCo.
        // Usamos --server.port=0 para que Spring Boot asigne un puerto aleatorio 
        // y no lance un error de "puerto en uso" durante la fase de tests.
        assertDoesNotThrow(() -> {
            App.main(new String[]{"--server.port=0"});
        }, "El método main debería ejecutarse sin lanzar excepciones");
    }
}