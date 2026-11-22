package com.example.ronda.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Forma única de los errores de la API:
 *
 *   { "error": { "codigo": "OTP_EXPIRADO", "mensaje": "El código venció..." } }
 *
 * Importante: la app decide qué hacer según el "codigo", que es estable.
 * El "mensaje" es texto que puede cambiar: sirve para mostrarlo tal cual
 * cuando no hace falta un tratamiento especial.
 *
 * La lee ApiErrorParser.
 */
public class ErrorResponse {

    @SerializedName("error")
    private Detalle error;

    public Detalle getError() {
        return error;
    }

    /** Lo que viene adentro de "error". Es una clase dentro de otra (static). */
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
