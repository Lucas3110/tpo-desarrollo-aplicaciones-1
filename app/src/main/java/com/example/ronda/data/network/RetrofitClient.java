package com.example.ronda.data.network;

import android.os.Build;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Instancia unica de Retrofit para toda la app (patron singleton).
 *
 * Retrofit es un objeto pesado: tiene pool de hilos, cache de conexiones,
 * etc. Crear uno por cada request seria un desperdicio, asi que se crea
 * una sola vez y se reutiliza.
 */
public class RetrofitClient {

    /**
     * Direccion de la PC vista DESDE EL EMULADOR de Android Studio.
     * 10.0.2.2 es un alias que el emulador redirige al localhost de la maquina.
     * Ojo: "localhost" NO sirve, porque desde el emulador apunta al propio emulador.
     */
    private static final String URL_EMULADOR = "http://10.0.2.2:3000/";

    /**
     * Direccion de la PC vista DESDE UN CELULAR FISICO en la misma red WiFi.
     *
     * Este valor hay que actualizarlo con la IP que imprime el backend al
     * arrancar, en la linea "Celular (WiFi) -> http://...". Cambia al cambiar
     * de red (casa, facultad, hotspot).
     */
    private static final String URL_RED_LOCAL = "http://192.168.0.153:3000/";

    private static Retrofit instance;

    public static Retrofit getInstance() {
        if (instance == null) {
            instance = new Retrofit.Builder()
                    .baseUrl(getBaseUrl())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return instance;
    }

    /** Atajo para no repetir create(...) en cada pantalla. */
    public static AuthApiService getAuthApi() {
        return getInstance().create(AuthApiService.class);
    }

    /**
     * El emulador y un celular real llegan a la PC por direcciones distintas,
     * asi que elegimos segun donde estemos corriendo. De esta forma el mismo
     * codigo funciona en los dos lados sin tener que editarlo antes de cada
     * prueba, ni acordarse de revertirlo antes de entregar.
     */
    private static String getBaseUrl() {
        return esEmulador() ? URL_EMULADOR : URL_RED_LOCAL;
    }

    /** Heuristica estandar: los emuladores se identifican en Build. */
    private static boolean esEmulador() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu");
    }
}
