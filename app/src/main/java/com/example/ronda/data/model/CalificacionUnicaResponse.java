package com.example.ronda.data.model;

import com.google.gson.annotations.SerializedName;

/** Respuesta de POST /operaciones/:id/calificacion: { "calificacion": { ... } }. */
public class CalificacionUnicaResponse {

    @SerializedName("calificacion")
    private CalificacionResponse calificacion;

    public CalificacionResponse getCalificacion() {
        return calificacion;
    }
}
