package com.example.ronda.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Cuerpo de POST /usuarios/me/email/confirmar (Punto 2).
 *
 * Segundo paso del cambio de email: con el codigo que llego al email nuevo,
 * la cuenta pasa a esa direccion.
 */
public class ConfirmarEmailRequest {

    @SerializedName("codigo")
    private final String codigo;

    public ConfirmarEmailRequest(String codigo) {
        this.codigo = codigo;
    }
}
