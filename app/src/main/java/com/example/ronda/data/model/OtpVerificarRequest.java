package com.example.ronda.data.model;

/** Cuerpo de POST /api/auth/otp/verificar. */
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
