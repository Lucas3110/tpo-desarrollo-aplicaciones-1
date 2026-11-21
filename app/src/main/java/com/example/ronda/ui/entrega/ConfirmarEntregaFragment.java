package com.example.ronda.ui.entrega;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.ronda.R;
import com.example.ronda.data.model.ConfirmarEntregaRequest;
import com.example.ronda.data.model.ConfirmarEntregaResponse;
import com.example.ronda.data.model.ErrorResponse;
import com.example.ronda.data.model.OperacionResponse;
import com.example.ronda.data.network.ApiErrorParser;
import com.example.ronda.data.network.OperacionApiService;
import com.example.ronda.data.repository.SessionRepository;
import com.example.ronda.ui.ofertas.FormatoOferta;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Punto 12: cierre de la operacion con QR.
 *
 * "El comprador escanea un codigo QR con la camara para confirmar que recibio
 * el articulo. [...] La app muestra un resultado visual claro: operacion
 * confirmada o codigo invalido, con un mensaje descriptivo."
 *
 * La pantalla es dueña de todo el flujo: pide el permiso de camara y abre el
 * lector (EscanerQr), manda lo leido a POST /operaciones/confirmar-entrega y
 * muestra el resultado. Se llega desde "Mis ofertas" o desde el detalle de la
 * publicacion y arranca abriendo la camara sola; si se cancela o falla, quedan
 * el boton para volver a intentar y la opcion de tipear el codigo impreso
 * debajo del QR (para celulares sin camara o cuando no se deja leer).
 *
 * El QR no lo genera la app: contiene el codigo que nacio en el backend al
 * aceptarse la oferta, y que el vendedor imprimio y entrego con el articulo.
 * Quien decide si es valido, si es mio y si la entrega seguia pendiente es el
 * backend; aca solo se traduce su respuesta a una pantalla.
 */
@AndroidEntryPoint
public class ConfirmarEntregaFragment extends Fragment {

    private static final String ESTADO_ESCANEO_LANZADO = "escaneoLanzado";
    private static final String ESTADO_CODIGO = "codigo";
    private static final String ESTADO_PANTALLA = "pantalla";
    private static final String ESTADO_TITULO = "titulo";
    private static final String ESTADO_MENSAJE = "mensaje";
    private static final String ESTADO_DETALLE = "detalle";

    @Inject
    OperacionApiService operacionApi;

    @Inject
    SessionRepository sesion;

    private EscanerQr escaner;
    private Call<ConfirmarEntregaResponse> llamada;

    /** Que se esta mostrando. Los textos se guardan aparte para sobrevivir a una rotacion. */
    private enum Pantalla { ESPERANDO, CONFIRMANDO, CONFIRMADA, RECHAZADA, SIN_CONEXION }

    private Pantalla pantalla = Pantalla.ESPERANDO;
    private String titulo;
    private String mensaje;
    private String detalle;
    /** El ultimo codigo que se intento confirmar: "Reintentar" lo reusa sin volver a escanear. */
    private String codigo;
    /** La camara se abre sola al entrar, pero una sola vez aunque la Activity se recree. */
    private boolean escaneoLanzado;

