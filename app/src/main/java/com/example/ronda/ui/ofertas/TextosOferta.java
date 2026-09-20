package com.example.ronda.ui.ofertas;

import android.content.Context;

import androidx.annotation.ColorRes;
import androidx.annotation.Nullable;

import com.example.ronda.R;
import com.example.ronda.data.model.OfertaResponse;

/**
 * Textos y colores que comparten el historial de una publicacion
 * (OfertasAdapter) y la seccion "Mis ofertas" (MisOfertasAdapter), para que
 * una oferta se lea igual en las dos pantallas.
 */
public final class TextosOferta {

    private TextosOferta() {
    }

    /** estadoTexto ya viene traducido del backend; si faltara, se traduce el codigo. */
    public static String estado(Context ctx, OfertaResponse o) {
        return o.getEstadoTexto() != null ? o.getEstadoTexto() : estadoDeCodigo(ctx, o.getEstado());
    }

    public static String estadoDeCodigo(Context ctx, @Nullable String codigo) {
        if (OfertaResponse.ACEPTADA.equals(codigo)) return ctx.getString(R.string.estado_oferta_aceptada);
        if (OfertaResponse.RECHAZADA.equals(codigo)) return ctx.getString(R.string.estado_oferta_rechazada);
        if (OfertaResponse.VENCIDA.equals(codigo)) return ctx.getString(R.string.estado_oferta_vencida);
        if (OfertaResponse.PENDIENTE.equals(codigo)) return ctx.getString(R.string.estado_oferta_pendiente);
        return codigo != null ? codigo : "";
    }

    @ColorRes
    public static int colorDeEstado(@Nullable String estado) {
        if (OfertaResponse.ACEPTADA.equals(estado)) return R.color.estado_aceptada;
        if (OfertaResponse.RECHAZADA.equals(estado)) return R.color.estado_rechazada;
        if (OfertaResponse.VENCIDA.equals(estado)) return R.color.estado_vencida;
        return R.color.estado_pendiente;
    }

    /**
     * Para "Mis ofertas": la otra parte del hilo, que el backend ya resuelve
     * en contraparte. "Para Carla" en las enviadas, "De Carla" en las recibidas.
     */
    public static String contraparte(Context ctx, OfertaResponse o, boolean esEnviada) {
        String nombre = o.getContraparte() != null && o.getContraparte().getNombre() != null
                ? o.getContraparte().getNombre() : "";
        return ctx.getString(esEnviada ? R.string.mis_ofertas_para : R.string.mis_ofertas_de, nombre);
    }

    /**
     * Aclara que la fila no es la propuesta original sino la respuesta con
     * otro precio. Null cuando no es una contraoferta.
     */
    @Nullable
    public static String contraoferta(Context ctx, OfertaResponse o, boolean esEnviada) {
        if (!o.isEsContraoferta()) return null;
        // Enviada o recibida lo define la pestana, no esperaMiRespuesta: ese
        // flag se apaga al responderla y la etiqueta se daba vuelta sola.
        return ctx.getString(esEnviada
                ? R.string.mis_ofertas_contraoferta_enviada
                : R.string.mis_ofertas_contraoferta_recibida);
    }

    /**
     * Cuanto falta para que expire. Solo para pendientes: a las demas ya no
     * les corre el plazo. Null si no corresponde mostrar nada.
     */
    @Nullable
    public static String vencimiento(Context ctx, OfertaResponse o, long ahoraMilis) {
        if (!o.isPendiente()) return null;
        FormatoOferta.Restante r = FormatoOferta.restante(o.getExpiraEn(), ahoraMilis);
        if (r == null) return null;
        if (r.estaVencida()) return ctx.getString(R.string.oferta_vencida);
        if (r.getDias() >= 1) {
            int dias = (int) r.getDias();
            return ctx.getResources().getQuantityString(R.plurals.oferta_vence_en_dias, dias, dias);
        }
        if (r.getHoras() >= 1) return ctx.getString(R.string.oferta_vence_en_horas, r.getHoras());
        return ctx.getString(R.string.oferta_vence_en_minutos, r.getMinutos());
    }
}
