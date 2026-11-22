package com.example.ronda.di;

import android.util.Log;

import com.example.ronda.BuildConfig;
import com.example.ronda.data.network.AuthApiService;
import com.example.ronda.data.network.OfertaApiService;
import com.example.ronda.data.network.OperacionApiService;
import com.example.ronda.data.network.PublicacionApiService;
import com.example.ronda.data.network.UsuarioApiService;
import com.example.ronda.data.repository.SessionRepository;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Módulo de Hilt para la red: le enseña a Hilt cómo construir OkHttp,
 * Retrofit y las interfaces de la API.
 *
 * Hace falta un módulo porque esas clases no son nuestras y no se les puede
 * poner @Inject en el constructor. Cada método con @Provides es una receta:
 * Hilt la ejecuta y le da el resultado a quien lo pida con @Inject. Así
 * ninguna pantalla arma su propio Retrofit ni lo busca con getInstance().
 *
 * Con @InstallIn(SingletonComponent.class) y @Singleton queda una única
 * instancia que vive lo que vive la app. Aunque dos hilos la pidan a la vez
 * no se crean dos: el código que genera Hilt para @Singleton usa synchronized.
 */
@Module
@InstallIn(SingletonComponent.class)
public class NetworkModule {

    private static final String CABECERA_AUTH = "Authorization";
    private static final String TAG_RED = "RondaRed";

    /**
     * Dirección del backend. Sale de app/build.gradle.kts al compilar
     * (BuildConfig.BASE_URL).
     *
     * Por defecto es http://10.0.2.2:3000/ (el alias con el que el emulador
     * ve el localhost de la PC). Para probar en un celular físico, cada uno
     * pone la IP de SU máquina en local.properties:
     *
     *     ronda.baseUrl=http://192.168.0.153:3000/
     *
     * local.properties no se sube al repositorio, así que nadie le cambia la
     * configuración a los demás.
     */
    private static final String BASE_URL = BuildConfig.BASE_URL;

    /** Cuánto se espera al servidor antes de dar el pedido por fallido. */
    private static final long TIMEOUT_SEGUNDOS = 15;

