package com.mycompany.app.entity;

import java.util.List;
import java.util.ArrayList;

public class Grupo {
    private String id;
    private String nombre;
    private String codigoGrupo;
    private Moneda moneda;
    private List<Usuario> usuarios;

    public Grupo(String id, String nombre, String codigoGrupo, Moneda moneda) {
        this.id = id;
        this.nombre = nombre;
        this.codigoGrupo = codigoGrupo;
        this.moneda = moneda;
        this.usuarios = new ArrayList<>();
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCodigoGrupo() {
        return codigoGrupo;
    }

    public void setCodigoGrupo(String codigoGrupo) {
        this.codigoGrupo = codigoGrupo;
    }

    public Moneda getMoneda() {
        return moneda;
    }

    public void setMoneda(Moneda moneda) {
        this.moneda = moneda;
    }

    public List<Usuario> getUsuarios() {
        return usuarios;
    }

    public void addUsuario(Usuario usuario) {
        this.usuarios.add(usuario);
    }

    public void removeUsuario(Usuario usuario) {
        this.usuarios.remove(usuario);
    }
}