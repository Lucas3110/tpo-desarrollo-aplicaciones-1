package com.example.ronda.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Cuerpo de PUT /usuarios/me (Punto 2).
 *
 * El email no va: cambiarlo obligaria a verificarlo de nuevo con un OTP, asi
 * que el backend lo ignora. Telefono y zonaId son opcionales: si van en null,
 * el backend los deja vacios.
 */
public class EditarPerfilRequest {

    @SerializedName("nombre")
    private final String nombre;

    @SerializedName("telefono")
    private final String telefono;

    @SerializedName("zonaId")
    private final Integer zonaId;

    public EditarPerfilRequest(String nombre, String telefono, Integer zonaId) {
        this.nombre = nombre;
        this.telefono = telefono;
        this.zonaId = zonaId;
    }
}
