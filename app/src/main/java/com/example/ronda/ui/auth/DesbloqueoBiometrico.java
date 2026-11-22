package com.example.ronda.ui.auth;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.concurrent.Executor;

import com.example.ronda.R;
import com.example.ronda.data.repository.SessionRepository;

/**
 * Desbloqueo de la sesión con la biometría del dispositivo (Punto 1).
 *
 * Se arma en tres pasos:
 *   1) BiometricManager  -> ¿se puede usar biometría en este dispositivo?
 *   2) PromptInfo        -> cómo se ve el diálogo
 *   3) BiometricPrompt   -> lo muestra y avisa el resultado por los callbacks
 *
 * No es una pantalla: es una clase con métodos static que usan otras.
 *   pedir()              lo llama LoginFragment al abrir la app, antes del auto-login
 *   ofrecerActivacion()  lo llaman el login y la pantalla del código, apenas
 *                        se crea una sesión
 *
 * Usa AndroidX Biometric, que funciona igual en todas las versiones de
 * Android que soporta la app. Es final y tiene el constructor privado: nadie
 * la hereda ni hace new.
 */
public final class DesbloqueoBiometrico {

    /**
     * Cómo avisa el resultado. Quien llama a pedir() le pasa un objeto con
     * estos tres métodos, y se ejecuta el que corresponda.
     */
    public interface Callback {
        /** La biometría se validó correctamente. */
        void onDesbloqueado();

        /**
         * El dispositivo no tiene biometría utilizable (sin hardware, no
         * disponible ahora, o sin ninguna huella o rostro cargado). No hay que
         * bloquear a la persona: se sigue con el flujo normal.
         */
        void onNoDisponible();

        /** La persona canceló, o el sistema cortó el intento. */
        void onCancelado();
    }

    private DesbloqueoBiometrico() {
        // Clase de utilidad: no se instancia.
    }

    /**
     * ¿Este dispositivo puede usar biometría fuerte ahora mismo?
     *
     * Sirve para no ofrecer el desbloqueo con huella en un equipo que no lo
     * soporta o donde la persona no cargó ninguna: prometerlo y que después
     * falle es peor que no ofrecerlo nunca.
     */
    public static boolean estaDisponible(@NonNull Context contexto) {
        return BiometricManager.from(contexto)
                .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                == BiometricManager.BIOMETRIC_SUCCESS;
    }

    /**
     * Después de crear una sesión (con contraseña, con el código del mail o
     * al confirmar una cuenta nueva) ofrece entrar con la huella la próxima
     * vez, y después sigue con {@code despues}.
     *
     * Si ya lo había activado antes no se vuelve a preguntar: solo se repone
     * la copia cifrada del token nuevo. Cerrar sesión borra el token cifrado
     * a propósito (si no, la huella resucitaría una sesión que la persona
     * cerró), pero deja la preferencia, así que hay que reponerlo acá.
     *
     * Y solo se ofrece si el dispositivo realmente puede hacerlo, para no
     * prometer algo que después falla.
     */
    public static void ofrecerActivacion(@NonNull Fragment fragment,
                                         @NonNull SessionRepository sesion,
                                         @NonNull String token,
                                         @NonNull Runnable despues) {
        if (sesion.esBiometriaActivada()) {
            sesion.guardarTokenCifrado(token);
            despues.run();
            return;
        }
        if (!estaDisponible(fragment.requireContext())) {
            despues.run();
            return;
        }

        // Cartel "¿Querés entrar con tu huella la próxima vez?". Con
        // setCancelable(false) hay que elegir una de las dos opciones.
        new AlertDialog.Builder(fragment.requireContext())
                .setTitle(R.string.biometria_activar_titulo)
                .setMessage(R.string.biometria_activar_mensaje)
                .setPositiveButton(R.string.biometria_activar_si, (d, cual) -> {
                    // Aceptó: se guarda la preferencia y la copia cifrada del token.
                    sesion.setBiometriaActivada(true);
                    sesion.guardarTokenCifrado(token);
                    despues.run();
                })
                .setNegativeButton(R.string.biometria_activar_no, (d, cual) -> despues.run())
                .setCancelable(false)
                .show();
    }

    /**
     * Muestra el cartel de la huella y avisa el resultado por el callback.
     * Lo usa el auto-login de LoginFragment.
     */
    public static void pedir(@NonNull Fragment fragment, @NonNull Callback callback) {
        Context contexto = fragment.requireContext();

        // Paso 1: preguntar primero si hay biometría fuerte disponible y cargada.
        if (!estaDisponible(contexto)) {
            // NO_HARDWARE, HW_UNAVAILABLE o NONE_ENROLLED: seguimos sin biometría.
            callback.onNoDisponible();
            return;
        }

        // Con el executor del thread principal, los callbacks llegan donde se
        // puede tocar la pantalla.
        Executor ejecutor = ContextCompat.getMainExecutor(contexto);

        // Paso 2: cómo se ve el diálogo.
        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(fragment.getString(R.string.biometria_titulo))
                .setSubtitle(fragment.getString(R.string.biometria_subtitulo))
                .setNegativeButtonText(fragment.getString(R.string.biometria_cancelar))
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build();

        // Paso 3: dispararlo y escuchar los callbacks.
        BiometricPrompt prompt = new BiometricPrompt(fragment, ejecutor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(
                            @NonNull BiometricPrompt.AuthenticationResult resultado) {
                        // La huella coincidió.
                        callback.onDesbloqueado();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        // La huella no coincide, pero el cartel sigue abierto para
                        // reintentar y el sistema ya muestra el aviso: no hay nada
                        // que hacer. Si falla muchas veces, el sistema corta el
                        // intento y llega onAuthenticationError.
                    }

                    @Override
                    public void onAuthenticationError(int codigo,
                                                      @NonNull CharSequence mensaje) {
                        // Cancelar, tocar el botón negativo o un error del sistema.
                        callback.onCancelado();
                    }
                });

        prompt.authenticate(info);
    }
}
