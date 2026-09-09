package com.example.ronda.data.model;
import com.google.gson.annotations.SerializedName;
public class AutorResponse {
    @SerializedName("id") private int id;
    @SerializedName("nombre") private String nombre;
    public int getId() { return id; }
    public String getNombre() { return nombre; }
}