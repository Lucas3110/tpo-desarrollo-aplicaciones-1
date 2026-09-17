package com.example.ronda.data.model;

import com.google.gson.annotations.SerializedName;

public class PublicacionResponse {
    @SerializedName("publicacion") private PublicacionItemResponse publicacion;
    public PublicacionItemResponse getPublicacion() { return publicacion; }
}
