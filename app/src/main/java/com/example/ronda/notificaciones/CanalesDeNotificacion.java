package com.example.ronda.notificaciones;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import com.example.ronda.R;

/**
 * Canales de notificacion (Punto 11). Desde Android 8 toda notificacion va en
 * un canal, y cada uno se puede apagar aparte desde los ajustes:
 *   - AVISOS: preguntas y ofertas, con sonido.
 *   - CONEXION: la notificacion fija del servicio, en silencio.
 * Crear un canal que ya existe no hace nada.
 */
public final class CanalesDeNotificacion {

    public static final String AVISOS = "avisos";
    public static final String CONEXION = "conexion_avisos";

    private CanalesDeNotificacion() {
    }

    public static void crear(Context contexto) {
        // Antes de Android 8 no hay canales.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationChannel avisos = new NotificationChannel(AVISOS,
                contexto.getString(R.string.canal_avisos_nombre),
                NotificationManager.IMPORTANCE_HIGH);
        avisos.setDescription(contexto.getString(R.string.canal_avisos_descripcion));

        NotificationChannel conexion = new NotificationChannel(CONEXION,
                contexto.getString(R.string.canal_conexion_nombre),
                NotificationManager.IMPORTANCE_MIN);
        conexion.setDescription(contexto.getString(R.string.canal_conexion_descripcion));
        conexion.setShowBadge(false);

        NotificationManager gestor = contexto.getSystemService(NotificationManager.class);
        if (gestor != null) {
            gestor.createNotificationChannel(avisos);
            gestor.createNotificationChannel(conexion);
        }
    }
}
