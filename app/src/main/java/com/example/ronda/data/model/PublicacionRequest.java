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

    public PublicacionRequest(String titulo, String descripcion, int categoriaId,
                              double precio, String estadoArticulo, int zonaId,
                              List<String> fotos) {
        this.titulo = titulo; this.descripcion = descripcion; this.categoriaId = categoriaId;
        this.precio = precio; this.estadoArticulo = estadoArticulo; this.zonaId = zonaId;
        this.fotos = fotos;
    }
}
