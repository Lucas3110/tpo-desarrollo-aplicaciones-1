package com.example.ronda.ui.home;

import androidx.annotation.StringRes;

import com.example.ronda.R;

/**
 * Los ordenamientos que acepta GET /publicaciones (parametro "orden").
 *
 * Junta en un solo lugar el valor que espera la API y el texto que ve la
 * persona: asi el Spinner y el request no se pueden desincronizar, que es
 * lo que pasaria con dos arrays paralelos mantenidos a mano.
 */
public enum Orden {
    RECIENTES("recientes", R.string.orden_recientes),
    PRECIO_ASC("precio_asc", R.string.orden_precio_asc),
    PRECIO_DESC("precio_desc", R.string.orden_precio_desc),
    /** Por distancia a la zona de la persona: necesita sesion con zona configurada. */
    CERCANIA("cercania", R.string.orden_cercania);

    private final String valorApi;
    @StringRes
    private final int textoRes;

    Orden(String valorApi, @StringRes int textoRes) {
        this.valorApi = valorApi;
        this.textoRes = textoRes;
    }

    /** Lo que viaja en la query: orden=precio_asc */
    public String getValorApi() {
        return valorApi;
    }

    @StringRes
    public int getTextoRes() {
        return textoRes;
    }

    /** El orden que corresponde a un valor de la API; RECIENTES si no se reconoce. */
    public static Orden desdeValorApi(String valor) {
        for (Orden orden : values()) {
            if (orden.valorApi.equals(valor)) {
                return orden;
            }
        }
        return RECIENTES;
    }
}
