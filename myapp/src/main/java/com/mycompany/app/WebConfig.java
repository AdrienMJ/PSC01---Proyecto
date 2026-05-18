package com.mycompany.app;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.uploads.dir}")
    private String uploadsDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = "file:" + uploadsDir.replace("\\", "/") + "/";
        String perfilesLocation = "file:" + uploadsDir.replace("\\", "/") + "/perfiles/";
        registry.addResourceHandler("/tickets/**")
                .addResourceLocations(location);
        registry.addResourceHandler("/perfiles/**")
            .addResourceLocations(perfilesLocation);
    }
}