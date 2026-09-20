package com.example.ronda.data.network;

import com.example.ronda.data.model.CalificacionUnicaResponse;
import com.example.ronda.data.model.CalificarRequest;
import com.example.ronda.data.model.HistorialResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
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
 *   400 TIPO_INVALIDO, FECHA_INVALIDA, RANGO_FECHAS_INVALIDO,
 *       ESTRELLAS_INVALIDAS, COMENTARIO_LARGO
 *   403 NO_SOS_PARTE
 *   404 OPERACION_NO_ENCONTRADA
 *   409 YA_CALIFICADA, PLAZO_VENCIDO
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

    /**
     * Califica a la otra parte de una operacion: estrellas de 1 a 5 y
     * comentario opcional. Solo dentro de los 7 dias y una vez por persona.
     * Responde 201 con la calificacion creada.
     */
    @POST("api/operaciones/{id}/calificacion")
    Call<CalificacionUnicaResponse> calificar(@Path("id") int operacionId,
                                              @Body CalificarRequest cuerpo);
}
