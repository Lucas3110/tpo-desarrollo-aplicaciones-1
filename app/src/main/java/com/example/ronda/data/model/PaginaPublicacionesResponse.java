package com.example.ronda.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Envoltorio paginado de GET /publicaciones (toPaginaDto en el backend):
 *
 *   { "items": [...], "pagina": 1, "limite": 20, "total": 12,
 *     "totalPaginas": 1, "hayMas": false }
 *
 * "hayMas" es lo unico que necesita el scroll infinito: si es true se pide
 * pagina + 1, si es false se deja de pedir.
 */
public class PaginaPublicacionesResponse {

    @SerializedName("items")
    private List<PublicacionItemResponse> items;

    @SerializedName("pagina")
    private int pagina;

    @SerializedName("limite")
    private int limite;

    @SerializedName("total")
    private int total;

    @SerializedName("totalPaginas")
    private int totalPaginas;

    @SerializedName("hayMas")
    private boolean hayMas;

    public List<PublicacionItemResponse> getItems() {
        return items;
    }

    public int getPagina() {
        return pagina;
    }

    public int getLimite() {
        return limite;
    }

    public int getTotal() {
        return total;
    }

    public int getTotalPaginas() {
        return totalPaginas;
    }

    public boolean isHayMas() {
        return hayMas;
    }
}
