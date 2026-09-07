package com.example.ronda.ui.perfil;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.ronda.R;
import com.example.ronda.data.model.PerfilResponse;
import com.example.ronda.data.network.AuthApiService;
import com.example.ronda.data.repository.SessionRepository;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Mi perfil (Punto 2).
 *
 * Primer paso: solo muestra el nombre de la persona logueada, que se pide a
 * GET /auth/me (el mismo endpoint que usa el auto-login). Los demas datos y
 * la edicion se agregan en pasos siguientes.
 *
 * Sigue el patron de las pantallas del Punto 1: enqueue, estaVivo() antes de
 * tocar la UI y sin bloquear el hilo principal.
 */
@AndroidEntryPoint
public class PerfilFragment extends Fragment {

    @Inject
    AuthApiService authApi;

    @Inject
    SessionRepository sesion;

    private ProgressBar progressBar;
    private TextView tvNombre;
    private Call<PerfilResponse> llamada;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_perfil, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressBar = view.findViewById(R.id.progressBar);
        tvNombre = view.findViewById(R.id.tvNombre);

        cargarNombre();
    }

    private void cargarNombre() {
        progressBar.setVisibility(View.VISIBLE);

        llamada = authApi.me(sesion.getBearer());
        llamada.enqueue(new Callback<PerfilResponse>() {
            @Override
            public void onResponse(@NonNull Call<PerfilResponse> call,
                                   @NonNull Response<PerfilResponse> response) {
                if (call.isCanceled() || !estaVivo()) return;
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null
                        && response.body().getUsuario() != null) {
                    tvNombre.setText(response.body().getUsuario().getNombre());
                } else {
                    mostrarFallback();
                }
            }

            @Override
            public void onFailure(@NonNull Call<PerfilResponse> call, @NonNull Throwable t) {
                if (call.isCanceled() || !estaVivo()) return;
                progressBar.setVisibility(View.GONE);
                mostrarFallback();
            }
        });
    }

    /** Sin respuesta del backend: al menos mostramos el email guardado. */
    private void mostrarFallback() {
        tvNombre.setText(sesion.getEmail());
        Toast.makeText(requireContext(), R.string.perfil_error_carga, Toast.LENGTH_LONG).show();
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
