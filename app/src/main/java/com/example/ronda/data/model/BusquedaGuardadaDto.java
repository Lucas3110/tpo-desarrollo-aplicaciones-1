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

    /** Los filtros en una linea legible; lo arma el backend. */
    @SerializedName("resumen")
    private String resumen;

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

    public String getResumen() {
        return resumen;
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
