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
import com.example.ronda.data.network.RetrofitClient;
import com.example.ronda.data.repository.SessionRepository;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Pantalla del codigo de verificacion. Cubre los dos caminos del enunciado
 * segun el argumento "proposito":
 *   REGISTRO -> confirmar la cuenta recien creada
 *   LOGIN    -> ingresar sin contrasena
 *
 * En los dos casos, verificar el codigo devuelve el token y crea la sesion.
 */
public class OtpFragment extends Fragment {

    /** Tiene que coincidir con OTP_RESEND_COOLDOWN_SECONDS del backend. */
    private static final int SEGUNDOS_DE_ESPERA = 60;

    private String email = "";
    private String proposito = "REGISTRO";

    private EditText etCodigo;
    private Button btnVerificar;
    private Button btnReenviar;
    private ProgressBar progressBar;

    private CountDownTimer cuentaRegresiva;
    private SessionRepository sesion;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_otp, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            email = getArguments().getString("email", "");
            proposito = getArguments().getString("proposito", "REGISTRO");
        }

        sesion = new SessionRepository(requireContext());

        TextView tvDestino = view.findViewById(R.id.tvDestino);
        etCodigo = view.findViewById(R.id.etCodigo);
        btnVerificar = view.findViewById(R.id.btnVerificar);
        btnReenviar = view.findViewById(R.id.btnReenviar);
        progressBar = view.findViewById(R.id.progressBar);

        tvDestino.setText(getString(R.string.otp_enviado_a, email));

        btnVerificar.setOnClickListener(v -> verificar(view));
        btnReenviar.setOnClickListener(v -> reenviar());

        // El backend acaba de mandar un codigo al entrar a esta pantalla,
        // asi que el reenvio arranca bloqueado por el mismo tiempo que el
        // cooldown del servidor. Si no, la API responderia 429.
        arrancarCuentaRegresiva(SEGUNDOS_DE_ESPERA);
    }

    // -----------------------------------------------------------------
    // Verificar el codigo
    // -----------------------------------------------------------------
    private void verificar(View view) {
        String codigo = etCodigo.getText().toString().trim();

        if (codigo.length() != 6) {
            etCodigo.setError(getString(R.string.error_codigo_incompleto));
            etCodigo.requestFocus();
            return;
        }

        mostrarCargando(true);

        RetrofitClient.getAuthApi()
                .verificarOtp(new OtpVerificarRequest(email, codigo, proposito))
                .enqueue(new Callback<SesionResponse>() {

                    @Override
                    public void onResponse(@NonNull Call<SesionResponse> call,
                                           @NonNull Response<SesionResponse> response) {
                        if (!estaVivo()) return;
                        mostrarCargando(false);

                        if (response.isSuccessful() && response.body() != null) {
                            SesionResponse cuerpo = response.body();
                            sesion.guardarSesion(cuerpo.getToken(),
                                    cuerpo.getUsuario().getEmail());

                            // Al confirmar una cuenta nueva mostramos la
                            // bienvenida y devolvemos a la persona al login.
                            // Al ingresar con codigo, en cambio, se queda en
                            // el Home como cualquier inicio de sesion.
                            Bundle args = new Bundle();
                            args.putBoolean("volverAlLogin",
                                    "REGISTRO".equals(proposito));
                            Navigation.findNavController(view)
                                    .navigate(R.id.action_otp_to_home, args);
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
                etCodigo.setError(mensaje);
                etCodigo.requestFocus();
                break;

            case "OTP_EXPIRADO":
            case "OTP_BLOQUEADO":
            case "OTP_INEXISTENTE":
                // Ya no sirve insistir con este codigo: hay que pedir uno nuevo,
                // asi que habilitamos el reenvio en el acto.
                etCodigo.setText("");
                habilitarReenvio();
                Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show();
                break;

            default:
                Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show();
        }
    }

    // -----------------------------------------------------------------
    // Reenviar el codigo
    // -----------------------------------------------------------------
    private void reenviar() {
        mostrarCargando(true);

        RetrofitClient.getAuthApi()
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
                            // 429 OTP_COOLDOWN: el servidor todavia no deja pedir otro.
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
    // Cuenta regresiva del boton "Reenviar"
    // -----------------------------------------------------------------
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

    @Override
    public void onDestroyView() {
        // Si no lo cancelamos, el timer sigue corriendo cuando la vista ya no
        // existe y el proximo onTick revienta con NullPointerException.
        cancelarCuentaRegresiva();
        super.onDestroyView();
    }

    // -----------------------------------------------------------------
    private void mostrarCargando(boolean cargando) {
        progressBar.setVisibility(cargando ? View.VISIBLE : View.GONE);
        btnVerificar.setEnabled(!cargando);
    }

    private boolean estaVivo() {
        return isAdded() && getView() != null;
    }
}
