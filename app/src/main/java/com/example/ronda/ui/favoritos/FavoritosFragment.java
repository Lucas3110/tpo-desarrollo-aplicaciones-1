package com.example.ronda.ui.favoritos;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ronda.R;
import com.example.ronda.data.model.BusquedaGuardadaDto;
import com.example.ronda.data.model.PublicacionItemResponse;
import com.example.ronda.data.model.ListaBusquedasGuardadasResponse;
import com.example.ronda.data.model.PaginaPublicacionesResponse;
import com.example.ronda.data.repository.FavoritosRepository;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class FavoritosFragment extends Fragment {

    private enum EstadoUI { CARGANDO, LISTA, VACIO, ERROR }

    @Inject
    FavoritosRepository favoritosRepository;
    
    private Call<PaginaPublicacionesResponse> llamadaFavoritos;
    private Call<ListaBusquedasGuardadasResponse> llamadaBusquedas;
    
    private List<PublicacionItemResponse> favoritos = new ArrayList<>();
    private List<BusquedaGuardadaDto> busquedas = new ArrayList<>();
    
    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private LinearLayout grupoVacio;
    private TextView tvVacio;
    private LinearLayout grupoError;
    private Button btnReintentar;

    private FavoritosAdapter favoritosAdapter;
    private BusquedasGuardadasAdapter busquedasAdapter;

    private int tabActual = 0; // 0 = Favoritos, 1 = Búsquedas

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favoritos, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tabLayout = view.findViewById(R.id.tabLayout);
        recyclerView = view.findViewById(R.id.recyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        grupoVacio = view.findViewById(R.id.grupoVacio);
        tvVacio = view.findViewById(R.id.tvVacio);
        grupoError = view.findViewById(R.id.grupoError);
        btnReintentar = view.findViewById(R.id.btnReintentar);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        favoritosAdapter = new FavoritosAdapter(new FavoritosAdapter.OnItemClickListener() {
            @Override
            public void onPublicacionClick(int id) {
                Bundle args = new Bundle();
                args.putInt("publicacionId", id);
                Navigation.findNavController(view).navigate(R.id.detallePublicacionFragment, args);
            }

            @Override
            public void onFavoritoClick(PublicacionItemResponse item) {
                // Optimistic toggle
                boolean eraFavorito = item.isFavorito();
                item.setEsFavorito(!eraFavorito);
                favoritosAdapter.notifyDataSetChanged();

                if (eraFavorito) {
                    favoritosRepository.quitarFavorito(item.getId()).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                cargarFavoritos();
                            } else {
                                item.setEsFavorito(true);
                                favoritosAdapter.notifyDataSetChanged();
                            }
                        }
                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            item.setEsFavorito(true);
                            favoritosAdapter.notifyDataSetChanged();
                        }
                    });
                } else {
                    favoritosRepository.agregarFavorito(item.getId()).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                cargarFavoritos();
                            } else {
                                item.setEsFavorito(false);
                                favoritosAdapter.notifyDataSetChanged();
                            }
                        }
                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            item.setEsFavorito(false);
                            favoritosAdapter.notifyDataSetChanged();
                        }
                    });
                }
            }
        });

        busquedasAdapter = new BusquedasGuardadasAdapter(item -> {
            NavController navController = Navigation.findNavController(view);
            // El requisito pide aplicar los filtros al volver al Home
            navController.getPreviousBackStackEntry().getSavedStateHandle()
                    .set("busquedaFiltros", item.getFiltros());
            navController.popBackStack();
        });

        recyclerView.setAdapter(favoritosAdapter);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                tabActual = tab.getPosition();
                actualizarLista();
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        btnReintentar.setOnClickListener(v -> actualizarLista());

        cargarFavoritos();
        cargarBusquedas();
    }

    private void actualizarLista() {
        if (tabActual == 0) {
            recyclerView.setAdapter(favoritosAdapter);
            if (favoritos.isEmpty()) {
                cargarFavoritos();
            } else {
                favoritosAdapter.submitList(new ArrayList<>(favoritos));
                mostrarEstado(EstadoUI.LISTA, "");
            }
        } else {
            recyclerView.setAdapter(busquedasAdapter);
            if (busquedas.isEmpty()) {
                cargarBusquedas();
            } else {
                busquedasAdapter.submitList(new ArrayList<>(busquedas));
                mostrarEstado(EstadoUI.LISTA, "");
            }
        }
    }

    private void cargarFavoritos() {
        if (llamadaFavoritos != null) llamadaFavoritos.cancel();
        // El spinner solo si no hay nada para mostrar: con la lista ya en
        // pantalla el refresco pasa por atras.
        if (tabActual == 0 && favoritos.isEmpty()) mostrarEstado(EstadoUI.CARGANDO, "");
        
        llamadaFavoritos = favoritosRepository.listarFavoritos(1, 50);
        llamadaFavoritos.enqueue(new Callback<PaginaPublicacionesResponse>() {
            @Override
            public void onResponse(Call<PaginaPublicacionesResponse> call, Response<PaginaPublicacionesResponse> response) {
                llamadaFavoritos = null;
                if (!isAdded()) return;
                
                if (response.isSuccessful() && response.body() != null) {
                    favoritos = response.body().getItems();
                    if (tabActual == 0) {
                        favoritosAdapter.submitList(new ArrayList<>(favoritos));
                        mostrarEstado(favoritos.isEmpty() ? EstadoUI.VACIO : EstadoUI.LISTA,
                                getString(R.string.favoritos_vacio));
                    }
                } else {
                    falloFavoritos();
                }
            }

            @Override
            public void onFailure(Call<PaginaPublicacionesResponse> call, Throwable t) {
                llamadaFavoritos = null;
                if (!isAdded() || call.isCanceled()) return;
                falloFavoritos();
            }
        });
    }

    private void cargarBusquedas() {
        if (llamadaBusquedas != null) llamadaBusquedas.cancel();
        if (tabActual == 1 && busquedas.isEmpty()) mostrarEstado(EstadoUI.CARGANDO, "");
        
        llamadaBusquedas = favoritosRepository.listarBusquedas();
        llamadaBusquedas.enqueue(new Callback<ListaBusquedasGuardadasResponse>() {
            @Override
            public void onResponse(Call<ListaBusquedasGuardadasResponse> call, Response<ListaBusquedasGuardadasResponse> response) {
                llamadaBusquedas = null;
                if (!isAdded()) return;
                
                if (response.isSuccessful() && response.body() != null) {
                    busquedas = response.body().getBusquedas();
                    if (tabActual == 1) {
                        busquedasAdapter.submitList(new ArrayList<>(busquedas));
                        mostrarEstado(busquedas.isEmpty() ? EstadoUI.VACIO : EstadoUI.LISTA,
                                getString(R.string.favoritos_busquedas_vacio));
                    }
                } else {
                    falloBusquedas();
                }
            }

            @Override
            public void onFailure(Call<ListaBusquedasGuardadasResponse> call, Throwable t) {
                llamadaBusquedas = null;
                if (!isAdded() || call.isCanceled()) return;
                falloBusquedas();
            }
        });
    }

    /**
     * Un refresco que falla no borra una lista que sigue valiendo: se avisa y
     * se deja lo que estaba. Solo cuando no hay nada que mostrar se ocupa la
     * pantalla con el error.
     */
    private void falloFavoritos() {
        if (tabActual != 0) return;
        if (!favoritos.isEmpty()) {
            Toast.makeText(requireContext(), R.string.publicar_error_conexion,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        mostrarEstado(EstadoUI.ERROR, "");
    }

    private void falloBusquedas() {
        if (tabActual != 1) return;
        if (!busquedas.isEmpty()) {
            Toast.makeText(requireContext(), R.string.publicar_error_conexion,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        mostrarEstado(EstadoUI.ERROR, "");
    }

    private void mostrarEstado(EstadoUI estado, String msjVacio) {
        recyclerView.setVisibility(estado == EstadoUI.LISTA ? View.VISIBLE : View.GONE);
        progressBar.setVisibility(estado == EstadoUI.CARGANDO ? View.VISIBLE : View.GONE);
        
        if (estado == EstadoUI.VACIO) {
            grupoVacio.setVisibility(View.VISIBLE);
            tvVacio.setText(msjVacio);
        } else {
            grupoVacio.setVisibility(View.GONE);
        }

        grupoError.setVisibility(estado == EstadoUI.ERROR ? View.VISIBLE : View.GONE);
    }
}
