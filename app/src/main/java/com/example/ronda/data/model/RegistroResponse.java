package com.example.ronda.data.model;

import com.google.gson.annotations.SerializedName;

/** Respuesta de POST /auth/registro. */
public class RegistroResponse {

    @SerializedName("mensaje")
    private String mensaje;

    @SerializedName("usuario")
    private UsuarioResponse usuario;

    /**
     * Solo viene cuando el backend corre con OTP_EXPOSE_IN_RESPONSE=true.
     * Sirve para probar sin abrir el mail; en produccion es null.
     */
    @SerializedName("codigoDesarrollo")
    private String codigoDesarrollo;

    public String getMensaje() {
        return mensaje;
    }

    public UsuarioResponse getUsuario() {
        return usuario;
    }

    public String getCodigoDesarrollo() {
        return codigoDesarrollo;
    }
}
