package com.example.ronda.ui.home;

import android.os.Bundle;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ronda.R;
import com.example.ronda.data.model.PublicacionDetalleResponse;
import com.example.ronda.data.model.EntregaResponse;
import com.example.ronda.ui.entrega.EnlaceEntrega;
import com.example.ronda.data.network.ApiErrorParser;
import com.example.ronda.data.model.ErrorResponse;
import com.example.ronda.data.network.PublicacionApiService;
import com.example.ronda.data.repository.CachePublicaciones;
import com.example.ronda.data.repository.SessionRepository;
import com.example.ronda.util.Conectividad;
import com.example.ronda.ui.ofertas.FormatoOferta;


import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class DetallePublicacionFragment extends Fragment {

    @Inject
    PublicacionApiService publicacionApi;

    @Inject
    SessionRepository sesion;

    /** Punto 6: lo que se abrio queda guardado para poder verlo sin red. */
    @Inject
    CachePublicaciones cache;

    @Inject
    Conectividad conectividad;

    @Inject
    com.example.ronda.data.repository.FavoritosRepository favoritosRepository;

    private int publicacionId = -1;
    private PublicacionDetalleResponse.Publicacion mPub;
    private boolean esFavorito = false;
    private Call<PublicacionDetalleResponse> llamadaDetalle;
    private View panelEntrega;
    private TextView tvDireccionEntrega;
    private Button btnComoLlegar;

    private TextView tvSinConexionDetalle;
    private ProgressBar progressBar;
    private ScrollView scrollView;
    private RecyclerView rvFotos;
    private TextView tvEstadoArticulo, tvTitulo, tvPrecio, tvDescripcion, tvVendedorNombre, tvReputacion;
    private Button btnPreguntar, btnOfertar, btnGuardar, btnGestionar, btnVerPerfil;

    public DetallePublicacionFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            publicacionId = getArguments().getInt("publicacionId", -1);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_detalle_publicacion, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressBar = view.findViewById(R.id.progressBar);
        tvSinConexionDetalle = view.findViewById(R.id.tvSinConexionDetalle);
        observarConexion();
        scrollView = view.findViewById(R.id.scrollView);
        
        rvFotos = view.findViewById(R.id.rvFotos);
        rvFotos.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        
        tvEstadoArticulo = view.findViewById(R.id.tvEstadoArticulo);
        tvTitulo = view.findViewById(R.id.tvTitulo);
        tvPrecio = view.findViewById(R.id.tvPrecio);
        tvDescripcion = view.findViewById(R.id.tvDescripcion);
        tvVendedorNombre = view.findViewById(R.id.tvVendedorNombre);
        tvReputacion = view.findViewById(R.id.tvReputacion);
        
        btnPreguntar = view.findViewById(R.id.btnPreguntar);
        btnOfertar = view.findViewById(R.id.btnOfertar);
        btnGuardar = view.findViewById(R.id.btnGuardar);
        btnGestionar = view.findViewById(R.id.btnGestionar);
        btnVerPerfil = view.findViewById(R.id.btnVerPerfil);
        panelEntrega = view.findViewById(R.id.panelEntrega);
        tvDireccionEntrega = view.findViewById(R.id.tvDireccionEntrega);
        btnComoLlegar = view.findViewById(R.id.btnComoLlegar);
        // Antes de salir a Maps, revalidar que el backend siga autorizando la
        // entrega: sin red no se puede, y conviene decirlo en vez de abrir
        // Maps con una direccion que quiza ya no corresponda.
        btnComoLlegar.setOnClickListener(v -> {
            if (hayConexionParaActuar()) cargarDetallePublicacion(true);
        });
        getChildFragmentManager().setFragmentResultListener(
                OfertasBottomSheet.RESULTADO_CERRADO, getViewLifecycleOwner(),
                (key, result) -> cargarDetallePublicacion());
        
        btnPreguntar.setOnClickListener(v -> {
            if (mPub == null || !hayConexionParaActuar()) return;
            boolean esVendedor = mPub.isEsMia();
            boolean puedePreguntar = mPub.getAcciones().isPuedePreguntar();
            PreguntasBottomSheet bottomSheet = PreguntasBottomSheet.newInstance(mPub.getId(), esVendedor, puedePreguntar);
            bottomSheet.show(getChildFragmentManager(), "PreguntasBottomSheet");
        });
        btnOfertar.setOnClickListener(v -> {
            if (mPub == null || !hayConexionParaActuar()) return;
            boolean esVendedor = mPub.isEsMia();
            boolean puedeOfertar = mPub.getAcciones().isPuedeOfertar();
            OfertasBottomSheet bottomSheet = OfertasBottomSheet.newInstance(mPub.getId(), esVendedor, puedeOfertar, mPub.getPrecio());
            bottomSheet.show(getChildFragmentManager(), "OfertasBottomSheet");
        });
        btnGuardar.setOnClickListener(v -> { if (hayConexionParaActuar()) toggleFavorito(); });
        btnGestionar.setOnClickListener(v -> {
            if (mPub == null || !hayConexionParaActuar()) return;
            new android.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.detalle_gestionar)
                .setItems(new CharSequence[]{
                        getString(R.string.detalle_gestionar_ver_preguntas),
                        getString(R.string.detalle_gestionar_ver_ofertas)}, (dialog, which) -> {
                    if (which == 0) {
                        PreguntasBottomSheet bottomSheet = PreguntasBottomSheet.newInstance(mPub.getId(), true, false);
                        bottomSheet.show(getChildFragmentManager(), "PreguntasBottomSheet");
                    } else {
                        OfertasBottomSheet bottomSheet = OfertasBottomSheet.newInstance(mPub.getId(), true, false, mPub.getPrecio());
                        bottomSheet.show(getChildFragmentManager(), "OfertasBottomSheet");
                    }
                })
                .show();
        });
        btnVerPerfil.setOnClickListener(v -> {
            if (mPub == null || mPub.getVendedor() == null) return;
            Bundle args = new Bundle();
            args.putInt("usuarioId", mPub.getVendedor().getId());
            Navigation.findNavController(requireView()).navigate(R.id.action_detalle_to_perfilPublico, args);
        });

        if (publicacionId == -1) {
            Toast.makeText(requireContext(), getString(R.string.error_publicacion_id), Toast.LENGTH_SHORT).show();
        }
    }


    // -----------------------------------------------------------------
    // Punto 6: modo sin conexion
    // -----------------------------------------------------------------

    /**
     * No se pudo llegar al servidor: si esta publicacion se abrio alguna vez
     * con conexion, se muestra la copia guardada.
     *
     * Si nunca se abrio, no hay nada: el listado guarda un resumen, pero la
     * descripcion, la galeria y los datos del vendedor solo llegan al pedir
     * el detalle. Se avisa con ese texto para que la persona entienda por que
     * unas publicaciones se ven sin red y otras no.
     */
    private void mostrarDesdeCache() {
        cache.detalleGuardado(publicacionId, guardado -> {
            if (!estaVivo()) return;

            if (guardado == null) {
                // Sin copia guardada no hay nada que dibujar. Se vuelve al
                // listado en vez de dejar una pantalla en blanco: el aviso
                // explica por que, y la persona queda donde puede seguir.
                Toast.makeText(requireContext(),
                        R.string.sin_conexion_detalle_no_guardado, Toast.LENGTH_LONG).show();
                Navigation.findNavController(requireView()).popBackStack();
                return;
            }

            poblarUi(guardado.publicacion);
            mostrarAvisoSinConexion();
        });
    }

    private void mostrarAvisoSinConexion() {
        if (tvSinConexionDetalle == null) return;
        tvSinConexionDetalle.setText(R.string.sin_conexion_aviso);
        tvSinConexionDetalle.setVisibility(View.VISIBLE);
    }

    /**
     * "Las acciones que requieren conexion quedan deshabilitadas mientras no
     * haya conectividad, mostrando un mensaje claro."
     *
     * Se chequea al tocar y no al dibujar la pantalla a proposito: la
     * conexion puede irse en cualquier momento, y un boton que se ve
     * habilitado pero avisa al tocarlo es mas claro que uno que aparece y
     * desaparece solo mientras la persona lo esta mirando.
     */
    private boolean hayConexionParaActuar() {
        if (conectividad.hayInternet()) return true;
        Toast.makeText(requireContext(), R.string.sin_conexion_accion, Toast.LENGTH_SHORT).show();
        return false;
    }

    /**
     * Cuando vuelve la conexion se recarga el detalle solo, para que deje de
     * verse la copia guardada y pase a verse la de verdad.
     */
    private void observarConexion() {
        conectividad.getEstado().observe(getViewLifecycleOwner(), hayInternet -> {
            if (!estaVivo()) return;
            boolean hay = Boolean.TRUE.equals(hayInternet);
            atenuarAccionesSinConexion(hay);

            if (!hay) {
                // Lo que se esta viendo puede ser la copia guardada: se avisa
                // aunque el detalle haya entrado con red y se haya caido despues.
                if (mPub != null) mostrarAvisoSinConexion();
                return;
            }

            if (tvSinConexionDetalle == null
                    || tvSinConexionDetalle.getVisibility() != View.VISIBLE) return;
            if (publicacionId == -1) return;

            Toast.makeText(requireContext(), R.string.sin_conexion_volvio, Toast.LENGTH_SHORT).show();
            cargarDetallePublicacion();
        });
    }

    /**
     * Los botones que necesitan red se ven apagados mientras no la hay.
     *
     * Siguen respondiendo al toque a proposito: asi se puede explicar por que
     * no se puede, que es la otra mitad de lo que pide el enunciado. Un boton
     * que directamente no reacciona deja a la persona sin saber si la app se
     * colgo o si le falta conexion.
     */
    private void atenuarAccionesSinConexion(boolean hayInternet) {
        float opacidad = hayInternet ? 1f : 0.4f;
        for (Button boton : new Button[]{btnPreguntar, btnOfertar, btnGuardar, btnGestionar,
                btnComoLlegar}) {
            if (boton != null) boton.setAlpha(opacidad);
        }
    }
    private void cargarDetallePublicacion() {
        cargarDetallePublicacion(false);
    }

    @Override public void onResume() {
        super.onResume();
        if (publicacionId != -1) cargarDetallePublicacion();
    }

    private void cargarDetallePublicacion(boolean abrirMapa) {
        if (!estaVivo() || publicacionId == -1) return;
        if (llamadaDetalle != null) llamadaDetalle.cancel();
        mPub = null;
        panelEntrega.setVisibility(View.GONE);
        tvDireccionEntrega.setText("");
        mostrarCargando(true);
        llamadaDetalle = publicacionApi.getDetallePublicacion(sesion.getBearer(), publicacionId);
        llamadaDetalle.enqueue(new Callback<PublicacionDetalleResponse>() {
            @Override
            public void onResponse(@NonNull Call<PublicacionDetalleResponse> call, @NonNull Response<PublicacionDetalleResponse> response) {
                if (!estaVivo() || call.isCanceled() || call != llamadaDetalle) return;
                mostrarCargando(false);

                if (response.isSuccessful() && response.body() != null
                        && response.body().getPublicacion() != null) {
                    // Punto 6: queda guardado para poder abrirlo sin conexion.
                    cache.guardarDetalle(response.body().getPublicacion());
                    if (tvSinConexionDetalle != null) tvSinConexionDetalle.setVisibility(View.GONE);
                    poblarUi(response.body().getPublicacion());
                    if (abrirMapa) abrirMapa(mPub.getEntrega());
                } else {
                    ErrorResponse.Detalle error = ApiErrorParser.parse(response);
                    String mensaje = ApiErrorParser.mensaje(error, getString(R.string.detalle_error_carga));
                    Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(@NonNull Call<PublicacionDetalleResponse> call, @NonNull Throwable t) {
                if (!estaVivo() || call.isCanceled() || call != llamadaDetalle) return;
                mostrarCargando(false);
                // Punto 6: antes de dar error, probamos con lo guardado.
                mostrarDesdeCache();
            }
        });
    }

    private void poblarUi(PublicacionDetalleResponse.Publicacion pub) {
        this.mPub = pub;
        scrollView.setVisibility(View.VISIBLE);
        EntregaResponse entrega = pub.getEntrega();
        panelEntrega.setVisibility(entrega == null ? View.GONE : View.VISIBLE);
        btnComoLlegar.setVisibility(entrega != null && entrega.tieneDestino() ? View.VISIBLE : View.GONE);
        if (entrega != null) {
            tvDireccionEntrega.setText(!entrega.getDireccion().isEmpty() ? entrega.getDireccion()
                    : entrega.tieneCoordenadas() ? getString(R.string.entrega_coordenadas, entrega.getDestino())
                    : getString(R.string.entrega_sin_direccion));
        }

        tvTitulo.setText(pub.getTitulo());
        // Mismo formato de precio que el listado del Home y las ofertas:
        // "$ 95.000" en vez de "$ 95000.00".
        tvPrecio.setText(FormatoOferta.precio(pub.getPrecio()));
        tvDescripcion.setText(pub.getDescripcion());
        
        if (pub.getFotos() != null && !pub.getFotos().isEmpty()) {
            FotosAdapter adapter = new FotosAdapter(pub.getFotos());
            rvFotos.setAdapter(adapter);
            rvFotos.setVisibility(View.VISIBLE);
        } else {
            rvFotos.setVisibility(View.GONE);
        }

        String fechaSimple = pub.getPublicadoEn() != null ? pub.getPublicadoEn().split("T")[0] : "";
        String cat = pub.getCategoria() != null ? pub.getCategoria().getNombre() : "";
        tvEstadoArticulo.setText(cat + " | " + pub.getEstadoArticuloTexto() + " | " + fechaSimple);

        if (pub.getVendedor() != null) {
            // La zona del vendedor es opcional: quien se registro sin elegirla la
            // recibe en null y encadenar getNombre() reventaba el detalle.
            String zonaVendedor = pub.getVendedor().getZona() != null
                    ? " - " + pub.getVendedor().getZona().getNombre()
                    : "";
            tvVendedorNombre.setText(pub.getVendedor().getNombre() + zonaVendedor);
            
            PublicacionDetalleResponse.Reputacion rep = pub.getVendedor().getReputacion();
            if (rep != null && rep.getPromedioEstrellas() != null) {
                // getString con argumentos: el recurso tiene %1$.1f y %2$d, asi que
                // String.format sobre el texto ya resuelto rompia en tiempo de ejecucion.
                tvReputacion.setText(getString(R.string.reputacion_formato,
                        rep.getPromedioEstrellas(), rep.getCantidadCalificaciones()));
            } else {
                tvReputacion.setText(getString(R.string.reputacion_vacia));
            }
        }

        if (mPub.getAcciones() != null) {
            // Mostrar siempre los botones para permitir abrir los historiales.
            btnPreguntar.setVisibility(pub.getAcciones().isPuedePreguntar() ? View.VISIBLE : View.GONE);
            // Ofertas requiere estar autenticado (el backend rechaza listarOfertas si no hay token).
            btnOfertar.setVisibility(mPub.getAcciones().isPuedeOfertar() ? View.VISIBLE : View.GONE);
            btnGuardar.setVisibility(mPub.getAcciones().isPuedeGuardar() ? View.VISIBLE : View.GONE);
            btnGestionar.setVisibility(mPub.getAcciones().isPuedeGestionar() ? View.VISIBLE : View.GONE);
            
            if (mPub.isEsMia()) {
                btnGuardar.setVisibility(View.GONE);
            } else {
                btnGuardar.setVisibility(View.VISIBLE);
                esFavorito = mPub.isEsFavorito();
                btnGuardar.setText(esFavorito ? R.string.detalle_quitar_favorito : R.string.detalle_guardar_favorito);
                if (btnGuardar instanceof com.google.android.material.button.MaterialButton) {
                    ((com.google.android.material.button.MaterialButton) btnGuardar)
                            .setIconResource(esFavorito ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
                }
            }
        }
    }
    
    private void toggleFavorito() {
        if (!sesion.haySesion()) {
            Navigation.findNavController(requireView()).navigate(R.id.action_home_to_auth);
            return;
        }
        if (esFavorito) {
            favoritosRepository.quitarFavorito(publicacionId).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        esFavorito = false;
                        btnGuardar.setText(R.string.detalle_guardar_favorito);
                        if (btnGuardar instanceof com.google.android.material.button.MaterialButton) {
                            ((com.google.android.material.button.MaterialButton) btnGuardar).setIconResource(R.drawable.ic_favorite_border);
                        }
                        Toast.makeText(requireContext(), getString(R.string.accion_quitar_guardar), Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    if (estaVivo() && !call.isCanceled()) {
                        Toast.makeText(requireContext(),
                                R.string.publicar_error_conexion, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            favoritosRepository.agregarFavorito(publicacionId).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        esFavorito = true;
                        btnGuardar.setText(R.string.detalle_quitar_favorito);
                        if (btnGuardar instanceof com.google.android.material.button.MaterialButton) {
                            ((com.google.android.material.button.MaterialButton) btnGuardar).setIconResource(R.drawable.ic_favorite);
                        }
                        Toast.makeText(requireContext(), getString(R.string.accion_guardar), Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    if (estaVivo() && !call.isCanceled()) {
                        Toast.makeText(requireContext(),
                                R.string.publicar_error_conexion, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void mostrarCargando(boolean cargando) {
        progressBar.setVisibility(cargando ? View.VISIBLE : View.GONE);
        if (cargando) scrollView.setVisibility(View.GONE);
    }

    private boolean estaVivo() {
        return isAdded() && getView() != null;
    }

    private void abrirMapa(EntregaResponse entrega) {
        String enlace = EnlaceEntrega.crear(entrega);
        if (enlace == null) {
            Toast.makeText(requireContext(), R.string.entrega_no_disponible, Toast.LENGTH_LONG).show();
            return;
        }
        // Google Maps maneja esta URL si está instalado; de lo contrario abre el navegador.
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(enlace)));
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(requireContext(), R.string.entrega_sin_maps, Toast.LENGTH_LONG).show();
        }
    }

    @Override public void onDestroyView() {
        if (llamadaDetalle != null) llamadaDetalle.cancel();
        mPub = null;
        super.onDestroyView();
    }
}
