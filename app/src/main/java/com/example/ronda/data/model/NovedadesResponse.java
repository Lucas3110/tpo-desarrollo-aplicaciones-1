package com.example.ronda.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

/**
 * Respuesta 200 de GET /api/notificaciones (Punto 11). ultimoId es el cursor:
 * va como desdeId en el pedido siguiente.
 */
public class NovedadesResponse {

    @SerializedName("notificaciones")
    private List<NotificacionResponse> notificaciones;

    @SerializedName("ultimoId")
    private int ultimoId;

    public List<NotificacionResponse> getNotificaciones() {
        return notificaciones != null ? notificaciones : Collections.emptyList();
    }

    public int getUltimoId() {
        return ultimoId;
    }
}
