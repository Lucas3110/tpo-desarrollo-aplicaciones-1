package com.example.ronda.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Respuesta de GET /usuarios/:id/perfil (Punto 2): lo que cualquiera puede
 * ver de una persona antes de operar con ella. A proposito no trae email ni
 * telefono.
 *
 * "Mi perfil" tambien lo usa para mostrar la reputacion propia, porque
 * GET /usuarios/me no la incluye.
 */
public class PerfilPublicoResponse {

    @SerializedName("perfil")
    private Perfil perfil;

    public Perfil getPerfil() {
        return perfil;
    }

    public static class Perfil {

        @SerializedName("id")
        private int id;

        @SerializedName("nombre")
        private String nombre;

        /** Null si la persona no eligio zona. */
        @SerializedName("zona")
        private ZonaResponse zona;

        /** URL de la foto de perfil, o null si no tiene. */
        @SerializedName("fotoUrl")
        private String fotoUrl;

        @SerializedName("miembroDesde")
        private String miembroDesde;

        /** Antiguedad en la plataforma, ya calculada por el backend. */
        @SerializedName("antiguedadDias")
        private int antiguedadDias;

        @SerializedName("reputacion")
        private ReputacionResponse reputacion;

        /** Mismo formato que el listado del Home (hasta 10). */
        @SerializedName("publicacionesActivas")
        private List<PublicacionItemResponse> publicacionesActivas;

        public int getId() {
            return id;
        }

        public String getNombre() {
            return nombre;
        }

        public ZonaResponse getZona() {
            return zona;
        }

        public String getFotoUrl() {
            return fotoUrl;
        }

        public String getMiembroDesde() {
            return miembroDesde;
        }

        public int getAntiguedadDias() {
            return antiguedadDias;
        }

        public ReputacionResponse getReputacion() {
            return reputacion;
        }

        public List<PublicacionItemResponse> getPublicacionesActivas() {
            return publicacionesActivas != null ? publicacionesActivas : new ArrayList<>();
        }
    }
}
