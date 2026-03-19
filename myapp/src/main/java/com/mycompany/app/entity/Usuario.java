package com.mycompany.app.entity;

import jakarta.persistence.*;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String email;
    private String password;

    @ManyToMany(mappedBy = "miembros")
    private List<Grupo> grupos = new ArrayList<>();

    // Constructor vacío obligatorio para JPA
    public Usuario() {}

    public Usuario(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    // Getters y Setters básicos
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
}