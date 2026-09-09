package com.example.ronda.data.model;
import com.google.gson.annotations.SerializedName;
import java.util.List;
public class ListaOfertasResponse {
    @SerializedName("ofertas") private List<OfertaResponse> ofertas;
    @SerializedName("esVendedor") private boolean esVendedor;
    public List<OfertaResponse> getOfertas() { return ofertas; }
    public boolean isEsVendedor() { return esVendedor; }
}