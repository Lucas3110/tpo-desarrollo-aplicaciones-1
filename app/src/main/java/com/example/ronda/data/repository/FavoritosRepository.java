package com.example.ronda.data.repository;

import com.example.ronda.data.model.GuardarBusquedaRequest;
import com.example.ronda.data.model.ListaBusquedasGuardadasResponse;
import com.example.ronda.data.model.PaginaPublicacionesResponse;
import com.example.ronda.data.network.FavoritosApiService;
import com.example.ronda.data.network.PublicacionApiService;
import com.example.ronda.data.repository.SessionRepository;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;

@Singleton
public class FavoritosRepository {

    private final FavoritosApiService apiService;
    private final PublicacionApiService publicacionApiService;
    private final SessionRepository sessionRepository;

    @Inject
    public FavoritosRepository(FavoritosApiService apiService, PublicacionApiService publicacionApiService, SessionRepository sessionRepository) {
        this.apiService = apiService;
        this.publicacionApiService = publicacionApiService;
        this.sessionRepository = sessionRepository;
    }

    private String getAuthToken() {
        return "Bearer " + sessionRepository.getToken();
    }

    public Call<PaginaPublicacionesResponse> listarFavoritos(Integer pagina, Integer limite) {
        return apiService.listarFavoritos(getAuthToken(), pagina, limite);
    }

    public Call<Void> agregarFavorito(int publicacionId) {
        return publicacionApiService.agregarFavorito(getAuthToken(), publicacionId);
    }

    public Call<Void> quitarFavorito(int publicacionId) {
        return publicacionApiService.quitarFavorito(getAuthToken(), publicacionId);
    }

    public Call<ListaBusquedasGuardadasResponse> listarBusquedas() {
        return apiService.listarBusquedas(getAuthToken());
    }

    public Call<Void> guardarBusqueda(GuardarBusquedaRequest request) {
        return apiService.guardarBusqueda(getAuthToken(), request);
    }

    public Call<PaginaPublicacionesResponse> resultadosBusqueda(int id, Integer pagina, Integer limite) {
        return apiService.resultadosBusqueda(getAuthToken(), id, pagina, limite);
    }

    public Call<Void> eliminarBusqueda(int id) {
        return apiService.eliminarBusqueda(getAuthToken(), id);
    }
}
