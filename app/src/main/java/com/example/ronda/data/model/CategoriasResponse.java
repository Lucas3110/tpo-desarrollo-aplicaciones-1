package com.example.ronda.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Respuesta de GET /categorias. El backend no devuelve la lista pelada sino
 * envuelta: { "categorias": [ { "id": 1, "nombre": "Celulares" }, ... ] }
 */
public class CategoriasResponse {

    @SerializedName("categorias")
    private List<CategoriaResponse> categorias;

    public List<CategoriaResponse> getCategorias() {
        return categorias;
    }
}
