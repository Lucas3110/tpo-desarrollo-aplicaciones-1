package com.example.ronda.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Una zona del catalogo (Palermo, Quilmes, Villa Urquiza...).
 *
 * La API la devuelve siempre con esta forma, tanto adentro del usuario
 * ("zona": { "id": 6, "nombre": "Palermo" }) como en cada publicacion y en
 * el catalogo de GET /zonas. Por eso es una clase aparte y no un String.
 */
public class ZonaResponse {

    @SerializedName("id")
    private int id;

    @SerializedName("nombre")
    private String nombre;

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }
}
