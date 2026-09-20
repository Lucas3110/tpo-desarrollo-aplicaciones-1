package com.example.ronda.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/** Respuesta de GET /usuarios/:id/calificaciones (publica, sin token). */
public class ListaCalificacionesResponse {

    @SerializedName("calificaciones")
    private List<CalificacionResponse> calificaciones;

    public List<CalificacionResponse> getCalificaciones() {
        return calificaciones != null ? calificaciones : new ArrayList<>();
    }
}
