package com.example.ronda.di;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Avisa que la sesión se murió.
 *
 * El problema que resuelve: el interceptor de OkHttp es el primero en
 * enterarse de que el backend devolvió 401 (token vencido o revocado), pero
 * corre en un hilo de red y no tiene forma de tocar la UI ni de navegar. La
 * Activity sí puede navegar, pero no se entera de lo que pasa en la red.
 *
 * Este singleton los conecta: el interceptor emite, la MainActivity observa y
 * manda al login. Con Hilt los dos reciben LA MISMA instancia sin tener que
 * pasársela de mano en mano.
 *
 * Importa para el Punto 1: si la sesión nunca venciera, pedir la huella al
 * abrir la app sería puramente decorativo.
 */
@Singleton
public class AuthEventBus {

    private final MutableLiveData<Boolean> sesionExpirada = new MutableLiveData<>();

    @Inject
    public AuthEventBus() {
        // Hilt lo construye.
    }

    public LiveData<Boolean> getSesionExpirada() {
        return sesionExpirada;
    }

    /**
     * postValue y NO setValue: esto se llama desde el hilo de red del
     * interceptor, y setValue desde fuera del hilo principal tira
     * IllegalStateException.
     */
    public void emitirSesionExpirada() {
        sesionExpirada.postValue(true);
    }

    /**
     * Se llama después de navegar al login. Sin esto, el LiveData conserva el
     * último valor y se lo vuelve a entregar al próximo observador — con lo
     * cual la app te patea al login apenas volvés a entrar.
     */
    public void limpiar() {
        sesionExpirada.postValue(false);
    }
}
