package com.example.ronda.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Cuerpo de POST /usuarios/me/email/solicitar (Punto 2).
 *
 * Primer paso del cambio de email: el backend manda un codigo al email NUEVO
 * y todavia no cambia nada.
 */
public class CambiarEmailRequest {

    @SerializedName("emailNuevo")
    private final String emailNuevo;

    public CambiarEmailRequest(String emailNuevo) {
        this.emailNuevo = emailNuevo;
    }
}
