package com.example.ronda.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Respuesta de GET /zonas: { "zonas": [ { "id": 6, "nombre": "Palermo", ... } ] }
 *
 * Cada zona trae ademas latitud y longitud, pero la app no las necesita:
 * Gson ignora los campos del JSON que la clase no declara.
 */
public class ZonasResponse {

    @SerializedName("zonas")
    private List<ZonaResponse> zonas;

    public List<ZonaResponse> getZonas() {
        return zonas;
    }
}
