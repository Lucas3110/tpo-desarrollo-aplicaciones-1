package com.example.ronda.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Punto 12: el cierre de una operacion, tal como lo manda la API dentro de la
 * oferta aceptada (GET /ofertas/mias), del detalle de la publicacion
 * (GET /publicaciones/:id) y de la confirmacion (POST /operaciones/confirmar-entrega).
 *
 * Que trae depende de quien mira, y eso ya lo resolvio el backend:
 *   - el vendedor recibe codigoEntrega: el texto que imprime como QR y entrega
 *     junto con el articulo;
 *   - el comprador recibe puedoConfirmar = true mientras la entrega siga
 *     pendiente, y nunca el codigo: lo tiene que escanear del QR fisico.
 */
public class CierreResponse {

    /** La oferta se acepto pero el comprador todavia no escaneo el QR. */
    public static final String PENDIENTE_ENTREGA = "PENDIENTE_ENTREGA";
    /** El comprador confirmo que recibio el articulo: la venta esta cerrada. */
    public static final String CONCRETADA = "CONCRETADA";

    @SerializedName("operacionId") private int operacionId;
    @SerializedName("estado") private String estado;
    /** El estado ya traducido para mostrar ("Entrega pendiente"). */
    @SerializedName("estadoTexto") private String estadoTexto;
    /** Solo para el vendedor y solo mientras este pendiente. Null para el comprador. */
    @SerializedName("codigoEntrega") private String codigoEntrega;
    /** true = soy el comprador y me toca escanear el QR al recibir el articulo. */
    @SerializedName("puedoConfirmar") private boolean puedoConfirmar;
    /** Cuando se cerro el precio (fecha ISO). */
    @SerializedName("aceptadaEn") private String aceptadaEn;
    /** Cuando se confirmo la entrega (fecha ISO). Null mientras este pendiente. */
    @SerializedName("concretadaEn") private String concretadaEn;

    public int getOperacionId() { return operacionId; }
    public String getEstado() { return estado; }
    public String getEstadoTexto() { return estadoTexto; }
    public String getCodigoEntrega() { return codigoEntrega; }
    public boolean isPuedoConfirmar() { return puedoConfirmar; }
    public String getAceptadaEn() { return aceptadaEn; }
    public String getConcretadaEn() { return concretadaEn; }

    public boolean estaPendiente() { return PENDIENTE_ENTREGA.equals(estado); }
    public boolean estaConcretada() { return CONCRETADA.equals(estado); }

    /** Soy el vendedor y la entrega sigue pendiente: tengo un codigo para imprimir. */
    public boolean tengoCodigoParaImprimir() {
        return codigoEntrega != null && !codigoEntrega.trim().isEmpty();
    }
}
