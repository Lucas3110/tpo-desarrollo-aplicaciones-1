package com.example.ronda.notificaciones;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.example.ronda.data.model.NotificacionResponse;
import com.example.ronda.ui.MainActivity;

/**
 * Que se abre al tocar un aviso (Punto 11): la publicacion, con el panel de
 * preguntas u ofertas segun el tipo. El de ofertas le sirve a los dos lados:
 * el vendedor ve todas y el comprador su negociacion.
 */
public final class AbrirAviso {

    /** Argumento del detalle: que panel abrir apenas carga la publicacion. */
    public static final String ARG_ABRIR = "abrir";
    public static final String ABRIR_PREGUNTAS = "PREGUNTAS";
    public static final String ABRIR_OFERTAS = "OFERTAS";

    /** Mismo nombre que el argumento del detalle en home_nav_graph. */
    private static final String ARG_PUBLICACION_ID = "publicacionId";

    private static final String EXTRA_PUBLICACION_ID = "aviso.publicacionId";
    private static final String EXTRA_ABRIR = "aviso.abrir";

    private AbrirAviso() {
    }

    /** SINGLE_TOP: con la app abierta llega por onNewIntent. */
    public static Intent intentPara(Context contexto, NotificacionResponse aviso) {
        return new Intent(contexto, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(EXTRA_PUBLICACION_ID, aviso.getPublicacionId())
                .putExtra(EXTRA_ABRIR, aviso.esDePreguntas() ? ABRIR_PREGUNTAS : ABRIR_OFERTAS);
    }

    /** Argumentos del detalle, o null si el Intent no viene de un aviso. */
    public static Bundle argumentosDelDetalle(Intent intent) {
        if (intent == null || !intent.hasExtra(EXTRA_PUBLICACION_ID)) return null;

        Bundle argumentos = new Bundle();
        argumentos.putInt(ARG_PUBLICACION_ID, intent.getIntExtra(EXTRA_PUBLICACION_ID, -1));
        argumentos.putString(ARG_ABRIR, intent.getStringExtra(EXTRA_ABRIR));
        return argumentos;
    }
}
