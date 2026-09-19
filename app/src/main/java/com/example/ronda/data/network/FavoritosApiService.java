package com.example.ronda.data.network;

import com.example.ronda.data.model.GuardarBusquedaRequest;
import com.example.ronda.data.model.ListaBusquedasGuardadasResponse;
import com.example.ronda.data.model.PaginaPublicacionesResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface FavoritosApiService {

    @GET("api/favoritos")
    Call<PaginaPublicacionesResponse> listarFavoritos(
            @Header("Authorization") String token,
            @Query("pagina") Integer pagina,
            @Query("limite") Integer limite
    );

    @GET("api/busquedas-guardadas")
    Call<ListaBusquedasGuardadasResponse> listarBusquedas(@Header("Authorization") String token);

    @POST("api/busquedas-guardadas")
    Call<Void> guardarBusqueda(
            @Header("Authorization") String token,
            @Body GuardarBusquedaRequest request
    );

    // Al pedir los resultados, el backend marca la búsqueda como vista
    @GET("api/busquedas-guardadas/{id}/resultados")
    Call<PaginaPublicacionesResponse> resultadosBusqueda(
            @Header("Authorization") String token,
            @Path("id") int id,
            @Query("pagina") Integer pagina,
            @Query("limite") Integer limite
    );

    @DELETE("api/busquedas-guardadas/{id}")
    Call<Void> eliminarBusqueda(
            @Header("Authorization") String token,
            @Path("id") int id
    );
}
