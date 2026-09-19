package com.example.ronda.ui.entrega;

import com.example.ronda.data.model.EntregaResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/** Construye únicamente enlaces de navegación de Maps; no abre URLs arbitrarias del servidor. */
public final class EnlaceEntrega {
    private EnlaceEntrega() {}

    public static String crear(EntregaResponse entrega) {
        if (entrega == null || !entrega.tieneDestino()) return null;
        try {
            return "https://www.google.com/maps/dir/?api=1&destination="
                    + URLEncoder.encode(entrega.getDestino(), "UTF-8")
                    + "&dir_action=navigate";
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e); // UTF-8 siempre está disponible en Android.
        }
    }
}
