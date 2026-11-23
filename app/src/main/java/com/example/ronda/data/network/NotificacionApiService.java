package com.example.ronda.data.network;

import com.example.ronda.data.model.NovedadesResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Avisos del Punto 11 por long polling. El backend retiene el pedido hasta
 * que hay un aviso nuevo (200) o pasan `espera` segundos (204, sin cuerpo).
 * Usa un cliente de OkHttp aparte con timeout mas largo (ver NetworkModule).
 * Sin @Header: el token lo pone el interceptor de JWT.
 */
public interface NotificacionApiService {

    /** Segundos que el backend retiene cada pedido. */
    int ESPERA_SEGUNDOS = 25;

    /**
     * Sin desdeId devuelve solo el cursor (ultimoId). Con desdeId espera avisos
     * con id mayor. Con 204 el body es null.
     */
    @GET("api/notificaciones")
    Call<NovedadesResponse> novedades(
            @Query("desdeId") Integer desdeId,
            @Query("espera") Integer espera
    );
}
