package com.mycompany.app;
//Hola prueba
import com.mycompany.app.entity.Usuario;
import com.mycompany.app.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public void run(String... args) {
        if (usuarioRepository.findByEmail("test@mail.com").isEmpty()) {
            usuarioRepository.save(new Usuario("Prueba", "test@mail.com", "000"));
        }
        if (usuarioRepository.findByEmail("adrien@mail.com").isEmpty()) {
            usuarioRepository.save(new Usuario("Adrien", "adrien@mail.com", "123"));
        }
        if (usuarioRepository.findByEmail("adrien@gmail.com").isEmpty()) {
            usuarioRepository.save(new Usuario("Adrien", "adrien@gmail.com", "123"));
        }
    }
}
