import os
file_path = r'app\src\main\java\com\example\ronda\data\network\PublicacionApiService.java'

content = '''package com.example.ronda.data.network;

import com.example.ronda.data.model.CategoriasResponse;
import com.example.ronda.data.model.PaginaPublicacionesResponse;
import com.example.ronda.data.model.ZonasResponse;
import com.example.ronda.data.model.PublicacionDetalleResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface PublicacionApiService {

    @GET("api/publicaciones")
    Call<PaginaPublicacionesResponse> listar(
            @Header("Authorization") String bearer,
            @Query("pagina") Integer pagina,
            @Query("limite") Integer limite,
            @Query("q") String q,
            @Query("categoriaId") Integer categoriaId,
            @Query("precioMin") Double precioMin,
            @Query("precioMax") Double precioMax,
            @Query("estadoArticulo") String estadoArticulo,
            @Query("zonaId") Integer zonaId,
            @Query("orden") String orden);

    @GET("api/categorias")
    Call<CategoriasResponse> categorias();

    @GET("api/zonas")
    Call<ZonasResponse> zonas();

    @GET("api/publicaciones/{id}")
    Call<PublicacionDetalleResponse> getDetallePublicacion(@Header("Authorization") String token, @Path("id") int id);
}
'''

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
