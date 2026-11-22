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
 * Alta de cuenta (Punto 1): POST /api/auth/registro.
 *
 * Si sale bien, el backend ya mandó el código al mail y se pasa a
 * OtpFragment con el propósito REGISTRO para confirmarlo.
 *
 * Dónde está cada cosa:
 *   onViewCreated           conecta los botones y precarga el email si vino del login
 *   registrar               valida en el celular y llama al backend
 *   mostrarErrorDeRegistro  marca el campo que corresponde a cada error
 *   irAVerificarCodigo      borra la sesión anterior y va a la pantalla del código
 */
@AndroidEntryPoint
public class RegistroFragment extends Fragment {

    // Dependencias que pone Hilt.
    @Inject
    AuthApiService authApi;

    @Inject
    SessionRepository sesion;

    // Vistas del layout fragment_registro.xml.
    private EditText etNombre;
    private EditText etEmail;
    private EditText etPassword;
    private Button btnRegistrarme;
    private ProgressBar progressBar;

    /** Ciclo de vida: infla el XML de la pantalla. */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_registro, container, false);
    }

    /** Ciclo de vida: la vista ya existe. Se buscan los campos y se conectan los botones. */
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
        // "Ya tengo cuenta": popBackStack vuelve a la pantalla anterior (el login).
        tvYaTengoCuenta.setOnClickListener(v ->
                Navigation.findNavController(view).popBackStack());

        // Si viene del login (se pidió un código con un email que no tiene
        // cuenta), el email ya llega escrito.
        String emailSugerido = getArguments() != null
                ? getArguments().getString("email", "") : "";
        if (!emailSugerido.isEmpty()) {
            etEmail.setText(emailSugerido);
        }
    }

    /** Botón "Registrarme". */
    private void registrar(View view) {
        String nombre = etNombre.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        // Validaciones locales: evitan un viaje al servidor que ya sabemos que
        // falla y marcan el campo exacto. El backend las repite igual, porque
        // nunca hay que confiar en el cliente.
        if (!validar(etNombre, ValidadorRegistro.validarNombre(nombre))) return;
        if (!validar(etEmail, ValidadorRegistro.validarEmail(email))) return;
        if (!validar(etPassword, ValidadorRegistro.validarPassword(password))) return;

        mostrarCargando(true);

        authApi
                .registrar(new RegistroRequest(email, password, nombre))
                .enqueue(new Callback<RegistroResponse>() {

                    @Override
                    public void onResponse(@NonNull Call<RegistroResponse> call,
                                           @NonNull Response<RegistroResponse> response) {
                        // enqueue nos devuelve en el hilo principal, así que
                        // acá ya se puede tocar la UI.
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
                        // Acá no hubo respuesta del servidor: sin red, backend
                        // apagado o URL mal.
                        if (!estaVivo()) return;
                        mostrarCargando(false);
                        Toast.makeText(requireContext(),
                                R.string.error_sin_conexion, Toast.LENGTH_LONG).show();
                    }
                });
    }

    /** El backend rechazó el alta: se muestra el error en el campo que corresponde. */
    private void mostrarErrorDeRegistro(Response<RegistroResponse> response) {
        // Una sola lectura del cuerpo: leerlo dos veces devuelve null y se
        // pierde el mensaje real que mandó la API.
        ErrorResponse.Detalle error = ApiErrorParser.parse(response);
        String codigo = ApiErrorParser.codigo(error);
        String mensaje = ApiErrorParser.mensaje(error,
                getString(R.string.error_registro_generico));

        if (codigo == null) {
            Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show();
            return;
        }

        // Cada código marca el campo que corresponde, así la persona ve el
        // error donde lo tiene que corregir y no en un Toast genérico.
        switch (codigo) {
            case "EMAIL_EN_USO":
            case "EMAIL_INVALIDO":
            case "EMAIL_LARGO":
                etEmail.setError(mensaje);
                etEmail.requestFocus();
                break;

            case "NOMBRE_REQUERIDO":
            case "NOMBRE_LARGO":
            case "NOMBRE_CON_NUMEROS":
            case "NOMBRE_INVALIDO":
                etNombre.setError(mensaje);
                etNombre.requestFocus();
                break;

            case "PASSWORD_CORTA":
            case "PASSWORD_LARGA":
                etPassword.setError(mensaje);
                etPassword.requestFocus();
                break;

            default:
                Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show();
        }
    }

    /** La cuenta se creó: se pasa a confirmar el código que llegó al mail. */
    private void irAVerificarCodigo(View view, String email) {
        // La cuenta nueva ya existe, así que lo que quedaba de la anterior
        // sobra. Sin esto la sesión de quien venía usando la app sobrevive al
        // alta: al volver al login, el auto-login la levanta y la persona
        // termina entrando con la cuenta de otro. Y si esa cuenta tenía el
        // desbloqueo con huella activado, encima se lo ofrece.
        sesion.olvidarTodo();

        Bundle args = new Bundle();
        args.putString("email", email);
        args.putString("proposito", "REGISTRO");
        Navigation.findNavController(view).navigate(R.id.action_registro_to_otp, args);
    }

    /** Marca el campo si la validación falló. Devuelve true si está todo bien. */
    private boolean validar(EditText campo, ValidadorRegistro.Resultado resultado) {
        if (resultado.esValido()) {
            campo.setError(null);
            return true;
        }
        campo.setError(getString(resultado.getMensajeError()));
        campo.requestFocus();
        return false;
    }

    /** Muestra la barra de carga y apaga el botón mientras se espera. */
    private void mostrarCargando(boolean cargando) {
        progressBar.setVisibility(cargando ? View.VISIBLE : View.GONE);
        btnRegistrarme.setEnabled(!cargando);
    }

    /**
     * La respuesta puede llegar después de que la persona se fue de la
     * pantalla. Si eso pasa, las vistas ya no existen y tocarlas revienta.
     */
    private boolean estaVivo() {
        return isAdded() && getView() != null;
    }
}
