package com.example.ronda.ui.home;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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
import com.example.ronda.data.model.CategoriaResponse;
import com.example.ronda.data.model.CategoriasResponse;
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
 * hayMas, se pide la siguiente y se agrega al final. El buscador manda el
 * texto como q y el backend busca en titulo y descripcion; el Spinner de
 * orden manda "orden"; el panel de filtros manda categoriaId, precioMin,
 * precioMax y estadoArticulo, todos combinables. Sigue el mismo patron que
 * las pantallas del Punto 1: enqueue, estaVivo() antes de tocar la UI y
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

    private static final String CLAVE_FILTROS = "filtros";
    private static final String CLAVE_PANEL_ABIERTO = "panelAbierto";
    private static final String TAG = "Home";

    // --- Datos (sobreviven a que se destruya la vista) ---
    private final List<PublicacionItemResponse> items = new ArrayList<>();
    /** Lo aplicado (no lo que se esta escribiendo): con esto se arma cada request. */
    private FiltrosPublicaciones filtros = new FiltrosPublicaciones();
    /** Catalogo para el Spinner; vive aca (no en el panel) para sobrevivir al back stack. */
    private List<CategoriaResponse> categorias;
    private Call<CategoriasResponse> llamadaCategorias;
    private boolean panelAbierto = false;
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
    private PanelFiltros panel;
    private Spinner spOrden;
    private EditText etBuscar;
    private ImageButton btnLimpiarBusqueda;
    private TextView tvVacio;
    private Button btnActualizar;
    private ListView lvPublicaciones;
    private ProgressBar progressBar;
    private View grupoVacio;
    private View grupoError;
    private TextView tvContador;
    private TextView tvError;
    private TextView tvUrlBase;
    private View pbCargandoMas;

    @Override
    @SuppressWarnings("deprecation") // getSerializable(String) sigue siendo la unica forma en minSdk 24
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Al rotar, Android recrea el Fragment y se pierden los campos: lo
        // aplicado se recupera del Bundle y la lista se vuelve a pedir.
        if (savedInstanceState != null) {
            FiltrosPublicaciones guardados =
                    (FiltrosPublicaciones) savedInstanceState.getSerializable(CLAVE_FILTROS);
            if (guardados != null) {
                filtros = guardados;
            }
            panelAbierto = savedInstanceState.getBoolean(CLAVE_PANEL_ABIERTO, false);
        }
    }

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

        panel = new PanelFiltros(view);
        if (categorias != null) {
            panel.setCategorias(categorias, filtros.getCategoriaId());
        }
        Button btnFiltros = view.findViewById(R.id.btnFiltros);
        Button btnLimpiarFiltros = view.findViewById(R.id.btnLimpiarFiltros);
        Button btnAplicarFiltros = view.findViewById(R.id.btnAplicarFiltros);
        spOrden = view.findViewById(R.id.spOrden);
        etBuscar = view.findViewById(R.id.etBuscar);
        btnLimpiarBusqueda = view.findViewById(R.id.btnLimpiarBusqueda);
        ImageButton btnBuscar = view.findViewById(R.id.btnBuscar);
        tvVacio = view.findViewById(R.id.tvVacio);
        lvPublicaciones = view.findViewById(R.id.lvPublicaciones);
        progressBar = view.findViewById(R.id.progressBar);
        grupoVacio = view.findViewById(R.id.grupoVacio);
        grupoError = view.findViewById(R.id.grupoError);
        tvContador = view.findViewById(R.id.tvContador);
        tvError = view.findViewById(R.id.tvError);
        tvUrlBase = view.findViewById(R.id.tvUrlBase);
        Button btnCerrarSesion = view.findViewById(R.id.btnCerrarSesion);
        btnActualizar = view.findViewById(R.id.btnActualizar);
        Button btnReintentar = view.findViewById(R.id.btnReintentar);

        configurarBuscador(btnBuscar);
        configurarOrden();
        configurarFiltros(btnFiltros, btnLimpiarFiltros, btnAplicarFiltros);

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
        btnActualizar.setOnClickListener(v -> {
            // En el vacio "con busqueda" el boton limpia; en el vacio a secas, actualiza.
            if (filtros.hayAlgoAplicado()) {
                limpiarTodo();
            } else {
                recargar();
            }
        });
        btnReintentar.setOnClickListener(v -> recargar());

        if (items.isEmpty()) {
            recargar();
        } else {
            // Volvimos de otra pantalla: lo que teniamos sigue valiendo.
            adapter.notifyDataSetChanged();
            actualizarContador();
            mostrarEstado(Estado.LISTA);
        }
        if (categorias == null) {
            cargarCategorias();
        }
    }

    // -----------------------------------------------------------------
    // Filtros: categoria, rango de precio y estado del articulo
    // -----------------------------------------------------------------

    private void configurarFiltros(Button btnFiltros, Button btnLimpiarFiltros,
                                   Button btnAplicarFiltros) {
        panel.volcar(filtros);
        panel.mostrar(panelAbierto);
        panel.actualizarBoton(filtros);

        btnFiltros.setOnClickListener(v -> {
            panelAbierto = !panelAbierto;
            if (panelAbierto) {
                // Se abre mostrando lo APLICADO: lo que se haya editado y no
                // aplicado la vez anterior se descarta.
                panel.volcar(filtros);
                if (categorias == null) cargarCategorias();
            }
            panel.mostrar(panelAbierto);
        });
        btnAplicarFiltros.setOnClickListener(v -> aplicarFiltros(filtros.claveDeFiltros()));
        btnLimpiarFiltros.setOnClickListener(v -> {
            // La "foto" de lo aplicado se toma ANTES de limpiar: si no, la
            // comparacion de aplicarFiltros() nunca ve el cambio y no recarga.
            String antes = filtros.claveDeFiltros();
            filtros.limpiarFiltros();
            panel.volcar(filtros);
            aplicarFiltros(antes);
        });
    }

    /**
     * Lee el panel, valida y, si algo cambio respecto de "antes" (la clave de
     * lo que estaba aplicado), recarga desde la pagina 1.
     */
    private void aplicarFiltros(String antes) {
        if (!panel.leerEn(filtros)) {
            return; // el panel ya marco el error en el campo
        }

        ocultarTeclado();
        panelAbierto = false;
        panel.mostrar(false);
        panel.actualizarBoton(filtros);

        if (!antes.equals(filtros.claveDeFiltros())) {
            recargar();
        }
    }

    /**
     * GET /categorias, sin token. Si falla no bloquea nada: el Spinner queda
     * con "Todas" y se reintenta la proxima vez que se abre el panel.
     */
    private void cargarCategorias() {
        if (llamadaCategorias != null) return;
        llamadaCategorias = publicacionApi.categorias();
        llamadaCategorias.enqueue(new Callback<CategoriasResponse>() {
            @Override
            public void onResponse(@NonNull Call<CategoriasResponse> call,
                                   @NonNull Response<CategoriasResponse> response) {
                llamadaCategorias = null;
                if (call.isCanceled() || !estaVivo()) return;
                if (response.isSuccessful() && response.body() != null
                        && response.body().getCategorias() != null) {
                    categorias = response.body().getCategorias();
                    panel.setCategorias(categorias, filtros.getCategoriaId());
                } else {
                    Log.w(TAG, "No se pudo cargar el catalogo de categorias: HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<CategoriasResponse> call, @NonNull Throwable t) {
                llamadaCategorias = null;
                if (call.isCanceled() || !estaVivo()) return;
                Log.w(TAG, "No se pudo cargar el catalogo de categorias", t);
            }
        });
    }

    // -----------------------------------------------------------------
    // Buscador
    // -----------------------------------------------------------------

    private void configurarBuscador(ImageButton btnBuscar) {
        // Si venimos de rotar o de otra pantalla, el campo muestra lo aplicado.
        if (filtros.getQ() != null && etBuscar.getText().toString().isEmpty()) {
            etBuscar.setText(filtros.getQ());
        }
        btnLimpiarBusqueda.setVisibility(etBuscar.getText().length() > 0 ? View.VISIBLE : View.GONE);

        // La "x" solo tiene sentido cuando hay algo escrito.
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnLimpiarBusqueda.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        // Lupa del teclado (imeOptions="actionSearch") y boton de la pantalla.
        etBuscar.setOnEditorActionListener((v, accion, evento) -> {
            if (accion == EditorInfo.IME_ACTION_SEARCH) {
                buscar();
                return true;
            }
            return false;
        });
        btnBuscar.setOnClickListener(v -> buscar());
        btnLimpiarBusqueda.setOnClickListener(v -> {
            etBuscar.setText("");
            buscar();
        });
    }

    /** Aplica el texto escrito. Si es igual a lo que ya esta aplicado, no pide de nuevo. */
    private void buscar() {
        ocultarTeclado();
        String anterior = filtros.getQ();
        filtros.setQ(etBuscar.getText().toString());
        boolean cambio = anterior == null ? filtros.getQ() != null : !anterior.equals(filtros.getQ());
        if (cambio) {
            recargar();
        }
    }

    /** Saca la busqueda y los filtros y vuelve al listado completo. */
    private void limpiarTodo() {
        filtros.limpiarTodo();
        etBuscar.setText("");
        // El panel y el boton "Filtros (n)" muestran lo aplicado: si no se
        // refrescan, el boton sigue diciendo (n) y un "Aplicar" sobre el panel
        // abierto volveria a poner los filtros que se acaban de sacar.
        panelAbierto = false;
        panel.mostrar(false);
        panel.volcar(filtros);
        panel.actualizarBoton(filtros);
        recargar();
    }

    private void ocultarTeclado() {
        InputMethodManager imm = (InputMethodManager)
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(etBuscar.getWindowToken(), 0);
        }
        etBuscar.clearFocus();
    }

    // -----------------------------------------------------------------
    // Orden
    // -----------------------------------------------------------------

    private void configurarOrden() {
        // Las opciones salen del enum: el texto para la persona y el valor
        // para la API viajan juntos.
        List<String> textos = new ArrayList<>();
        for (Orden orden : Orden.values()) {
            textos.add(getString(orden.getTextoRes()));
        }
        ArrayAdapter<String> adapterOrden = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, textos);
        adapterOrden.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spOrden.setAdapter(adapterOrden);
        spOrden.setSelection(Orden.desdeValorApi(filtros.getOrden()).ordinal(), false);

        spOrden.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View vista, int posicion, long id) {
                String elegido = Orden.values()[posicion].getValorApi();
                // El Spinner tambien avisa al inflarse y al seleccionar por
                // codigo: si el orden no cambio, no se pide nada.
                if (elegido.equals(filtros.getOrden())) return;
                filtros.setOrden(elegido);
                recargar();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
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

        // Todo se combina en un solo request; los parametros en null no
        // viajan en la URL.
        llamadaEnCurso = publicacionApi.listar(
                sesion.getBearerOpcional(),
                pagina,
                LIMITE_PAGINA,
                filtros.getQ(),
                filtros.getCategoriaId(),
                filtros.getPrecioMin(),
                filtros.getPrecioMax(),
                filtros.getEstadoArticuloParam(),
                null,   // zonaId
                filtros.getOrden());

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

    /** Dos vacios distintos: "no hay nada" y "no hay nada CON esta busqueda". */
    private void mostrarVacio() {
        if (filtros.hayAlgoAplicado()) {
            tvVacio.setText(R.string.home_vacio_con_filtros);
            btnActualizar.setText(R.string.home_limpiar_todo);
        } else {
            tvVacio.setText(R.string.home_vacio_sin_filtros);
            btnActualizar.setText(R.string.home_actualizar);
        }
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
        if ("RANGO_PRECIO_INVALIDO".equals(codigo)) {
            // La app ya valida minimo <= maximo antes de mandar, asi que verlo
            // significa que el backend tiene una regla extra: se muestra en el
            // campo, con el panel abierto, y la lista queda como estaba.
            panelAbierto = true;
            panel.mostrar(true);
            panel.mostrarErrorPrecioMax(mensaje);
            mostrarCargandoMas(false);
            mostrarEstado(items.isEmpty() ? Estado.VACIO : Estado.LISTA);
            return;
        }
        if ("ESTADO_ARTICULO_INVALIDO".equals(codigo) || "ORDEN_INVALIDO".equals(codigo)
                || "PARAMETRO_INVALIDO".equals(codigo)) {
            // Solo mandamos valores de diccionarios fijos: si ves esto en la
            // demo, el bug es nuestro, no de quien usa la app.
            Log.w(TAG, "Query invalida -> " + codigo + " (" + call.request().url() + ")");
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
        if (estado == Estado.VACIO) {
            mostrarVacio();
        }
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
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(CLAVE_FILTROS, filtros);
        // El LinearLayout no guarda su visibility solo.
        outState.putBoolean(CLAVE_PANEL_ABIERTO, panelAbierto);
    }

    @Override
    public void onDestroyView() {
        // Una respuesta que llegue con la vista destruida no tiene donde pintarse.
        cancelarLlamadaEnCurso();
        if (llamadaCategorias != null) {
            llamadaCategorias.cancel();
            llamadaCategorias = null;
        }
        super.onDestroyView();
    }

    private boolean estaVivo() {
        return isAdded() && getView() != null;
    }
}
