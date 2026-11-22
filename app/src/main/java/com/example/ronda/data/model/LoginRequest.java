package com.example.ronda.data.model;

/**
 * Cuerpo de POST /api/auth/login. Gson lo convierte a este JSON:
 *   { "email": "...", "password": "..." }
 * Por eso los campos se llaman igual que las claves que espera el backend.
 */
public class LoginRequest {

    private final String email;
    private final String password;

    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }
}
