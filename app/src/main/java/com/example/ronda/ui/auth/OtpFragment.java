package com.example.ronda.ui.auth;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.ronda.R;
import com.example.ronda.data.model.MensajeResponse;
import com.example.ronda.data.model.OtpEnviarRequest;
import com.example.ronda.data.model.OtpVerificarRequest;
import com.example.ronda.data.model.SesionResponse;
import com.example.ronda.data.model.ErrorResponse;
import com.example.ronda.data.network.ApiErrorParser;
import com.example.ronda.data.network.AuthApiService;
import com.example.ronda.data.repository.SessionRepository;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Pantalla del código de verificación (Punto 1). La usan los dos caminos,
 * según el argumento "proposito" que llega por el nav graph:
 *   REGISTRO -> confirmar la cuenta recién creada
 *   LOGIN    -> ingresar sin contraseña
 *
 * Verificar el código devuelve un token en los dos casos, y en los dos se
 * crea la sesión. Lo único que cambia es a dónde va después: la cuenta
 * nueva pasa por la bienvenida antes del Home.
 *
 * Dónde está cada cosa:
 *   onViewCreated               lee los argumentos y arranca la cuenta regresiva
 *   verificar                   POST /api/auth/otp/verificar y crea la sesión
 *   mostrarErrorDeVerificacion  qué hacer según el error (OTP_INVALIDO, ...)
 *   reenviar                    pide otro código
 *   arrancarCuentaRegresiva     los 60 segundos del botón "Reenviar"
 *   onDestroyView               corta el timer al salir de la pantalla
 */
@AndroidEntryPoint
public class OtpFragment extends Fragment {

    // Dependencias que pone Hilt, igual que en el login.
    @Inject
    AuthApiService authApi;

    @Inject
    SessionRepository sesion;

    /** Tiene que coincidir con OTP_RESEND_COOLDOWN_SECONDS del backend. */
    private static final int SEGUNDOS_DE_ESPERA = 60;

    // Los argumentos que llegan del login o del registro.
    private String email = "";
    private String proposito = "REGISTRO";

    // Vistas del layout fragment_otp.xml.
    private EditText etCodigo;
    private Button btnVerificar;
    private Button btnReenviar;
    private ProgressBar progressBar;

    /** El timer del botón "Reenviar". Se guarda para poder cortarlo. */
    private CountDownTimer cuentaRegresiva;