    private ImageView ivResultado;
    private ProgressBar progressBar;
    private TextView tvTitulo;
    private TextView tvMensaje;
    private TextView tvDetalle;
    private Button btnPrincipal;
    private Button btnSecundario;
    private Button btnManual;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            escaneoLanzado = savedInstanceState.getBoolean(ESTADO_ESCANEO_LANZADO);
            codigo = savedInstanceState.getString(ESTADO_CODIGO);
            pantalla = Pantalla.valueOf(savedInstanceState.getString(ESTADO_PANTALLA, Pantalla.ESPERANDO.name()));
            titulo = savedInstanceState.getString(ESTADO_TITULO);
            mensaje = savedInstanceState.getString(ESTADO_MENSAJE);
            detalle = savedInstanceState.getString(ESTADO_DETALLE);
            // Una confirmacion que quedo a medias al rotar se vuelve a pedir.
            if (pantalla == Pantalla.CONFIRMANDO) pantalla = Pantalla.SIN_CONEXION;
        }

        // Los launchers del permiso y del lector se registran aca, antes de que
        // el Fragment llegue a STARTED: la API de resultados lo exige.
        escaner = new EscanerQr(this, new EscanerQr.Listener() {
            @Override
            public void onCodigoLeido(@NonNull String contenido) {
                confirmar(contenido);
            }

            @Override
            public void onCancelado() {
                // Cerro la camara sin leer: queda la pantalla inicial, con el
                // boton para volver a abrirla o tipear el codigo.
                if (pantalla != Pantalla.CONFIRMADA) mostrar(Pantalla.ESPERANDO, null, null, null);
            }

            @Override
            public void onSinPermiso() {
                mostrar(Pantalla.RECHAZADA, getString(R.string.confirmar_entrega_sin_permiso_titulo),
                        getString(R.string.confirmar_entrega_sin_permiso), null);
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_confirmar_entrega, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ivResultado = view.findViewById(R.id.ivResultado);
        progressBar = view.findViewById(R.id.progressBar);
        tvTitulo = view.findViewById(R.id.tvTitulo);
        tvMensaje = view.findViewById(R.id.tvMensaje);
        tvDetalle = view.findViewById(R.id.tvDetalle);
        btnPrincipal = view.findViewById(R.id.btnPrincipal);
        btnSecundario = view.findViewById(R.id.btnSecundario);
        btnManual = view.findViewById(R.id.btnManual);
        btnManual.setOnClickListener(v -> pedirCodigoAMano());

        pintar();

        if (!escaneoLanzado) {
            escaneoLanzado = true;
            escanear();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ESTADO_ESCANEO_LANZADO, escaneoLanzado);
        outState.putString(ESTADO_CODIGO, codigo);
        outState.putString(ESTADO_PANTALLA, pantalla.name());
        outState.putString(ESTADO_TITULO, titulo);
        outState.putString(ESTADO_MENSAJE, mensaje);
        outState.putString(ESTADO_DETALLE, detalle);
    }

    // -----------------------------------------------------------------
    // Leer el codigo: camara o a mano
    // -----------------------------------------------------------------

    private void escanear() {
        if (!escaner.hayCamara()) {
            Toast.makeText(requireContext(), R.string.confirmar_entrega_sin_camara, Toast.LENGTH_LONG).show();
            pedirCodigoAMano();
            return;
        }
        escaner.escanear();
    }

    /** Para un celular sin camara o un QR que no se deja leer: el codigo esta impreso debajo. */
    private void pedirCodigoAMano() {
        EditText campo = new EditText(requireContext());
        campo.setHint(R.string.confirmar_entrega_manual_titulo);
        campo.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        if (codigo != null) campo.setText(codigo);
        int margen = (int) (24 * getResources().getDisplayMetrics().density);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.confirmar_entrega_manual_titulo)
                .setMessage(R.string.confirmar_entrega_manual_explicacion)
                .setView(campo, margen, 0, margen, 0)
                .setPositiveButton(R.string.confirmar_entrega_manual_ok,
                        (dialogo, cual) -> confirmar(campo.getText().toString()))
                .setNegativeButton(R.string.accion_cancelar, null)
                .show();
    }

    // -----------------------------------------------------------------
    // Confirmar contra el backend
    // -----------------------------------------------------------------

    private void confirmar(String leido) {
        codigo = CodigoEntrega.normalizar(leido);
        if (!CodigoEntrega.pareceDeRonda(codigo)) {
            // Un QR cualquiera (una URL, una red wifi) no es de Ronda: se dice
            // al instante, sin gastar un viaje al servidor.
            mostrar(Pantalla.RECHAZADA, getString(R.string.confirmar_entrega_invalido_titulo),
                    getString(R.string.confirmar_entrega_invalido_local), null);
            return;
        }
        enviar();
    }

    private void enviar() {
        mostrar(Pantalla.CONFIRMANDO, null, null, null);
        if (llamada != null) llamada.cancel();
        llamada = operacionApi.confirmarEntrega(new ConfirmarEntregaRequest(codigo));
        llamada.enqueue(new Callback<ConfirmarEntregaResponse>() {
            @Override
            public void onResponse(@NonNull Call<ConfirmarEntregaResponse> call,
                                   @NonNull Response<ConfirmarEntregaResponse> response) {
                if (call.isCanceled() || !estaVivo()) return;

                if (response.code() == 401) {
                    volverAlLogin();
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    ConfirmarEntregaResponse r = response.body();
                    mostrar(Pantalla.CONFIRMADA, getString(R.string.confirmar_entrega_ok_titulo),
                            r.getMensaje(), detalleDe(r.getOperacion()));
                    return;
                }

                // El backend ya separa los casos con un codigo estable; el
                // mensaje descriptivo viene de el. "Codigo invalido" es el
                // titulo que pide el enunciado; para los demas (lo escaneo
                // otra persona, ya estaba confirmada) se dice que no se pudo.
                ErrorResponse.Detalle error = ApiErrorParser.parse(response);
                String codigoError = ApiErrorParser.codigo(error);
                boolean invalido = "CODIGO_INVALIDO".equals(codigoError) || "CODIGO_REQUERIDO".equals(codigoError);
                mostrar(Pantalla.RECHAZADA,
                        getString(invalido ? R.string.confirmar_entrega_invalido_titulo
                                : R.string.confirmar_entrega_rechazada_titulo),
                        ApiErrorParser.mensaje(error, getString(R.string.confirmar_entrega_error_generico)),
                        null);
            }

            @Override
            public void onFailure(@NonNull Call<ConfirmarEntregaResponse> call, @NonNull Throwable t) {
                if (call.isCanceled() || !estaVivo()) return;
                mostrar(Pantalla.SIN_CONEXION, getString(R.string.confirmar_entrega_sin_conexion_titulo),
                        getString(R.string.error_sin_conexion), null);
            }
        });
    }

    /** Articulo, vendedor, monto y fecha, como en una fila del historial. */
    @Nullable
    private String detalleDe(@Nullable OperacionResponse operacion) {
        if (operacion == null) return null;
        String articulo = operacion.getArticulo() != null && operacion.getArticulo().getTitulo() != null
                ? operacion.getArticulo().getTitulo() : "";
        String vendedor = operacion.getContraparte() != null && operacion.getContraparte().getNombre() != null
                ? operacion.getContraparte().getNombre() : "";
        return getString(R.string.confirmar_entrega_ok_detalle, articulo, vendedor,
                FormatoOferta.precio(operacion.getMontoFinal()), FormatoOferta.fechaCorta(operacion.getFecha()));
    }

    // -----------------------------------------------------------------
    // Pintar
    // -----------------------------------------------------------------

    private void mostrar(Pantalla nueva, @Nullable String titulo, @Nullable String mensaje, @Nullable String detalle) {
        this.pantalla = nueva;
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.detalle = detalle;
        if (getView() != null) pintar();
    }

    private void pintar() {
        boolean confirmando = pantalla == Pantalla.CONFIRMANDO;
        progressBar.setVisibility(confirmando ? View.VISIBLE : View.GONE);
        ivResultado.setVisibility(confirmando ? View.GONE : View.VISIBLE);
        btnPrincipal.setVisibility(confirmando ? View.GONE : View.VISIBLE);
        btnSecundario.setVisibility(confirmando ? View.GONE : View.VISIBLE);
        btnManual.setVisibility(View.GONE);
        tvDetalle.setVisibility(detalle != null ? View.VISIBLE : View.GONE);
        tvDetalle.setText(detalle);

        switch (pantalla) {
            case ESPERANDO:
                ivResultado.setImageResource(R.drawable.ic_qr_entrega);
                tvTitulo.setText(R.string.confirmar_entrega_titulo);
                tvMensaje.setText(R.string.confirmar_entrega_indicacion);
                boton(btnPrincipal, R.string.confirmar_entrega_escanear, v -> escanear());
                boton(btnSecundario, R.string.confirmar_entrega_volver, v -> volver());
                btnManual.setVisibility(View.VISIBLE);
                break;
            case CONFIRMANDO:
                tvTitulo.setText(R.string.confirmar_entrega_confirmando);
                tvMensaje.setText(codigo);
                break;
            case CONFIRMADA:
                ivResultado.setImageResource(R.drawable.ic_entrega_confirmada);
                tvTitulo.setText(titulo);
                tvMensaje.setText(mensaje);
                boton(btnPrincipal, R.string.confirmar_entrega_ver_historial, v -> Navigation
                        .findNavController(requireView()).navigate(R.id.action_confirmarEntrega_to_historial));
                boton(btnSecundario, R.string.confirmar_entrega_listo, v -> volver());
                break;
            case RECHAZADA:
                ivResultado.setImageResource(R.drawable.ic_codigo_invalido);
                tvTitulo.setText(titulo);
                tvMensaje.setText(mensaje);
                boton(btnPrincipal, R.string.confirmar_entrega_reintentar_escaneo, v -> escanear());
                boton(btnSecundario, R.string.confirmar_entrega_volver, v -> volver());
                btnManual.setVisibility(View.VISIBLE);
                break;
            case SIN_CONEXION:
                ivResultado.setImageResource(R.drawable.ic_qr_entrega);
                tvTitulo.setText(titulo);
                tvMensaje.setText(mensaje);
                boton(btnPrincipal, R.string.confirmar_entrega_reintentar, v -> enviar());
                boton(btnSecundario, R.string.confirmar_entrega_volver, v -> volver());
                break;
        }
    }

    private static void boton(Button boton, int texto, View.OnClickListener alTocar) {
        boton.setText(texto);
        boton.setOnClickListener(alTocar);
    }

    // -----------------------------------------------------------------
    // Navegacion
    // -----------------------------------------------------------------

    private void volver() {
        Navigation.findNavController(requireView()).popBackStack();
    }

    private void volverAlLogin() {
        sesion.cerrarSesion();
        Toast.makeText(requireContext(), R.string.home_sesion_vencida, Toast.LENGTH_LONG).show();
        Navigation.findNavController(requireView()).navigate(R.id.action_confirmarEntrega_to_auth);
    }

    private boolean estaVivo() {
        return isAdded() && getView() != null;
    }

    @Override
    public void onDestroyView() {
        if (llamada != null) llamada.cancel();
        super.onDestroyView();
    }
}
