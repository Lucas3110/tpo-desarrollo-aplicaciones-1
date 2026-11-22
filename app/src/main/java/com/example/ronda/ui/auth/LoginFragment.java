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
import androidx.appcompat.app.AlertDialog;
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
import com.example.ronda.data.network.AuthApiService;
import com.example.ronda.data.repository.SessionRepository;
import com.example.ronda.util.Conectividad;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Pantalla de ingreso (Punto 1). Es la primera que se ve al abrir la app.
 *
 * Hay dos formas de entrar:
 *   - email + contraseña  -> POST /api/auth/login
 *   - código por email    -> POST /api/auth/otp/enviar y después OtpFragment
 *
 * Además hace el auto-login: si al abrir la app hay un token guardado y
 * sigue sirviendo, entra directo al Home (pidiendo la huella si se activó).
 *
 * Dónde está cada cosa:
 *   onCreateView, onViewCreated  arman la pantalla y conectan los botones
 *   intentarAutoLogin            al abrir la app: ¿hay sesión? ¿pide huella?
 *   validarSesionYEntrar         ¿hay red? y le pregunta a /auth/me si el token sirve
 *   login                        ingreso con contraseña
 *   mostrarErrorDeLogin          qué hacer según el error del backend
 *   pedirCodigoDeIngreso         ingreso con código
 *   ofrecerRegistro              mail sin cuenta: ofrece crearla
 *
 * La anotación @AndroidEntryPoint le avisa a Hilt que esta pantalla recibe
 * dependencias con @Inject.
 */
@AndroidEntryPoint
public class LoginFragment extends Fragment {

    // Dependencias. Hilt las crea y las deja puestas antes de que arranque la
    // pantalla: por eso acá no hay ningún new ni getInstance().
    @Inject
    AuthApiService authApi;          // los pedidos al backend de /api/auth

    @Inject
    SessionRepository sesion;        // donde queda guardado el token

    /** Punto 6: sin red no se puede validar el token contra el servidor. */
    @Inject
    Conectividad conectividad;

    // Vistas del layout fragment_login.xml. Se buscan en onViewCreated.
    private EditText etEmail;
    private EditText etPassword;
    private Button btnIngresar;
    private ProgressBar progressBar;

