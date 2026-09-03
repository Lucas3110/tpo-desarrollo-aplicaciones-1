package com.example.ronda.data.network;

import com.example.ronda.data.model.PublicacionDetalleResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface PublicacionApiService {
        @GET("api/publicaciones/{id}")
    Call<PublicacionDetalleResponse> getDetallePublicacion(@retrofit2.http.Header("Authorization") String token, @Path("id") int id);
}


