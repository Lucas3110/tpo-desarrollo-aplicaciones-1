package com.example.ronda.data.model;

import com.google.gson.annotations.SerializedName;

/** Sólo llega del servidor para el vendedor o el comprador con oferta aceptada. */
public class EntregaResponse {
    @SerializedName("direccion") private String direccion;
    @SerializedName("latitud") private Double latitud;
    @SerializedName("longitud") private Double longitud;

    public String getDireccion() { return direccion == null ? "" : direccion.trim(); }
    public boolean tieneCoordenadas() {
        return latitud != null && longitud != null
                && !latitud.isNaN() && !longitud.isNaN()
                && latitud >= -90 && latitud <= 90
                && longitud >= -180 && longitud <= 180;
    }
    public String getDestino() {
        return tieneCoordenadas() ? latitud + "," + longitud : getDireccion();
    }
    public boolean tieneDestino() { return !getDestino().isEmpty(); }
}
