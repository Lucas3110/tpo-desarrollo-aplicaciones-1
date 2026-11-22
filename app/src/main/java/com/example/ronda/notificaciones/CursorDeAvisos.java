package com.example.ronda.notificaciones;

import android.content.Context;
import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Id del ultimo aviso recibido (Punto 11). Guardado en SharedPreferences para
 * no repetir avisos al reabrir la app y recibir los que llegaron mientras
 * estaba cerrada.
 */
@Singleton
public class CursorDeAvisos {

    private static final String ARCHIVO = "avisos_ronda";
    private static final String CLAVE_ULTIMO_ID = "ultimoId";
    private static final int SIN_CURSOR = -1;

    private final SharedPreferences prefs;

    @Inject
    public CursorDeAvisos(@ApplicationContext Context contexto) {
        this.prefs = contexto.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE);
    }

    /** null si todavia no se pidio nada con esta sesion. */
    public Integer ultimoId() {
        int valor = prefs.getInt(CLAVE_ULTIMO_ID, SIN_CURSOR);
        return valor == SIN_CURSOR ? null : valor;
    }

    public void guardar(int ultimoId) {
        prefs.edit().putInt(CLAVE_ULTIMO_ID, ultimoId).apply();
    }

    /** Sin cursor el proximo pedido trae el id actual, sin avisos viejos. */
    public void olvidar() {
        prefs.edit().remove(CLAVE_ULTIMO_ID).apply();
    }
}
