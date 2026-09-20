package com.example.ronda.ui.perfil;

import android.app.Dialog;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.ronda.R;
import com.example.ronda.data.model.CambiarEmailRequest;
import com.example.ronda.data.model.ConfirmarEmailRequest;
import com.example.ronda.data.model.ErrorResponse;
import com.example.ronda.data.model.MensajeResponse;
import com.example.ronda.data.model.PerfilResponse;
import com.example.ronda.data.network.ApiErrorParser;
import com.example.ronda.data.network.UsuarioApiService;
import com.example.ronda.ui.auth.ValidadorRegistro;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Cambio de email del Punto 2, en dos pasos dentro del mismo dialogo:
 *
 *   1. Se escribe el email nuevo -> POST /usuarios/me/email/solicitar. El
 *      backend manda un codigo a ESA direccion y todavia no cambia nada.
 *   2. Se escribe el codigo -> POST /usuarios/me/email/confirmar. Recien
 *      ahi la cuenta pasa al email nuevo.
 *
 * Es un DialogFragment para sobrevivir a la rotacion: el paso, el email
 * pedido y el momento en que se habilita el reenvio se guardan en el estado.
 * Al terminar devuelve el email nuevo con la API de resultados de Fragments
 * (CLAVE_RESULTADO / RES_EMAIL) y quien lo mostro actualiza su pantalla.
 *
 * El 401 no se trata aca: el interceptor de red ya limpia la sesion y la
 * Activity vuelve al login.
 */
@AndroidEntryPoint
public class CambiarEmailDialogFragment extends DialogFragment {

    public static final String CLAVE_RESULTADO = "cambiar_email";
    public static final String RES_EMAIL = "email";

    /** Tiene que coincidir con OTP_RESEND_COOLDOWN_SECONDS del backend. */
    private static final int SEGUNDOS_DE_ESPERA = 60;
    private static final int LARGO_CODIGO = 6;

    private static final String ESTADO_PASO = "paso";
    private static final String ESTADO_EMAIL = "emailPedido";
    private static final String ESTADO_REENVIO = "reenvioDisponibleEn";

    private static final int PASO_EMAIL = 0;
    private static final int PASO_CODIGO = 1;

    @Inject
    UsuarioApiService usuarioApi;

    private TextView tvInstruccion;
    private EditText etEmailNuevo;
    private EditText etCodigo;
    private ProgressBar progressBar;

    private int paso = PASO_EMAIL;
    /** Email al que se pidio el codigo (ya recortado). */
    private String emailPedido = "";
    /** Momento (ms de reloj) desde el cual se puede pedir otro codigo. */
    private long reenvioDisponibleEn = 0;
    private boolean enCurso = false;

    private Call<MensajeResponse> llamadaSolicitar;
    private Call<PerfilResponse> llamadaConfirmar;
    private CountDownTimer cuentaRegresiva;

    public static CambiarEmailDialogFragment newInstance() {
        return new CambiarEmailDialogFragment();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            paso = savedInstanceState.getInt(ESTADO_PASO, PASO_EMAIL);
            emailPedido = savedInstanceState.getString(ESTADO_EMAIL, "");
            reenvioDisponibleEn = savedInstanceState.getLong(ESTADO_REENVIO, 0);
        }

        View vista = getLayoutInflater().inflate(R.layout.dialog_cambiar_email, null);
        tvInstruccion = vista.findViewById(R.id.tvInstruccion);
        etEmailNuevo = vista.findViewById(R.id.etEmailNuevo);
        etCodigo = vista.findViewById(R.id.etCodigo);
        progressBar = vista.findViewById(R.id.progressBar);

