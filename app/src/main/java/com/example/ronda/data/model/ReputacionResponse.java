package com.example.ronda.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Reputacion de una persona (Punto 2): promedio de las calificaciones
 * recibidas y operaciones concretadas de cada lado. Espeja
 * toReputacionDto del backend.
 */
public class ReputacionResponse {

    /** Null mientras no tenga calificaciones: la pantalla tiene que contemplarlo. */
    @SerializedName("promedioEstrellas")
    private Double promedioEstrellas;

    @SerializedName("cantidadCalificaciones")
    private int cantidadCalificaciones;

    @SerializedName("operacionesComoVendedor")
    private int operacionesComoVendedor;

    @SerializedName("operacionesComoComprador")
    private int operacionesComoComprador;

    public Double getPromedioEstrellas() {
        return promedioEstrellas;
    }

    public int getCantidadCalificaciones() {
        return cantidadCalificaciones;
    }

    public int getOperacionesComoVendedor() {
        return operacionesComoVendedor;
    }

    public int getOperacionesComoComprador() {
        return operacionesComoComprador;
    }

    public boolean tienePromedio() {
        return promedioEstrellas != null;
    }
}
