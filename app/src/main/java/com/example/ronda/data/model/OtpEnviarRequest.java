package com.example.ronda.data.model;

/**
 * Cuerpo de POST /api/auth/otp/enviar (Gson lo convierte a JSON).
 * proposito: "REGISTRO" para reenviar el código de alta,
 *            "LOGIN" para pedir un código de ingreso.
 */
public class OtpEnviarRequest {

    private final String email;
    private final String proposito;

    public OtpEnviarRequest(String email, String proposito) {
        this.email = email;
        this.proposito = proposito;
    }
}
