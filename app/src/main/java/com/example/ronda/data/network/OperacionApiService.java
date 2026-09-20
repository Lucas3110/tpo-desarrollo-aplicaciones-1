package com.example.ronda.data.network;

import com.example.ronda.data.model.HistorialResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Historial de operaciones y calificaciones (Punto 9). Todos exigen sesion.
 *
 * Como OfertaApiService, ningun metodo recibe la cabecera Authorization: la
 * agrega el interceptor de OkHttp de NetworkModule. Un 401 sin sesion lo trata
 * la pantalla (vuelve al login).
 *
 * Los @Query en null no se mandan: null significa "sin ese filtro".
 *
 * Errores propios (siempre { error: { codigo, mensaje } }):
 *   400 TIPO_INVALIDO, FECHA_INVALIDA, RANGO_FECHAS_INVALIDO
 */
public interface OperacionApiService {

    /**
     * Historial separado en compras y ventas.
     *
     * @param tipo  COMPRA o VENTA (null = las dos)
     * @param desde AAAA-MM-DD, incluida (null = sin limite)
     * @param hasta AAAA-MM-DD, incluida hasta el final del dia (null = sin limite)
     */
    @GET("api/operaciones")
    Call<HistorialResponse> historial(@Query("tipo") String tipo,
                                      @Query("desde") String desde,
                                      @Query("hasta") String hasta);
}
