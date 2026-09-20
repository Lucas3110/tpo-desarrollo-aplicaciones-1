package com.example.ronda.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class GuardarBusquedaRequest {
    @SerializedName("nombre")
    private String nombre;

    @SerializedName("filtros")
    private Map<String, String> filtros;

    public GuardarBusquedaRequest(String nombre, Map<String, String> filtros) {
        this.nombre = nombre;
        this.filtros = filtros;
    }

    public String getNombre() {
        return nombre;
    }

    public Map<String, String> getFiltros() {
        return filtros;
    }
}
