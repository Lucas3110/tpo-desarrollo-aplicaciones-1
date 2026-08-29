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
import com.example.ronda.data.model.RegistroRequest;
import com.example.ronda.data.model.RegistroResponse;
import com.example.ronda.data.network.ApiErrorParser;
import com.example.ronda.data.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Alta de cuenta: POST /api/auth/registro.
 * Si sale bien, el backend ya mando el codigo OTP y pasamos a OtpFragment.
 */
public class RegistroFragment extends Fragment {

    private EditText etNombre;
    private EditText etEmail;
    private EditText etPassword;
    private Button btnRegistrarme;
    private ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_registro, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etNombre = view.findViewById(R.id.etNombre);
        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        btnRegistrarme = view.findViewById(R.id.btnRegistrarme);
        progressBar = view.findViewById(R.id.progressBar);
        TextView tvYaTengoCuenta = view.findViewById(R.id.tvYaTengoCuenta);

        btnRegistrarme.setOnClickListener(v -> registrar(view));
        tvYaTengoCuenta.setOnClickListener(v ->
                Navigation.findNavController(view).popBackStack());
    }

    private void registrar(View view) {
        String nombre = etNombre.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        // Validaciones locales: evitan un viaje al servidor que ya sabemos que falla.
        // El backend igual las repite, porque nunca hay que confiar en el cliente.
        if (email.isEmpty() || !email.contains("@")) {
            etEmail.setError(getString(R.string.error_email_invalido));
            etEmail.requestFocus();
            return;
        }
        if (password.length() < 6) {
            etPassword.setError(getString(R.string.error_password_corta));
            etPassword.requestFocus();
            return;
        }

        mostrarCargando(true);

        RetrofitClient.getAuthApi()
                .registrar(new RegistroRequest(email, password, nombre))
                .enqueue(new Callback<RegistroResponse>() {

                    @Override
                    public void onResponse(@NonNull Call<RegistroResponse> call,
                                           @NonNull Response<RegistroResponse> response) {
                        // enqueue nos devuelve en el hilo principal, asi que
                        // aca ya se puede tocar la UI.
                        if (!estaVivo()) return;
                        mostrarCargando(false);

                        if (response.isSuccessful()) {
                            irAVerificarCodigo(view, email);
                        } else {
                            mostrarErrorDeRegistro(response);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<RegistroResponse> call,
                                          @NonNull Throwable t) {
                        // Aca no hubo respuesta del servidor: sin red, backend
                        // apagado o URL mal.
                        if (!estaVivo()) return;
                        mostrarCargando(false);
                        Toast.makeText(requireContext(),
                                R.string.error_sin_conexion, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void mostrarErrorDeRegistro(Response<RegistroResponse> response) {
        String codigo = ApiErrorParser.codigoDe(response);
        String mensaje = ApiErrorParser.mensajeDe(response,
                getString(R.string.error_registro_generico));

        if ("EMAIL_EN_USO".equals(codigo)) {
            etEmail.setError(mensaje);
            etEmail.requestFocus();
        } else if ("PASSWORD_CORTA".equals(codigo)) {
            etPassword.setError(mensaje);
            etPassword.requestFocus();
        } else {
            Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show();
        }
    }

    private void irAVerificarCodigo(View view, String email) {
        Bundle args = new Bundle();
        args.putString("email", email);
        args.putString("proposito", "REGISTRO");
        Navigation.findNavController(view).navigate(R.id.action_registro_to_otp, args);
    }

    private void mostrarCargando(boolean cargando) {
        progressBar.setVisibility(cargando ? View.VISIBLE : View.GONE);
        btnRegistrarme.setEnabled(!cargando);
    }

    /**
     * La respuesta puede llegar despues de que el usuario se fue de la pantalla.
     * Si eso pasa, las vistas ya no existen y tocarlas revienta.
     */
    private boolean estaVivo() {
        return isAdded() && getView() != null;
    }
}
