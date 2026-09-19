package com.example.ronda.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class BorradorResponse {
    @SerializedName("borrador") private Borrador borrador;
    public Borrador getBorrador() { return borrador; }
    public static class Borrador {
        @SerializedName("paso") private int paso;
        @SerializedName("datos") private Map<String, Object> datos;
        public int getPaso() { return paso; }
        public Map<String, Object> getDatos() { return datos; }
    }
}
