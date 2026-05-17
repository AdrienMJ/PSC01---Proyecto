package com.mycompany.app.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String email;
    @JsonProperty(access = Access.WRITE_ONLY)
    private String password;

    @Enumerated(EnumType.STRING)
    private Moneda monedaPredeterminada = Moneda.EURO;

    @ManyToMany(mappedBy = "miembros")
    @JsonIgnore
    private List<Grupo> grupos = new ArrayList<>();

    @ManyToMany(fetch = jakarta.persistence.FetchType.EAGER)
    @JoinTable(
        name = "usuario_contactos",
        joinColumns = @JoinColumn(name = "usuario_id"),
        inverseJoinColumns = @JoinColumn(name = "contacto_id")
    )
    @JsonIgnore
    private List<Usuario> contactos = new ArrayList<>();

    public Usuario() {}

    public Usuario(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Moneda getMonedaPredeterminada() { return monedaPredeterminada; }
    public void setMonedaPredeterminada(Moneda monedaPredeterminada) { this.monedaPredeterminada = monedaPredeterminada; }
    public List<Grupo> getGrupos() { return grupos; }
    public List<Usuario> getContactos() { return contactos; }
    public void setContactos(List<Usuario> contactos) { this.contactos = contactos; }
}