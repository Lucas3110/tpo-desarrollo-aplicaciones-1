package com.example.ronda.data.network;

import com.example.ronda.data.model.ErrorResponse;
import com.google.gson.Gson;

import retrofit2.Response;

/**
 * Cuando la API responde 4xx o 5xx, Retrofit NO parsea el cuerpo: lo deja
 * crudo en response.errorBody(). Esta clase lo convierte al
 * { error: { codigo, mensaje } } que devuelve el backend.
 *
 * IMPORTANTE: el errorBody es un stream y solo se puede leer UNA vez. La
 * segunda lectura tira IllegalStateException("closed") y se pierde el error
 * real. Por eso el uso correcto es:
 *
 *     Detalle error = ApiErrorParser.parse(response);   // se lee aca, una sola vez
 *     String codigo  = ApiErrorParser.codigo(error);
 *     String mensaje = ApiErrorParser.mensaje(error, "texto por defecto");
 *
 * Las funciones codigo() y mensaje() reciben el Detalle ya parseado, no el
 * Response, justamente para que no se pueda leer el cuerpo dos veces.
 */
public class ApiErrorParser {

    /** Lee el cuerpo del error UNA vez. Devuelve null si no se pudo interpretar. */
    public static ErrorResponse.Detalle parse(Response<?> response) {
        if (response == null || response.errorBody() == null) {
            return null;
        }
        try {
            String json = response.errorBody().string();
            ErrorResponse error = new Gson().fromJson(json, ErrorResponse.class);
            if (error != null && error.getError() != null) {
                return error.getError();
            }
        } catch (Exception ignorada) {
            // El cuerpo no era el JSON que esperabamos: caemos al valor por defecto.
        }
        return null;
    }

    /** Codigo estable del error (EMAIL_EN_USO, OTP_EXPIRADO, ...) o null. */
    public static String codigo(ErrorResponse.Detalle detalle) {
        return detalle != null ? detalle.getCodigo() : null;
    }

    /** Mensaje que mando el backend, o el de respaldo si no se pudo leer. */
    public static String mensaje(ErrorResponse.Detalle detalle, String porDefecto) {
        if (detalle != null && detalle.getMensaje() != null) {
            return detalle.getMensaje();
        }
        return porDefecto;
    }
}
