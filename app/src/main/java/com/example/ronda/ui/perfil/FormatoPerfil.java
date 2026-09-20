package com.example.ronda.ui.perfil;

import android.content.Context;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.ronda.R;
import com.example.ronda.data.model.ReputacionResponse;

/**
 * Lo que comparten las pantallas de perfil (Mi perfil y el perfil publico):
 * la foto y los textos de reputacion y antiguedad.
 */
public final class FormatoPerfil {

    private static final int DIAS_POR_MES = 30;
    private static final int DIAS_POR_ANIO = 365;

    private FormatoPerfil() {
    }

    /**
     * "4,5 ★ · 12 calificaciones". Sin calificaciones el promedio viene null
     * y se dice eso, en vez de mostrar un "0,0" que parece una mala nota.
     */
    public static String reputacionEstrellas(Context ctx, @Nullable ReputacionResponse rep) {
        if (rep == null || !rep.tienePromedio()) {
            return ctx.getString(R.string.reputacion_vacia);
        }
        int cantidad = rep.getCantidadCalificaciones();
        return ctx.getResources().getQuantityString(R.plurals.perfil_reputacion_estrellas,
                cantidad, rep.getPromedioEstrellas(), cantidad);
    }

    /** "Ventas concretadas: 8 · Compras concretadas: 3". */
    public static String reputacionOperaciones(Context ctx, @Nullable ReputacionResponse rep) {
        int ventas = rep != null ? rep.getOperacionesComoVendedor() : 0;
        int compras = rep != null ? rep.getOperacionesComoComprador() : 0;
        return ctx.getString(R.string.perfil_reputacion_operaciones, ventas, compras);
    }

    /**
     * Antiguedad en la plataforma en la unidad que se lea mejor: "hoy",
     * dias hasta el mes, despues meses y despues anios. Los dias los calcula
     * el backend (antiguedadDias), la app no hace cuentas con fechas.
     */
    public static String antiguedad(Context ctx, int dias) {
        if (dias <= 0) {
            return ctx.getString(R.string.perfil_miembro_hoy);
        }
        if (dias < DIAS_POR_MES) {
            return ctx.getResources().getQuantityString(R.plurals.perfil_miembro_dias, dias, dias);
        }
        if (dias < DIAS_POR_ANIO) {
            int meses = dias / DIAS_POR_MES;
            return ctx.getResources().getQuantityString(R.plurals.perfil_miembro_meses, meses, meses);
        }
        int anios = dias / DIAS_POR_ANIO;
        return ctx.getResources().getQuantityString(R.plurals.perfil_miembro_anios, anios, anios);
    }

    /**
     * Foto circular con Glide. Sin foto, o si la URL no se puede cargar
     * (por ejemplo un content:// al que se perdio el permiso), queda el
     * avatar por defecto en vez de un hueco.
     */
    public static void cargarAvatar(ImageView vista, @Nullable String url) {
        if (url == null || url.isEmpty()) {
            Glide.with(vista).clear(vista);
            vista.setImageResource(R.drawable.ic_persona);
            return;
        }
        Glide.with(vista)
                .load(url)
                .circleCrop()
                .placeholder(R.drawable.ic_persona)
                .error(R.drawable.ic_persona)
                .into(vista);
    }
}
