package com.example.ronda.data.model;
import com.google.gson.annotations.SerializedName;
public class EstadoOfertaRequest {
    @SerializedName("estado") private String estado;
    public EstadoOfertaRequest(String estado) { this.estado = estado; }
}