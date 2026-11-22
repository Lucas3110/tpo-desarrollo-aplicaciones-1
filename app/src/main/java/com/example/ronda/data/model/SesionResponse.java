package com.example.ronda.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Respuesta de POST /api/auth/login y de POST /api/auth/otp/verificar:
 * el token (JWT) y los datos de la persona.
 *
 * Los dos endpoints devuelven la misma forma a propósito, así que alcanza
 * con una sola clase para los dos caminos de ingreso.
 */
public class SesionResponse {

    @SerializedName("token")
    private String token;

    @SerializedName("usuario")
    private UsuarioResponse usuario;

    public String getToken() {
        return token;
    }

    public UsuarioResponse getUsuario() {
        return usuario;
    }
}
