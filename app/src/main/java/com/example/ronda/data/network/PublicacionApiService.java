package com.example.ronda.data.network;

import com.example.ronda.data.model.CategoriasResponse;
import com.example.ronda.data.model.PaginaPublicacionesResponse;
import com.example.ronda.data.model.ZonasResponse;
import com.example.ronda.data.model.BorradorResponse;
import com.example.ronda.data.model.CambiarEstadoRequest;
import com.example.ronda.data.model.GuardarBorradorRequest;
import com.example.ronda.data.model.MisPublicacionesResponse;
import com.example.ronda.data.model.PublicacionRequest;
import com.example.ronda.data.model.PublicacionResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Endpoints del Punto 3 (explorar publicaciones).
 *
 * Todos son GET con parametros en la URL, asi que se usa @Query en vez de
 * @Body. Regla de Retrofit que hace todo mas simple: si un @Query o un
 * @Header vale null, NO se manda. Por eso los parametros son Integer/Double
 * y no int/double: null significa "sin ese filtro".
 */
public interface PublicacionApiService {

    /**
     * Listado paginado con buscador, filtros combinables y orden.
     *
     * @param bearer         "Bearer <token>" o null. Es opcional: sin token el
     *                       listado funciona igual, pero con token viene
     *                       esFavorito en cada item y se puede pedir orden=cercania.
     * @param pagina         empieza en 1
     * @param limite         tope 50 (el backend lo recorta), default 20
     * @param q              texto libre, busca en titulo y descripcion
     * @param categoriaId    id de categoria
     * @param precioMin      precio minimo, >= 0
     * @param precioMax      precio maximo, >= precioMin o el backend responde
     *                       RANGO_PRECIO_INVALIDO
     * @param estadoArticulo NUEVO, COMO_NUEVO o USADO. Varios separados por
     *                       coma: "NUEVO,COMO_NUEVO"
     * @param zonaId         id de zona
     * @param orden          recientes (default), precio_asc, precio_desc o
     *                       cercania (exige sesion con zona: si no,
     *                       400 SIN_ZONA_CONFIGURADA)
     */
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

    /** Catalogo de categorias para el filtro. Publico. */
    @GET("api/categorias")
    Call<CategoriasResponse> categorias();

    /** Catalogo de zonas para el filtro. Publico. */
    @GET("api/zonas")
    Call<ZonasResponse> zonas();

    /** Punto 5: crea una publicación y elimina el borrador en el servidor. */
    @POST("api/publicaciones")
    Call<PublicacionResponse> crear(@Header("Authorization") String bearer,
                                    @Body PublicacionRequest request);

    /** Publicaciones de la persona autenticada. */
    @GET("api/publicaciones/mias")
    Call<MisPublicacionesResponse> mias(@Header("Authorization") String bearer,
                                        @Query("estado") String estado);

    /** Pausa o reactiva una publicación propia. */
    @PATCH("api/publicaciones/{id}/estado")
    Call<PublicacionResponse> cambiarEstado(@Header("Authorization") String bearer,
                                            @Path("id") int id,
                                            @Body CambiarEstadoRequest request);

    @GET("api/publicaciones/borrador")
    Call<BorradorResponse> obtenerBorrador(@Header("Authorization") String bearer);

    @PUT("api/publicaciones/borrador")
    Call<BorradorResponse> guardarBorrador(@Header("Authorization") String bearer,
                                           @Body GuardarBorradorRequest request);

    @DELETE("api/publicaciones/borrador")
    Call<Void> descartarBorrador(@Header("Authorization") String bearer);

    /** Obtiene el detalle completo de una publicacion, incluyendo acciones permitidas segun sesion. */
    @GET("api/publicaciones/{id}")
    Call<com.example.ronda.data.model.PublicacionDetalleResponse> getDetallePublicacion(@Header("Authorization") String token, @Path("id") int id);

    @retrofit2.http.POST("api/publicaciones/{id}/favorito")
    Call<Void> agregarFavorito(@Header("Authorization") String token, @Path("id") int id);

    @retrofit2.http.DELETE("api/publicaciones/{id}/favorito")
    Call<Void> quitarFavorito(@Header("Authorization") String token, @Path("id") int id);

    // --- Punto 4: Interacciones ---

    @GET("api/publicaciones/{id}/preguntas")
    Call<com.example.ronda.data.model.ListaPreguntasResponse> listarPreguntas(@Path("id") int id);

    @retrofit2.http.POST("api/publicaciones/{id}/preguntas")
    Call<com.example.ronda.data.model.PreguntaUnicaResponse> hacerPregunta(@Header("Authorization") String token, @Path("id") int id, @retrofit2.http.Body com.example.ronda.data.model.PreguntarRequest request);

    @retrofit2.http.POST("api/preguntas/{id}/respuesta")
    Call<com.example.ronda.data.model.PreguntaUnicaResponse> responderPregunta(@Header("Authorization") String token, @Path("id") int id, @retrofit2.http.Body com.example.ronda.data.model.ResponderRequest request);

    @GET("api/publicaciones/{id}/ofertas")
    Call<com.example.ronda.data.model.ListaOfertasResponse> listarOfertas(@Header("Authorization") String token, @Path("id") int id);

    @retrofit2.http.POST("api/publicaciones/{id}/ofertas")
    Call<com.example.ronda.data.model.OfertaUnicaResponse> hacerOferta(@Header("Authorization") String token, @Path("id") int id, @retrofit2.http.Body com.example.ronda.data.model.OfertarRequest request);

    @retrofit2.http.PATCH("api/ofertas/{id}")
    Call<com.example.ronda.data.model.OfertaUnicaResponse> responderOferta(@Header("Authorization") String token, @Path("id") int id, @retrofit2.http.Body com.example.ronda.data.model.EstadoOfertaRequest request);

}
