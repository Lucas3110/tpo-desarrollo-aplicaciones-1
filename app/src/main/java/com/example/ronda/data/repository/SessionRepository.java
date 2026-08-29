package com.example.ronda.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Guarda la sesion en el celular.
 *
 * SharedPreferences viene incluido en Android (no es una libreria extra) y
 * persiste en disco, asi que el token sobrevive a cerrar y volver a abrir
 * la app. Eso es lo que permite el auto-login.
 */
public class SessionRepository {

    private static final String ARCHIVO = "sesion_ronda";
    private static final String CLAVE_TOKEN = "token";
    private static final String CLAVE_EMAIL = "email";

    private final SharedPreferences prefs;

    public SessionRepository(Context contexto) {
        // getApplicationContext evita quedarse con una referencia al Fragment.
        prefs = contexto.getApplicationContext()
                .getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE);
    }

    public void guardarSesion(String token, String email) {
        prefs.edit()
                .putString(CLAVE_TOKEN, token)
                .putString(CLAVE_EMAIL, email)
                .apply();
    }

    public String getToken() {
        return prefs.getString(CLAVE_TOKEN, null);
    }

    public String getEmail() {
        return prefs.getString(CLAVE_EMAIL, null);
    }

    public boolean haySesion() {
        return getToken() != null;
    }

    /** Valor exacto que espera la cabecera Authorization. */
    public String getBearer() {
        return "Bearer " + getToken();
    }

    public void cerrarSesion() {
        prefs.edit().clear().apply();
    }
}
