package com.example.ronda.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Un aviso de GET /api/notificaciones (Punto 11). Titulo y mensaje ya vienen
 * armados por el backend. publicacionId viene siempre; ofertaId o preguntaId
 * segun el tipo (el otro en null).
 */
public class NotificacionResponse {

    public static final String PREGUNTA_NUEVA = "PREGUNTA_NUEVA";
    public static final String PREGUNTA_RESPONDIDA = "PREGUNTA_RESPONDIDA";

    @SerializedName("id")
    private int id;

    /**
     * PREGUNTA_NUEVA, PREGUNTA_RESPONDIDA, OFERTA_NUEVA, OFERTA_ACEPTADA,
     * OFERTA_RECHAZADA o CONTRAOFERTA.
     */
    @SerializedName("tipo")
    private String tipo;

    @SerializedName("titulo")
    private String titulo;

    @SerializedName("mensaje")
    private String mensaje;

    @SerializedName("publicacionId")
    private int publicacionId;

    @SerializedName("ofertaId")
    private Integer ofertaId;

    @SerializedName("preguntaId")
    private Integer preguntaId;

    @SerializedName("creadoEn")
    private String creadoEn;

    public int getId() {
        return id;
    }

    public String getTipo() {
        return tipo;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public int getPublicacionId() {
        return publicacionId;
    }

    public Integer getOfertaId() {
        return ofertaId;
    }

    public Integer getPreguntaId() {
        return preguntaId;
    }

    public String getCreadoEn() {
        return creadoEn;
    }

    /** Los dos tipos de preguntas; todo lo demas es de ofertas. */
    public boolean esDePreguntas() {
        return PREGUNTA_NUEVA.equals(tipo) || PREGUNTA_RESPONDIDA.equals(tipo);
    }
}
