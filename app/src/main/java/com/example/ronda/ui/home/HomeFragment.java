package com.example.ronda.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.ronda.R;
import com.example.ronda.data.model.ErrorResponse;
import com.example.ronda.data.model.PaginaPublicacionesResponse;
import com.example.ronda.data.model.PublicacionItemResponse;
import com.example.ronda.data.network.ApiErrorParser;
import com.example.ronda.data.network.PublicacionApiService;
import com.example.ronda.data.repository.SessionRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Home: explorar publicaciones (Punto 3).
 *
 * Pide el listado a GET /publicaciones y lo muestra en un ListView. Sigue el
 * mismo patron que las pantallas del Punto 1: enqueue, estaVivo() antes de
 * tocar la UI y ApiErrorParser.parse() una sola vez.
 *
 * Los datos viven en campos del Fragment y no en la vista: cuando se navega
 * al detalle y se vuelve, Navigation destruye la vista pero no el Fragment,
 * asi que la lista se vuelve a mostrar sin pedirla de nuevo.
 */
@AndroidEntryPoint
public class HomeFragment extends Fragment {

    @Inject
    PublicacionApiService publicacionApi;

    @Inject
    SessionRepository sesion;

    /** Que se ve en el area central: una sola de estas vistas a la vez. */
    private enum Estado { CARGANDO, LISTA, VACIO, ERROR }

    // --- Datos (sobreviven a que se destruya la vista) ---
    private final List<PublicacionItemResponse> items = new ArrayList<>();
    private PublicacionAdapter adapter;
    private int total = 0;
    private boolean hayMas = false;
    private boolean cargando = false;
    private Call<PaginaPublicacionesResponse> llamadaEnCurso;

    // --- Vistas ---
    private ListView lvPublicaciones;
    private ProgressBar progressBar;
    private View grupoVacio;
    private View grupoError;
    private TextView tvContador;
    private TextView tvError;
    private TextView tvUrlBase;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        lvPublicaciones = view.findViewById(R.id.lvPublicaciones);
        progressBar = view.findViewById(R.id.progressBar);
        grupoVacio = view.findViewById(R.id.grupoVacio);
        grupoError = view.findViewById(R.id.grupoError);
        tvContador = view.findViewById(R.id.tvContador);
        tvError = view.findViewById(R.id.tvError);
        tvUrlBase = view.findViewById(R.id.tvUrlBase);
        Button btnCerrarSesion = view.findViewById(R.id.btnCerrarSesion);
        Button btnActualizar = view.findViewById(R.id.btnActualizar);
        Button btnReintentar = view.findViewById(R.id.btnReintentar);

        // El adapter se crea una sola vez y trabaja sobre la misma lista
        // "items"; la vista nueva simplemente se engancha a el.
        if (adapter == null) {
            adapter = new PublicacionAdapter(items);
        }
        lvPublicaciones.setAdapter(adapter);
        lvPublicaciones.setOnItemClickListener((parent, fila, posicion, id) ->
                abrirDetalle(adapter.getItem(posicion)));

        btnCerrarSesion.setOnClickListener(v -> cerrarSesion());
        btnActualizar.setOnClickListener(v -> recargar());
        btnReintentar.setOnClickListener(v -> recargar());

