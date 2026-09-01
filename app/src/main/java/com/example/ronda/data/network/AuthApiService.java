package com.example.ronda.data.network;

import com.example.ronda.data.model.LoginRequest;
import com.example.ronda.data.model.MensajeResponse;
import com.example.ronda.data.model.OtpEnviarRequest;
import com.example.ronda.data.model.OtpVerificarRequest;
import com.example.ronda.data.model.PerfilResponse;
import com.example.ronda.data.model.RegistroRequest;
import com.example.ronda.data.model.RegistroResponse;
import com.example.ronda.data.model.SesionResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

/**
 * Endpoints del Punto 1. Retrofit convierte esta interfaz en llamadas HTTP:
 *   @POST  -> verbo y path (relativo a la BASE_URL que define NetworkModule)
 *   @Body  -> el objeto se serializa a JSON con Gson
 *   @Header-> agrega una cabecera a la request
 */
public interface AuthApiService {

    @POST("api/auth/registro")
    Call<RegistroResponse> registrar(@Body RegistroRequest body);

    @POST("api/auth/otp/enviar")
    Call<MensajeResponse> enviarOtp(@Body OtpEnviarRequest body);

    @POST("api/auth/otp/verificar")
    Call<SesionResponse> verificarOtp(@Body OtpVerificarRequest body);

    @POST("api/auth/login")
    Call<SesionResponse> login(@Body LoginRequest body);

    /** Ruta privada: el bearer se arma con SessionRepository.getBearer(). */
    @GET("api/auth/me")
    Call<PerfilResponse> me(@Header("Authorization") String bearer);
}
