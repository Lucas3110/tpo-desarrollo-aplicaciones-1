package com.example.ronda.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
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
import com.google.android.material.snackbar.Snackbar;

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
 * Pide el listado a GET /publicaciones y lo muestra en un ListView, de a
 * paginas: cuando el scroll llega cerca del final y el backend dice que
 * hayMas, se pide la siguiente y se agrega al final. Sigue el mismo patron
 * que las pantallas del Punto 1: enqueue, estaVivo() antes de tocar la UI y
 * ApiErrorParser.parse() una sola vez.
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

    /**
     * Publicaciones por pagina. El backend admite hasta 50; con 10 el
     * paginado se ve con pocas publicaciones cargadas.
     */
    private static final int LIMITE_PAGINA = 10;

    /** Cuantas filas antes del final se empieza a pedir la pagina siguiente. */
    private static final int UMBRAL_PRECARGA = 3;

    // --- Datos (sobreviven a que se destruya la vista) ---
    private final List<PublicacionItemResponse> items = new ArrayList<>();
    private PublicacionAdapter adapter;
    private int total = 0;
    private int paginaActual = 0;
    private boolean hayMas = false;
    private boolean cargando = false;
    /**
     * Fallo la carga de una pagina siguiente: el scroll no insiste mientras el
     * aviso esta a la vista. Vuelve a false cuando el Snackbar se cierra (por
     * "Reintentar", por tiempo o por gesto), asi el paginado nunca queda trabado.
     */
    private boolean errorPaginado = false;
    private Snackbar snackbarPaginado;
    private Call<PaginaPublicacionesResponse> llamadaEnCurso;

    // --- Vistas ---
    private ListView lvPublicaciones;
    private ProgressBar progressBar;
    private View grupoVacio;
    private View grupoError;
    private TextView tvContador;
    private TextView tvError;
    private TextView tvUrlBase;
    private View pbCargandoMas;

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

        // Pie con el spinner de "cargando mas". Va antes de setAdapter y no
        // es clickeable, asi el tap sobre el no cuenta como una publicacion.
        View pie = getLayoutInflater().inflate(R.layout.item_cargando_mas, lvPublicaciones, false);
        pbCargandoMas = pie.findViewById(R.id.pbCargandoMas);
        lvPublicaciones.addFooterView(pie, null, false);

        // El adapter se crea una sola vez y trabaja sobre la misma lista
        // "items"; la vista nueva simplemente se engancha a el.
        if (adapter == null) {
            adapter = new PublicacionAdapter(items);
        }
        lvPublicaciones.setAdapter(adapter);
        lvPublicaciones.setOnItemClickListener((parent, fila, posicion, id) ->
                abrirDetalle(adapter.getItem(posicion)));
        lvPublicaciones.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView vista, int estado) {
                // No hace falta reaccionar al cambio de estado del gesto.
            }

            @Override
            public void onScroll(AbsListView vista, int primeraVisible,
                                 int cantidadVisibles, int totalEnLista) {
                boolean cercaDelFinal = primeraVisible + cantidadVisibles
                        >= totalEnLista - UMBRAL_PRECARGA;
                if (cercaDelFinal && hayMas && !cargando && !errorPaginado
                        && !items.isEmpty()) {
                    cargarPagina(paginaActual + 1);
                }
            }
        });

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
        paginaActual = 0;
        hayMas = false;
        errorPaginado = false;
        if (snackbarPaginado != null) {
            snackbarPaginado.dismiss();
            snackbarPaginado = null;
        }
        mostrarCargandoMas(false);
        mostrarEstado(Estado.CARGANDO);
        cargarPagina(1);
    }

    private void cargarPagina(int pagina) {
        cargando = true;
        if (pagina > 1) {
            mostrarCargandoMas(true);
        }

        // Los parametros en null no viajan en la URL: por ahora solo se piden
        // la pagina y el limite, los filtros y el orden llegan en las
        // proximas entregas.
        llamadaEnCurso = publicacionApi.listar(
                sesion.getBearerOpcional(),
                pagina,
                LIMITE_PAGINA,
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
                    mostrarPagina(response.body(), pagina);
                } else {
                    // Se lee el cuerpo UNA sola vez.
                    ErrorResponse.Detalle error = ApiErrorParser.parse(response);
                    manejarErrorApi(response.code(), ApiErrorParser.codigo(error),
                            ApiErrorParser.mensaje(error, getString(R.string.home_error_carga)),
                            call, pagina);
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
                if (pagina == 1) {
                    mostrarError(getString(mensaje), call);
                } else {
                    mostrarErrorPaginado();
                }
            }
        });
    }

    private void mostrarPagina(PaginaPublicacionesResponse respuesta, int paginaPedida) {
        List<PublicacionItemResponse> nuevos = respuesta.getItems() != null
                ? respuesta.getItems()
                : Collections.<PublicacionItemResponse>emptyList();

        total = respuesta.getTotal();
        paginaActual = respuesta.getPagina();
        hayMas = respuesta.isHayMas();
        mostrarCargandoMas(false);

        if (paginaPedida == 1) {
            adapter.reemplazar(nuevos);
            lvPublicaciones.setSelection(0);
        } else {
            // Se suman al final; las repetidas por un corrimiento del backend
            // las descarta el adapter.
            adapter.agregar(nuevos);
        }

        actualizarContador();
        mostrarEstado(items.isEmpty() ? Estado.VACIO : Estado.LISTA);
    }

    /**
     * Errores que SI respondio el backend (4xx / 5xx). Se decide por el codigo
     * estable, nunca por el texto del mensaje.
     */
    private void manejarErrorApi(int httpCode, String codigo, String mensaje, Call<?> call,
                                 int pagina) {
        if (httpCode == 401) {
            // Token vencido o invalido: lo que dice el apunte de Retrofit,
            // "redirigir al login". Hoy el listado acepta visitantes, asi que
            // no deberia pasar, pero es la respuesta correcta si pasa.
            sesion.cerrarSesion();
            Toast.makeText(requireContext(), R.string.home_sesion_vencida, Toast.LENGTH_LONG).show();
            Navigation.findNavController(requireView()).navigate(R.id.action_home_to_auth);
            return;
        }
        if (pagina > 1) {
            if ("PAGINA_INVALIDA".equals(codigo) || "LIMITE_INVALIDO".equals(codigo)) {
                // No tiene sentido insistir con esa pagina: se da por terminado.
                hayMas = false;
                mostrarCargandoMas(false);
                return;
            }
            // La lista que ya se ve sigue valiendo: solo se avisa abajo.
            mostrarErrorPaginado();
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

    /**
     * Fallo una pagina siguiente: la lista se conserva, se apaga el pie y se
     * ofrece reintentar en un Snackbar. Mientras el aviso esta a la vista el
     * scroll no vuelve a pedir (si no, con el backend caido cada gesto
     * dispararia un request y un aviso nuevo). Cuando el aviso se cierra, por
     * "Reintentar", por tiempo o por gesto, el proximo scroll reintenta solo.
     */
    private void mostrarErrorPaginado() {
        errorPaginado = true;
        mostrarCargandoMas(false);
        snackbarPaginado = Snackbar.make(requireView(), R.string.home_error_mas_publicaciones,
                Snackbar.LENGTH_LONG);
        snackbarPaginado.setAction(R.string.home_reintentar, v -> {
            // Si mientras tanto hubo una recarga (cambio de filtros u orden),
            // hayMas quedo en false hasta que responda la pagina 1: no se pisa.
            if (!estaVivo() || cargando || !hayMas) return;
            cargarPagina(paginaActual + 1);
        });
        snackbarPaginado.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar snackbar, int motivo) {
                errorPaginado = false;
                if (snackbarPaginado == snackbar) snackbarPaginado = null;
            }
        });
        snackbarPaginado.show();
    }

    private void mostrarCargandoMas(boolean visible) {
        if (pbCargandoMas != null) {
            pbCargandoMas.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
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
