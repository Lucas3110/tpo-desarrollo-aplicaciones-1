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
import com.example.ronda.data.model.CalificacionResponse;
import com.example.ronda.data.model.ErrorResponse;
import com.example.ronda.data.model.ListaCalificacionesResponse;
import com.example.ronda.data.model.PerfilPublicoResponse;
import com.example.ronda.data.model.PublicacionItemResponse;
import com.example.ronda.data.network.ApiErrorParser;
import com.example.ronda.data.network.UsuarioApiService;
import com.example.ronda.ui.home.PublicacionAdapter;
import com.example.ronda.ui.ofertas.FormatoOferta;

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
 * reputacion, publicaciones activas (tocar una abre su detalle) y las
 * calificaciones que recibio.
 *
 * Llega con el argumento usuarioId desde el detalle de una publicacion, "Mis
 * ofertas", las ofertas de una publicacion y el propio "Mi perfil". Se pide a
 * GET /usuarios/{id}/perfil, que es publico: no expone email ni telefono.
 *
 * Las calificaciones vienen de GET /usuarios/{id}/calificaciones, tambien
 * publico. Se piden despues del perfil y si fallan solo se avisa en su
 * seccion: el resto del perfil se sigue viendo.
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
    private TextView tvEstadoCalificaciones;
    private LinearLayout llCalificaciones;

    private int usuarioId = -1;
    private Call<PerfilPublicoResponse> llamada;
    private Call<ListaCalificacionesResponse> llamadaCalificaciones;

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
        tvEstadoCalificaciones = view.findViewById(R.id.tvEstadoCalificaciones);
        llCalificaciones = view.findViewById(R.id.llCalificaciones);
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
                cargarCalificaciones();
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
    // Calificaciones recibidas
    // -----------------------------------------------------------------

    private void cargarCalificaciones() {
        if (llamadaCalificaciones != null) llamadaCalificaciones.cancel();
        llCalificaciones.removeAllViews();
        tvEstadoCalificaciones.setVisibility(View.GONE);

        llamadaCalificaciones = usuarioApi.calificaciones(usuarioId);
        llamadaCalificaciones.enqueue(new Callback<ListaCalificacionesResponse>() {
            @Override
            public void onResponse(@NonNull Call<ListaCalificacionesResponse> call,
                                   @NonNull Response<ListaCalificacionesResponse> response) {
                if (call.isCanceled() || !estaVivo()) return;
                if (!response.isSuccessful() || response.body() == null) {
                    avisarEnCalificaciones(R.string.perfil_publico_error_calificaciones);
                    return;
                }
                poblarCalificaciones(response.body().getCalificaciones());
            }

            @Override
            public void onFailure(@NonNull Call<ListaCalificacionesResponse> call, @NonNull Throwable t) {
                if (call.isCanceled() || !estaVivo()) return;
                avisarEnCalificaciones(R.string.perfil_publico_error_calificaciones);
            }
        });
    }

    private void poblarCalificaciones(List<CalificacionResponse> calificaciones) {
        llCalificaciones.removeAllViews();
        if (calificaciones.isEmpty()) {
            avisarEnCalificaciones(R.string.perfil_publico_sin_calificaciones);
            return;
        }
        tvEstadoCalificaciones.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (CalificacionResponse c : calificaciones) {
            View fila = inflater.inflate(R.layout.item_calificacion, llCalificaciones, false);
            ((TextView) fila.findViewById(R.id.tvEstrellas)).setText(FormatoPerfil.estrellas(c.getEstrellas()));
            ((TextView) fila.findViewById(R.id.tvRol)).setText(c.esComoVendedor()
                    ? R.string.calificacion_como_vendedor : R.string.calificacion_como_comprador);

            TextView tvComentario = fila.findViewById(R.id.tvComentario);
            tvComentario.setText(c.tieneComentario() ? c.getComentario().trim() : "");
            tvComentario.setVisibility(c.tieneComentario() ? View.VISIBLE : View.GONE);

            ((TextView) fila.findViewById(R.id.tvMeta)).setText(detalleDe(c));
            llCalificaciones.addView(fila);
        }
    }

    /** "Martin Sosa · 16/09/2026 · Notebook Lenovo": quien, cuando y por que articulo. */
    private String detalleDe(CalificacionResponse c) {
        String autor = c.getAutor() != null && c.getAutor().getNombre() != null
                ? c.getAutor().getNombre() : "";
        String fecha = FormatoOferta.fecha(c.getFecha());
        String articulo = c.getArticulo();
        if (articulo == null || articulo.isEmpty()) {
            return getString(R.string.calificacion_meta, autor, fecha);
        }
        return getString(R.string.calificacion_meta_articulo, autor, fecha, articulo);
    }

    private void avisarEnCalificaciones(int mensaje) {
        tvEstadoCalificaciones.setText(mensaje);
        tvEstadoCalificaciones.setVisibility(View.VISIBLE);
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
        if (llamadaCalificaciones != null) llamadaCalificaciones.cancel();
        super.onDestroyView();
    }

    private boolean estaVivo() {
        return isAdded() && getView() != null;
    }
}
