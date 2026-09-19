package com.example.ronda.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

/**
 * GET /ofertas/mias: las ofertas que envie y las que recibi, de la mas nueva
 * a la mas vieja, con su estado ya actualizado (el backend vence las
 * caducadas antes de responder).
 */
public class MisOfertasResponse {

    @SerializedName("enviadas") private List<OfertaResponse> enviadas;
    @SerializedName("recibidas") private List<OfertaResponse> recibidas;

    /** Nunca null: una lista vacia es mas facil de mostrar que un null. */
    public List<OfertaResponse> getEnviadas() {
        return enviadas != null ? enviadas : Collections.<OfertaResponse>emptyList();
    }

    public List<OfertaResponse> getRecibidas() {
        return recibidas != null ? recibidas : Collections.<OfertaResponse>emptyList();
    }

    /**
     * Cuantas recibidas esperan mi respuesta, para el indicador del Home. El
     * backend ya marca cada una con esperaMiRespuesta; aca solo se cuentan.
     */
    public int getPendientesRecibidas() {
        int cuenta = 0;
        for (OfertaResponse oferta : getRecibidas()) {
            if (oferta.isEsperaMiRespuesta()) cuenta++;
        }
        return cuenta;
    }
}
