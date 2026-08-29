package com.example.ronda.data.model;

import com.google.gson.annotations.SerializedName;

/** Respuesta de POST /auth/otp/enviar. */
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