    /**
     * El cliente HTTP. Retrofit usa OkHttp por debajo, y se configura a mano
     * por tres motivos:
     *
     * 1. Los timeouts: si el celular apunta a una IP que no responde, la app
     *    espera 15 s y cae en onFailure con un IOException, en vez del default
     *    de 10 s.
     *
     * 2. El interceptor del JWT, que le pone el header Authorization a cada
     *    pedido.
     *
     * 3. El interceptor de sesión vencida (Punto 1), que reacciona al 401.
     *
     * El orden importa: primero se pone el token y después se mira la
     * respuesta. Los interceptores corren en el orden en que se agregan a la
     * ida, y al revés a la vuelta.
     *
     * Los parámetros (sesion y eventos) también los pasa Hilt.
     */
    @Provides
    @Singleton
    public OkHttpClient provideOkHttpClient(SessionRepository sesion, AuthEventBus eventos) {
        return new OkHttpClient.Builder()
                .addInterceptor(interceptorDeJwt(sesion))
                .addInterceptor(interceptorDeSesionVencida(sesion, eventos))
                .connectTimeout(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Un interceptor es código que OkHttp corre en cada pedido. "cadena" es la
     * cadena de interceptores: cadena.request() es el pedido y
     * cadena.proceed() lo manda y devuelve la respuesta.
     *
     * Este lee el token guardado en SessionRepository (nuestro "TokenManager")
     * y, si hay sesión, le agrega solo el header "Authorization: Bearer" con
     * el token. Así las interfaces de Retrofit no tienen que recibirlo en
     * cada método.
     *
     * Si un pedido ya trae el header (algunas interfaces de otros puntos lo
     * pasan con @Header), se respeta tal cual: nunca se manda dos veces.
     */
    private static Interceptor interceptorDeJwt(SessionRepository sesion) {
        return cadena -> {
            Request original = cadena.request();
            String token = sesion.getToken();

            if (token == null || original.header(CABECERA_AUTH) != null) {
                // Sin sesión (login, registro, código) o ya venía puesto.
                return cadena.proceed(original);
            }

            // Una copia del pedido, con el header agregado.
            Request conToken = original.newBuilder()
                    .header(CABECERA_AUTH, "Bearer " + token)
                    .build();

            // Para verificar en Logcat que el token viaja.
            Log.d(TAG_RED, "JWT agregado a " + original.method() + " "
                    + original.url().encodedPath());
            return cadena.proceed(conToken);
        };
    }

    /**
     * Un 401 del backend significa que el token ya no sirve: venció (dura 7
     * días) o el usuario dejó de existir. Sin esto, la app se quedaría
     * mostrando errores sueltos en cada pantalla sin entender por qué.
     *
     * Se hace en el interceptor y no en cada Fragment porque el 401 puede
     * llegar en cualquier pedido, y repetir el chequeo en los 20 callbacks
     * de la app sería imposible de mantener.
     *
     * Esto es lo que le da sentido al desbloqueo biométrico: la sesión puede
     * vencer, y entonces volver a entrar significa algo.
     */
    private static Interceptor interceptorDeSesionVencida(SessionRepository sesion,
                                                          AuthEventBus eventos) {
        return cadena -> {
            // Primero deja salir el pedido y espera la respuesta.
            Response respuesta = cadena.proceed(cadena.request());

            if (respuesta.code() == 401 && sesion.haySesion()) {
                // Ojo: sin el chequeo de haySesion, un login con contraseña
                // incorrecta (que también responde 401) dispararía el evento
                // y patearía a una pantalla de login en la que ya estamos.
                sesion.cerrarSesion();
                // MainActivity escucha este aviso y vuelve al login.
                eventos.emitirSesionExpirada();
            }
            return respuesta;
        };
    }

    /**
     * Retrofit arma los pedidos HTTP a partir de las interfaces. Usa el OkHttp
     * de arriba (con los interceptores) y Gson para pasar de JSON a objetos
     * Java y al revés.
     */
    @Provides
    @Singleton
    public Retrofit provideRetrofit(OkHttpClient cliente) {
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(cliente)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    /**
     * Punto 1. retrofit.create() genera la clase que implementa
     * AuthApiService: nosotros solo escribimos la interfaz.
     */
    @Provides
    @Singleton
    public AuthApiService provideAuthApiService(Retrofit retrofit) {
        return retrofit.create(AuthApiService.class);
    }

    /**
     * Cada interfaz de Retrofit necesita su propio @Provides: sin esto, un
     * Fragment que la pida con @Inject no compila ("PublicacionApiService
     * cannot be provided without an @Provides-annotated method").
     */
    @Provides
    @Singleton
    public PublicacionApiService providePublicacionApiService(Retrofit retrofit) {
        return retrofit.create(PublicacionApiService.class);
    }

    /** Datos personales del Punto 2. Mismo patrón que las otras interfaces. */
    @Provides
    @Singleton
    public UsuarioApiService provideUsuarioApiService(Retrofit retrofit) {
        return retrofit.create(UsuarioApiService.class);
    }

    /**
     * Ofertas y negociación del Punto 7. Tampoco recibe el token por
     * parámetro: se lo pone el interceptor del JWT.
     */
    @Provides
    @Singleton
    public OfertaApiService provideOfertaApiService(Retrofit retrofit) {
        return retrofit.create(OfertaApiService.class);
    }

    @Provides
    @Singleton
    public com.example.ronda.data.network.FavoritosApiService provideFavoritosApiService(Retrofit retrofit) {
        return retrofit.create(com.example.ronda.data.network.FavoritosApiService.class);
    }

    /** Historial de operaciones y calificaciones (Punto 9). */
    @Provides
    @Singleton
    public OperacionApiService provideOperacionApiService(Retrofit retrofit) {
        return retrofit.create(OperacionApiService.class);
    }
}
