package com.mycompany.app.repository;

import com.mycompany.app.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    // Esto te servirá para evitar que dos personas se registren con el mismo email
    Optional<Usuario> findByEmail(String email);
}
