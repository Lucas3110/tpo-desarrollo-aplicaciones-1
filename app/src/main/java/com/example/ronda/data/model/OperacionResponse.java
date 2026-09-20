package com.example.ronda.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Una fila del historial de operaciones (Punto 9).
 *
 * El backend resuelve el punto de vista: la misma operacion es COMPRA para
 * uno y VENTA para el otro, y contraparte ya viene calculada, asi que la
 * app no necesita saber de que lado estuvo la persona.
 */
public class OperacionResponse {

    public static final String COMPRA = "COMPRA";
    public static final String VENTA = "VENTA";

    @SerializedName("id")
    private int id;

    /** COMPRA o VENTA, desde el punto de vista de quien consulta. */
    @SerializedName("tipo")
    private String tipo;

    /** Cuando se concreto (fecha ISO en UTC). */
    @SerializedName("fecha")
    private String fecha;

    @SerializedName("montoFinal")
    private double montoFinal;

    @SerializedName("articulo")
    private Articulo articulo;

    @SerializedName("contraparte")
    private AutorResponse contraparte;

    @SerializedName("calificacion")
    private Calificacion calificacion;

    public int getId() {
        return id;
    }

    public String getTipo() {
        return tipo;
    }

    public String getFecha() {
        return fecha;
    }

    public double getMontoFinal() {
        return montoFinal;
    }

    public Articulo getArticulo() {
        return articulo;
    }

    public AutorResponse getContraparte() {
        return contraparte;
    }

    public Calificacion getCalificacion() {
        return calificacion;
    }

    public boolean esCompra() {
        return COMPRA.equals(tipo);
    }

    public static class Articulo {

        /** Null si la publicacion se borro despues de la operacion. */
        @SerializedName("id")
        private Integer id;

        @SerializedName("titulo")
        private String titulo;

        @SerializedName("fotoPrincipal")
        private String fotoPrincipal;

        public Integer getId() {
            return id;
        }

        public String getTitulo() {
            return titulo;
        }

        public String getFotoPrincipal() {
            return fotoPrincipal;
        }
    }

    /** Estado de la calificacion de esta operacion para quien consulta. */
    public static class Calificacion {

        @SerializedName("puedeCalificar")
        private boolean puedeCalificar;

        @SerializedName("yaCalifique")
        private boolean yaCalifique;

        /** Dias que quedan de la ventana de 7. Null si ya se cerro. */
        @SerializedName("diasRestantes")
        private Integer diasRestantes;

        /** Las estrellas que puse yo, si ya califique. */
        @SerializedName("misEstrellas")
        private Integer misEstrellas;

        /** Las que me puso la contraparte a mi, si ya lo hizo. */
        @SerializedName("estrellasRecibidas")
        private Integer estrellasRecibidas;

        public boolean isPuedeCalificar() {
            return puedeCalificar;
        }

        public boolean isYaCalifique() {
            return yaCalifique;
        }

        public Integer getDiasRestantes() {
            return diasRestantes;
        }

        public Integer getMisEstrellas() {
            return misEstrellas;
        }

        public Integer getEstrellasRecibidas() {
            return estrellasRecibidas;
        }
    }
}