        if (items.isEmpty()) {
            recargar();
        } else {
            // Volvimos de otra pantalla: lo que teniamos sigue valiendo.
            adapter.notifyDataSetChanged();
            actualizarContador();
            mostrarEstado(Estado.LISTA);
        }
    }

    // -----------------------------------------------------------------
    // Cargar publicaciones
    // -----------------------------------------------------------------

    /** Vuelve a pedir todo desde la primera pagina. */
    private void recargar() {
        cancelarLlamadaEnCurso();
        // Se vacia lo anterior: si la carga nueva falla, no queda una lista
        // vieja que no corresponde a lo pedido (y al volver de otra pantalla
        // se vuelve a intentar en vez de mostrarla).
        adapter.reemplazar(Collections.<PublicacionItemResponse>emptyList());
        total = 0;
        mostrarEstado(Estado.CARGANDO);
        cargarPagina(1);
    }

    private void cargarPagina(int pagina) {
        cargando = true;

        // Los parametros en null no viajan en la URL: por ahora solo se pide
        // la pagina, los filtros y el orden llegan en las proximas entregas.
        llamadaEnCurso = publicacionApi.listar(
                sesion.getBearerOpcional(),
                pagina,
                null,   // limite (default del backend: 20)
                null,   // q
                null,   // categoriaId
                null,   // precioMin
                null,   // precioMax
                null,   // estadoArticulo
                null,   // zonaId
                null);  // orden (default del backend: recientes)

        llamadaEnCurso.enqueue(new Callback<PaginaPublicacionesResponse>() {

            @Override
            public void onResponse(@NonNull Call<PaginaPublicacionesResponse> call,
                                   @NonNull Response<PaginaPublicacionesResponse> response) {
                // Cancelada por nosotros o el Fragment ya no esta: no hay nada que pintar.
                if (call.isCanceled() || !estaVivo()) return;
                cargando = false;

                if (response.isSuccessful() && response.body() != null) {
                    mostrarPagina(response.body());
                } else {
                    // Se lee el cuerpo UNA sola vez.
                    ErrorResponse.Detalle error = ApiErrorParser.parse(response);
                    manejarErrorApi(response.code(), ApiErrorParser.codigo(error),
                            ApiErrorParser.mensaje(error, getString(R.string.home_error_carga)),
                            call);
                }
            }

            @Override
            public void onFailure(@NonNull Call<PaginaPublicacionesResponse> call,
                                  @NonNull Throwable t) {
                if (call.isCanceled() || !estaVivo()) return;
                cargando = false;

                // IOException = no hubo respuesta: sin red, backend apagado, URL
                // mal. Cualquier otra cosa (por ejemplo JsonSyntaxException) es
                // que la respuesta llego pero no tiene la forma que espera la
                // app: el problema es nuestro, y conviene que se note distinto.
                int mensaje = t instanceof IOException
                        ? R.string.error_sin_conexion
                        : R.string.home_error_respuesta_invalida;
                mostrarError(getString(mensaje), call);
            }
        });
    }

    private void mostrarPagina(PaginaPublicacionesResponse pagina) {
        List<PublicacionItemResponse> nuevos = pagina.getItems() != null
                ? pagina.getItems()
                : Collections.<PublicacionItemResponse>emptyList();

        total = pagina.getTotal();
        hayMas = pagina.isHayMas();
        adapter.reemplazar(nuevos);
        lvPublicaciones.setSelection(0);

        actualizarContador();
        mostrarEstado(items.isEmpty() ? Estado.VACIO : Estado.LISTA);
    }

    /**
     * Errores que SI respondio el backend (4xx / 5xx). Se decide por el codigo
     * estable, nunca por el texto del mensaje.
     */
    private void manejarErrorApi(int httpCode, String codigo, String mensaje, Call<?> call) {
        if (httpCode == 401) {
            // Token vencido o invalido: lo que dice el apunte de Retrofit,
            // "redirigir al login". Hoy el listado acepta visitantes, asi que
            // no deberia pasar, pero es la respuesta correcta si pasa.
            sesion.cerrarSesion();
            Toast.makeText(requireContext(), R.string.home_sesion_vencida, Toast.LENGTH_LONG).show();
            Navigation.findNavController(requireView()).navigate(R.id.action_home_to_auth);
            return;
        }
        // RUTA_NO_ENCONTRADA, ERROR_INTERNO, BASE_NO_DISPONIBLE, o un cuerpo
        // que no era el JSON esperado (codigo null): se muestra lo que dijo
        // el servidor y se ofrece reintentar.
        mostrarError(mensaje, call);
    }

    // -----------------------------------------------------------------
    // Acciones
    // -----------------------------------------------------------------

    /**
     * El detalle es el Punto 4: cuando exista, aca va la navegacion con el id.
     * Mientras tanto, un aviso pensado para quien usa la app, no para el equipo.
     */
    private void abrirDetalle(PublicacionItemResponse publicacion) {
        Toast.makeText(requireContext(),
                getString(R.string.home_detalle_pendiente, publicacion.getTitulo()),
                Toast.LENGTH_SHORT).show();
    }

    private void cerrarSesion() {
        // Borrar el token es lo que corta la sesion: sin el, el auto-login
        // del LoginFragment no se dispara.
        sesion.cerrarSesion();
        Navigation.findNavController(requireView()).navigate(R.id.action_home_to_auth);
    }

    // -----------------------------------------------------------------
    // Estados de la pantalla
    // -----------------------------------------------------------------

    private void mostrarEstado(Estado estado) {
        progressBar.setVisibility(estado == Estado.CARGANDO ? View.VISIBLE : View.GONE);
        lvPublicaciones.setVisibility(estado == Estado.LISTA ? View.VISIBLE : View.GONE);
        grupoVacio.setVisibility(estado == Estado.VACIO ? View.VISIBLE : View.GONE);
        grupoError.setVisibility(estado == Estado.ERROR ? View.VISIBLE : View.GONE);
    }

    private void mostrarError(String mensaje, Call<?> call) {
        tvError.setText(mensaje);
        // La URL a la que se le pego, para darse cuenta rapido si el celular
        // esta apuntando a una IP vieja de la PC.
        String servidor = call.request().url().scheme() + "://"
                + call.request().url().host() + ":" + call.request().url().port() + "/";
        tvUrlBase.setText(getString(R.string.home_url_base, servidor));
        mostrarEstado(Estado.ERROR);
    }

    private void actualizarContador() {
        // plurals: "1 de 1 publicación" / "10 de 22 publicaciones".
        tvContador.setText(getResources().getQuantityString(
                R.plurals.home_contador, total, items.size(), total));
    }

    // -----------------------------------------------------------------
    // Ciclo de vida
    // -----------------------------------------------------------------

    private void cancelarLlamadaEnCurso() {
        if (llamadaEnCurso != null) {
            llamadaEnCurso.cancel();
            llamadaEnCurso = null;
        }
        cargando = false;
    }

    @Override
    public void onDestroyView() {
        // Una respuesta que llegue con la vista destruida no tiene donde pintarse.
        cancelarLlamadaEnCurso();
        super.onDestroyView();
    }

    private boolean estaVivo() {
        return isAdded() && getView() != null;
    }
}
