package com.example.ronda.notificaciones;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.ronda.R;
import com.example.ronda.data.model.NotificacionResponse;
import com.example.ronda.data.model.NovedadesResponse;
import com.example.ronda.data.network.NotificacionApiService;
import com.example.ronda.data.repository.SessionRepository;
import com.example.ronda.ui.MainActivity;
import com.example.ronda.util.Conectividad;

import java.io.IOException;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Punto 11: espera avisos por long polling y los muestra como notificaciones.
 *
 * Cada pedido queda abierto en el backend hasta que hay un aviso (200) o
 * pasan 25 s (204), y enseguida se manda el siguiente.
 *
 * Es un servicio de primer plano para que Android no lo frene con la app en
 * segundo plano; por eso muestra una notificacion fija (canal CONEXION). Los
 * pedidos van con execute() en un hilo propio.
 */
@AndroidEntryPoint
public class AvisosService extends Service {

    private static final String TAG = "AvisosService";

    /** Id de la notificacion fija. Los avisos usan otro tag y no se pisan. */
    private static final int ID_NOTIFICACION_FIJA = 1;
    private static final String TAG_AVISO = "aviso";

    /** Sin red no tiene sentido pedir: se vuelve a mirar cada tanto. */
    private static final long ESPERA_SIN_RED_MS = 5_000;
    /** Ante errores, cada reintento espera el doble, hasta 30 s. */
    private static final long ESPERA_TRAS_ERROR_MIN_MS = 2_000;
    private static final long ESPERA_TRAS_ERROR_MAX_MS = 30_000;

    /** Para no arrancarlo de nuevo si ya esta corriendo. */
    private static volatile boolean corriendo;

    @Inject
    NotificacionApiService notificacionApi;

    @Inject
    CursorDeAvisos cursor;

    @Inject
    SessionRepository sesion;

    @Inject
    Conectividad conectividad;

    private Thread hilo;
    private volatile boolean activo;
    /** El pedido abierto, para poder cortarlo al frenar el servicio. */
    private volatile Call<NovedadesResponse> llamadaEnCurso;

    /** Llamar con la app en primer plano: Android 12+ no deja arrancarlo desde segundo plano. */
    public static void iniciar(Context contexto) {
        if (corriendo) return;
        ContextCompat.startForegroundService(contexto,
                new Intent(contexto, AvisosService.class));
    }

    public static void detener(Context contexto) {
        contexto.stopService(new Intent(contexto, AvisosService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        corriendo = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // startForeground tiene que llamarse enseguida o Android cierra la app.
        mostrarNotificacionFija();

        if (!sesion.haySesion()) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (hilo == null) {
            activo = true;
            hilo = new Thread(this::escuchar, "avisos-long-polling");
            hilo.start();
        }
        // Si Android mata el proceso, arranca de nuevo al abrir la app.
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        activo = false;
        Call<NovedadesResponse> llamada = llamadaEnCurso;
        if (llamada != null) llamada.cancel();
        if (hilo != null) hilo.interrupt();
        corriendo = false;
        super.onDestroy();
    }

    /** No se usa bind. */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // -----------------------------------------------------------------
    // El loop
    // -----------------------------------------------------------------

    private void escuchar() {
        long esperaTrasError = ESPERA_TRAS_ERROR_MIN_MS;

        while (activo) {
            if (!conectividad.hayInternet()) {
                if (!dormir(ESPERA_SIN_RED_MS)) break;
                continue;
            }

            Integer desdeId = cursor.ultimoId();
            try {
                // Sin cursor el backend solo devuelve desde donde esperar.
                Call<NovedadesResponse> llamada = notificacionApi.novedades(desdeId,
                        desdeId == null ? null : NotificacionApiService.ESPERA_SEGUNDOS);
                llamadaEnCurso = llamada;
                Response<NovedadesResponse> respuesta = llamada.execute();
                if (!activo) break;

                if (respuesta.code() == 204) {
                    // Paso la espera sin novedades: se vuelve a preguntar.
                    esperaTrasError = ESPERA_TRAS_ERROR_MIN_MS;
                    continue;
                }

                if (respuesta.isSuccessful() && respuesta.body() != null) {
                    NovedadesResponse novedades = respuesta.body();
                    if (desdeId != null) {
                        for (NotificacionResponse aviso : novedades.getNotificaciones()) {
                            mostrarAviso(aviso);
                        }
                    }
                    cursor.guardar(novedades.getUltimoId());
                    esperaTrasError = ESPERA_TRAS_ERROR_MIN_MS;
                    continue;
                }

                if (respuesta.code() == 401) {
                    // Token vencido: el interceptor ya cerro la sesion.
                    break;
                }

                Log.w(TAG, "El backend respondió " + respuesta.code() + " a los avisos");
            } catch (IOException e) {
                // Sin red, servidor caido o pedido cancelado al frenar.
                if (!activo) break;
                Log.w(TAG, "No se pudieron pedir los avisos: " + e.getMessage());
            } finally {
                llamadaEnCurso = null;
            }

            if (!dormir(esperaTrasError)) break;
            esperaTrasError = Math.min(esperaTrasError * 2, ESPERA_TRAS_ERROR_MAX_MS);
        }

        if (activo) stopSelf();
    }

    /** Devuelve false si el servicio se freno mientras dormia. */
    private boolean dormir(long milisegundos) {
        try {
            Thread.sleep(milisegundos);
            return activo;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    // -----------------------------------------------------------------
    // Notificaciones
    // -----------------------------------------------------------------

    private void mostrarNotificacionFija() {
        Notification fija = new NotificationCompat.Builder(this, CanalesDeNotificacion.CONEXION)
                .setSmallIcon(R.drawable.ic_aviso)
                .setContentTitle(getString(R.string.avisos_conexion_titulo))
                .setContentText(getString(R.string.avisos_conexion_texto))
                .setContentIntent(abrirApp())
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true)
                .build();

        // Android 14+: el tipo tiene que coincidir con el del manifest.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(ID_NOTIFICACION_FIJA, fija,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(ID_NOTIFICACION_FIJA, fija);
        }
    }

    /** Una notificacion por aviso, con el id del aviso como id. */
    private void mostrarAviso(NotificacionResponse aviso) {
        // Sin el permiso (Android 13+) el sistema no la mostraria: se saltea.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Notification notificacion = new NotificationCompat.Builder(this, CanalesDeNotificacion.AVISOS)
                .setSmallIcon(R.drawable.ic_aviso)
                .setContentTitle(aviso.getTitulo())
                .setContentText(aviso.getMensaje())
                // El mensaje puede ser una pregunta entera: expandida se lee completa.
                .setStyle(new NotificationCompat.BigTextStyle().bigText(aviso.getMensaje()))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setContentIntent(abrirAviso(aviso))
                .setAutoCancel(true)
                .build();

        NotificationManagerCompat.from(this).notify(TAG_AVISO, aviso.getId(), notificacion);
    }

    /**
     * Abre la publicacion del aviso. requestCode = id del aviso: con el mismo
     * requestCode y FLAG_UPDATE_CURRENT todos abririan el ultimo.
     */
    private PendingIntent abrirAviso(NotificacionResponse aviso) {
        return PendingIntent.getActivity(this, aviso.getId(), AbrirAviso.intentPara(this, aviso),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** Al tocar la notificacion fija se abre Ronda. */
    private PendingIntent abrirApp() {
        Intent intent = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
