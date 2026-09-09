package com.example.ronda.data.model;
import com.google.gson.annotations.SerializedName;
public class PreguntarRequest {
    @SerializedName("texto") private String texto;
    public PreguntarRequest(String texto) { this.texto = texto; }
}