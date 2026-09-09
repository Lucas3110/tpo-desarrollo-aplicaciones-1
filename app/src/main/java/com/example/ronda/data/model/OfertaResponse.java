package com.example.ronda.data.model;
import com.google.gson.annotations.SerializedName;
public class OfertaResponse {
    @SerializedName("id") private int id;
    @SerializedName("monto") private double monto;
    @SerializedName("estado") private String estado;
    @SerializedName("respondidaEn") private String respondidaEn;
    @SerializedName("creadoEn") private String creadoEn;
    @SerializedName("autor") private AutorResponse autor;

    public int getId() { return id; }
    public double getMonto() { return monto; }
    public String getEstado() { return estado; }
    public String getRespondidaEn() { return respondidaEn; }
    public String getCreadoEn() { return creadoEn; }
    public AutorResponse getAutor() { return autor; }
}