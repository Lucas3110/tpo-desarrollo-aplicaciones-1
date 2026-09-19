package com.example.ronda.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class BusquedaGuardadaDto {
    @SerializedName("id")
    private int id;

    @SerializedName("nombre")
    private String nombre;

    @SerializedName("filtros")
    private Map<String, String> filtros;

    @SerializedName("novedades")
    private int novedades;

    @SerializedName("ultimoVistoEn")
    private String ultimoVistoEn;

    @SerializedName("creadoEn")
    private String creadoEn;

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public Map<String, String> getFiltros() {
        return filtros;
    }

    public int getNovedades() {
        return novedades;
    }

    public String getUltimoVistoEn() {
        return ultimoVistoEn;
    }

    public String getCreadoEn() {
        return creadoEn;
    }
}
