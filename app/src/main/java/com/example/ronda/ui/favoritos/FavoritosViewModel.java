package com.example.ronda.ui.favoritos;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.ronda.data.model.BusquedaGuardadaDto;
import com.example.ronda.data.model.ListaBusquedasGuardadasResponse;
import com.example.ronda.data.model.PaginaPublicacionesResponse;
import com.example.ronda.data.model.PublicacionItemResponse;
import com.example.ronda.data.repository.FavoritosRepository;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@HiltViewModel
public class FavoritosViewModel extends ViewModel {

    public enum EstadoUI {
        CARGANDO,
        LISTA,
        VACIO,
        ERROR
    }

    private final FavoritosRepository repository;

    private final MutableLiveData<EstadoUI> estadoFavoritos = new MutableLiveData<>(EstadoUI.CARGANDO);
    private final MutableLiveData<List<PublicacionItemResponse>> favoritos = new MutableLiveData<>();

    private final MutableLiveData<EstadoUI> estadoBusquedas = new MutableLiveData<>(EstadoUI.CARGANDO);
    private final MutableLiveData<List<BusquedaGuardadaDto>> busquedas = new MutableLiveData<>();

    @Inject
    public FavoritosViewModel(FavoritosRepository repository) {
        this.repository = repository;
    }

    public LiveData<EstadoUI> getEstadoFavoritos() { return estadoFavoritos; }
    public LiveData<List<PublicacionItemResponse>> getFavoritos() { return favoritos; }

    public LiveData<EstadoUI> getEstadoBusquedas() { return estadoBusquedas; }
    public LiveData<List<BusquedaGuardadaDto>> getBusquedas() { return busquedas; }

    public void cargarFavoritos() {
        estadoFavoritos.setValue(EstadoUI.CARGANDO);
        repository.listarFavoritos(1, 50).enqueue(new Callback<PaginaPublicacionesResponse>() {
            @Override
            public void onResponse(Call<PaginaPublicacionesResponse> call, Response<PaginaPublicacionesResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<PublicacionItemResponse> items = response.body().getItems();
                    if (items.isEmpty()) {
                        estadoFavoritos.setValue(EstadoUI.VACIO);
                    } else {
                        favoritos.setValue(items);
                        estadoFavoritos.setValue(EstadoUI.LISTA);
                    }
                } else {
                    estadoFavoritos.setValue(EstadoUI.ERROR);
                }
            }

            @Override
            public void onFailure(Call<PaginaPublicacionesResponse> call, Throwable t) {
                estadoFavoritos.setValue(EstadoUI.ERROR);
            }
        });
    }

    public void cargarBusquedas() {
        estadoBusquedas.setValue(EstadoUI.CARGANDO);
        repository.listarBusquedas().enqueue(new Callback<ListaBusquedasGuardadasResponse>() {
            @Override
            public void onResponse(Call<ListaBusquedasGuardadasResponse> call, Response<ListaBusquedasGuardadasResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<BusquedaGuardadaDto> items = response.body().getBusquedas();
                    if (items.isEmpty()) {
                        estadoBusquedas.setValue(EstadoUI.VACIO);
                    } else {
                        busquedas.setValue(items);
                        estadoBusquedas.setValue(EstadoUI.LISTA);
                    }
                } else {
                    estadoBusquedas.setValue(EstadoUI.ERROR);
                }
            }

            @Override
            public void onFailure(Call<ListaBusquedasGuardadasResponse> call, Throwable t) {
                estadoBusquedas.setValue(EstadoUI.ERROR);
            }
        });
    }

    public void toggleFavorito(PublicacionItemResponse item) {
        if (item.isFavorito()) {
            repository.quitarFavorito(item.getId()).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        cargarFavoritos(); // Recargar la lista
                    }
                }
                @Override
                public void onFailure(Call<Void> call, Throwable t) {}
            });
        } else {
            repository.agregarFavorito(item.getId()).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        cargarFavoritos();
                    }
                }
                @Override
                public void onFailure(Call<Void> call, Throwable t) {}
            });
        }
    }
}
