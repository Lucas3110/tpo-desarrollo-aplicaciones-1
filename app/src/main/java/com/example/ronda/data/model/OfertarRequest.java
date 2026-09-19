package com.example.ronda.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Cuerpo de POST /publicaciones/{id}/ofertas y de POST /ofertas/{id}/contraoferta.
 *
 * El mensaje es opcional (el backend responde MENSAJE_LARGO si se pasa del
 * tope). Si viene vacio se manda null y Gson omite el campo, que es lo mismo
 * que no mandarlo.
 */
public class OfertarRequest {

    @SerializedName("monto") private double monto;
    @SerializedName("mensaje") private String mensaje;

    public OfertarRequest(double monto) {
        this(monto, null);
    }

    public OfertarRequest(double monto, String mensaje) {
        this.monto = monto;
        this.mensaje = (mensaje == null || mensaje.trim().isEmpty()) ? null : mensaje.trim();
    }

    public double getMonto() { return monto; }
    public String getMensaje() { return mensaje; }
}
