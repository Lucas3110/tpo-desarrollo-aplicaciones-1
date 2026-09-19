package com.example.ronda.data.model;

import com.google.gson.annotations.SerializedName;

public class CambiarEstadoRequest {
    @SerializedName("estado") private final String estado;
    public CambiarEstadoRequest(String estado) { this.estado = estado; }
}
