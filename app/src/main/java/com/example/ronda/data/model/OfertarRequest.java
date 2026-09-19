package com.example.ronda.data.model;
import com.google.gson.annotations.SerializedName;
public class OfertarRequest {
    @SerializedName("monto") private double monto;
    public OfertarRequest(double monto) { this.monto = monto; }
}