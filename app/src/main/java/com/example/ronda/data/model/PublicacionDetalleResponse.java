package com.example.ronda.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PublicacionDetalleResponse {
    @SerializedName("publicacion")
    private Publicacion publicacion;

    public Publicacion getPublicacion() { return publicacion; }

    public static class Publicacion {
        @SerializedName("id")
        private int id;
        @SerializedName("titulo")
        private String titulo;
        @SerializedName("descripcion")
        private String descripcion;
        @SerializedName("precio")
        private double precio;
        @SerializedName("estadoArticuloTexto")
        private String estadoArticuloTexto;
        @SerializedName("publicadoEn")
        private String publicadoEn;
        @SerializedName("categoria")
        private Categoria categoria;
        
        @SerializedName("fotos")
        private List<Foto> fotos;
        @SerializedName("vendedor")
        private Vendedor vendedor;
        @SerializedName("acciones")
        private Acciones acciones;

        public int getId() { return id; }
        public String getTitulo() { return titulo; }
        public String getDescripcion() { return descripcion; }
        public double getPrecio() { return precio; }
        public String getEstadoArticuloTexto() { return estadoArticuloTexto; }
        public String getPublicadoEn() { return publicadoEn; }
        public Categoria getCategoria() { return categoria; }
        public List<Foto> getFotos() { return fotos; }
        public Vendedor getVendedor() { return vendedor; }
        public Acciones getAcciones() { return acciones; }
    }

    public static class Categoria {
        @SerializedName("nombre")
        private String nombre;
        public String getNombre() { return nombre; }
    }

    public static class Foto {
        @SerializedName("url")
        private String url;
        public String getUrl() { return url; }
    }

    public static class Vendedor {
        @SerializedName("id")
        private int id;
        @SerializedName("nombre")
        private String nombre;
        @SerializedName("zona")
        private Zona zona;
        @SerializedName("reputacion")
        private Reputacion reputacion;

        public int getId() { return id; }
        public String getNombre() { return nombre; }
        public Zona getZona() { return zona; }
        public Reputacion getReputacion() { return reputacion; }
    }

    public static class Zona {
        @SerializedName("nombre")
        private String nombre;
        public String getNombre() { return nombre; }
    }

    public static class Reputacion {
        @SerializedName("promedioEstrellas")
        private Double promedioEstrellas; 
        @SerializedName("cantidadCalificaciones")
        private int cantidadCalificaciones;

        public Double getPromedioEstrellas() { return promedioEstrellas; }
        public int getCantidadCalificaciones() { return cantidadCalificaciones; }
    }

    public static class Acciones {
        @SerializedName("puedePreguntar")
        private boolean puedePreguntar;
        @SerializedName("puedeOfertar")
        private boolean puedeOfertar;
        @SerializedName("puedeGuardar")
        private boolean puedeGuardar;
        @SerializedName("puedeGestionar")
        private boolean puedeGestionar;

        public boolean isPuedePreguntar() { return puedePreguntar; }
        public boolean isPuedeOfertar() { return puedeOfertar; }
        public boolean isPuedeGuardar() { return puedeGuardar; }
        public boolean isPuedeGestionar() { return puedeGestionar; }
    }
}
