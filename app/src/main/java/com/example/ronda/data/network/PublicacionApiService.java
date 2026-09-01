package com.example.ronda.data.network;

import com.example.ronda.data.model.CategoriasResponse;
import com.example.ronda.data.model.PaginaPublicacionesResponse;
import com.example.ronda.data.model.ZonasResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
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
}
