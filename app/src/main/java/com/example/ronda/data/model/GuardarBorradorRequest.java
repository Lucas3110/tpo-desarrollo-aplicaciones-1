package com.example.ronda.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class GuardarBorradorRequest {
    @SerializedName("paso") private final int paso;
    @SerializedName("datos") private final Map<String, Object> datos;
    public GuardarBorradorRequest(int paso, Map<String, Object> datos) {
        this.paso = paso; this.datos = datos;
    }
}
