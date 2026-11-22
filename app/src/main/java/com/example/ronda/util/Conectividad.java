package com.example.ronda.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Dice si hay internet, y avisa cuando eso cambia (Punto 6).
 *
 * Dos formas de usarlo, porque hacen falta las dos cosas:
 *
 *   hayInternet()   -> una pregunta puntual. Es lo que se consulta antes de
 *                      preguntar, ofertar o guardar en favoritos.
 *   getEstado()     -> un LiveData. La pantalla lo observa y se entera sola
 *                      cuando vuelve la conexión, que es lo que dispara la
 *                      actualización automática.
 *
 * Ojo con NET_CAPABILITY_VALIDATED: no alcanza con estar conectado a una red
 * (el WiFi de un bar con portal cautivo está "conectado" y no llega a
 * internet). VALIDATED es Android diciendo que la red realmente sale afuera.
 *
 * Es @Singleton: una sola escucha de la red para toda la app.
 */
@Singleton
public class Conectividad {

    /** El servicio de Android que sabe el estado de la red. */
    private final ConnectivityManager gestor;
    /** Lo que se publica cada vez que el sistema avisa algo de la red. */
    private final MutableLiveData<Boolean> estado = new MutableLiveData<>();

    /**
     * Lo que observan las pantallas. El sistema avisa cada vez que cambia
     * algo de la red (por ejemplo, la señal), aunque siga habiendo internet, y
     * LiveData entrega cada aviso aunque traiga el mismo valor. Sin este
     * filtro la pantalla lo tomaba como "volvió la conexión" y, si el servidor
     * no respondía, recargaba cada pocos segundos sin parar.
     * distinctUntilChanged deja pasar solo los cambios de verdad.
     */
    private final LiveData<Boolean> cambios = Transformations.distinctUntilChanged(estado);

    /** Arranca con el estado de ahora y se queda escuchando los cambios. */
    @Inject
    public Conectividad(@ApplicationContext Context contexto) {
        gestor = (ConnectivityManager) contexto.getSystemService(Context.CONNECTIVITY_SERVICE);
        estado.setValue(hayInternet());
        registrarEscucha();
    }

    /** ¿Se puede salir a internet ahora mismo? */
    public boolean hayInternet() {
        if (gestor == null) return false;

        // La red que está usando el celular (WiFi o datos), si hay alguna.
        Network red = gestor.getActiveNetwork();
        if (red == null) return false;

        // Tiene que tener salida a internet Y estar validada por Android.
        NetworkCapabilities caps = gestor.getNetworkCapabilities(red);
        return caps != null
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    /**
     * Avisa solo cuando cambia: cuando se va internet y cuando vuelve.
     * Conviene observarlo en vez de preguntar cada dos segundos.
     */
    public LiveData<Boolean> getEstado() {
        return cambios;
    }

    /**
     * Se escucha la red *por defecto*, no cualquiera que cumpla un filtro.
     *
     * La diferencia importa: con registerNetworkCallback(request, ...) el
     * onLost llega por una red puntual, y ahí volver a preguntar
     * hayInternet() es una carrera: a veces el sistema todavía no terminó de
     * dar de baja la red y contesta que sí, se publica "hay internet" y, como
     * después no llega ningún callback más, la pantalla queda mintiendo.
     *
     * Con la red por defecto no hay ambigüedad: si se perdió, no hay salida a
     * internet, y se publica false sin volver a preguntar nada.
     */
    private void registrarEscucha() {
        if (gestor == null) return;

        // El sistema llama a estos métodos cuando cambia la red por defecto.
        gestor.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network red) {
                // Recién aparece: todavía puede no estar validada, así que se
                // consulta. El onCapabilitiesChanged de abajo lo corrige.
                estado.postValue(hayInternet());
            }

            @Override
            public void onLost(@NonNull Network red) {
                // postValue y no setValue: esto llega en un hilo del sistema.
                estado.postValue(false);
            }

            @Override
            public void onCapabilitiesChanged(@NonNull Network red,
                                              @NonNull NetworkCapabilities caps) {
                // Es el que avisa cuando una red pasa a estar validada, o sea
                // cuando el WiFi del bar finalmente deja pasar tráfico. Se lee
                // de las capacidades que llegan, no del estado global.
                estado.postValue(
                        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED));
            }
        });
    }
}
