package com.example.ronda.data.model;
import com.google.gson.annotations.SerializedName;
public class ResponderRequest {
    @SerializedName("respuesta") private String respuesta;
    public ResponderRequest(String respuesta) { this.respuesta = respuesta; }
}