package com.example.ronda.ui.home; 

import android.os.Bundle;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ronda.R;
import com.example.ronda.data.model.PublicacionDetalleResponse;
import com.example.ronda.data.network.ApiErrorParser;
import com.example.ronda.data.model.ErrorResponse;
import com.example.ronda.data.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DetallePublicacionFragment extends Fragment {

    private int publicacionId = -1;

    private ProgressBar progressBar;
    private ScrollView scrollView;
    private RecyclerView rvFotos;
    private TextView tvEstadoArticulo, tvTitulo, tvPrecio, tvDescripcion, tvVendedorNombre, tvReputacion;
    private Button btnPreguntar, btnOfertar, btnGuardar, btnGestionar, btnVerPerfil;

    public DetallePublicacionFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            publicacionId = getArguments().getInt("publicacionId", -1);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_detalle_publicacion, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressBar = view.findViewById(R.id.progressBar);
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

        
        btnPreguntar.setOnClickListener(v -> Toast.makeText(requireContext(), "Abrir chat de preguntas...", Toast.LENGTH_SHORT).show());
        btnOfertar.setOnClickListener(v -> Toast.makeText(requireContext(), "Abrir flujo de oferta...", Toast.LENGTH_SHORT).show());
        btnGuardar.setOnClickListener(v -> Toast.makeText(requireContext(), "Guardado en favoritos!", Toast.LENGTH_SHORT).show());
        btnGestionar.setOnClickListener(v -> Toast.makeText(requireContext(), "Abrir gestiA3n de publicaciA3n...", Toast.LENGTH_SHORT).show());
        btnVerPerfil.setOnClickListener(v -> Toast.makeText(requireContext(), "Ver perfil pAoblico...", Toast.LENGTH_SHORT).show());

        if (publicacionId != -1) {
            cargarDetallePublicacion();
        } else {
            Toast.makeText(requireContext(), "Falta el ID de la publicación", Toast.LENGTH_SHORT).show();
        }
    }

    private void cargarDetallePublicacion() {
        mostrarCargando(true);

        RetrofitClient.getPublicacionApi().getDetallePublicacion(new com.example.ronda.data.repository.SessionRepository(requireContext()).getBearer(), publicacionId)
                .enqueue(new Callback<PublicacionDetalleResponse>() {
                    
                    @Override
                    public void onResponse(@NonNull Call<PublicacionDetalleResponse> call,
                                           @NonNull Response<PublicacionDetalleResponse> response) {
                        if (!estaVivo()) return;
                        mostrarCargando(false);

                        if (response.isSuccessful() && response.body() != null) {
                            poblarUi(response.body().getPublicacion());
                        } else {
                            ErrorResponse.Detalle error = ApiErrorParser.parse(response);
                            String mensaje = ApiErrorParser.mensaje(error, getString(R.string.error_generico));
                            Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<PublicacionDetalleResponse> call, @NonNull Throwable t) {
                        if (!estaVivo()) return;
                        mostrarCargando(false);
                        Toast.makeText(requireContext(), R.string.error_sin_conexion, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void poblarUi(PublicacionDetalleResponse.Publicacion pub) {
        scrollView.setVisibility(View.VISIBLE);

        tvTitulo.setText(pub.getTitulo());
        tvPrecio.setText("$ " + pub.getPrecio());
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
                tvReputacion.setText(String.format("Reputación: %.1f estrellas (%d operaciones)", 
                        rep.getPromedioEstrellas(), rep.getCantidadCalificaciones()));
            } else {
                tvReputacion.setText("Aún no tiene calificaciones");
            }
        }

        if (pub.getAcciones() != null) {
            btnPreguntar.setVisibility(pub.getAcciones().isPuedePreguntar() ? View.VISIBLE : View.GONE);
            btnOfertar.setVisibility(pub.getAcciones().isPuedeOfertar() ? View.VISIBLE : View.GONE);
            btnGuardar.setVisibility(pub.getAcciones().isPuedeGuardar() ? View.VISIBLE : View.GONE);
            btnGestionar.setVisibility(pub.getAcciones().isPuedeGestionar() ? View.VISIBLE : View.GONE);
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




