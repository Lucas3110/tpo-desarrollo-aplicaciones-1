package com.example.ronda.data.network;

import com.example.ronda.data.model.EditarPerfilRequest;
import com.example.ronda.data.model.PerfilResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PUT;

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
}
