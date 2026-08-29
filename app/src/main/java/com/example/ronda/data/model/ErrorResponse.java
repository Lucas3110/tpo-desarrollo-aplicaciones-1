package com.example.ronda.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Forma unica de los errores de la API:
 *
 *   { "error": { "codigo": "OTP_EXPIRADO", "mensaje": "El codigo vencio..." } }
 *
 * Importante: la UI decide que mostrar segun el "codigo", que es estable.
 * El "mensaje" es texto que puede cambiar, sirve para mostrarlo tal cual
 * cuando no necesitamos un tratamiento especial.
 */
public class ErrorResponse {

    @SerializedName("error")
    private Detalle error;

    public Detalle getError() {
        return error;
    }

    public static class Detalle {

        @SerializedName("codigo")
        private String codigo;

        @SerializedName("mensaje")
        private String mensaje;

        public String getCodigo() {
            return codigo;
        }

        public String getMensaje() {
            return mensaje;
        }
    }
}
