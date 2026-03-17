package com.mycompany.app.entity;

import jakarta.persistence.*;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "grupos")
public class Grupo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;

    @ManyToMany
    @JoinTable(
      name = "grupo_usuarios",
      joinColumns = @JoinColumn(name = "grupo_id"),
      inverseJoinColumns = @JoinColumn(name = "usuario_id")
    )
    private List<Usuario> miembros = new ArrayList<>();

    @OneToMany(mappedBy = "grupo")
    private List<Gasto> gastos = new ArrayList<>();

    public Grupo() {}
    public Grupo(String nombre) { this.nombre = nombre; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public List<Usuario> getMiembros() { return miembros; }
}