        // Los botones se conectan en onStart: si se pasan aca, el dialogo se
        // cierra solo al tocarlos aunque lo escrito no sea valido.
        return new AlertDialog.Builder(requireContext())
                .setTitle(R.string.cambiar_email_titulo)
                .setView(vista)
                .setPositiveButton(R.string.cambiar_email_enviar, null)
                .setNeutralButton(R.string.otp_reenviar, null)
                .setNegativeButton(R.string.accion_cancelar, null)
                .create();
    }

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog dialogo = (AlertDialog) getDialog();
        if (dialogo == null) return;

        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (paso == PASO_EMAIL) pedirCodigo(false);
            else confirmar();
        });
        dialogo.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> pedirCodigo(true));

        mostrarPaso();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ESTADO_PASO, paso);
        outState.putString(ESTADO_EMAIL, emailPedido);
        outState.putLong(ESTADO_REENVIO, reenvioDisponibleEn);
    }

    // -----------------------------------------------------------------
    // Pasos
    // -----------------------------------------------------------------

    /** Muestra el campo, el texto y los botones que corresponden al paso actual. */
    private void mostrarPaso() {
        AlertDialog dialogo = (AlertDialog) getDialog();
        if (dialogo == null) return;
        Button principal = dialogo.getButton(AlertDialog.BUTTON_POSITIVE);
        Button reenviar = dialogo.getButton(AlertDialog.BUTTON_NEUTRAL);

        boolean pidiendoEmail = paso == PASO_EMAIL;
        etEmailNuevo.setVisibility(pidiendoEmail ? View.VISIBLE : View.GONE);
        etCodigo.setVisibility(pidiendoEmail ? View.GONE : View.VISIBLE);
        reenviar.setVisibility(pidiendoEmail ? View.GONE : View.VISIBLE);
        principal.setText(pidiendoEmail ? R.string.cambiar_email_enviar
                : R.string.cambiar_email_confirmar);
        tvInstruccion.setText(pidiendoEmail
                ? getString(R.string.cambiar_email_instruccion)
                : getString(R.string.cambiar_email_codigo_instruccion, emailPedido));

        if (pidiendoEmail) {
            cancelarCuentaRegresiva();
            etEmailNuevo.requestFocus();
        } else {
            retomarCuentaRegresiva();
            etCodigo.requestFocus();
        }
    }

    // -----------------------------------------------------------------
    // Primer paso: pedir el codigo (tambien es el reenvio del segundo)
    // -----------------------------------------------------------------

    private void pedirCodigo(boolean esReenvio) {
        if (enCurso) return;

        // En el reenvio se vuelve a pedir al mismo email; en el primer pedido
        // sale de lo que se escribio.
        String email = esReenvio ? emailPedido : etEmailNuevo.getText().toString().trim();
        if (!esReenvio) {
            ValidadorRegistro.Resultado ok = ValidadorRegistro.validarEmail(email);
            if (!ok.esValido()) {
                etEmailNuevo.setError(getString(ok.getMensajeError()));
                etEmailNuevo.requestFocus();
                return;
            }
        }

        marcarEnCurso(true);
        final String pedido = email;
        llamadaSolicitar = usuarioApi.solicitarCambioEmail(new CambiarEmailRequest(pedido));
        llamadaSolicitar.enqueue(new Callback<MensajeResponse>() {
            @Override
            public void onResponse(@NonNull Call<MensajeResponse> call,
                                   @NonNull Response<MensajeResponse> response) {
                if (call.isCanceled() || !estaVivo()) return;
                marcarEnCurso(false);

                if (response.isSuccessful()) {
                    emailPedido = pedido;
                    reenvioDisponibleEn = System.currentTimeMillis() + SEGUNDOS_DE_ESPERA * 1000L;
                    paso = PASO_CODIGO;
                    etCodigo.setText("");
                    mostrarPaso();
                    if (esReenvio) {
                        Toast.makeText(requireContext(), R.string.otp_reenviado,
                                Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
                mostrarErrorDeSolicitud(response, esReenvio);
            }

            @Override
            public void onFailure(@NonNull Call<MensajeResponse> call, @NonNull Throwable t) {
                if (call.isCanceled() || !estaVivo()) return;
                marcarEnCurso(false);
                Toast.makeText(requireContext(), R.string.error_sin_conexion,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void mostrarErrorDeSolicitud(Response<MensajeResponse> response, boolean esReenvio) {
        ErrorResponse.Detalle error = ApiErrorParser.parse(response);
        String codigo = ApiErrorParser.codigo(error);
        String mensaje = ApiErrorParser.mensaje(error, getString(R.string.cambiar_email_error_generico));

        if (codigo != null) {
            switch (codigo) {
                case "EMAIL_INVALIDO":
                case "EMAIL_LARGO":
                case "EMAIL_IGUAL_AL_ACTUAL":
                case "EMAIL_EN_USO":
                    // Error de la direccion: se corrige en el primer paso.
                    paso = PASO_EMAIL;
                    mostrarPaso();
                    etEmailNuevo.setError(mensaje);
                    etEmailNuevo.requestFocus();
                    return;
                case "OTP_COOLDOWN":
                    if (esReenvio) {
                        // El servidor todavia no deja pedir otro: se espera de nuevo.
                        reenvioDisponibleEn = System.currentTimeMillis()
                                + SEGUNDOS_DE_ESPERA * 1000L;
                        retomarCuentaRegresiva();
                    }
                    break;
                default:
                    break;
            }
        }
        Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show();
    }

    // -----------------------------------------------------------------
    // Segundo paso: confirmar con el codigo
    // -----------------------------------------------------------------

    private void confirmar() {
        if (enCurso) return;

        String codigo = etCodigo.getText().toString().trim();
        if (codigo.length() != LARGO_CODIGO) {
            etCodigo.setError(getString(R.string.error_codigo_incompleto));
            etCodigo.requestFocus();
            return;
        }

        marcarEnCurso(true);
        llamadaConfirmar = usuarioApi.confirmarCambioEmail(new ConfirmarEmailRequest(codigo));
        llamadaConfirmar.enqueue(new Callback<PerfilResponse>() {
            @Override
            public void onResponse(@NonNull Call<PerfilResponse> call,
                                   @NonNull Response<PerfilResponse> response) {
                if (call.isCanceled() || !estaVivo()) return;
                marcarEnCurso(false);

                if (response.isSuccessful() && response.body() != null
                        && response.body().getUsuario() != null) {
                    Bundle resultado = new Bundle();
                    resultado.putString(RES_EMAIL, response.body().getUsuario().getEmail());
                    getParentFragmentManager().setFragmentResult(CLAVE_RESULTADO, resultado);
                    dismiss();
                    return;
                }
                mostrarErrorDeConfirmacion(response);
            }

            @Override
            public void onFailure(@NonNull Call<PerfilResponse> call, @NonNull Throwable t) {
                if (call.isCanceled() || !estaVivo()) return;
                marcarEnCurso(false);
                Toast.makeText(requireContext(), R.string.error_sin_conexion,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void mostrarErrorDeConfirmacion(Response<PerfilResponse> response) {
        ErrorResponse.Detalle error = ApiErrorParser.parse(response);
        String codigo = ApiErrorParser.codigo(error);
        String mensaje = ApiErrorParser.mensaje(error, getString(R.string.error_codigo_generico));

        if (codigo != null) {
            switch (codigo) {
                case "OTP_INVALIDO":
                case "CODIGO_REQUERIDO":
                    etCodigo.setError(mensaje);
                    etCodigo.requestFocus();
                    return;
                case "OTP_EXPIRADO":
                case "OTP_BLOQUEADO":
                case "OTP_INEXISTENTE":
                    // Insistir con este codigo no sirve: se habilita pedir uno nuevo ya.
                    etCodigo.setText("");
                    reenvioDisponibleEn = 0;
                    retomarCuentaRegresiva();
                    break;
                case "EMAIL_EN_USO":
                    // Alguien se registro con ese email mientras tanto.
                    paso = PASO_EMAIL;
                    mostrarPaso();
                    etEmailNuevo.setError(mensaje);
                    return;
                default:
                    break;
            }
        }
        Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show();
    }

    // -----------------------------------------------------------------
    // Cuenta regresiva del boton "Reenviar"
    // -----------------------------------------------------------------

    /**
     * Deja el boton al dia con reenvioDisponibleEn: bloqueado con la cuenta
     * regresiva si falta tiempo, habilitado si ya paso. Sirve tanto al pedir
     * como al volver de una rotacion.
     */
    private void retomarCuentaRegresiva() {
        cancelarCuentaRegresiva();
        AlertDialog dialogo = (AlertDialog) getDialog();
        if (dialogo == null) return;
        Button reenviar = dialogo.getButton(AlertDialog.BUTTON_NEUTRAL);

        long faltaMs = reenvioDisponibleEn - System.currentTimeMillis();
        if (faltaMs <= 0) {
            habilitarReenvio(reenviar);
            return;
        }

        reenviar.setEnabled(false);
        cuentaRegresiva = new CountDownTimer(faltaMs, 1000L) {
            @Override
            public void onTick(long restanteMs) {
                if (!estaVivo()) return;
                // +999 para que el ultimo tick muestre 1 y no 0.
                reenviar.setText(getString(R.string.otp_reenviar_en, (restanteMs + 999) / 1000));
            }

            @Override
            public void onFinish() {
                if (!estaVivo()) return;
                habilitarReenvio(reenviar);
            }
        }.start();
    }

    private void habilitarReenvio(Button reenviar) {
        reenviar.setText(R.string.otp_reenviar);
        reenviar.setEnabled(!enCurso);
    }

    private void cancelarCuentaRegresiva() {
        if (cuentaRegresiva != null) {
            cuentaRegresiva.cancel();
            cuentaRegresiva = null;
        }
    }

    // -----------------------------------------------------------------
    // Estado de la pantalla y ciclo de vida
    // -----------------------------------------------------------------

    /** Mientras hay una request en vuelo se bloquean los botones para no duplicarla. */
    private void marcarEnCurso(boolean valor) {
        enCurso = valor;
        progressBar.setVisibility(valor ? View.VISIBLE : View.GONE);
        AlertDialog dialogo = (AlertDialog) getDialog();
        if (dialogo == null) return;
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!valor);
        if (valor) {
            dialogo.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(false);
        } else if (paso == PASO_CODIGO) {
            retomarCuentaRegresiva();
        }
    }

    @Override
    public void onDestroyView() {
        // Sin esto el timer sigue vivo con el dialogo ya destruido.
        cancelarCuentaRegresiva();
        if (llamadaSolicitar != null) llamadaSolicitar.cancel();
        if (llamadaConfirmar != null) llamadaConfirmar.cancel();
        super.onDestroyView();
    }

    private boolean estaVivo() {
        return isAdded() && getDialog() != null;
    }
}
