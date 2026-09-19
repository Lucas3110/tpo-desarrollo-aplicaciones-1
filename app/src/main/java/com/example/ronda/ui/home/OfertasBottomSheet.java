package com.example.ronda.ui.home;

import android.os.Bundle;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ronda.R;
import com.example.ronda.data.model.ErrorResponse;
import com.example.ronda.data.model.EstadoOfertaRequest;
import com.example.ronda.data.model.ListaOfertasResponse;
import com.example.ronda.data.model.OfertaResponse;
import com.example.ronda.data.model.OfertaUnicaResponse;
import com.example.ronda.data.model.OfertarRequest;
import com.example.ronda.data.network.ApiErrorParser;
import com.example.ronda.data.network.OfertaApiService;
import com.example.ronda.ui.ofertas.ContraofertaDialogFragment;
import com.example.ronda.ui.ofertas.FormatoOferta;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Ofertas de una publicacion, desde su detalle (Puntos 4 y 7).
 *
 * Arriba el historial (el vendedor ve todas; un interesado, las suyas y las
 * contraofertas que recibio) y abajo el formulario para proponer un precio
 * distinto con un mensaje breve opcional. Usa OfertaApiService, que no
 * recibe el token: lo agrega el interceptor de OkHttp (clase 5).
 */
@AndroidEntryPoint
public class OfertasBottomSheet extends BottomSheetDialogFragment {
    public static final String RESULTADO_CERRADO = "ofertas_cerradas";

    private static final String ARG_PUBLICACION_ID = "publicacionId";
    private static final String ARG_ES_VENDEDOR = "esVendedor";
    private static final String ARG_PUEDE_OFERTAR = "puedeOfertar";
    private static final String ARG_PRECIO_PUBLICADO = "precioPublicado";

    /** Tope del backend para el mensaje (responde MENSAJE_LARGO si se pasa). */
    static final int MAX_MENSAJE = 200;

    private int publicacionId;
    private boolean esVendedor;
    private boolean puedeOfertar;
    private double precioPublicado;

    private RecyclerView rvOfertas;
    private ProgressBar pbLoading;
    private TextView tvSinOfertas;
    private LinearLayout llHacerOferta;
    private TextView tvPrecioPublicado;
    private EditText etMontoOferta;
    private EditText etMensajeOferta;
    private TextView tvContadorMensaje;
    private Button btnEnviarOferta;

    private OfertasAdapter adapter;

    @Inject
    OfertaApiService ofertaApi;

    private Call<ListaOfertasResponse> llamadaLista;
    private Call<OfertaUnicaResponse> llamadaOfertar;
    private Call<OfertaUnicaResponse> llamadaResponder;
    private Call<OfertaUnicaResponse> llamadaContraofertar;

    /**
     * Los Fragments (y este dialogo lo es) los recrea el sistema con el
     * constructor vacio, por ejemplo al rotar la pantalla: si recibieran los
     * datos por constructor, al volver quedarian en cero o directamente no
     * se podrian instanciar. Por eso viajan en el Bundle de argumentos.
     *
     * @param precioPublicado para mostrarlo y frenar antes de mandar una
     *                        oferta que lo supere (el backend igual lo valida).
     */
    public static OfertasBottomSheet newInstance(int publicacionId, boolean esVendedor,
                                                 boolean puedeOfertar, double precioPublicado) {
        Bundle args = new Bundle();
        args.putInt(ARG_PUBLICACION_ID, publicacionId);
        args.putBoolean(ARG_ES_VENDEDOR, esVendedor);
        args.putBoolean(ARG_PUEDE_OFERTAR, puedeOfertar);
        args.putDouble(ARG_PRECIO_PUBLICADO, precioPublicado);
        OfertasBottomSheet dialogo = new OfertasBottomSheet();
        dialogo.setArguments(args);
        return dialogo;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = requireArguments();
        publicacionId = args.getInt(ARG_PUBLICACION_ID);
        esVendedor = args.getBoolean(ARG_ES_VENDEDOR);
        puedeOfertar = args.getBoolean(ARG_PUEDE_OFERTAR);
        precioPublicado = args.getDouble(ARG_PRECIO_PUBLICADO);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_ofertas, container, false);

        rvOfertas = view.findViewById(R.id.rvOfertas);
        pbLoading = view.findViewById(R.id.pbLoadingOfertas);
        tvSinOfertas = view.findViewById(R.id.tvSinOfertas);
        llHacerOferta = view.findViewById(R.id.llHacerOferta);
        tvPrecioPublicado = view.findViewById(R.id.tvPrecioPublicado);
        etMontoOferta = view.findViewById(R.id.etMontoOferta);
        etMensajeOferta = view.findViewById(R.id.etMensajeOferta);
        tvContadorMensaje = view.findViewById(R.id.tvContadorMensaje);
        btnEnviarOferta = view.findViewById(R.id.btnEnviarOferta);

        rvOfertas.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new OfertasAdapter();
        rvOfertas.setAdapter(adapter);

        if (puedeOfertar) {
            configurarFormulario();
        }

