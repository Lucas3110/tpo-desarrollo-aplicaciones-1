package com.example.ronda.di;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Avisa que la sesión venció (Punto 1).
 *
 * El problema que resuelve: el interceptor de OkHttp es el primero en
 * enterarse de que el backend devolvió 401 (token vencido o inválido), pero
 * corre en un hilo de red y no puede tocar la UI ni navegar. La Activity sí
 * puede navegar, pero no se entera de lo que pasa en la red.
 *
 * Esta clase los conecta: el interceptor emite, la MainActivity observa y
 * manda al login. Como es @Singleton, Hilt les da a los dos LA MISMA
 * instancia sin tener que pasársela de mano en mano.
 *
 * El aviso viaja en un LiveData: un valor que se puede observar. Quien lo
 * observa se entera cada vez que cambia.
 *
 * Importa para el Punto 1: si la sesión nunca venciera, pedir la huella al
 * abrir la app sería puramente decorativo.
 */
@Singleton
public class AuthEventBus {

    /**
     * Por dentro es Mutable (acá se cambia el valor). Hacia afuera se entrega
     * como LiveData, que solo se puede observar.
     */
    private final MutableLiveData<Boolean> sesionExpirada = new MutableLiveData<>();

    /** Con @Inject en el constructor, Hilt sabe crearla sola, sin módulo. */
    @Inject
    public AuthEventBus() {
        // Hilt lo construye.
    }

    /** Lo que observa MainActivity. */
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
     * Lo llama MainActivity apenas recibe el aviso. Sin esto, el LiveData
     * conserva el último valor y se lo vuelve a entregar al próximo
     * observador, con lo cual la app te patea al login apenas volvés a entrar.
     */
    public void limpiar() {
        sesionExpirada.postValue(false);
    }
}
