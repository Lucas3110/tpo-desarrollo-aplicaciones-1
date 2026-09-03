package com.example.ronda.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Una fila del listado del Home (GET /publicaciones).
 *
 * Espeja toPublicacionListadoDto del backend: es la version liviana de la
 * publicacion, con lo justo para dibujar la tarjeta. La descripcion y la
 * galeria completa llegan recien en el detalle (Punto 4).
 */
public class PublicacionItemResponse {

    @SerializedName("id")
    private int id;

    @SerializedName("titulo")
    private String titulo;

    /** El backend ya lo convierte a numero (MySQL lo devuelve como texto). */
    @SerializedName("precio")
    private double precio;

    /** Condicion de la cosa: NUEVO, COMO_NUEVO o USADO. */
    @SerializedName("estadoArticulo")
    private String estadoArticulo;

    /** El mismo estado pero legible ("Como nuevo"), listo para mostrar. */
    @SerializedName("estadoArticuloTexto")
    private String estadoArticuloTexto;

    /** Situacion del aviso: ACTIVA, PAUSADA o VENDIDA. No confundir con el anterior. */
    @SerializedName("estado")
    private String estado;

    @SerializedName("categoria")
    private CategoriaResponse categoria;

    /** Zona del vendedor, que es lo que pide mostrar la consigna. */
    @SerializedName("zona")
    private ZonaResponse zona;

    /** URL de la primera foto, o null si la publicacion no tiene fotos. */
    @SerializedName("fotoPrincipal")
    private String fotoPrincipal;

    @SerializedName("cantidadFotos")
    private int cantidadFotos;

    /** Fecha ISO 8601, por ejemplo "2026-08-28T21:08:24.000Z". */
    @SerializedName("creadoEn")
    private String creadoEn;

    /**
     * Solo viene cuando el listado se pidio con token. Sin sesion el campo no
     * esta en el JSON y Gson lo deja en null, por eso es Boolean y no boolean.
     */
    @SerializedName("esFavorito")
    private Boolean esFavorito;

    public int getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public double getPrecio() {
        return precio;
    }

    public String getEstadoArticulo() {
        return estadoArticulo;
    }

    public String getEstadoArticuloTexto() {
        return estadoArticuloTexto;
    }

    public String getEstado() {
        return estado;
    }

    public CategoriaResponse getCategoria() {
        return categoria;
    }

    public ZonaResponse getZona() {
        return zona;
    }

    public String getFotoPrincipal() {
        return fotoPrincipal;
    }

    public int getCantidadFotos() {
        return cantidadFotos;
    }

    public String getCreadoEn() {
        return creadoEn;
    }

    /** false si el campo no vino (sin sesion) o si no esta guardada. */
    public boolean isFavorito() {
        return Boolean.TRUE.equals(esFavorito);
    }
}
