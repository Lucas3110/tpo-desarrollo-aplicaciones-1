package com.example.ronda.ui.perfil;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.ronda.R;
import com.example.ronda.data.model.ErrorResponse;
import com.example.ronda.data.model.PerfilPublicoResponse;
import com.example.ronda.data.model.PublicacionItemResponse;
import com.example.ronda.data.network.ApiErrorParser;
import com.example.ronda.data.network.UsuarioApiService;
import com.example.ronda.ui.home.PublicacionAdapter;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Perfil publico de una persona (Punto 2): lo que se consulta antes de
 * operar con ella. Foto, nombre, zona, antiguedad en la plataforma,
 * reputacion y publicaciones activas (tocar una abre su detalle).
 *
 * Llega con el argumento usuarioId desde el detalle de una publicacion, "Mis
 * ofertas", las ofertas de una publicacion y el propio "Mi perfil". Se pide a
 * GET /usuarios/{id}/perfil, que es publico: no expone email ni telefono.
 *
 * Las publicaciones se dibujan con las mismas filas del Home
 * (PublicacionAdapter) agregadas a un contenedor: son pocas (el backend
 * manda hasta 10), asi que no hace falta una lista con scroll propio dentro
 * del ScrollView.
 */
@AndroidEntryPoint
public class PerfilPublicoFragment extends Fragment {

    /** Nombre del argumento, el mismo que declara el nav graph. */
    public static final String ARG_USUARIO_ID = "usuarioId";

    @Inject
    UsuarioApiService usuarioApi;

    private ProgressBar progressBar;
    private View grupoContenido;
    private View grupoError;
    private TextView tvError;
    private ImageView ivFoto;
    private TextView tvNombre;
    private TextView tvZona;
    private TextView tvAntiguedad;
    private TextView tvReputacionEstrellas;
    private TextView tvReputacionOperaciones;
    private TextView tvSinPublicaciones;
    private LinearLayout llPublicaciones;

    private int usuarioId = -1;
    private Call<PerfilPublicoResponse> llamada;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_perfil_publico, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressBar = view.findViewById(R.id.progressBar);
        grupoContenido = view.findViewById(R.id.grupoContenido);
        grupoError = view.findViewById(R.id.grupoError);
        tvError = view.findViewById(R.id.tvError);
        ivFoto = view.findViewById(R.id.ivFoto);
        tvNombre = view.findViewById(R.id.tvNombre);
        tvZona = view.findViewById(R.id.tvZona);
        tvAntiguedad = view.findViewById(R.id.tvAntiguedad);
        tvReputacionEstrellas = view.findViewById(R.id.tvReputacionEstrellas);
        tvReputacionOperaciones = view.findViewById(R.id.tvReputacionOperaciones);
        tvSinPublicaciones = view.findViewById(R.id.tvSinPublicaciones);
        llPublicaciones = view.findViewById(R.id.llPublicaciones);
        Button btnReintentar = view.findViewById(R.id.btnReintentar);

        btnReintentar.setOnClickListener(v -> cargar());

        usuarioId = getArguments() != null ? getArguments().getInt(ARG_USUARIO_ID, -1) : -1;
        if (usuarioId == -1) {
            mostrarError(getString(R.string.perfil_publico_error_carga));
            return;
        }
        cargar();
    }

    // -----------------------------------------------------------------
    // Cargar
    // -----------------------------------------------------------------

    private void cargar() {
        mostrarEstado(true, false);

        llamada = usuarioApi.perfilPublico(usuarioId);
        llamada.enqueue(new Callback<PerfilPublicoResponse>() {
            @Override
            public void onResponse(@NonNull Call<PerfilPublicoResponse> call,
                                   @NonNull Response<PerfilPublicoResponse> response) {
                if (call.isCanceled() || !estaVivo()) return;

                if (!response.isSuccessful() || response.body() == null
                        || response.body().getPerfil() == null) {
                    // 404 USUARIO_NO_ENCONTRADO u otro: el mensaje lo pone el backend.
                    ErrorResponse.Detalle error = ApiErrorParser.parse(response);
                    mostrarError(ApiErrorParser.mensaje(error,
                            getString(R.string.perfil_publico_error_carga)));
                    return;
                }
                poblar(response.body().getPerfil());
                mostrarEstado(false, true);
            }

            @Override
            public void onFailure(@NonNull Call<PerfilPublicoResponse> call, @NonNull Throwable t) {
                if (call.isCanceled() || !estaVivo()) return;
                mostrarError(getString(R.string.error_sin_conexion));
            }
        });
    }

    private void poblar(PerfilPublicoResponse.Perfil perfil) {
        Context ctx = requireContext();

        FormatoPerfil.cargarAvatar(ivFoto, perfil.getFotoUrl());
        tvNombre.setText(perfil.getNombre());
        // La zona es opcional: sin zona no se deja un hueco.
        boolean hayZona = perfil.getZona() != null && perfil.getZona().getNombre() != null;
        tvZona.setText(hayZona ? perfil.getZona().getNombre() : "");
        tvZona.setVisibility(hayZona ? View.VISIBLE : View.GONE);
        tvAntiguedad.setText(FormatoPerfil.antiguedad(ctx, perfil.getAntiguedadDias()));
        tvReputacionEstrellas.setText(FormatoPerfil.reputacionEstrellas(ctx, perfil.getReputacion()));
        tvReputacionOperaciones.setText(FormatoPerfil.reputacionOperaciones(ctx, perfil.getReputacion()));

        poblarPublicaciones(perfil.getPublicacionesActivas());
    }

    private void poblarPublicaciones(List<PublicacionItemResponse> publicaciones) {
        llPublicaciones.removeAllViews();
        tvSinPublicaciones.setVisibility(publicaciones.isEmpty() ? View.VISIBLE : View.GONE);

        PublicacionAdapter filas = new PublicacionAdapter(new ArrayList<>(publicaciones));
        for (int i = 0; i < filas.getCount(); i++) {
            View fila = filas.getView(i, null, llPublicaciones);
            int publicacionId = filas.getItem(i).getId();
            fila.setOnClickListener(v -> abrirDetalle(publicacionId));
            llPublicaciones.addView(fila);
        }
    }

    // -----------------------------------------------------------------
    // Navegacion y estados
    // -----------------------------------------------------------------

    private void abrirDetalle(int publicacionId) {
        Bundle args = new Bundle();
        args.putInt("publicacionId", publicacionId);
        Navigation.findNavController(requireView())
                .navigate(R.id.action_perfilPublico_to_detalle, args);
    }

    private void mostrarEstado(boolean cargando, boolean contenido) {
        progressBar.setVisibility(cargando ? View.VISIBLE : View.GONE);
        grupoContenido.setVisibility(contenido ? View.VISIBLE : View.GONE);
        grupoError.setVisibility(View.GONE);
    }

    private void mostrarError(String mensaje) {
        tvError.setText(mensaje);
        progressBar.setVisibility(View.GONE);
        grupoContenido.setVisibility(View.GONE);
        grupoError.setVisibility(View.VISIBLE);
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
