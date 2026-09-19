package com.example.ronda.ui.ofertas;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.ronda.R;
import com.example.ronda.data.model.OfertaResponse;

/**
 * Dialogo para responder una oferta pendiente con otro precio (Punto 7).
 *
 * Es un DialogFragment y no un AlertDialog suelto para que sobreviva a la
 * rotacion. Devuelve lo cargado con la API de resultados de Fragments:
 * quien lo muestra escucha CLAVE_RESULTADO en su FragmentManager y recibe
 * un Bundle con ofertaId, monto y mensaje. Asi lo comparten la seccion
 * "Mis ofertas" y el historial del detalle sin acoplarse entre si.
 */
public class ContraofertaDialogFragment extends DialogFragment {

    public static final String CLAVE_RESULTADO = "contraoferta";
    public static final String RES_OFERTA_ID = "ofertaId";
    public static final String RES_MONTO = "monto";
    public static final String RES_MENSAJE = "mensaje";

    private static final String ARG_OFERTA_ID = "ofertaId";
    private static final String ARG_MONTO_ORIGINAL = "montoOriginal";
    private static final String ARG_PRECIO_PUBLICADO = "precioPublicado";
    private static final String ARG_TITULO = "titulo";
    private static final int MAX_MENSAJE = 200;

    private EditText etMonto;
    private EditText etMensaje;
    private TextView tvContador;

    /**
     * En "Mis ofertas" la oferta trae su publicacion; en el historial del
     * detalle no, asi que el precio y el titulo se pasan aparte.
     */
    public static ContraofertaDialogFragment newInstance(OfertaResponse oferta) {
        OfertaResponse.Publicacion pub = oferta.getPublicacion();
        return newInstance(oferta, pub != null ? pub.getPrecio() : 0, pub != null ? pub.getTitulo() : "");
    }

    public static ContraofertaDialogFragment newInstance(OfertaResponse oferta,
                                                         double precioPublicado, String titulo) {
        Bundle args = new Bundle();
        args.putInt(ARG_OFERTA_ID, oferta.getId());
        args.putDouble(ARG_MONTO_ORIGINAL, oferta.getMonto());
        args.putDouble(ARG_PRECIO_PUBLICADO, precioPublicado);
        args.putString(ARG_TITULO, titulo);
        ContraofertaDialogFragment dialogo = new ContraofertaDialogFragment();
        dialogo.setArguments(args);
        return dialogo;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle args = requireArguments();
        double montoOriginal = args.getDouble(ARG_MONTO_ORIGINAL);
        double precioPublicado = args.getDouble(ARG_PRECIO_PUBLICADO);

        View vista = getLayoutInflater().inflate(R.layout.dialog_contraoferta, null);
        TextView tvOfertaActual = vista.findViewById(R.id.tvOfertaActual);
        TextView tvPrecioPublicado = vista.findViewById(R.id.tvPrecioPublicado);
        etMonto = vista.findViewById(R.id.etMonto);
        etMensaje = vista.findViewById(R.id.etMensaje);
        tvContador = vista.findViewById(R.id.tvContador);

        tvOfertaActual.setText(getString(R.string.contraoferta_oferta_actual,
                FormatoOferta.precio(montoOriginal), args.getString(ARG_TITULO)));
        if (precioPublicado > 0) {
            tvPrecioPublicado.setText(getString(R.string.oferta_precio_publicado,
                    FormatoOferta.precio(precioPublicado)));
            tvPrecioPublicado.setVisibility(View.VISIBLE);
        }
        actualizarContador();
        etMensaje.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                actualizarContador();
            }
        });

        // Los botones se conectan en onStart: si se pasan aca, el dialogo se
        // cierra solo al tocarlos aunque el monto no sea valido.
        return new AlertDialog.Builder(requireContext())
                .setTitle(R.string.contraoferta_titulo)
                .setView(vista)
                .setPositiveButton(R.string.contraoferta_enviar, null)
                .setNegativeButton(R.string.accion_cancelar, null)
                .create();
    }

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog dialogo = (AlertDialog) getDialog();
        if (dialogo == null) return;
        Button enviar = dialogo.getButton(AlertDialog.BUTTON_POSITIVE);
        enviar.setOnClickListener(v -> enviar());
    }

    private void actualizarContador() {
        tvContador.setText(getString(R.string.oferta_mensaje_contador, etMensaje.getText().length(), MAX_MENSAJE));
    }

    /**
     * Mismas reglas que el backend, para frenar antes de llamar: mayor a 0,
     * no mayor al precio publicado y distinto del monto que te ofrecieron
     * (si es igual, corresponde aceptar).
     */
    private void enviar() {
        Bundle args = requireArguments();
        double montoOriginal = args.getDouble(ARG_MONTO_ORIGINAL);
        double precioPublicado = args.getDouble(ARG_PRECIO_PUBLICADO);

        String texto = etMonto.getText().toString().trim();
        if (texto.isEmpty()) {
            etMonto.setError(getString(R.string.oferta_error_monto_vacio));
            etMonto.requestFocus();
            return;
        }
        double monto;
        try {
            monto = Double.parseDouble(texto.replace(',', '.'));
        } catch (NumberFormatException e) {
            monto = 0;
        }
        if (monto <= 0) {
            etMonto.setError(getString(R.string.oferta_error_monto_invalido));
            etMonto.requestFocus();
            return;
        }
        if (precioPublicado > 0 && monto > precioPublicado) {
            etMonto.setError(getString(R.string.oferta_error_mayor_al_precio, FormatoOferta.precio(precioPublicado)));
            etMonto.requestFocus();
            return;
        }
        if (monto == montoOriginal) {
            etMonto.setError(getString(R.string.contraoferta_error_mismo_monto));
            etMonto.requestFocus();
            return;
        }
        String mensaje = etMensaje.getText().toString().trim();
        if (mensaje.length() > MAX_MENSAJE) {
            etMensaje.setError(getString(R.string.oferta_error_mensaje_largo, MAX_MENSAJE));
            etMensaje.requestFocus();
            return;
        }

        Bundle resultado = new Bundle();
        resultado.putInt(RES_OFERTA_ID, args.getInt(ARG_OFERTA_ID));
        resultado.putDouble(RES_MONTO, monto);
        resultado.putString(RES_MENSAJE, mensaje);
        getParentFragmentManager().setFragmentResult(CLAVE_RESULTADO, resultado);
        dismiss();
    }
}
