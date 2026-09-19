package com.example.ronda.ui.favoritos;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ronda.R;
import com.example.ronda.data.model.BusquedaGuardadaDto;
import com.example.ronda.data.model.PublicacionItemResponse;
import com.google.android.material.tabs.TabLayout;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class FavoritosFragment extends Fragment {

    private FavoritosViewModel viewModel;
    
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
        viewModel = new ViewModelProvider(this).get(FavoritosViewModel.class);

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
                viewModel.toggleFavorito(item);
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

        observarViewModel();
        
        viewModel.cargarFavoritos();
        viewModel.cargarBusquedas();
    }

    private void actualizarLista() {
        if (tabActual == 0) {
            recyclerView.setAdapter(favoritosAdapter);
            viewModel.cargarFavoritos();
        } else {
            recyclerView.setAdapter(busquedasAdapter);
            viewModel.cargarBusquedas();
        }
    }

    private void observarViewModel() {
        viewModel.getFavoritos().observe(getViewLifecycleOwner(), items -> {
            if (tabActual == 0) favoritosAdapter.submitList(items);
        });

        viewModel.getEstadoFavoritos().observe(getViewLifecycleOwner(), estado -> {
            if (tabActual == 0) mostrarEstado(estado, "No tenés favoritos guardados");
        });

        viewModel.getBusquedas().observe(getViewLifecycleOwner(), items -> {
            if (tabActual == 1) busquedasAdapter.submitList(items);
        });

        viewModel.getEstadoBusquedas().observe(getViewLifecycleOwner(), estado -> {
            if (tabActual == 1) mostrarEstado(estado, "No tenés búsquedas guardadas");
        });
    }

    private void mostrarEstado(FavoritosViewModel.EstadoUI estado, String msjVacio) {
        recyclerView.setVisibility(estado == FavoritosViewModel.EstadoUI.LISTA ? View.VISIBLE : View.GONE);
        progressBar.setVisibility(estado == FavoritosViewModel.EstadoUI.CARGANDO ? View.VISIBLE : View.GONE);
        
        if (estado == FavoritosViewModel.EstadoUI.VACIO) {
            grupoVacio.setVisibility(View.VISIBLE);
            tvVacio.setText(msjVacio);
        } else {
            grupoVacio.setVisibility(View.GONE);
        }

        grupoError.setVisibility(estado == FavoritosViewModel.EstadoUI.ERROR ? View.VISIBLE : View.GONE);
    }
}
