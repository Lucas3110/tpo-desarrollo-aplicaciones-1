package com.example.ronda.ui.ofertas;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ronda.R;
import com.example.ronda.data.model.ErrorResponse;
import com.example.ronda.data.model.MisOfertasResponse;
import com.example.ronda.data.model.OfertaResponse;
import com.example.ronda.data.network.ApiErrorParser;
import com.example.ronda.data.network.OfertaApiService;
import com.example.ronda.data.repository.SessionRepository;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.tabs.TabLayout;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Seccion "Mis ofertas" (Punto 7): las que envie y las que recibi, en dos
 * pestañas. Se pide todo junto a GET /ofertas/mias cada vez que se entra a
 * la pantalla y con el boton "Actualizar", asi los estados (aceptada,
 * rechazada, vencida) estan siempre al dia. El backend vence las pendientes
 * caducadas antes de responder, no hace falta calcular nada aca.
 */
@AndroidEntryPoint
public class MisOfertasFragment extends Fragment {

    private static final int TAB_ENVIADAS = 0;
    private static final int TAB_RECIBIDAS = 1;

    @Inject
    OfertaApiService ofertaApi;

    @Inject
    SessionRepository sesion;

    private TabLayout tabs;
    private RecyclerView rvOfertas;
    private ProgressBar progressBar;
    private View grupoVacio;
    private View grupoError;
    private TextView tvVacio;
    private TextView tvError;

    private MisOfertasAdapter adapter;
    /** Ultima respuesta del servidor; se recorre segun la pestaña elegida. */
    private MisOfertasResponse datos;
    private int tabActual = TAB_ENVIADAS;
    private Call<MisOfertasResponse> llamada;

    private enum Estado { CARGANDO, LISTA, VACIO, ERROR }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mis_ofertas, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tabs = view.findViewById(R.id.tabs);
        rvOfertas = view.findViewById(R.id.rvOfertas);
        progressBar = view.findViewById(R.id.progressBar);
        grupoVacio = view.findViewById(R.id.grupoVacio);
        grupoError = view.findViewById(R.id.grupoError);
        tvVacio = view.findViewById(R.id.tvVacio);
        tvError = view.findViewById(R.id.tvError);
        Button btnActualizar = view.findViewById(R.id.btnActualizar);
        Button btnReintentar = view.findViewById(R.id.btnReintentar);

        adapter = new MisOfertasAdapter(this::abrirDetalle);
        rvOfertas.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvOfertas.setAdapter(adapter);

        configurarTabs();
        btnActualizar.setOnClickListener(v -> cargar());
        btnReintentar.setOnClickListener(v -> cargar());

        cargar();
    }

    private void configurarTabs() {
        tabs.addTab(tabs.newTab().setText(R.string.mis_ofertas_tab_enviadas));
        tabs.addTab(tabs.newTab().setText(R.string.mis_ofertas_tab_recibidas));
        // Al volver de otra pantalla la vista se recrea: se respeta la pestaña que estaba.
        TabLayout.Tab guardada = tabs.getTabAt(tabActual);
        if (guardada != null) tabs.selectTab(guardada);
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                tabActual = tab.getPosition();
                mostrarTab();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    // -----------------------------------------------------------------
    // Cargar
    // -----------------------------------------------------------------

    private void cargar() {
        mostrarEstado(Estado.CARGANDO);

        llamada = ofertaApi.misOfertas();
        llamada.enqueue(new Callback<MisOfertasResponse>() {
            @Override
            public void onResponse(@NonNull Call<MisOfertasResponse> call,
                                   @NonNull Response<MisOfertasResponse> response) {
                if (call.isCanceled() || !estaVivo()) return;

                if (response.code() == 401) {
                    volverAlLogin();
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    ErrorResponse.Detalle error = ApiErrorParser.parse(response);
                    mostrarError(ApiErrorParser.mensaje(error, getString(R.string.mis_ofertas_error_carga)));
                    return;
                }

                datos = response.body();
                actualizarTitulosDeTabs();
                mostrarTab();
            }

            @Override
            public void onFailure(@NonNull Call<MisOfertasResponse> call, @NonNull Throwable t) {
                if (call.isCanceled() || !estaVivo()) return;
                mostrarError(getString(R.string.error_sin_conexion));
            }
        });
    }

    /** "Enviadas (3)" y "Recibidas (2)", con un globo con las recibidas que esperan respuesta. */
    private void actualizarTitulosDeTabs() {
        TabLayout.Tab enviadas = tabs.getTabAt(TAB_ENVIADAS);
        TabLayout.Tab recibidas = tabs.getTabAt(TAB_RECIBIDAS);
        if (enviadas == null || recibidas == null) return;

        enviadas.setText(getString(R.string.mis_ofertas_tab_con_cantidad,
                getString(R.string.mis_ofertas_tab_enviadas), datos.getEnviadas().size()));
        recibidas.setText(getString(R.string.mis_ofertas_tab_con_cantidad,
                getString(R.string.mis_ofertas_tab_recibidas), datos.getRecibidas().size()));

        int pendientes = datos.getPendientesRecibidas();
        if (pendientes > 0) {
            BadgeDrawable globo = recibidas.getOrCreateBadge();
            globo.setNumber(pendientes);
        } else {
            recibidas.removeBadge();
        }
    }

    /** Muestra la lista de la pestaña elegida, o el vacio propio de esa pestaña. */
    private void mostrarTab() {
        if (datos == null) return;
        boolean enviadas = tabActual == TAB_ENVIADAS;
        List<OfertaResponse> lista = enviadas ? datos.getEnviadas() : datos.getRecibidas();
        adapter.mostrar(lista, enviadas);
        if (lista.isEmpty()) {
            tvVacio.setText(enviadas ? R.string.mis_ofertas_vacio_enviadas : R.string.mis_ofertas_vacio_recibidas);
            mostrarEstado(Estado.VACIO);
        } else {
            rvOfertas.scrollToPosition(0);
            mostrarEstado(Estado.LISTA);
        }
    }

    // -----------------------------------------------------------------
    // Navegacion y estados
    // -----------------------------------------------------------------

    private void abrirDetalle(OfertaResponse oferta) {
        if (oferta.getPublicacion() == null) return;
        Bundle args = new Bundle();
        args.putInt("publicacionId", oferta.getPublicacion().getId());
        Navigation.findNavController(requireView()).navigate(R.id.action_misOfertas_to_detalle, args);
    }

    private void volverAlLogin() {
        sesion.cerrarSesion();
        Toast.makeText(requireContext(), R.string.home_sesion_vencida, Toast.LENGTH_LONG).show();
        Navigation.findNavController(requireView()).navigate(R.id.action_misOfertas_to_auth);
    }

    private void mostrarError(String mensaje) {
        tvError.setText(mensaje);
        mostrarEstado(Estado.ERROR);
    }

    private void mostrarEstado(Estado estado) {
        progressBar.setVisibility(estado == Estado.CARGANDO ? View.VISIBLE : View.GONE);
        rvOfertas.setVisibility(estado == Estado.LISTA ? View.VISIBLE : View.GONE);
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
