package com.example.ronda.ui.historial;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.ronda.R;

/**
 * Dialogo para calificar a la otra parte de una operacion (Punto 9):
 * estrellas de 1 a 5 y un comentario breve opcional.
 *
 * Es un DialogFragment para sobrevivir a la rotacion. Devuelve lo cargado con
 * la API de resultados de Fragments: quien lo muestra escucha CLAVE_RESULTADO
 * y recibe operacionId, estrellas y comentario (null si quedo vacio); el
 * pedido a la API lo hace la pantalla.
 */
public class CalificarDialogFragment extends DialogFragment {

    public static final String CLAVE_RESULTADO = "calificar";
    public static final String RES_OPERACION_ID = "operacionId";
    public static final String RES_ESTRELLAS = "estrellas";
    public static final String RES_COMENTARIO = "comentario";

    private static final String ARG_OPERACION_ID = "operacionId";
    private static final String ARG_ARTICULO = "articulo";
    private static final String ARG_CONTRAPARTE = "contraparte";
    /** Mismo tope que COMENTARIO_MAX del backend. */
    private static final int MAX_COMENTARIO = 500;

    private RatingBar rbEstrellas;
    private EditText etComentario;
    private TextView tvContador;

    /**
     * @param articulo    titulo de la publicacion, o null si se borro
     * @param contraparte nombre de la persona a calificar
     */
    public static CalificarDialogFragment newInstance(int operacionId, @Nullable String articulo,
                                                      String contraparte) {
        Bundle args = new Bundle();
        args.putInt(ARG_OPERACION_ID, operacionId);
        args.putString(ARG_ARTICULO, articulo);
        args.putString(ARG_CONTRAPARTE, contraparte);
        CalificarDialogFragment dialogo = new CalificarDialogFragment();
        dialogo.setArguments(args);
        return dialogo;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle args = requireArguments();
        String articulo = args.getString(ARG_ARTICULO);
        String contraparte = args.getString(ARG_CONTRAPARTE, "");

        View vista = getLayoutInflater().inflate(R.layout.dialog_calificar, null);
        TextView tvACalificar = vista.findViewById(R.id.tvACalificar);
        rbEstrellas = vista.findViewById(R.id.rbEstrellas);
        etComentario = vista.findViewById(R.id.etComentario);
        tvContador = vista.findViewById(R.id.tvContador);

        tvACalificar.setText(articulo != null && !articulo.isEmpty()
                ? getString(R.string.calificar_a, contraparte, articulo)
                : getString(R.string.calificar_a_sin_articulo, contraparte));

        actualizarContador();
        etComentario.addTextChangedListener(new TextWatcher() {
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

        // El boton positivo se conecta en onStart: si se pasa aca, el dialogo
        // se cierra solo al tocarlo aunque falten las estrellas.
        return new AlertDialog.Builder(requireContext())
                .setTitle(R.string.calificar_titulo)
                .setView(vista)
                .setPositiveButton(R.string.calificar_enviar, null)
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
        tvContador.setText(getString(R.string.oferta_mensaje_contador,
                etComentario.getText().length(), MAX_COMENTARIO));
    }

    /** Mismas reglas que el backend: 1 a 5 estrellas y comentario de hasta 500 caracteres. */
    private void enviar() {
        int estrellas = Math.round(rbEstrellas.getRating());
        if (estrellas < 1) {
            Toast.makeText(requireContext(), R.string.calificar_error_estrellas,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        String comentario = etComentario.getText().toString().trim();
        if (comentario.length() > MAX_COMENTARIO) {
            etComentario.setError(getString(R.string.calificar_error_comentario_largo, MAX_COMENTARIO));
            etComentario.requestFocus();
            return;
        }

        Bundle resultado = new Bundle();
        resultado.putInt(RES_OPERACION_ID, requireArguments().getInt(ARG_OPERACION_ID));
        resultado.putInt(RES_ESTRELLAS, estrellas);
        resultado.putString(RES_COMENTARIO, comentario.isEmpty() ? null : comentario);
        getParentFragmentManager().setFragmentResult(CLAVE_RESULTADO, resultado);
        dismiss();
    }
}
