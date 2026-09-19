package com.example.ronda.data.network;

import com.example.ronda.data.model.EstadoOfertaRequest;
import com.example.ronda.data.model.ListaOfertasResponse;
import com.example.ronda.data.model.MisOfertasResponse;
import com.example.ronda.data.model.OfertaUnicaResponse;
import com.example.ronda.data.model.OfertarRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Endpoints de ofertas y negociacion (Puntos 4 y 7). Todos exigen sesion.
 *
 * A diferencia de las interfaces anteriores, ningun metodo recibe la cabecera
 * Authorization: la agrega el interceptor de OkHttp configurado en
 * NetworkModule (clase 5, "JWT + LocalStorage") leyendo el token guardado en
 * SessionRepository. Sin sesion la request sale sin token y el backend
 * responde 401, que la pantalla trata como "volver al login".
 *
 * Errores propios de estos endpoints (siempre { error: { codigo, mensaje } }):
 *   400 MONTO_INVALIDO, OFERTA_MAYOR_AL_PRECIO, MENSAJE_LARGO,
 *       CONTRAOFERTA_MISMO_MONTO, ESTADO_OFERTA_INVALIDO, PUBLICACION_NO_ACTIVA
 *   403 ES_TU_PUBLICACION, NO_TE_CORRESPONDE, NO_SOS_EL_VENDEDOR
 *   404 OFERTA_NO_ENCONTRADA
 *   409 OFERTA_YA_RESPONDIDA, OFERTA_VENCIDA, YA_ES_CONTRAOFERTA
 */
public interface OfertaApiService {

    /** Las que envie y las que recibi, siempre actualizadas (seccion "Mis ofertas"). */
    @GET("api/ofertas/mias")
    Call<MisOfertasResponse> misOfertas();

    /**
     * Ofertas de una publicacion. El vendedor ve todas; un interesado ve su
     * hilo: las suyas y las contraofertas que recibio.
     */
    @GET("api/publicaciones/{id}/ofertas")
    Call<ListaOfertasResponse> deLaPublicacion(@Path("id") int publicacionId);

    /**
     * Proponer un precio, con mensaje breve opcional. Una pendiente por
     * persona y publicacion: volver a ofertar reemplaza monto, mensaje y
     * plazo en vez de acumular.
     */
    @POST("api/publicaciones/{id}/ofertas")
    Call<OfertaUnicaResponse> ofertar(@Path("id") int publicacionId, @Body OfertarRequest cuerpo);

    /**
     * Aceptar o rechazar. Responde a quien NO propuso el monto: una oferta
     * del comprador la contesta el vendedor y una contraoferta, el comprador.
     */
    @PATCH("api/ofertas/{id}")
    Call<OfertaUnicaResponse> responder(@Path("id") int ofertaId, @Body EstadoOfertaRequest cuerpo);

    /**
     * Responder con otro precio. Solo el vendedor, y solo sobre una oferta
     * del comprador. La original queda RECHAZADA y la respuesta es la
     * contraoferta nueva, PENDIENTE.
     */
    @POST("api/ofertas/{id}/contraoferta")
    Call<OfertaUnicaResponse> contraofertar(@Path("id") int ofertaId, @Body OfertarRequest cuerpo);
}