        escucharContraoferta();
        cargarOfertas();
        return view;
    }

    /** El dialogo de contraoferta devuelve lo cargado por la API de resultados de Fragments. */
    private void escucharContraoferta() {
        getChildFragmentManager().setFragmentResultListener(ContraofertaDialogFragment.CLAVE_RESULTADO,
                this, (clave, resultado) -> contraofertar(
                        resultado.getInt(ContraofertaDialogFragment.RES_OFERTA_ID),
                        resultado.getDouble(ContraofertaDialogFragment.RES_MONTO),
                        resultado.getString(ContraofertaDialogFragment.RES_MENSAJE)));
    }

    private void configurarFormulario() {
        llHacerOferta.setVisibility(View.VISIBLE);
        if (precioPublicado > 0) {
            tvPrecioPublicado.setText(getString(R.string.oferta_precio_publicado,
                    FormatoOferta.precio(precioPublicado)));
            tvPrecioPublicado.setVisibility(View.VISIBLE);
        }
        actualizarContador();
        etMensajeOferta.addTextChangedListener(new TextWatcher() {
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
        btnEnviarOferta.setOnClickListener(v -> hacerOferta());
    }

    private void actualizarContador() {
        tvContadorMensaje.setText(getString(R.string.oferta_mensaje_contador,
                etMensajeOferta.getText().length(), MAX_MENSAJE));
    }

    // -----------------------------------------------------------------
    // Historial
    // -----------------------------------------------------------------

    private void cargarOfertas() {
        pbLoading.setVisibility(View.VISIBLE);
        tvSinOfertas.setVisibility(View.GONE);

        llamadaLista = ofertaApi.deLaPublicacion(publicacionId);
        llamadaLista.enqueue(new Callback<ListaOfertasResponse>() {
            @Override
            public void onResponse(@NonNull Call<ListaOfertasResponse> call,
                                   @NonNull Response<ListaOfertasResponse> response) {
                if (call.isCanceled() || !estaVivo()) return;
                pbLoading.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    ListaOfertasResponse lista = response.body();
                    boolean vacia = lista.getOfertas() == null || lista.getOfertas().isEmpty();
                    tvSinOfertas.setVisibility(vacia ? View.VISIBLE : View.GONE);
                    adapter.setOfertas(lista.getOfertas(), lista.isEsVendedor(),
                            OfertasBottomSheet.this::responderOferta,
                            OfertasBottomSheet.this::abrirContraoferta);
                } else {
                    ErrorResponse.Detalle error = ApiErrorParser.parse(response);
                    manejarError(response.code(), error, getString(R.string.ofertas_error_carga));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ListaOfertasResponse> call, @NonNull Throwable t) {
                if (call.isCanceled() || !estaVivo()) return;
                pbLoading.setVisibility(View.GONE);
                avisar(getString(R.string.error_sin_conexion));
            }
        });
    }

    // -----------------------------------------------------------------
    // Ofertar
    // -----------------------------------------------------------------

    private void hacerOferta() {
        Double monto = leerMonto();
        if (monto == null) return;

        String mensaje = etMensajeOferta.getText().toString().trim();
        if (mensaje.length() > MAX_MENSAJE) {
            etMensajeOferta.setError(getString(R.string.oferta_error_mensaje_largo, MAX_MENSAJE));
            etMensajeOferta.requestFocus();
            return;
        }

        btnEnviarOferta.setEnabled(false);
        llamadaOfertar = ofertaApi.ofertar(publicacionId, new OfertarRequest(monto, mensaje));
        llamadaOfertar.enqueue(new Callback<OfertaUnicaResponse>() {
            @Override
            public void onResponse(@NonNull Call<OfertaUnicaResponse> call,
                                   @NonNull Response<OfertaUnicaResponse> response) {
                if (call.isCanceled() || !estaVivo()) return;
                btnEnviarOferta.setEnabled(true);

                if (response.isSuccessful()) {
                    etMontoOferta.setText("");
                    etMensajeOferta.setText("");
                    avisar(getString(R.string.oferta_enviada));
                    cargarOfertas();
                } else {
                    ErrorResponse.Detalle error = ApiErrorParser.parse(response);
                    manejarError(response.code(), error, getString(R.string.oferta_error_enviar));
                }
            }

            @Override
            public void onFailure(@NonNull Call<OfertaUnicaResponse> call, @NonNull Throwable t) {
                if (call.isCanceled() || !estaVivo()) return;
                btnEnviarOferta.setEnabled(true);
                avisar(getString(R.string.error_sin_conexion));
            }
        });
    }

    /**
     * Valida el monto antes de molestar al servidor: obligatorio, mayor a 0
     * y no mayor al precio publicado. El error se muestra en el campo.
     */
    @Nullable
    private Double leerMonto() {
        String texto = etMontoOferta.getText().toString().trim();
        if (texto.isEmpty()) {
            etMontoOferta.setError(getString(R.string.oferta_error_monto_vacio));
            etMontoOferta.requestFocus();
            return null;
        }
        double monto;
        try {
            // El teclado numerico puede poner coma como separador decimal.
            monto = Double.parseDouble(texto.replace(',', '.'));
        } catch (NumberFormatException e) {
            monto = 0;
        }
        if (monto <= 0) {
            etMontoOferta.setError(getString(R.string.oferta_error_monto_invalido));
            etMontoOferta.requestFocus();
            return null;
        }
        if (precioPublicado > 0 && monto > precioPublicado) {
            etMontoOferta.setError(getString(R.string.oferta_error_mayor_al_precio,
                    FormatoOferta.precio(precioPublicado)));
            etMontoOferta.requestFocus();
            return null;
        }
        return monto;
    }

    // -----------------------------------------------------------------
    // Aceptar / rechazar
    // -----------------------------------------------------------------

    private void responderOferta(int ofertaId, String estado) {
        llamadaResponder = ofertaApi.responder(ofertaId, new EstadoOfertaRequest(estado));
        llamadaResponder.enqueue(new Callback<OfertaUnicaResponse>() {
            @Override
            public void onResponse(@NonNull Call<OfertaUnicaResponse> call,
                                   @NonNull Response<OfertaUnicaResponse> response) {
                if (call.isCanceled() || !estaVivo()) return;

                if (response.isSuccessful()) {
                    avisar(getString(OfertaResponse.ACEPTADA.equals(estado)
                            ? R.string.oferta_aceptada_ok : R.string.oferta_rechazada_ok));
                } else {
                    ErrorResponse.Detalle error = ApiErrorParser.parse(response);
                    manejarError(response.code(), error, getString(R.string.oferta_error_responder));
                }
                // Con exito o con error (vencida, ya respondida) el estado cambio: se refresca.
                cargarOfertas();
            }

            @Override
            public void onFailure(@NonNull Call<OfertaUnicaResponse> call, @NonNull Throwable t) {
                if (call.isCanceled() || !estaVivo()) return;
                avisar(getString(R.string.error_sin_conexion));
            }
        });
    }

    // -----------------------------------------------------------------
    // Contraofertar (solo el vendedor, sobre una oferta del comprador)
    // -----------------------------------------------------------------

    private void abrirContraoferta(OfertaResponse oferta) {
        ContraofertaDialogFragment.newInstance(oferta, precioPublicado, tituloPublicacion())
                .show(getChildFragmentManager(), ContraofertaDialogFragment.CLAVE_RESULTADO);
    }

    private String tituloPublicacion() {
        return getString(R.string.ofertas_titulo);
    }

    private void contraofertar(int ofertaId, double monto, String mensaje) {
        llamadaContraofertar = ofertaApi.contraofertar(ofertaId, new OfertarRequest(monto, mensaje));
        llamadaContraofertar.enqueue(new Callback<OfertaUnicaResponse>() {
            @Override
            public void onResponse(@NonNull Call<OfertaUnicaResponse> call,
                                   @NonNull Response<OfertaUnicaResponse> response) {
                if (call.isCanceled() || !estaVivo()) return;

                if (response.isSuccessful()) {
                    avisar(getString(R.string.contraoferta_enviada));
                } else {
                    ErrorResponse.Detalle error = ApiErrorParser.parse(response);
                    manejarError(response.code(), error, getString(R.string.contraoferta_error_enviar));
                }
                cargarOfertas();
            }

            @Override
            public void onFailure(@NonNull Call<OfertaUnicaResponse> call, @NonNull Throwable t) {
                if (call.isCanceled() || !estaVivo()) return;
                avisar(getString(R.string.error_sin_conexion));
            }
        });
    }

    // -----------------------------------------------------------------
    // Errores y ciclo de vida
    // -----------------------------------------------------------------

    /**
     * Errores que SI respondio el backend. Se decide por el codigo estable:
     * los de validacion van al campo que corresponde, el resto se muestra
     * tal cual lo mando el servidor.
     */
    private void manejarError(int httpCode, ErrorResponse.Detalle error, String porDefecto) {
        String codigo = ApiErrorParser.codigo(error);
        String mensaje = ApiErrorParser.mensaje(error, porDefecto);
        if (httpCode == 401) {
            avisar(getString(R.string.home_sesion_vencida));
            dismiss();
            return;
        }
        if ("MONTO_INVALIDO".equals(codigo) || "OFERTA_MAYOR_AL_PRECIO".equals(codigo)) {
            etMontoOferta.setError(mensaje);
            etMontoOferta.requestFocus();
            return;
        }
        if ("MENSAJE_LARGO".equals(codigo)) {
            etMensajeOferta.setError(mensaje);
            etMensajeOferta.requestFocus();
            return;
        }
        avisar(mensaje);
    }

    private void avisar(String mensaje) {
        Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        cancelar(llamadaLista);
        cancelar(llamadaOfertar);
        cancelar(llamadaResponder);
        cancelar(llamadaContraofertar);
        super.onDestroyView();
    }

    private void cancelar(@Nullable Call<?> call) {
        if (call != null) call.cancel();
    }

    private boolean estaVivo() {
        return isAdded() && getView() != null;
    }

    @Override public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (isAdded()) {
            getParentFragmentManager().setFragmentResult(RESULTADO_CERRADO, new Bundle());
        }
    }
}
