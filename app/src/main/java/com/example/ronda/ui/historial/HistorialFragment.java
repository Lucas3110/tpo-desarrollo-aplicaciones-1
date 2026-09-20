package com.example.ronda.ui.historial;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ronda.R;
import com.example.ronda.data.model.ErrorResponse;
import com.example.ronda.data.model.HistorialResponse;
import com.example.ronda.data.model.OperacionResponse;
import com.example.ronda.data.network.ApiErrorParser;
import com.example.ronda.data.network.OperacionApiService;
import com.example.ronda.data.repository.SessionRepository;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Historial de operaciones (Punto 9): las compras y ventas concretadas, con
 * fecha, articulo, monto final y contraparte.
 *
 * Los filtros se resuelven en el backend: GET /operaciones acepta tipo
 * (COMPRA o VENTA) y un rango desde/hasta como AAAA-MM-DD, asi que cada cambio
 * de filtro vuelve a pedir la lista. Se pide tambien al entrar a la pantalla,
 * para que lo recien concretado aparezca.
 *
 * El selector de fechas de Material trabaja en milisegundos UTC: para armar
 * el AAAA-MM-DD se formatea en UTC. Con la zona del celular, en Argentina
 * (UTC-3) el rango se correria un dia.
 */
@AndroidEntryPoint
public class HistorialFragment extends Fragment {

    private static final int TIPO_TODAS = 0;
    private static final int TIPO_COMPRAS = 1;
    private static final int TIPO_VENTAS = 2;

    private static final String ESTADO_TIPO = "tipo";
    private static final String ESTADO_DESDE = "desde";
    private static final String ESTADO_HASTA = "hasta";
    private static final long SIN_FECHA = Long.MIN_VALUE;

    @Inject
    OperacionApiService operacionApi;

    @Inject
    SessionRepository sesion;

    private Spinner spTipo;
    private Button btnFechas;
    private Button btnLimpiar;
    private RecyclerView rvHistorial;
    private ProgressBar progressBar;
    private View grupoVacio;
    private View grupoError;
    private TextView tvVacio;
    private TextView tvError;

    private HistorialAdapter adapter;
    private int tipo = TIPO_TODAS;
    /** Rango elegido, en milisegundos UTC (null = sin limite). */
    @Nullable
    private Long desdeMs;
    @Nullable
    private Long hastaMs;
    private Call<HistorialResponse> llamada;

