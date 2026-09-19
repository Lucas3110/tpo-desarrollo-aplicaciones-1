package com.example.ronda.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PublicacionRequest {
    @SerializedName("titulo") private final String titulo;
    @SerializedName("descripcion") private final String descripcion;
    @SerializedName("categoriaId") private final int categoriaId;
    @SerializedName("precio") private final double precio;
    @SerializedName("estadoArticulo") private final String estadoArticulo;
    @SerializedName("zonaId") private final int zonaId;
    @SerializedName("fotos") private final List<String> fotos;
    @SerializedName("direccion") private final String direccion;
    @SerializedName("latitud") private final Double latitud;
    @SerializedName("longitud") private final Double longitud;

    public PublicacionRequest(String titulo, String descripcion, int categoriaId,
                              double precio, String estadoArticulo, int zonaId,
                              List<String> fotos, String direccion, Double latitud, Double longitud) {
        this.titulo = titulo; this.descripcion = descripcion; this.categoriaId = categoriaId;
        this.precio = precio; this.estadoArticulo = estadoArticulo; this.zonaId = zonaId;
        this.fotos = fotos;
        this.direccion = direccion == null || direccion.trim().isEmpty() ? null : direccion.trim();
        if (this.direccion != null && this.direccion.length() > 255) {
            throw new IllegalArgumentException("Dirección demasiado larga");
        }
        boolean parValido = coordenadasValidas(latitud, longitud);
        this.latitud = parValido ? latitud : null;
        this.longitud = parValido ? longitud : null;
    }

    public static boolean coordenadasValidas(Double latitud, Double longitud) {
        return latitud != null && longitud != null
                && !latitud.isNaN() && !longitud.isNaN()
                && latitud >= -90 && latitud <= 90
                && longitud >= -180 && longitud <= 180;
    }
}
