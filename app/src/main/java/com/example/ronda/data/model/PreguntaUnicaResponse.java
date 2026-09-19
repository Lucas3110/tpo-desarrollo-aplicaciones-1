package com.example.ronda.data.model;
import com.google.gson.annotations.SerializedName;
public class PreguntaUnicaResponse {
    @SerializedName("pregunta") private PreguntaResponse pregunta;
    public PreguntaResponse getPregunta() { return pregunta; }
}