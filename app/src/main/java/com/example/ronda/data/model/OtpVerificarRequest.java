package com.example.ronda.data.model;

/**
 * Cuerpo de POST /api/auth/otp/verificar: el email, el código de 6 dígitos
 * y para qué es (REGISTRO o LOGIN).
 */
public class OtpVerificarRequest {

    private final String email;
    private final String codigo;
    private final String proposito;

    public OtpVerificarRequest(String email, String codigo, String proposito) {
        this.email = email;
        this.codigo = codigo;
        this.proposito = proposito;
    }
}
