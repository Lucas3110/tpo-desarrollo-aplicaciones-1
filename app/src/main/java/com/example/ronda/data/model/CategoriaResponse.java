package com.example.ronda.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Una categoria del catalogo (Celulares, Computacion, Hogar y muebles...).
 * Aparece embebida en cada publicacion y como lista en GET /categorias.
 */
public class CategoriaResponse {

    @SerializedName("id")
    private int id;

    @SerializedName("nombre")
    private String nombre;

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }
}