    private enum Estado { CARGANDO, LISTA, VACIO, ERROR }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            tipo = savedInstanceState.getInt(ESTADO_TIPO, TIPO_TODAS);
            long desde = savedInstanceState.getLong(ESTADO_DESDE, SIN_FECHA);
            long hasta = savedInstanceState.getLong(ESTADO_HASTA, SIN_FECHA);
            desdeMs = desde == SIN_FECHA ? null : desde;
            hastaMs = hasta == SIN_FECHA ? null : hasta;
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ESTADO_TIPO, tipo);
        outState.putLong(ESTADO_DESDE, desdeMs != null ? desdeMs : SIN_FECHA);
        outState.putLong(ESTADO_HASTA, hastaMs != null ? hastaMs : SIN_FECHA);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_historial, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        spTipo = view.findViewById(R.id.spTipo);
        btnFechas = view.findViewById(R.id.btnFechas);
        btnLimpiar = view.findViewById(R.id.btnLimpiar);
        rvHistorial = view.findViewById(R.id.rvHistorial);
        progressBar = view.findViewById(R.id.progressBar);
        grupoVacio = view.findViewById(R.id.grupoVacio);
        grupoError = view.findViewById(R.id.grupoError);
        tvVacio = view.findViewById(R.id.tvVacio);
        tvError = view.findViewById(R.id.tvError);
        Button btnActualizar = view.findViewById(R.id.btnActualizar);
        Button btnReintentar = view.findViewById(R.id.btnReintentar);

        adapter = new HistorialAdapter(new HistorialAdapter.Listener() {
            @Override
            public void onArticulo(OperacionResponse operacion) {
                abrirArticulo(operacion);
            }

            @Override
            public void onContraparte(OperacionResponse operacion) {
                abrirPerfil(operacion);
            }
        });
        rvHistorial.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvHistorial.setAdapter(adapter);

        configurarFiltros();
        btnActualizar.setOnClickListener(v -> cargar());
        btnReintentar.setOnClickListener(v -> cargar());

        cargar();
    }

    // -----------------------------------------------------------------
    // Filtros
    // -----------------------------------------------------------------

    private void configurarFiltros() {
        ArrayAdapter<String> opciones = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, new String[]{
                getString(R.string.historial_tipo_todas),
                getString(R.string.historial_tipo_compras),
                getString(R.string.historial_tipo_ventas)});
        opciones.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTipo.setAdapter(opciones);
        // Sin animar y antes del listener, asi restaurar el filtro no dispara otra carga.
        spTipo.setSelection(tipo, false);
        spTipo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View vista, int posicion, long id) {
                if (posicion == tipo) return;
                tipo = posicion;
                actualizarFiltros();
                cargar();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        btnFechas.setOnClickListener(v -> elegirFechas());
        btnLimpiar.setOnClickListener(v -> {
            tipo = TIPO_TODAS;
            desdeMs = null;
            hastaMs = null;
            spTipo.setSelection(TIPO_TODAS, false);
            actualizarFiltros();
            cargar();
        });
        actualizarFiltros();
    }

    private void elegirFechas() {
        MaterialDatePicker.Builder<Pair<Long, Long>> constructor =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText(R.string.historial_elegir_fechas)
                        // No tiene sentido buscar en el futuro.
                        .setCalendarConstraints(new CalendarConstraints.Builder()
                                .setValidator(DateValidatorPointBackward.now())
                                .build());
        if (desdeMs != null && hastaMs != null) {
            constructor.setSelection(new Pair<>(desdeMs, hastaMs));
        }
        MaterialDatePicker<Pair<Long, Long>> selector = constructor.build();
        selector.addOnPositiveButtonClickListener(seleccion -> {
            desdeMs = seleccion.first;
            hastaMs = seleccion.second;
            actualizarFiltros();
            cargar();
        });
        selector.show(getChildFragmentManager(), "fechas");
    }

    /** El boton de fechas muestra el rango elegido y "Limpiar" aparece solo si hay algo que limpiar. */
    private void actualizarFiltros() {
        btnFechas.setText(desdeMs != null && hastaMs != null
                ? getString(R.string.historial_fechas_rango, legible(desdeMs), legible(hastaMs))
                : getString(R.string.historial_fechas));
        btnLimpiar.setVisibility(hayFiltros() ? View.VISIBLE : View.GONE);
    }

    private boolean hayFiltros() {
        return tipo != TIPO_TODAS || desdeMs != null || hastaMs != null;
    }

    @Nullable
    private String tipoParametro() {
        if (tipo == TIPO_COMPRAS) return OperacionResponse.COMPRA;
        if (tipo == TIPO_VENTAS) return OperacionResponse.VENTA;
        return null;
    }

    /** AAAA-MM-DD, como espera el backend. UTC: es la zona en que trabaja el selector. */
    private static String aIso(long utcMs) {
        SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        formato.setTimeZone(TimeZone.getTimeZone("UTC"));
        return formato.format(new Date(utcMs));
    }

    /** dd/MM/aaaa para mostrar en el boton. */
    private static String legible(long utcMs) {
        SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
        formato.setTimeZone(TimeZone.getTimeZone("UTC"));
        return formato.format(new Date(utcMs));
    }

    // -----------------------------------------------------------------
    // Cargar
    // -----------------------------------------------------------------

    private void cargar() {
        if (llamada != null) llamada.cancel();
        mostrarEstado(Estado.CARGANDO);

        llamada = operacionApi.historial(tipoParametro(),
                desdeMs != null ? aIso(desdeMs) : null,
                hastaMs != null ? aIso(hastaMs) : null);
        llamada.enqueue(new Callback<HistorialResponse>() {
            @Override
            public void onResponse(@NonNull Call<HistorialResponse> call,
                                   @NonNull Response<HistorialResponse> response) {
                if (call.isCanceled() || !estaVivo()) return;

                if (response.code() == 401) {
                    volverAlLogin();
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    // FECHA_INVALIDA, RANGO_FECHAS_INVALIDO, TIPO_INVALIDO u otro: el backend explica.
                    ErrorResponse.Detalle error = ApiErrorParser.parse(response);
                    mostrarError(ApiErrorParser.mensaje(error, getString(R.string.historial_error_carga)));
                    return;
                }
                mostrar(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<HistorialResponse> call, @NonNull Throwable t) {
                if (call.isCanceled() || !estaVivo()) return;
                mostrarError(getString(R.string.error_sin_conexion));
            }
        });
    }

    /** Arma la lista con un encabezado por seccion que tenga operaciones. */
    private void mostrar(HistorialResponse historial) {
        List<OperacionResponse> compras = historial.getCompras();
        List<OperacionResponse> ventas = historial.getVentas();

        List<Object> filas = new ArrayList<>();
        if (!compras.isEmpty()) {
            filas.add(getString(R.string.historial_seccion_compras, compras.size()));
            filas.addAll(compras);
        }
        if (!ventas.isEmpty()) {
            filas.add(getString(R.string.historial_seccion_ventas, ventas.size()));
            filas.addAll(ventas);
        }

        if (filas.isEmpty()) {
            tvVacio.setText(hayFiltros() ? R.string.historial_vacio_filtros : R.string.historial_vacio);
            mostrarEstado(Estado.VACIO);
            return;
        }
        adapter.mostrar(filas);
        rvHistorial.scrollToPosition(0);
        mostrarEstado(Estado.LISTA);
    }

    // -----------------------------------------------------------------
    // Navegacion y estados
    // -----------------------------------------------------------------

    private void abrirArticulo(OperacionResponse operacion) {
        // Sin id la publicacion se borro despues de la operacion: no hay a donde ir.
        if (operacion.getArticulo() == null || operacion.getArticulo().getId() == null) return;
        Bundle args = new Bundle();
        args.putInt("publicacionId", operacion.getArticulo().getId());
        Navigation.findNavController(requireView()).navigate(R.id.action_historial_to_detalle, args);
    }

    private void abrirPerfil(OperacionResponse operacion) {
        if (operacion.getContraparte() == null) return;
        Bundle args = new Bundle();
        args.putInt("usuarioId", operacion.getContraparte().getId());
        Navigation.findNavController(requireView()).navigate(R.id.action_historial_to_perfilPublico, args);
    }

    private void volverAlLogin() {
        sesion.cerrarSesion();
        Toast.makeText(requireContext(), R.string.home_sesion_vencida, Toast.LENGTH_LONG).show();
        Navigation.findNavController(requireView()).navigate(R.id.action_historial_to_auth);
    }

    private void mostrarError(String mensaje) {
        tvError.setText(mensaje);
        mostrarEstado(Estado.ERROR);
    }

    private void mostrarEstado(Estado estado) {
        progressBar.setVisibility(estado == Estado.CARGANDO ? View.VISIBLE : View.GONE);
        rvHistorial.setVisibility(estado == Estado.LISTA ? View.VISIBLE : View.GONE);
        grupoVacio.setVisibility(estado == Estado.VACIO ? View.VISIBLE : View.GONE);
        grupoError.setVisibility(estado == Estado.ERROR ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        if (llamada != null) llamada.cancel();
        super.onDestroyView();
    }

    private boolean estaVivo() {
        return isAdded() && getView() != null;
    }
}
