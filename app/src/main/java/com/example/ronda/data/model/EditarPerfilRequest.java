package com.example.ronda.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Cuerpo de PUT /usuarios/me (Punto 2).
 *
 * El email no va: cambiarlo obligaria a verificarlo de nuevo con un OTP, asi
 * que el backend lo ignora. Telefono y zonaId son opcionales: si van en null,
 * el backend los deja vacios.
 *
 * fotoUrl: el backend reemplaza la foto en CADA guardado, asi que si no se
 * manda la actual se borra. Siempre hay que mandarla; para quitarla, cadena
 * vacia (Gson no serializa los null).
 */
public class EditarPerfilRequest {

    @SerializedName("nombre")
    private final String nombre;

    @SerializedName("telefono")
    private final String telefono;

    @SerializedName("zonaId")
    private final Integer zonaId;

    @SerializedName("fotoUrl")
    private final String fotoUrl;

    public EditarPerfilRequest(String nombre, String telefono, Integer zonaId, String fotoUrl) {
        this.nombre = nombre;
        this.telefono = telefono;
        this.zonaId = zonaId;
        this.fotoUrl = fotoUrl != null ? fotoUrl : "";
    }
}
