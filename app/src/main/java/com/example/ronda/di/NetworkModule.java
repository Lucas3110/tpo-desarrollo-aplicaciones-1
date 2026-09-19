package com.example.ronda.di;

import com.example.ronda.BuildConfig;
import com.example.ronda.data.network.AuthApiService;
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
import okhttp3.Response;
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
     * Direccion del backend, inyectada en tiempo de compilacion desde
     * app/build.gradle.kts.
     *
     * Por defecto es http://10.0.2.2:3000/ (el emulador). Para probar en un
     * celular fisico, cada uno pone la IP de su PC en local.properties:
     *
     *     ronda.baseUrl=http://192.168.0.153:3000/
     *
     * local.properties no se commitea, asi que nadie le rompe la
     * configuracion a los demas.
     */
    private static final String BASE_URL = BuildConfig.BASE_URL;

    /** Cuanto se espera al servidor antes de dar la request por fallida. */
    private static final long TIMEOUT_SEGUNDOS = 15;

    /**
     * Retrofit usa OkHttp por debajo. Se configura el cliente a mano para
     * fijar los timeouts (apunte "API REST y Retrofit", consideracion 3):
     * si el celular apunta a una IP que no responde, la app espera 15 s y
     * cae en onFailure con un IOException, en vez del default de 10 s.
     *
     * Ademas lleva el interceptor que detecta la sesion vencida (Punto 1).
     */
    @Provides
    @Singleton
    public OkHttpClient provideOkHttpClient(SessionRepository sesion, AuthEventBus eventos) {
        return new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)
                .addInterceptor(interceptorDeSesionVencida(sesion, eventos))
                .build();
    }

    /**
     * Un 401 del backend significa que el token ya no sirve: vencio (dura 7
     * dias) o el usuario dejo de existir. Sin esto, la app se quedaria
     * mostrando errores sueltos en cada pantalla sin entender por que.
     *
     * Se hace en el interceptor y no en cada Fragment porque el 401 puede
     * llegar en cualquier request, y repetir el chequeo en los 20 callbacks
     * de la app seria imposible de mantener.
     *
     * Esto es lo que le da sentido real al desbloqueo biometrico: la sesion
     * puede morir, y entonces volver a entrar significa algo.
     */
    private static Interceptor interceptorDeSesionVencida(SessionRepository sesion,
                                                          AuthEventBus eventos) {
        return cadena -> {
            Response respuesta = cadena.proceed(cadena.request());

            if (respuesta.code() == 401 && sesion.haySesion()) {
                // Ojo: sin el chequeo de haySesion, un login con contrasena
                // incorrecta (que tambien responde 401) dispararia el evento
                // y patearia a una pantalla de login en la que ya estamos.
                sesion.cerrarSesion();
                eventos.emitirSesionExpirada();
            }
            return respuesta;
        };
    }

    @Provides
    @Singleton
    public Retrofit provideRetrofit(OkHttpClient cliente) {
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
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
     * Cada interfaz de Retrofit necesita su propio @Provides: sin esto, un
     * Fragment que la pida con @Inject no compila ("PublicacionApiService
     * cannot be provided without an @Provides-annotated method").
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

}
