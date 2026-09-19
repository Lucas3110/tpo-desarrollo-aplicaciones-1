package com.example.ronda.data.model;
import com.google.gson.annotations.SerializedName;
public class OfertaUnicaResponse {
    @SerializedName("oferta") private OfertaResponse oferta;
    public OfertaResponse getOferta() { return oferta; }
}