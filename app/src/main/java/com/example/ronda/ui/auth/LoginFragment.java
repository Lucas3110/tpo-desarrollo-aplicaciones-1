package com.example.ronda.ui.auth;

import android.os.Bundle;
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
import com.example.ronda.data.model.LoginRequest;
import com.example.ronda.data.model.MensajeResponse;
import com.example.ronda.data.model.OtpEnviarRequest;
import com.example.ronda.data.model.PerfilResponse;
import com.example.ronda.data.model.SesionResponse;
import com.example.ronda.data.model.ErrorResponse;
import com.example.ronda.data.network.ApiErrorParser;
import com.example.ronda.data.network.RetrofitClient;
import com.example.ronda.data.repository.SessionRepository;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Pantalla de ingreso. Cubre los dos caminos del enunciado:
 *   - email + contrasena  -> POST /auth/login
 *   - codigo por email    -> POST /auth/otp/enviar y luego OtpFragment
 *
 * Ademas hace auto-login: si hay un token guardado y sigue siendo valido,
 * entra directo al Home sin pedir nada.
 */
public class LoginFragment extends Fragment {

    private EditText etEmail;
    private EditText etPassword;
    private Button btnIngresar;
    private ProgressBar progressBar;
    private SessionRepository sesion;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sesion = new SessionRepository(requireContext());

        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        btnIngresar = view.findViewById(R.id.btnIngresar);
        progressBar = view.findViewById(R.id.progressBar);
        TextView tvIngresarConCodigo = view.findViewById(R.id.tvIngresarConCodigo);
        TextView tvCrearCuenta = view.findViewById(R.id.tvCrearCuenta);

        btnIngresar.setOnClickListener(v -> login(view));
        tvIngresarConCodigo.setOnClickListener(v -> pedirCodigoDeIngreso(view));
        tvCrearCuenta.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_login_to_registro));

        // Si el email quedo guardado de la ultima sesion, lo precargamos.
        if (sesion.getEmail() != null) {
            etEmail.setText(sesion.getEmail());
        }

        intentarAutoLogin(view);
    }

    // -----------------------------------------------------------------
    // Auto-login: el token guardado, ¿sigue sirviendo?
    // -----------------------------------------------------------------
    private void intentarAutoLogin(View view) {
        if (!sesion.haySesion()) {
            return;
        }

        mostrarCargando(true);

        RetrofitClient.getAuthApi().me(sesion.getBearer())
                .enqueue(new Callback<PerfilResponse>() {

                    @Override
                    public void onResponse(@NonNull Call<PerfilResponse> call,
                                           @NonNull Response<PerfilResponse> response) {
                        if (!estaVivo()) return;
                        mostrarCargando(false);

                        if (response.isSuccessful()) {
                            Navigation.findNavController(view)
                                    .navigate(R.id.action_auth_to_home);
                        } else {
                            // 401: el token vencio o ya no vale. Lo tiramos y
                            // que el usuario ingrese de nuevo.
                            sesion.cerrarSesion();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<PerfilResponse> call,
                                          @NonNull Throwable t) {
                        // Sin red no podemos validar el token, pero tampoco
                        // conviene borrarlo: se queda en el login y listo.
                        if (!estaVivo()) return;
                        mostrarCargando(false);
                    }
                });
    }

    // -----------------------------------------------------------------
    // Ingreso con email y contrasena
    // -----------------------------------------------------------------
    private void login(View view) {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        ValidadorRegistro.Resultado emailOk = ValidadorRegistro.validarEmail(email);
        if (!emailOk.esValido()) {
            etEmail.setError(getString(emailOk.getMensajeError()));
            etEmail.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            etPassword.setError(getString(R.string.error_password_vacia));
            etPassword.requestFocus();
            return;
        }

        mostrarCargando(true);

        RetrofitClient.getAuthApi().login(new LoginRequest(email, password))
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
                            Navigation.findNavController(view)
                                    .navigate(R.id.action_auth_to_home);
                        } else {
                            mostrarErrorDeLogin(view, response, email);
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

    private void mostrarErrorDeLogin(View view, Response<SesionResponse> response, String email) {
        ErrorResponse.Detalle error = ApiErrorParser.parse(response);
        String codigo = ApiErrorParser.codigo(error);
        String mensaje = ApiErrorParser.mensaje(error,
                getString(R.string.error_login_generico));

        if ("EMAIL_NO_VERIFICADO".equals(codigo)) {
            // La cuenta existe pero nunca se confirmo: en vez de dejarlo
            // trabado, lo mandamos a completar la verificacion.
            Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show();
            irAVerificarCodigo(view, email, "REGISTRO");
            return;
        }

        Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show();
    }

    // -----------------------------------------------------------------
    // Ingreso con codigo (sin contrasena)
    // -----------------------------------------------------------------
    private void pedirCodigoDeIngreso(View view) {
        String email = etEmail.getText().toString().trim();

        if (!ValidadorRegistro.validarEmail(email).esValido()) {
            etEmail.setError(getString(R.string.error_email_para_codigo));
            etEmail.requestFocus();
            return;
        }

        mostrarCargando(true);

        RetrofitClient.getAuthApi()
                .enviarOtp(new OtpEnviarRequest(email, "LOGIN"))
                .enqueue(new Callback<MensajeResponse>() {

                    @Override
                    public void onResponse(@NonNull Call<MensajeResponse> call,
                                           @NonNull Response<MensajeResponse> response) {
                        if (!estaVivo()) return;
                        mostrarCargando(false);

                        // Tanto el 200 como el 429 OTP_COOLDOWN significan que hay
                        // un codigo vigente, asi que en los dos casos vamos a la
                        // pantalla de verificacion.
                        if (response.isSuccessful() || response.code() == 429) {
                            irAVerificarCodigo(view, email, "LOGIN");
                        } else {
                            Toast.makeText(requireContext(),
                                    ApiErrorParser.mensaje(ApiErrorParser.parse(response),
                                            getString(R.string.error_reenvio_generico)),
                                    Toast.LENGTH_LONG).show();
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
    private void irAVerificarCodigo(View view, String email, String proposito) {
        Bundle args = new Bundle();
        args.putString("email", email);
        args.putString("proposito", proposito);
        Navigation.findNavController(view).navigate(R.id.action_login_to_otp, args);
    }

    private void mostrarCargando(boolean cargando) {
        progressBar.setVisibility(cargando ? View.VISIBLE : View.GONE);
        btnIngresar.setEnabled(!cargando);
    }

    private boolean estaVivo() {
        return isAdded() && getView() != null;
    }
}
