package com.example.ronda.ui.home;

import android.os.Bundle;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
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
import com.example.ronda.data.network.ApiErrorParser;
import com.example.ronda.data.model.ErrorResponse;
import com.example.ronda.data.network.PublicacionApiService;
import com.example.ronda.data.repository.CachePublicaciones;
import com.example.ronda.data.repository.SessionRepository;
import com.example.ronda.util.Conectividad;
import com.example.ronda.ui.ofertas.FormatoOferta;

import java.util.Date;

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

    private int publicacionId = -1;
    private PublicacionDetalleResponse.Publicacion mPub;
    private boolean esFavorito = false;

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
        btnVerPerfil.setOnClickListener(v -> Toast.makeText(requireContext(), getString(R.string.accion_perfil), Toast.LENGTH_SHORT).show());

        if (publicacionId != -1) {
            cargarDetallePublicacion();
        } else {
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
            mostrarAvisoSinConexion(guardado.guardadoEn);
        });
    }

    private void mostrarAvisoSinConexion(long guardadoEn) {
        if (tvSinConexionDetalle == null) return;

        CharSequence cuando;
        int plantilla;
        if (DateUtils.isToday(guardadoEn)) {
            cuando = DateFormat.getTimeFormat(requireContext()).format(new Date(guardadoEn));
            plantilla = R.string.sin_conexion_datos_de;
        } else {
            cuando = DateUtils.getRelativeTimeSpanString(guardadoEn);
            plantilla = R.string.sin_conexion_datos_de_fecha;
        }
        tvSinConexionDetalle.setText(getString(plantilla, cuando));
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
            if (!Boolean.TRUE.equals(hayInternet) || !estaVivo()) return;
            if (tvSinConexionDetalle == null
                    || tvSinConexionDetalle.getVisibility() != View.VISIBLE) return;
            if (publicacionId == -1) return;

            cargarDetallePublicacion();
        });
    }
    private void cargarDetallePublicacion() {
        mostrarCargando(true);
        publicacionApi.getDetallePublicacion(sesion.getBearer(), publicacionId).enqueue(new Callback<PublicacionDetalleResponse>() {
            @Override
            public void onResponse(@NonNull Call<PublicacionDetalleResponse> call, @NonNull Response<PublicacionDetalleResponse> response) {
                if (!estaVivo()) return;
                mostrarCargando(false);

                if (response.isSuccessful() && response.body() != null) {
                    // Punto 6: queda guardado para poder abrirlo sin conexion.
                    cache.guardarDetalle(response.body().getPublicacion());
                    if (tvSinConexionDetalle != null) tvSinConexionDetalle.setVisibility(View.GONE);
                    poblarUi(response.body().getPublicacion());
                } else {
                    ErrorResponse.Detalle error = ApiErrorParser.parse(response);
                    String mensaje = ApiErrorParser.mensaje(error, getString(R.string.detalle_error_carga));
                    Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(@NonNull Call<PublicacionDetalleResponse> call, @NonNull Throwable t) {
                if (!estaVivo()) return;
                mostrarCargando(false);
                // Punto 6: antes de dar error, probamos con lo guardado.
                mostrarDesdeCache();
            }
        });
    }

    private void poblarUi(PublicacionDetalleResponse.Publicacion pub) {
        this.mPub = pub;
        scrollView.setVisibility(View.VISIBLE);

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
            tvVendedorNombre.setText(pub.getVendedor().getNombre() + " - " + pub.getVendedor().getZona().getNombre());
            
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
            
            esFavorito = mPub.isEsFavorito();
            btnGuardar.setText(esFavorito ? R.string.detalle_quitar_favorito : R.string.detalle_guardar_favorito);
        }
    }
    
    private void toggleFavorito() {
        if (esFavorito) {
            publicacionApi.quitarFavorito(sesion.getBearer(), publicacionId).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        esFavorito = false;
                        btnGuardar.setText(R.string.detalle_guardar_favorito);
                        Toast.makeText(requireContext(), getString(R.string.accion_quitar_guardar), Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<Void> call, Throwable t) {}
            });
        } else {
            publicacionApi.agregarFavorito(sesion.getBearer(), publicacionId).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        esFavorito = true;
                        btnGuardar.setText(R.string.detalle_quitar_favorito);
                        Toast.makeText(requireContext(), getString(R.string.accion_guardar), Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<Void> call, Throwable t) {}
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
}
