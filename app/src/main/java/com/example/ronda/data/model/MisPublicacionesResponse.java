package com.example.ronda.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.Collections;
import java.util.List;

public class MisPublicacionesResponse {
    @SerializedName("items") private List<PublicacionItemResponse> items;
    @SerializedName("resumen") private Resumen resumen;
    public List<PublicacionItemResponse> getItems() { return items == null ? Collections.emptyList() : items; }
    public Resumen getResumen() { return resumen; }
    public static class Resumen {
        @SerializedName("activas") private int activas;
        @SerializedName("pausadas") private int pausadas;
        @SerializedName("vendidas") private int vendidas;
        @SerializedName("total") private int total;
        public int getActivas() { return activas; }
        public int getPausadas() { return pausadas; }
        public int getVendidas() { return vendidas; }
        public int getTotal() { return total; }
    }
}
