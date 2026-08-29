package com.example.ronda.data.model;

/**
 * Cuerpo de POST /api/auth/otp/enviar.
 * proposito: "REGISTRO" para reenviar el codigo de alta,
 *            "LOGIN" para pedir un codigo de ingreso.
 */
public class OtpEnviarRequest {

    private final String email;
    private final String proposito;

    public OtpEnviarRequest(String email, String proposito) {
        this.email = email;
        this.proposito = proposito;
    }
}
