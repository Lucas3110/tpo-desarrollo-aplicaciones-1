package com.example.ronda.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Una calificacion, tal como la devuelve la API (Punto 9). Espeja
 * toCalificacionDto del backend.
 */
public class CalificacionResponse {

    public static final String ROL_COMPRADOR = "COMPRADOR";
    public static final String ROL_VENDEDOR = "VENDEDOR";

    @SerializedName("id")
    private int id;

    /** De 1 a 5. */
    @SerializedName("estrellas")
    private int estrellas;

    /** Opcional: null si quien califico no escribio nada. */
    @SerializedName("comentario")
    private String comentario;

    /** Por que papel se lo califico: COMPRADOR o VENDEDOR. */
    @SerializedName("rolCalificado")
    private String rolCalificado;

    @SerializedName("fecha")
    private String fecha;

    /** Quien califico. */
    @SerializedName("autor")
    private AutorResponse autor;

    /** Titulo de la publicacion de la operacion (null si se borro despues). */
    @SerializedName("articulo")
    private String articulo;

    public int getId() {
        return id;
    }

    public int getEstrellas() {
        return estrellas;
    }

    public String getComentario() {
        return comentario;
    }

    public String getRolCalificado() {
        return rolCalificado;
    }

    public String getFecha() {
        return fecha;
    }

    public AutorResponse getAutor() {
        return autor;
    }

    public String getArticulo() {
        return articulo;
    }

    public boolean tieneComentario() {
        return comentario != null && !comentario.trim().isEmpty();
    }

    public boolean esComoVendedor() {
        return ROL_VENDEDOR.equals(rolCalificado);
    }
}
