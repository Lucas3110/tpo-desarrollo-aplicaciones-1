package com.example.ronda.ui.publicar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.ronda.R;
import com.example.ronda.data.model.CambiarEstadoRequest;
import com.example.ronda.data.model.ErrorResponse;
import com.example.ronda.data.model.MisPublicacionesResponse;
import com.example.ronda.data.model.PublicacionItemResponse;
import com.example.ronda.data.model.PublicacionResponse;
import com.example.ronda.data.network.ApiErrorParser;
import com.example.ronda.data.network.PublicacionApiService;
import com.example.ronda.data.repository.SessionRepository;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Lista las publicaciones propias y permite pausar/reactivar según el estado. */
@AndroidEntryPoint
public class MisPublicacionesFragment extends Fragment {
    @Inject PublicacionApiService api;
    @Inject SessionRepository sesion;
    private final List<PublicacionItemResponse> items = new ArrayList<>();
    private MisPublicacionesAdapter adapter;
    private ProgressBar progreso; private TextView tvEstado, tvResumen; private Spinner filtro;
    private Call<?> llamada;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        return i.inflate(R.layout.fragment_mis_publicaciones, c, false);
    }
    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s); progreso = v.findViewById(R.id.pbMias); tvEstado = v.findViewById(R.id.tvEstadoMias);
        tvResumen = v.findViewById(R.id.tvResumenMias); filtro = v.findViewById(R.id.spFiltroEstado);
        ListView lista = v.findViewById(R.id.lvMias); adapter = new MisPublicacionesAdapter(items, this::confirmarCambio);
        lista.setAdapter(adapter);
        filtro.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Todas", "Activas", "Pausadas", "Vendidas"}));
        filtro.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, View x, int pos, long id) { cargar(); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) { }
        });
        tvEstado.setOnClickListener(x -> cargar());
        v.findViewById(R.id.btnNuevaPublicacion).setOnClickListener(x -> Navigation.findNavController(v).navigate(R.id.action_mis_publicaciones_to_publicar));
    }

    private void cargar() {
        if (!estaVivo()) return; progreso.setVisibility(View.VISIBLE); tvEstado.setVisibility(View.GONE);
        String[] codigos = {null, "ACTIVA", "PAUSADA", "VENDIDA"};
        llamada = api.mias(sesion.getBearer(), codigos[filtro.getSelectedItemPosition()]);
        ((Call<MisPublicacionesResponse>)llamada).enqueue(new Callback<MisPublicacionesResponse>() {
            @Override public void onResponse(@NonNull Call<MisPublicacionesResponse> c, @NonNull Response<MisPublicacionesResponse> r) {
                if (!estaVivo()) return; progreso.setVisibility(View.GONE);
                if (r.code() == 401) { irLogin(); return; }
                if (!r.isSuccessful() || r.body() == null) { error(); return; }
                items.clear(); items.addAll(r.body().getItems()); adapter.notifyDataSetChanged();
                MisPublicacionesResponse.Resumen resumen = r.body().getResumen();
                if (resumen != null) tvResumen.setText(getString(R.string.mias_resumen, resumen.getActivas(), resumen.getPausadas(), resumen.getVendidas()));
                tvEstado.setText(R.string.mias_vacio); tvEstado.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            }
            @Override public void onFailure(@NonNull Call<MisPublicacionesResponse> c, @NonNull Throwable t) { if (estaVivo() && !c.isCanceled()) { progreso.setVisibility(View.GONE); error(); } }
        });
    }

    private void confirmarCambio(PublicacionItemResponse item) {
        String nuevo = "ACTIVA".equals(item.getEstado()) ? "PAUSADA" : "ACTIVA";
        String accion = "ACTIVA".equals(nuevo) ? "reactivar" : "pausar";
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("¿" + Character.toUpperCase(accion.charAt(0)) + accion.substring(1) + " publicación?")
                .setMessage(item.getTitulo()).setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("Sí", (d, w) -> cambiarEstado(item, nuevo)).show();
    }
    private void cambiarEstado(PublicacionItemResponse item, String nuevo) {
        llamada = api.cambiarEstado(sesion.getBearer(), item.getId(), new CambiarEstadoRequest(nuevo));
        ((Call<PublicacionResponse>)llamada).enqueue(new Callback<PublicacionResponse>() {
            @Override public void onResponse(@NonNull Call<PublicacionResponse> c, @NonNull Response<PublicacionResponse> r) {
                if (!estaVivo()) return; if (r.code() == 401) { irLogin(); return; }
                if (r.isSuccessful()) cargar(); else { ErrorResponse.Detalle e = ApiErrorParser.parse(r); Toast.makeText(requireContext(), ApiErrorParser.mensaje(e, "No pudimos cambiar el estado"), Toast.LENGTH_LONG).show(); }
            }
            @Override public void onFailure(@NonNull Call<PublicacionResponse> c, @NonNull Throwable t) { if (estaVivo() && !c.isCanceled()) Toast.makeText(requireContext(), R.string.publicar_error_conexion, Toast.LENGTH_SHORT).show(); }
        });
    }
    private void error() { tvEstado.setText(R.string.mias_error); tvEstado.setVisibility(View.VISIBLE); }
    private boolean estaVivo() { return isAdded() && getView() != null; }
    private void irLogin() { sesion.cerrarSesion(); Navigation.findNavController(requireView()).navigate(R.id.action_mis_publicaciones_to_auth); }
    @Override public void onDestroyView() { if (llamada != null) llamada.cancel(); super.onDestroyView(); }
}
