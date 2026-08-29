package com.example.ronda.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Respuesta de POST /auth/login y de POST /auth/otp/verificar.
 * Los dos endpoints devuelven la misma forma a proposito, asi que alcanza
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