    /** Ciclo de vida: infla el XML de la pantalla. */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_otp, container, false);
    }

    /** Ciclo de vida: la vista ya existe. Se leen los argumentos y se conectan los botones. */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // getArguments() trae el Bundle que mandó la pantalla anterior.
        if (getArguments() != null) {
            email = getArguments().getString("email", "");
            proposito = getArguments().getString("proposito", "REGISTRO");
        }

        TextView tvDestino = view.findViewById(R.id.tvDestino);
        etCodigo = view.findViewById(R.id.etCodigo);
        btnVerificar = view.findViewById(R.id.btnVerificar);
        btnReenviar = view.findViewById(R.id.btnReenviar);
        progressBar = view.findViewById(R.id.progressBar);

        // "Te mandamos un código a ...".
        tvDestino.setText(getString(R.string.otp_enviado_a, email));

        btnVerificar.setOnClickListener(v -> verificar(view));
        btnReenviar.setOnClickListener(v -> reenviar());

        // El backend acaba de mandar un código al entrar a esta pantalla,
        // así que el reenvío arranca bloqueado por el mismo tiempo que el
        // cooldown del servidor. Si no, la API respondería 429.
        arrancarCuentaRegresiva(SEGUNDOS_DE_ESPERA);
    }

    // -----------------------------------------------------------------
    // Verificar el código
    // -----------------------------------------------------------------

    /** Botón "Verificar": manda el código al backend. */
    private void verificar(View view) {
        String codigo = etCodigo.getText().toString().trim();

        // El código siempre tiene 6 dígitos: si no, ni se llama al servidor.
        if (codigo.length() != 6) {
            etCodigo.setError(getString(R.string.error_codigo_incompleto));
            etCodigo.requestFocus();
            return;
        }

        mostrarCargando(true);

        authApi
                .verificarOtp(new OtpVerificarRequest(email, codigo, proposito))
                .enqueue(new Callback<SesionResponse>() {

                    @Override
                    public void onResponse(@NonNull Call<SesionResponse> call,
                                           @NonNull Response<SesionResponse> response) {
                        if (!estaVivo()) return;
                        mostrarCargando(false);

                        if (response.isSuccessful() && response.body() != null) {
                            SesionResponse cuerpo = response.body();

                            // Los dos caminos crean la sesión: el token que
                            // devuelve el backend al verificar ya sirve para
                            // entrar, también con una cuenta recién creada.
                            String emailSesion = cuerpo.getUsuario() != null
                                    ? cuerpo.getUsuario().getEmail() : email;
                            sesion.guardarSesion(cuerpo.getToken(), emailSesion);
                            sesion.guardarZona(cuerpo.getUsuario() != null
                                    ? cuerpo.getUsuario().getZona() : null);

                            // Recién ahora hay un token, así que es el momento
                            // de ofrecer la huella, igual que en el login.
                            // "seguir" es lo que se hace después: la cuenta
                            // nueva va a la bienvenida y el ingreso con código
                            // va directo al Home.
                            Runnable seguir = "REGISTRO".equals(proposito)
                                    ? () -> irABienvenida(view)
                                    : () -> irAlHome(view);
                            DesbloqueoBiometrico.ofrecerActivacion(OtpFragment.this, sesion,
                                    cuerpo.getToken(), seguir);
                        } else {
                            mostrarErrorDeVerificacion(response);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<SesionResponse> call,
                                          @NonNull Throwable t) {
                        if (!estaVivo()) return;
                        mostrarCargando(false);
                        Toast.makeText(requireContext(),
                                R.string.error_sin_conexion, Toast.LENGTH_LONG).show();
                    }
                });
    }

    /** Cuenta nueva confirmada: la bienvenida, y desde ahí el Home. */
    private void irABienvenida(View view) {
        if (!estaVivo()) return;
        Bundle args = new Bundle();
        args.putString("email", email);
        Navigation.findNavController(view).navigate(R.id.action_otp_to_bienvenida, args);
    }

    /** Ingreso con código: un inicio de sesión como cualquier otro. */
    private void irAlHome(View view) {
        if (!estaVivo()) return;
        Navigation.findNavController(view).navigate(R.id.action_otp_to_home);
    }

    /** El backend rechazó el código: cada código de error tiene su respuesta. */
    private void mostrarErrorDeVerificacion(Response<SesionResponse> response) {
        ErrorResponse.Detalle error = ApiErrorParser.parse(response);
        String codigo = ApiErrorParser.codigo(error);
        String mensaje = ApiErrorParser.mensaje(error,
                getString(R.string.error_codigo_generico));

        if (codigo == null) {
            Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show();
            return;
        }

        switch (codigo) {
            case "OTP_INVALIDO":
                // Código mal escrito: se marca el campo para corregirlo.
                etCodigo.setError(mensaje);
                etCodigo.requestFocus();
                break;

            case "OTP_EXPIRADO":
            case "OTP_BLOQUEADO":
            case "OTP_INEXISTENTE":
                // Ya no sirve insistir con este código: hay que pedir uno nuevo,
                // así que habilitamos el reenvío en el acto.
                etCodigo.setText("");
                habilitarReenvio();
                Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show();
                break;

            default:
                Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show();
        }
    }

    // -----------------------------------------------------------------
    // Reenviar el código
    // -----------------------------------------------------------------

    /** Botón "Reenviar": pide otro código con el mismo propósito. */
    private void reenviar() {
        mostrarCargando(true);

        authApi
                .enviarOtp(new OtpEnviarRequest(email, proposito))
                .enqueue(new Callback<MensajeResponse>() {

                    @Override
                    public void onResponse(@NonNull Call<MensajeResponse> call,
                                           @NonNull Response<MensajeResponse> response) {
                        if (!estaVivo()) return;
                        mostrarCargando(false);

                        if (response.isSuccessful()) {
                            etCodigo.setText("");
                            arrancarCuentaRegresiva(SEGUNDOS_DE_ESPERA);
                            Toast.makeText(requireContext(),
                                    R.string.otp_reenviado, Toast.LENGTH_SHORT).show();
                        } else {
                            // 429 OTP_COOLDOWN: el servidor todavía no deja pedir otro.
                            Toast.makeText(requireContext(),
                                    ApiErrorParser.mensaje(ApiErrorParser.parse(response),
                                            getString(R.string.error_reenvio_generico)),
                                    Toast.LENGTH_LONG).show();
                            arrancarCuentaRegresiva(SEGUNDOS_DE_ESPERA);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<MensajeResponse> call,
                                          @NonNull Throwable t) {
                        if (!estaVivo()) return;
                        mostrarCargando(false);
                        Toast.makeText(requireContext(),
                                R.string.error_sin_conexion, Toast.LENGTH_LONG).show();
                    }
                });
    }

    // -----------------------------------------------------------------
    // Cuenta regresiva del botón "Reenviar"
    // -----------------------------------------------------------------

    /**
     * Bloquea "Reenviar" y muestra los segundos que faltan. CountDownTimer
     * llama a onTick cada segundo y a onFinish cuando termina.
     */
    private void arrancarCuentaRegresiva(int segundos) {
        cancelarCuentaRegresiva();
        btnReenviar.setEnabled(false);

        cuentaRegresiva = new CountDownTimer(segundos * 1000L, 1000L) {
            @Override
            public void onTick(long restanteMs) {
                if (!estaVivo()) return;
                btnReenviar.setText(getString(R.string.otp_reenviar_en, restanteMs / 1000));
            }

            @Override
            public void onFinish() {
                if (!estaVivo()) return;
                habilitarReenvio();
            }
        }.start();
    }

    /** Deja "Reenviar" disponible ya, sin esperar. */
    private void habilitarReenvio() {
        cancelarCuentaRegresiva();
        btnReenviar.setText(R.string.otp_reenviar);
        btnReenviar.setEnabled(true);
    }

    private void cancelarCuentaRegresiva() {
        if (cuentaRegresiva != null) {
            cuentaRegresiva.cancel();
            cuentaRegresiva = null;
        }
    }

    /** Ciclo de vida: la vista se está por destruir (por ejemplo, al salir de la pantalla). */
    @Override
    public void onDestroyView() {
        // Si no lo cancelamos, el timer sigue corriendo cuando la vista ya no
        // existe y el próximo onTick revienta con NullPointerException.
        cancelarCuentaRegresiva();
        super.onDestroyView();
    }

    // -----------------------------------------------------------------
    // Ayudas
    // -----------------------------------------------------------------

    /** Muestra la barra de carga y apaga "Verificar" mientras se espera. */
    private void mostrarCargando(boolean cargando) {
        progressBar.setVisibility(cargando ? View.VISIBLE : View.GONE);
        btnVerificar.setEnabled(!cargando);
    }

    /** ¿La pantalla sigue a la vista? La respuesta puede llegar cuando ya se fue. */
    private boolean estaVivo() {
        return isAdded() && getView() != null;
    }
}
