package com.example.ronda.data.model;

/** Cuerpo de POST /api/auth/registro. */
public class RegistroRequest {

    private final String email;
    private final String password;
    private final String nombre;

    public RegistroRequest(String email, String password, String nombre) {
        this.email = email;
        this.password = password;
        this.nombre = nombre;
    }
}
