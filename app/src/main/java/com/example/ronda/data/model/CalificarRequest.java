package com.example.ronda.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Cuerpo de POST /operaciones/:id/calificacion (Punto 9).
 *
 * El rol no se manda: si quien califica es el comprador esta calificando al
 * vendedor, y al reves, y eso ya lo sabe el backend. El comentario es
 * opcional (maximo 500): en null Gson no lo incluye.
 */
public class CalificarRequest {

    @SerializedName("estrellas")
    private final int estrellas;

    @SerializedName("comentario")
    private final String comentario;

    public CalificarRequest(int estrellas, String comentario) {
        this.estrellas = estrellas;
        this.comentario = comentario;
    }
}
