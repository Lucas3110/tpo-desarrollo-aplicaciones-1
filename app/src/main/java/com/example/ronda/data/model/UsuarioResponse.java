package com.example.ronda.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * El objeto "usuario" que devuelve la API.
 * Espeja el DTO del backend (src/dtos/usuarioDto.js): los nombres de los
 * campos tienen que coincidir con las claves del JSON, o Gson los deja en null.
 */
public class UsuarioResponse {

    @SerializedName("id")
    private int id;

    @SerializedName("email")
    private String email;

    @SerializedName("nombre")
    private String nombre;

    @SerializedName("telefono")
    private String telefono;

    /**
     * Puede ser null: el usuario recien registrado todavia no eligio zona.
     * OJO: es un objeto { id, nombre }, no un texto. Si se declara como String,
     * Gson tira JsonSyntaxException al parsear la sesion de cualquier usuario
     * con zona y el login termina en onFailure como si no hubiera conexion.
     */
    @SerializedName("zona")
    private ZonaResponse zona;

    @SerializedName("emailVerificado")
    private boolean emailVerificado;

    @SerializedName("creadoEn")
    private String creadoEn;

    public int getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getNombre() {
        return nombre;
    }

    public String getTelefono() {
        return telefono;
    }

    public ZonaResponse getZona() {
        return zona;
    }

    public boolean isEmailVerificado() {
        return emailVerificado;
    }

    public String getCreadoEn() {
        return creadoEn;
    }
}
