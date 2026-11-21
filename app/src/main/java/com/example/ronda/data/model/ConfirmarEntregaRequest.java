package com.example.ronda.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Cuerpo de POST /operaciones/confirmar-entrega (Punto 12): el texto que la
 * app leyo del QR fisico. Viaja tal cual se leyo; el backend lo normaliza
 * (mayusculas, sin espacios) y decide si es un codigo valido.
 */
public class ConfirmarEntregaRequest {
    @SerializedName("codigo") private final String codigo;

    public ConfirmarEntregaRequest(String codigo) { this.codigo = codigo; }

    public String getCodigo() { return codigo; }
}
