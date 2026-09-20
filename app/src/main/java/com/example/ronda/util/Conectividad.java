package com.example.ronda.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Dice si hay internet, y avisa cuando eso cambia (Punto 6).
 *
 * Dos formas de usarlo, porque el enunciado pide las dos cosas:
 *
 *   hayInternet()   -> una pregunta puntual. Es lo que se consulta antes de
 *                      preguntar, ofertar o guardar en favoritos.
 *   getEstado()     -> un LiveData. La pantalla lo observa y se entera sola
 *                      cuando vuelve la conexión, que es lo que dispara el
 *                      "al recuperar la conexión, la app actualiza".
 *
 * Ojo con NET_CAPABILITY_VALIDATED: no alcanza con estar conectado a una red
 * (el WiFi de un bar con portal cautivo está "conectado" y no llega a
 * internet). VALIDATED es Android diciendo que la red realmente sale afuera.
 */
@Singleton
public class Conectividad {

    private final ConnectivityManager gestor;
    private final MutableLiveData<Boolean> estado = new MutableLiveData<>();

    @Inject
    public Conectividad(@ApplicationContext Context contexto) {
        gestor = (ConnectivityManager) contexto.getSystemService(Context.CONNECTIVITY_SERVICE);
        estado.setValue(hayInternet());
        registrarEscucha();
    }

    /** ¿Se puede salir a internet ahora mismo? */
    public boolean hayInternet() {
        if (gestor == null) return false;

        Network red = gestor.getActiveNetwork();
        if (red == null) return false;

        NetworkCapabilities caps = gestor.getNetworkCapabilities(red);
        return caps != null
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    /**
     * Cambia solo cuando el sistema avisa que apareció o se fue una red.
     * Vale la pena observarlo en vez de preguntar cada dos segundos.
     */
    public LiveData<Boolean> getEstado() {
        return estado;
    }

    /**
     * Se escucha la red *por defecto*, no cualquiera que cumpla un filtro.
     *
     * La diferencia importa: con registerNetworkCallback(request, ...) el
     * onLost llega por una red puntual, y ahi volver a preguntar
     * hayInternet() es una carrera — a veces el sistema todavia no termino de
     * dar de baja la red y contesta que si, se publica "hay internet" y como
     * despues no llega ningun callback mas, la pantalla queda mintiendo.
     *
     * Con la red por defecto no hay ambiguedad: si se perdio, no hay salida a
     * internet, y se publica false sin volver a preguntar nada.
     */
    private void registrarEscucha() {
        if (gestor == null) return;

        gestor.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network red) {
                // Recien aparece: todavia puede no estar validada, asi que se
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
                // cuando el WiFi del bar finalmente deja pasar trafico. Se lee
                // de las capacidades que llegan, no del estado global.
                estado.postValue(
                        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED));
            }
        });
    }
}
