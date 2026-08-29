package com.example.ronda.data.network;

import com.example.ronda.data.model.ErrorResponse;
import com.google.gson.Gson;

import retrofit2.Response;

/**
 * Cuando la API responde 4xx o 5xx, Retrofit NO parsea el cuerpo: lo deja
 * crudo en response.errorBody(). Esta clase lo convierte al
 * { error: { codigo, mensaje } } que devuelve el backend.
 */
public class ApiErrorParser {

    /** Devuelve el detalle del error, o null si no se pudo leer. */
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
    public static String codigoDe(Response<?> response) {
        ErrorResponse.Detalle detalle = parse(response);
        return detalle != null ? detalle.getCodigo() : null;
    }

    /** Mensaje del backend, o el que le pasemos si no se pudo leer. */
    public static String mensajeDe(Response<?> response, String porDefecto) {
        ErrorResponse.Detalle detalle = parse(response);
        if (detalle != null && detalle.getMensaje() != null) {
            return detalle.getMensaje();
        }
        return porDefecto;
    }
}
