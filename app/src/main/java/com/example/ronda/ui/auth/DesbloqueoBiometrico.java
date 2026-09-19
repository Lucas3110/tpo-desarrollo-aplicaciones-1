package com.example.ronda.ui.auth;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.concurrent.Executor;

import com.example.ronda.R;

/**
 * Desbloqueo de la sesion con la biometria del dispositivo (Punto 1).
 *
 * Sigue el trio de la clase de Biometria, en tres pasos:
 *   1) BiometricManager  -> ¿se puede usar biometria en este dispositivo?
 *   2) PromptInfo        -> como se ve el dialogo
 *   3) BiometricPrompt   -> lo dispara y avisa por los callbacks
 *
 * No agrega ninguna pantalla: se usa desde el LoginFragment, delante del
 * auto-login. Usa AndroidX Biometric, que unifica la API desde API 21+.
 */
public final class DesbloqueoBiometrico {

    /** Resultado del intento de desbloqueo. */
    public interface Callback {
        /** La biometria se valido correctamente. */
        void onDesbloqueado();

        /**
         * El dispositivo no tiene biometria utilizable (sin hardware, no
         * disponible ahora, o sin ninguna huella/rostro enrolado). No hay que
         * bloquear al usuario: se sigue con el flujo normal.
         */
        void onNoDisponible();

        /** El usuario cancelo, o el sistema corto el intento. */
        void onCancelado();
    }

    private DesbloqueoBiometrico() {
        // Clase de utilidad: no se instancia.
    }

    /**
     * ¿Este dispositivo puede usar biometria fuerte ahora mismo?
     *
     * Sirve para no ofrecer el desbloqueo con huella en un equipo que no lo
     * soporta o donde la persona no enrolo ninguna: prometerlo y que despues
     * falle es peor que no ofrecerlo nunca.
     */
    public static boolean estaDisponible(@NonNull Context contexto) {
        return BiometricManager.from(contexto)
                .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                == BiometricManager.BIOMETRIC_SUCCESS;
    }

    public static void pedir(@NonNull Fragment fragment, @NonNull Callback callback) {
        Context contexto = fragment.requireContext();

        // Paso 1 — preguntar primero: ¿hay biometria fuerte disponible y enrolada?
        if (!estaDisponible(contexto)) {
            // NO_HARDWARE, HW_UNAVAILABLE o NONE_ENROLLED: seguimos sin biometria.
            callback.onNoDisponible();
            return;
        }

        Executor ejecutor = ContextCompat.getMainExecutor(contexto);

        // Paso 2 — como se ve el dialogo.
        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(fragment.getString(R.string.biometria_titulo))
                .setSubtitle(fragment.getString(R.string.biometria_subtitulo))
                .setNegativeButtonText(fragment.getString(R.string.biometria_cancelar))
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build();

        // Paso 3 — dispararlo y escuchar los callbacks.
        BiometricPrompt prompt = new BiometricPrompt(fragment, ejecutor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(
                            @NonNull BiometricPrompt.AuthenticationResult resultado) {
                        callback.onDesbloqueado();
                    }

                    @Override
                    public void onAuthenticationError(int codigo,
                                                      @NonNull CharSequence mensaje) {
                        // Cancelar, tocar el boton negativo o un error del sistema.
                        callback.onCancelado();
                    }

                    // onAuthenticationFailed (una huella que no coincide) deja el
                    // prompt abierto para reintentar, asi que no hacemos nada.
                });

        prompt.authenticate(info);
    }
}