    /** Ciclo de vida: Android le pide a la pantalla su vista y se infla el XML. */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    /**
     * Ciclo de vida: la vista ya existe. Se buscan sus elementos por id, se
     * dice qué hace cada botón y se prueba el auto-login.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // findViewById busca cada elemento del XML por su id (R.id.etEmail, ...).
        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        btnIngresar = view.findViewById(R.id.btnIngresar);
        progressBar = view.findViewById(R.id.progressBar);
        TextView tvIngresarConCodigo = view.findViewById(R.id.tvIngresarConCodigo);
        TextView tvCrearCuenta = view.findViewById(R.id.tvCrearCuenta);

        // Qué pasa al tocar cada cosa. "v -> login(view)" es una lambda: el
        // código que corre cuando se toca el botón.
        btnIngresar.setOnClickListener(v -> login(view));
        tvIngresarConCodigo.setOnClickListener(v -> pedirCodigoDeIngreso(view));
        tvCrearCuenta.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_login_to_registro));

        // Si el email quedó guardado de la última sesión, lo precargamos.
        if (sesion.getEmail() != null) {
            etEmail.setText(sesion.getEmail());
        }

        intentarAutoLogin(view);
    }

    // -----------------------------------------------------------------
    // Auto-login: el token guardado, ¿sigue sirviendo?
    // -----------------------------------------------------------------

    /**
     * Al abrir la app, si hay una sesión guardada, intenta entrar sin pedir
     * nada. Si la persona activó la huella, primero la pide.
     */
    private void intentarAutoLogin(View view) {
        // Sin token guardado no hay nada que recuperar: se queda en el login.
        if (!sesion.haySesion()) {
            return;
        }

        // Punto 1: si la persona activó el desbloqueo biométrico, la sesión
        // queda detrás de la huella. Si no lo activó, el auto-login es el de
        // siempre: no le pedimos algo que nunca eligió.
        if (!sesion.esBiometriaActivada()) {
            validarSesionYEntrar(view);
            return;
        }

        // Muestra el cartel de la huella. El resultado llega a uno de estos
        // tres métodos (es una clase anónima: se escribe acá mismo).
        DesbloqueoBiometrico.pedir(this, new DesbloqueoBiometrico.Callback() {
            @Override
            public void onDesbloqueado() {
                // La huella no reemplaza al token: lo que hace es autorizar que
                // recuperemos la copia cifrada y la usemos como sesión activa.
                String token = sesion.getTokenCifrado();
                if (token != null) {
                    sesion.guardarSesion(token, sesion.getEmail());
                }
                validarSesionYEntrar(view);
            }

            @Override
            public void onNoDisponible() {
                // El dispositivo dejó de tener biometría utilizable (por ejemplo,
                // borraron todas las huellas). Se desactiva la preferencia para
                // no dejar a la persona trabada pidiéndole algo imposible.
                sesion.setBiometriaActivada(false);
                validarSesionYEntrar(view);
            }

            @Override
            public void onCancelado() {
                // Se queda en el login y puede entrar con email y contraseña.
                if (!estaVivo()) return;
                Toast.makeText(requireContext(),
                        R.string.biometria_cancelada, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** Va al Home. La acción del nav graph saca todo lo de auth de la pila. */
    private void irAlHome(View view) {
        if (!estaVivo()) return;
        Navigation.findNavController(view).navigate(R.id.action_auth_to_home);
    }

    /**
     * Valida contra el backend que el token guardado siga sirviendo y, si es
     * así, entra directo al Home.
     */
    private void validarSesionYEntrar(View view) {
        if (!estaVivo()) return;

        // Punto 6: sin conexión no hay a quién preguntarle si el token sigue
        // valiendo, y quedarse en el login dejaría la app inutilizable: el
        // modo sin conexión andaría, pero no habría forma de llegar a él.
        //
        // El token vive 7 días y está guardado acá: alcanza para dejar entrar.
        // Si resulta que ya no servía, el primer pedido que se haga al
        // recuperar la red devuelve 401 y el interceptor manda de vuelta al
        // login. O sea que la validación no se pierde, se pospone.
        if (!conectividad.hayInternet()) {
            irAlHome(view);
            return;
        }

        mostrarCargando(true);

        // GET /api/auth/me: el interceptor le pone el token. Si el backend
        // contesta bien, la sesión sigue viva.
        authApi.me()
                .enqueue(new Callback<PerfilResponse>() {

                    @Override
                    public void onResponse(@NonNull Call<PerfilResponse> call,
                                           @NonNull Response<PerfilResponse> response) {
                        if (!estaVivo()) return;
                        mostrarCargando(false);

                        if (response.isSuccessful()) {
                            // Aprovechamos para refrescar la zona guardada: es
                            // lo que usa el Home para "Solo mi zona".
                            if (response.body() != null && response.body().getUsuario() != null) {
                                sesion.guardarZona(response.body().getUsuario().getZona());
                            }
                            Navigation.findNavController(view)
                                    .navigate(R.id.action_auth_to_home);
                        } else {
                            // El backend no aceptó el token (lo normal es un
                            // 401 porque venció). Se borra y la persona
                            // ingresa de nuevo.
                            sesion.cerrarSesion();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<PerfilResponse> call,
                                          @NonNull Throwable t) {
                        // No hubo respuesta (servidor caído o se cortó la red):
                        // no se puede validar el token, pero tampoco conviene
                        // borrarlo. Se queda en el login y listo.
                        if (!estaVivo()) return;
                        mostrarCargando(false);
                    }
                });
    }

    // -----------------------------------------------------------------
    // Ingreso con email y contraseña
    // -----------------------------------------------------------------

    /** Botón "Ingresar": valida los campos y llama a POST /api/auth/login. */
    private void login(View view) {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        // Primero se valida en el celular: si falta algo, se marca el campo y
        // ni se llama al servidor.
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

        // enqueue manda el pedido en otro hilo: la pantalla no se congela
        // mientras espera la respuesta. Cuando llega, Retrofit llama a
        // onResponse u onFailure ya en el hilo principal.
        authApi.login(new LoginRequest(email, password))
                .enqueue(new Callback<SesionResponse>() {

                    // onResponse: el servidor contestó, aunque sea con un error
                    // (401, 500). Por eso se mira isSuccessful().
                    @Override
                    public void onResponse(@NonNull Call<SesionResponse> call,
                                           @NonNull Response<SesionResponse> response) {
                        if (!estaVivo()) return;
                        mostrarCargando(false);

                        if (response.isSuccessful() && response.body() != null) {
                            // Llegó el token: se guarda la sesión (token, email
                            // y zona) en SessionRepository.
                            SesionResponse cuerpo = response.body();
                            sesion.guardarSesion(cuerpo.getToken(),
                                    cuerpo.getUsuario().getEmail());
                            sesion.guardarZona(cuerpo.getUsuario().getZona());
                            // Punto 1: recién ahora hay un token que guardar
                            // cifrado, así que es el momento de ofrecer la
                            // huella. Cuando la persona responde, sigue al Home.
                            DesbloqueoBiometrico.ofrecerActivacion(LoginFragment.this, sesion,
                                    cuerpo.getToken(), () -> irAlHome(view));
                        } else {
                            mostrarErrorDeLogin(view, response, email);
                        }
                    }

                    // onFailure: no hubo respuesta (sin red, timeout o el
                    // servidor apagado).
                    @Override
                    public void onFailure(@NonNull Call<SesionResponse> call,
                                          @NonNull Throwable t) {
                        if (!estaVivo()) return;
                        mostrarCargando(false);
                        android.util.Log.e("ERROR_RED", "Fallo de conexion", t);
                        Toast.makeText(requireContext(),
                                R.string.error_sin_conexion, Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * El backend rechazó el login. Se lee el error una sola vez y se decide
     * qué hacer según su código.
     */
    private void mostrarErrorDeLogin(View view, Response<SesionResponse> response, String email) {
        ErrorResponse.Detalle error = ApiErrorParser.parse(response);
        String codigo = ApiErrorParser.codigo(error);
        String mensaje = ApiErrorParser.mensaje(error,
                getString(R.string.error_login_generico));

        if ("EMAIL_NO_VERIFICADO".equals(codigo)) {
            // La cuenta existe pero nunca se confirmó: en vez de dejarlo
            // trabado, lo mandamos a completar la verificación.
            Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show();
            irAVerificarCodigo(view, email, "REGISTRO");
            return;
        }

        // Cualquier otro error (por ejemplo, CREDENCIALES_INVALIDAS) se muestra
        // con el mensaje que mandó el backend.
        Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show();
    }

    // -----------------------------------------------------------------
    // Ingreso con código (sin contraseña)
    // -----------------------------------------------------------------

    /**
     * "Ingresar con un código": le pide al backend que mande un código de 6
     * dígitos al mail (propósito LOGIN) y pasa a la pantalla del código.
     */
    private void pedirCodigoDeIngreso(View view) {
        String email = etEmail.getText().toString().trim();

        if (!ValidadorRegistro.validarEmail(email).esValido()) {
            etEmail.setError(getString(R.string.error_email_para_codigo));
            etEmail.requestFocus();
            return;
        }

        mostrarCargando(true);

        authApi
                .enviarOtp(new OtpEnviarRequest(email, "LOGIN"))
                .enqueue(new Callback<MensajeResponse>() {

                    @Override
                    public void onResponse(@NonNull Call<MensajeResponse> call,
                                           @NonNull Response<MensajeResponse> response) {
                        if (!estaVivo()) return;
                        mostrarCargando(false);

                        // Tanto el 200 como el 429 OTP_COOLDOWN significan que hay
                        // un código vigente, así que en los dos casos vamos a la
                        // pantalla de verificación.
                        if (response.isSuccessful() || response.code() == 429) {
                            irAVerificarCodigo(view, email, "LOGIN");
                        } else {
                            // Una sola lectura del cuerpo: leerlo dos veces
                            // devuelve null y se pierde el código del error.
                            ErrorResponse.Detalle error = ApiErrorParser.parse(response);
                            if ("USUARIO_NO_ENCONTRADO".equals(ApiErrorParser.codigo(error))) {
                                ofrecerRegistro(view, email);
                                return;
                            }
                            Toast.makeText(requireContext(),
                                    ApiErrorParser.mensaje(error,
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

    /**
     * Se pidió un código para un email que no tiene cuenta: en vez de quedarse
     * en el error, se le ofrece registrarse.
     *
     * Solo pasa con el código: con contraseña el backend responde lo mismo
     * que con una contraseña mal escrita, para no revelar qué mails tienen
     * cuenta. Con el código avisa USUARIO_NO_ENCONTRADO, y ahí se ofrece crear
     * la cuenta con el email ya escrito.
     */
    private void ofrecerRegistro(View view, String email) {
        // AlertDialog: el cartel con título, mensaje y dos botones.
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.login_sin_cuenta_titulo)
                .setMessage(getString(R.string.login_sin_cuenta_mensaje, email))
                .setPositiveButton(R.string.login_sin_cuenta_crear, (d, cual) -> {
                    if (!estaVivo()) return;
                    // El email viaja como argumento a la pantalla de registro.
                    Bundle args = new Bundle();
                    args.putString("email", email);
                    Navigation.findNavController(view)
                            .navigate(R.id.action_login_to_registro, args);
                })
                .setNegativeButton(R.string.login_sin_cuenta_cancelar, null)
                .show();
    }

    // -----------------------------------------------------------------
    // Ayudas
    // -----------------------------------------------------------------

    /**
     * Va a la pantalla del código. El email y el propósito (LOGIN o REGISTRO)
     * viajan en un Bundle: son los argumentos que declara auth_nav_graph.xml.
     */
    private void irAVerificarCodigo(View view, String email, String proposito) {
        Bundle args = new Bundle();
        args.putString("email", email);
        args.putString("proposito", proposito);
        Navigation.findNavController(view).navigate(R.id.action_login_to_otp, args);
    }

    /** Muestra la barra de carga y apaga el botón, para no mandar el pedido dos veces. */
    private void mostrarCargando(boolean cargando) {
        progressBar.setVisibility(cargando ? View.VISIBLE : View.GONE);
        btnIngresar.setEnabled(!cargando);
    }

    /**
     * La respuesta del servidor puede llegar cuando la persona ya se fue de
     * esta pantalla. Si la vista ya no existe, tocarla rompe la app: por eso
     * cada callback pregunta esto antes de hacer nada.
     */
    private boolean estaVivo() {
        return isAdded() && getView() != null;
    }
}
