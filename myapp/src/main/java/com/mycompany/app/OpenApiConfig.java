package com.mycompany.app;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Clon de Tricount - Proyecto PSC01")
                        .version("1.0")
                        .description("Documentación interactiva de los endpoints del servidor para la gestión de gastos compartidos, grupos y liquidación de deudas.")
                        .contact(new Contact()
                                .name("Equipo PSC01")
                                .email("adrien@demo.com")));
    }
}
