package com.example.ronda.data.network;

import com.example.ronda.data.model.CambiarEmailRequest;
import com.example.ronda.data.model.ConfirmarEmailRequest;
import com.example.ronda.data.model.EditarPerfilRequest;
import com.example.ronda.data.model.ListaCalificacionesResponse;
import com.example.ronda.data.model.MensajeResponse;
import com.example.ronda.data.model.PerfilPublicoResponse;
import com.example.ronda.data.model.PerfilResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

/**
 * Datos personales del Punto 2. Cuelgan de /api/usuarios.
 *
 * El catalogo de zonas para el selector no esta aca: ya lo expone
 * PublicacionApiService.zonas() y se reutiliza.
 */
public interface UsuarioApiService {

    /** Mis datos. { "usuario": { ... } }, misma forma que /auth/me. */
    @GET("api/usuarios/me")
    Call<PerfilResponse> misDatos(@Header("Authorization") String bearer);

    /**
     * Editar nombre, telefono y zona. El backend responde
     * { "mensaje": "...", "usuario": { ... } }; alcanza con PerfilResponse
     * (el mensaje se ignora).
     */
    @PUT("api/usuarios/me")
    Call<PerfilResponse> actualizarMisDatos(@Header("Authorization") String bearer,
                                            @Body EditarPerfilRequest body);

    /**
     * Cambio de email, primer paso: manda un codigo al email NUEVO, todavia no
     * cambia nada. Tambien sirve para reenviarlo (429 OTP_COOLDOWN si se pide
     * antes de los 60 segundos). Sin @Header: lo agrega el interceptor de JWT.
     *
     * Errores: 400 EMAIL_INVALIDO, EMAIL_LARGO, EMAIL_IGUAL_AL_ACTUAL;
     *          409 EMAIL_EN_USO; 429 OTP_COOLDOWN.
     */
    @POST("api/usuarios/me/email/solicitar")
    Call<MensajeResponse> solicitarCambioEmail(@Body CambiarEmailRequest body);

    /**
     * Cambio de email, segundo paso: con el codigo correcto la cuenta pasa al
     * email nuevo. Responde { "mensaje", "usuario" }; el token sigue valiendo.
     *
     * Errores: 400 CODIGO_REQUERIDO, OTP_INVALIDO, OTP_EXPIRADO,
     *          OTP_INEXISTENTE; 409 EMAIL_EN_USO; 429 OTP_BLOQUEADO.
     */
    @POST("api/usuarios/me/email/confirmar")
    Call<PerfilResponse> confirmarCambioEmail(@Body ConfirmarEmailRequest body);

    /**
     * Perfil publico de cualquier persona: reputacion, antiguedad y
     * publicaciones activas. Es publico, asi que sirve tambien para el propio
     * perfil (de ahi sale la reputacion de "Mi perfil"). Sin @Header: si hay
     * sesion el interceptor la agrega, si no, va sin token.
     */
    @GET("api/usuarios/{id}/perfil")
    Call<PerfilPublicoResponse> perfilPublico(@Path("id") int usuarioId);

    /**
     * Calificaciones recibidas por una persona (Punto 9), las mas nuevas
     * primero. Publico: se consulta antes de operar con alguien.
     */
    @GET("api/usuarios/{id}/calificaciones")
    Call<ListaCalificacionesResponse> calificaciones(@Path("id") int usuarioId);
}
