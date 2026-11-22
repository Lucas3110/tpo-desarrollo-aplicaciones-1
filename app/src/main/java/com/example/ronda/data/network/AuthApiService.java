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
import retrofit2.http.POST;

/**
 * Los 5 endpoints del Punto 1, escritos como una interfaz de Retrofit: es el
 * contrato con el backend. Acá solo se declaran los métodos; la clase que
 * hace cada pedido HTTP la genera Retrofit (ver provideAuthApiService en
 * NetworkModule).
 *
 * Cómo se lee cada método:
 *   - la anotación @POST o @GET dice el verbo y la ruta, que se suma a la
 *     BASE_URL;
 *   - con @Body, el objeto se convierte a JSON con Gson y viaja en el cuerpo;
 *   - devuelve un Call: el pedido todavía no salió. Sale cuando la pantalla
 *     llama a enqueue(), y la respuesta JSON vuelve convertida en el objeto
 *     que dice el Call (por ejemplo, SesionResponse).
 *
 * Ningún método recibe el token: el header Authorization lo agrega el
 * interceptor de NetworkModule en cada pedido.
 */
public interface AuthApiService {

    /** Crear la cuenta. Si sale bien, el backend manda el código al mail. */
    @POST("api/auth/registro")
    Call<RegistroResponse> registrar(@Body RegistroRequest body);

    /** Pedir un código, o pedir otro. El propósito dice para qué: REGISTRO o LOGIN. */
    @POST("api/auth/otp/enviar")
    Call<MensajeResponse> enviarOtp(@Body OtpEnviarRequest body);

    /** Confirmar el código. Si está bien, devuelve el token: se crea la sesión. */
    @POST("api/auth/otp/verificar")
    Call<SesionResponse> verificarOtp(@Body OtpVerificarRequest body);

    /** Ingreso con email y contraseña. También devuelve el token. */
    @POST("api/auth/login")
    Call<SesionResponse> login(@Body LoginRequest body);

    /**
     * Ruta privada: el interceptor le agrega el header con el token. Si
     * responde bien, la sesión sigue viva. La usa el auto-login.
     */
    @GET("api/auth/me")
    Call<PerfilResponse> me();
}
