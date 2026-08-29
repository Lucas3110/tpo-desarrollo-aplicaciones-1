package com.example.ronda.data.model;

import com.google.gson.annotations.SerializedName;

/** Respuesta de GET /auth/me. */
public class PerfilResponse {

    @SerializedName("usuario")
    private UsuarioResponse usuario;

    public UsuarioResponse getUsuario() {
        return usuario;
    }
}
