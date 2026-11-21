package com.example.ronda.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Respuesta de POST /operaciones/confirmar-entrega (Punto 12) cuando el QR era
 * valido: la operacion ya CONCRETADA (con la misma forma que una fila del
 * historial del Punto 9), la publicacion ya VENDIDA y un mensaje listo para
 * mostrar. Los errores (CODIGO_INVALIDO, NO_SOS_EL_COMPRADOR,
 * ENTREGA_YA_CONFIRMADA) llegan como ErrorResponse, no por aca.
 */
public class ConfirmarEntregaResponse {
    @SerializedName("mensaje") private String mensaje;
    @SerializedName("cierre") private CierreResponse cierre;
    @SerializedName("operacion") private OperacionResponse operacion;
    /** Null si la publicacion se borro despues de cerrar el precio. */
    @SerializedName("publicacion") private Publicacion publicacion;

    public String getMensaje() { return mensaje; }
    public CierreResponse getCierre() { return cierre; }
    public OperacionResponse getOperacion() { return operacion; }
    public Publicacion getPublicacion() { return publicacion; }

    public static class Publicacion {
        @SerializedName("id") private int id;
        @SerializedName("titulo") private String titulo;
        @SerializedName("estado") private String estado;

        public int getId() { return id; }
        public String getTitulo() { return titulo; }
        public String getEstado() { return estado; }
        public boolean estaVendida() { return "VENDIDA".equals(estado); }
    }
}
