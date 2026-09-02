package com.example.ronda.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Guarda la sesión en el celular.
 *
 * SharedPreferences viene incluido en Android (no es una librería extra) y
 * persiste en disco, así que el token sobrevive a cerrar y volver a abrir la
 * app. Eso es lo que permite el auto-login.
 *
 * Con Hilt ya no se instancia a mano: se pide con @Inject y el
 * @ApplicationContext lo provee el framework, así que nadie tiene que
 * acordarse de pasar el contexto correcto (usar el del Fragment filtraría una
 * referencia a una pantalla ya destruida).
 */
@Singleton
public class SessionRepository {

    private static final String ARCHIVO = "sesion_ronda";
    private static final String CLAVE_TOKEN = "token";
    private static final String CLAVE_EMAIL = "email";

    private final SharedPreferences prefs;

    @Inject
    public SessionRepository(@ApplicationContext Context contexto) {
        prefs = contexto.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE);
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

    /**
     * Para las rutas con token opcional (el listado de publicaciones): si no
     * hay sesion devuelve null y Retrofit directamente no manda la cabecera,
     * en vez de mandar "Bearer null".
     */
    public String getBearerOpcional() {
        return haySesion() ? getBearer() : null;
    }

    public void cerrarSesion() {
        prefs.edit().clear().apply();
    }
}
