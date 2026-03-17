package com.mycompany.app;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AppTest {

    @Test
    void contextLoads() {
        // Esta prueba simplemente verifica que la aplicación 
        // de Spring Boot puede arrancar sin errores.
        assertTrue(true);
    }
}