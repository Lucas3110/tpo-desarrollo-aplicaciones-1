package com.example.ronda.data.model;

import java.util.List;

public class PublicacionDetalleResponse {
    private Publicacion publicacion;

    public Publicacion getPublicacion() { return publicacion; }

    public static class Publicacion {
        private int id;
        private String titulo;
        private String descripcion;
        private String precio;
        private String estadoArticuloTexto;
        private String publicadoEn;
        private Categoria categoria;
        
        private List<Foto> fotos;
        private Vendedor vendedor;
        private Acciones acciones;

        public String getTitulo() { return titulo; }
        public String getDescripcion() { return descripcion; }
        public String getPrecio() { return precio; }
        public String getEstadoArticuloTexto() { return estadoArticuloTexto; }
        public String getPublicadoEn() { return publicadoEn; }
        public Categoria getCategoria() { return categoria; }
        public List<Foto> getFotos() { return fotos; }
        public Vendedor getVendedor() { return vendedor; }
        public Acciones getAcciones() { return acciones; }
    }

    
    public static class Categoria {
        private String nombre;
        public String getNombre() { return nombre; }
    }

    public static class Foto {
        private String url;
        public String getUrl() { return url; }
    }

    public static class Vendedor {
        private String nombre;
        private Zona zona;
        private Reputacion reputacion;

        public String getNombre() { return nombre; }
        public Zona getZona() { return zona; }
        public Reputacion getReputacion() { return reputacion; }
    }

    public static class Zona {
        private String nombre;
        public String getNombre() { return nombre; }
    }

    public static class Reputacion {
        private Double promedioEstrellas; 
        private int cantidadCalificaciones;

        public Double getPromedioEstrellas() { return promedioEstrellas; }
        public int getCantidadCalificaciones() { return cantidadCalificaciones; }
    }

    public static class Acciones {
        private boolean puedePreguntar;
        private boolean puedeOfertar;
        private boolean puedeGuardar;
        private boolean puedeGestionar;

        public boolean isPuedePreguntar() { return puedePreguntar; }
        public boolean isPuedeOfertar() { return puedeOfertar; }
        public boolean isPuedeGuardar() { return puedeGuardar; }
        public boolean isPuedeGestionar() { return puedeGestionar; }
    }
}
