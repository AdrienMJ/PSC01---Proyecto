package com.mycompany.app.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "grupos")
public class Grupo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String nombre;

    @Column(name = "id_creador")
    private Long idCreador;

    @Enumerated(EnumType.STRING)//Guarda el nombre de la moneda, no el índice
    private Moneda moneda;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
      name = "grupo_usuarios",
      joinColumns = @JoinColumn(name = "grupo_id"),
      inverseJoinColumns = @JoinColumn(name = "usuario_id")
    )
    private List<Usuario> miembros = new ArrayList<>();

    @OneToMany(mappedBy = "grupo", cascade = CascadeType.ALL)
    private List<Gasto> gastos = new ArrayList<>();

    public Grupo() {}

    public Grupo(String nombre, Moneda moneda) {
        this.nombre = nombre;
        this.moneda = moneda;
    }

    // Método de conveniencia para añadir miembros (Sincroniza ambos lados)
    public void addMiembro(Usuario usuario) {
        this.miembros.add(usuario);
        usuario.getGrupos().add(this);
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Long getIdCreador() { return idCreador; }
    public void setIdCreador(Long idCreador) { this.idCreador = idCreador; }
    public Moneda getMoneda() { return moneda; }
    public void setMoneda(Moneda moneda) { this.moneda = moneda; }
    public List<Usuario> getMiembros() { return miembros; }
    public void setMiembros(List<Usuario> miembros) {this.miembros = miembros;}
    public List<Gasto> getGastos() { return gastos; }
}