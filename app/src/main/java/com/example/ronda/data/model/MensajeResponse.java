package com.example.ronda.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Respuesta de POST /api/auth/otp/enviar: un mensaje para mostrar y, solo en
 * desarrollo, el código (igual que en RegistroResponse).
 */
public class MensajeResponse {

    @SerializedName("mensaje")
    private String mensaje;

    @SerializedName("codigoDesarrollo")
    private String codigoDesarrollo;

    public String getMensaje() {
        return mensaje;
    }

    public String getCodigoDesarrollo() {
        return codigoDesarrollo;
    }
}
