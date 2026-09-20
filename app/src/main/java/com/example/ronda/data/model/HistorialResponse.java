package com.example.ronda.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Respuesta de GET /operaciones: el historial ya separado en compras y
 * ventas, como pide la consigna. Con el filtro tipo, la otra lista viene vacia.
 */
public class HistorialResponse {

    @SerializedName("compras")
    private List<OperacionResponse> compras;

    @SerializedName("ventas")
    private List<OperacionResponse> ventas;

    public List<OperacionResponse> getCompras() {
        return compras != null ? compras : new ArrayList<>();
    }

    public List<OperacionResponse> getVentas() {
        return ventas != null ? ventas : new ArrayList<>();
    }
}
