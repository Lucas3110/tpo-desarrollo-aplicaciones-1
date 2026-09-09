package com.example.ronda.data.model;
import com.google.gson.annotations.SerializedName;
import java.util.List;
public class ListaPreguntasResponse {
    @SerializedName("preguntas") private List<PreguntaResponse> preguntas;
    public List<PreguntaResponse> getPreguntas() { return preguntas; }
}