package com.example.ronda.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ListaBusquedasGuardadasResponse {
    @SerializedName("busquedas")
    private List<BusquedaGuardadaDto> busquedas;

    @SerializedName("totalNovedades")
    private int totalNovedades;

    public List<BusquedaGuardadaDto> getBusquedas() {
        return busquedas;
    }

    public int getTotalNovedades() {
        return totalNovedades;
    }
}
