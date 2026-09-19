package com.example.ronda.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Una oferta o contraoferta, tal como la devuelve la API (Puntos 4 y 7).
 *
 * Como lo modela el backend: un hilo de negociacion por persona interesada.
 * El comprador ofrece un monto (origen COMPRADOR) y el vendedor puede
 * responder con otro precio (origen VENDEDOR, esContraoferta true y
 * contraofertaDeId apuntando a la original). Ojo con "autor": es siempre la
 * persona interesada del hilo, aunque el monto lo haya propuesto el
 * vendedor, asi que para saber quien propuso hay que mirar origen.
 *
 * publicacion, contraparte y esperaMiRespuesta solo vienen en
 * GET /ofertas/mias; en el listado de una publicacion son null/false.
 */
public class OfertaResponse {

    /** Estados posibles. estadoTexto ya viene traducido para mostrar. */
    public static final String PENDIENTE = "PENDIENTE";
    public static final String ACEPTADA = "ACEPTADA";
    public static final String RECHAZADA = "RECHAZADA";
    public static final String VENCIDA = "VENCIDA";

    /** Quien propuso el monto. */
    public static final String ORIGEN_COMPRADOR = "COMPRADOR";
    public static final String ORIGEN_VENDEDOR = "VENDEDOR";

    @SerializedName("id") private int id;
    @SerializedName("monto") private double monto;
    /** Mensaje breve opcional de quien propuso el monto. Null si no escribio nada. */
    @SerializedName("mensaje") private String mensaje;
    @SerializedName("estado") private String estado;
    @SerializedName("estadoTexto") private String estadoTexto;
    @SerializedName("origen") private String origen;
    @SerializedName("esContraoferta") private boolean esContraoferta;
    /** Id de la oferta original si esta es una contraoferta; null si no. */
    @SerializedName("contraofertaDeId") private Integer contraofertaDeId;
    @SerializedName("respondidaEn") private String respondidaEn;
    /** Fecha ISO en la que la oferta pendiente caduca sola. */
    @SerializedName("expiraEn") private String expiraEn;
    @SerializedName("creadoEn") private String creadoEn;
    /** La persona interesada del hilo (no necesariamente quien propuso este monto). */
    @SerializedName("autor") private AutorResponse autor;
    @SerializedName("publicacion") private Publicacion publicacion;
    /** La otra parte, ya resuelta por el backend. Solo en "mis ofertas". */
    @SerializedName("contraparte") private AutorResponse contraparte;
    /** true = me toca responder a mi. Solo en "mis ofertas". */
    @SerializedName("esperaMiRespuesta") private boolean esperaMiRespuesta;

    public int getId() { return id; }
    public double getMonto() { return monto; }
    public String getMensaje() { return mensaje; }
    public String getEstado() { return estado; }
    public String getEstadoTexto() { return estadoTexto; }
    public String getOrigen() { return origen; }
    public boolean isEsContraoferta() { return esContraoferta; }
    public Integer getContraofertaDeId() { return contraofertaDeId; }
    public String getRespondidaEn() { return respondidaEn; }
    public String getExpiraEn() { return expiraEn; }
    public String getCreadoEn() { return creadoEn; }
    public AutorResponse getAutor() { return autor; }
    public Publicacion getPublicacion() { return publicacion; }
    public AutorResponse getContraparte() { return contraparte; }
    public boolean isEsperaMiRespuesta() { return esperaMiRespuesta; }

    public boolean isPendiente() { return PENDIENTE.equals(estado); }
    public boolean isAceptada() { return ACEPTADA.equals(estado); }
    public boolean tieneMensaje() { return mensaje != null && !mensaje.trim().isEmpty(); }

    /** El monto lo propuso el vendedor (es una contraoferta suya). */
    public boolean laPropusoElVendedor() { return ORIGEN_VENDEDOR.equals(origen); }

    /**
     * Si a quien mira le toca aceptar o rechazar esta oferta. En el listado
     * de una publicacion hay que pasarle si es el vendedor (lo dice
     * ListaOfertasResponse), porque ahi el backend no manda
     * esperaMiRespuesta: una oferta del comprador la responde el vendedor y
     * una contraoferta del vendedor la responde el comprador.
     */
    public boolean puedoResponderla(boolean soyElVendedor) {
        if (!isPendiente()) return false;
        return soyElVendedor != laPropusoElVendedor();
    }

    /**
     * Contraofertar es solo del vendedor y solo sobre una oferta del
     * comprador: el backend responde NO_SOS_EL_VENDEDOR o YA_ES_CONTRAOFERTA
     * en cualquier otro caso.
     */
    public boolean puedoContraofertarla(boolean soyElVendedor) {
        return soyElVendedor && isPendiente() && !laPropusoElVendedor();
    }

    /** Lo justo de la publicacion para mostrar la oferta fuera de su detalle. */
    public static class Publicacion {
        @SerializedName("id") private int id;
        @SerializedName("titulo") private String titulo;
        @SerializedName("precio") private double precio;
        @SerializedName("estado") private String estado;
        @SerializedName("fotoPrincipal") private String fotoPrincipal;

        public int getId() { return id; }
        public String getTitulo() { return titulo; }
        public double getPrecio() { return precio; }
        public String getEstado() { return estado; }
        public String getFotoPrincipal() { return fotoPrincipal; }
        public boolean estaActiva() { return "ACTIVA".equals(estado); }
    }
}
