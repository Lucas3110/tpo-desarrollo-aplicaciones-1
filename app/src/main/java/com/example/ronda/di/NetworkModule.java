package com.example.ronda.di;

import android.os.Build;
import android.util.Log;

import com.example.ronda.data.network.AuthApiService;
import com.example.ronda.data.network.OfertaApiService;
import com.example.ronda.data.network.PublicacionApiService;
import com.example.ronda.data.network.UsuarioApiService;
import com.example.ronda.data.repository.SessionRepository;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Le enseña a Hilt cómo construir las dependencias de red.
 *
 * Reemplaza al singleton manual que era RetrofitClient: en vez de que cada
 * pantalla pida la instancia con getInstance(), Hilt la crea una sola vez y
 * se la inyecta a quien la declare con @Inject.
 *
 * @InstallIn(SingletonComponent.class) + @Singleton = una única instancia que
 * vive lo que vive la app.
 */
@Module
@InstallIn(SingletonComponent.class)
public class NetworkModule {

    /**
     * Direcciones de la PC vistas desde el dispositivo.
     *
     * 10.0.2.2 es el alias que el emulador redirige al localhost de la máquina.
     * Ojo: "localhost" NO sirve, porque desde el emulador apunta al emulador.
     */
    private static final String URL_EMULADOR = "http://10.0.2.2:3000/";

    /**
     * Para un celular físico en la misma WiFi. Hay que actualizarla con la IP
     * que imprime el backend al arrancar, en la línea "Celular (WiFi) -> ...".
     * Cambia al cambiar de red (casa, facultad, hotspot).
     */
    private static final String URL_RED_LOCAL = "http://192.168.1.37:3000/";

    /** Cuanto se espera al servidor antes de dar la request por fallida. */
    private static final long TIMEOUT_SEGUNDOS = 15;

    private static final String TAG_RED = "Red";
    private static final String CABECERA_AUTH = "Authorization";

    /**
     * Retrofit usa OkHttp por debajo. Se configura el cliente a mano por dos
     * motivos:
     *
     * 1. Los timeouts (apunte "API REST y Retrofit", consideracion 3): si el
     *    celular apunta a una IP que no responde, la app espera 15 s y cae en
     *    onFailure con un IOException, en vez del default de 10 s.
     *
     * 2. El interceptor del JWT (clase 5, "JWT + LocalStorage"): antes de
     *    enviar cada request lee el token guardado en SessionRepository
     *    (nuestro "TokenManager") y, si hay sesion, agrega solo la cabecera
     *    Authorization. Asi las interfaces nuevas no tienen que recibir el
     *    bearer a mano en cada metodo. Si una request ya trae la cabecera
     *    (las interfaces de los Puntos 1 a 4 la pasan con @Header), se
     *    respeta tal cual: nunca se manda dos veces.
     */
    @Provides
    @Singleton
    public OkHttpClient provideOkHttpClient(SessionRepository sesion) {
        return new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    String token = sesion.getToken();
                    if (token == null || original.header(CABECERA_AUTH) != null) {
                        // Sin sesion (login, registro, OTP) o ya venia puesta.
                        return chain.proceed(original);
                    }
                    Request conToken = original.newBuilder()
                            .header(CABECERA_AUTH, "Bearer " + token)
                            .build();
                    // Para verificar en Logcat que el token viaja (hands-on de la clase 5).
                    Log.d(TAG_RED, "JWT agregado a " + original.method() + " "
                            + original.url().encodedPath());
                    return chain.proceed(conToken);
                })
                .connectTimeout(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)
                .build();
    }

    @Provides
    @Singleton
    public Retrofit provideRetrofit(OkHttpClient cliente) {
        return new Retrofit.Builder()
                .baseUrl(getBaseUrl())
                .client(cliente)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    @Provides
    @Singleton
    public AuthApiService provideAuthApiService(Retrofit retrofit) {
        return retrofit.create(AuthApiService.class);
    }

    /**
     * Interfaz del Punto 3. Cada interfaz de Retrofit necesita su propio
     * @Provides: sin esto, un Fragment que la pida con @Inject no compila
     * ("PublicacionApiService cannot be provided without an @Provides-annotated
     * method"). Cuando se sumen las de los Puntos 4, 5 y 6, se agregan acá
     * copiando este mismo método.
     */
    @Provides
    @Singleton
    public PublicacionApiService providePublicacionApiService(Retrofit retrofit) {
        return retrofit.create(PublicacionApiService.class);
    }

    /** Datos personales del Punto 2. Mismo patron que las otras interfaces. */
    @Provides
    @Singleton
    public UsuarioApiService provideUsuarioApiService(Retrofit retrofit) {
        return retrofit.create(UsuarioApiService.class);
    }

    /**
     * Ofertas y negociacion del Punto 7. Es la primera interfaz que no recibe
     * el token por parametro: se lo pone el interceptor de provideOkHttpClient.
     */
    @Provides
    @Singleton
    public OfertaApiService provideOfertaApiService(Retrofit retrofit) {
        return retrofit.create(OfertaApiService.class);
    }

    /**
     * El emulador y un celular real llegan a la PC por direcciones distintas,
     * así que se elige según dónde esté corriendo. De esta forma el mismo
     * código funciona en los dos lados sin editarlo antes de cada prueba.
     */
    private static String getBaseUrl() {
        return esEmulador() ? URL_EMULADOR : URL_RED_LOCAL;
    }

    /** Heurística estándar: los emuladores se identifican en Build. */
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
