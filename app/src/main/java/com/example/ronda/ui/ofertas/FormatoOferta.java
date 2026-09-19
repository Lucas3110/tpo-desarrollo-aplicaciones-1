package com.example.ronda.ui.ofertas;

import androidx.annotation.Nullable;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Formatos que comparten las pantallas de ofertas: el monto como precio
 * argentino y las fechas ISO que manda el backend (siempre en UTC, con
 * milisegundos y "Z": "2026-09-17T22:50:37.000Z").
 *
 * No toca vistas ni recursos, asi se prueba en la JVM con ./gradlew test.
 * Las pantallas eligen el string (con plural) a partir de Restante.
 */
public final class FormatoOferta {

    private static final String PATRON_ISO = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final String PATRON_CORTO = "dd/MM/yyyy HH:mm";
    private static final long MILIS_POR_MINUTO = 60L * 1000;
    private static final long MINUTOS_POR_HORA = 60;
    private static final long MINUTOS_POR_DIA = 24 * MINUTOS_POR_HORA;

    private FormatoOferta() {
    }

    /** "$ 160.000" para montos enteros y "$ 160.000,50" si hay centavos. */
    public static String precio(double monto) {
        NumberFormat formato = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-AR"));
        formato.setMinimumFractionDigits(0);
        formato.setMaximumFractionDigits(2);
        return formato.format(monto);
    }

    /** Milisegundos desde 1970 de una fecha ISO del backend, o null si no se pudo leer. */
    @Nullable
    public static Long milisDe(@Nullable String iso) {
        if (iso == null || iso.isEmpty()) return null;
        SimpleDateFormat formato = new SimpleDateFormat(PATRON_ISO, Locale.US);
        formato.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            Date fecha = formato.parse(iso);
            return fecha != null ? fecha.getTime() : null;
        } catch (ParseException e) {
            return null;
        }
    }

    /** "15/09/2026 19:50" en la hora del celular. Cadena vacia si la fecha no se pudo leer. */
    public static String fechaCorta(@Nullable String iso) {
        return fechaCorta(iso, TimeZone.getDefault());
    }

    public static String fechaCorta(@Nullable String iso, TimeZone zona) {
        Long milis = milisDe(iso);
        if (milis == null) return "";
        SimpleDateFormat formato = new SimpleDateFormat(PATRON_CORTO, Locale.US);
        formato.setTimeZone(zona);
        return formato.format(new Date(milis));
    }

    /**
     * Cuanto falta para que venza una oferta, redondeado hacia arriba al
     * minuto. Null si el backend no mando venceEn.
     */
    @Nullable
    public static Restante restante(@Nullable String venceEnIso, long ahoraMilis) {
        Long vence = milisDe(venceEnIso);
        if (vence == null) return null;
        long diferencia = vence - ahoraMilis;
        if (diferencia <= 0) return new Restante(0);
        return new Restante((diferencia + MILIS_POR_MINUTO - 1) / MILIS_POR_MINUTO);
    }

    /** Tiempo que le queda a una oferta pendiente. */
    public static final class Restante {

        private final long minutosTotales;

        Restante(long minutosTotales) {
            this.minutosTotales = minutosTotales;
        }

        public boolean estaVencida() {
            return minutosTotales <= 0;
        }

        public long getDias() {
            return minutosTotales / MINUTOS_POR_DIA;
        }

        public long getHoras() {
            return minutosTotales / MINUTOS_POR_HORA;
        }

        public long getMinutos() {
            return minutosTotales;
        }
    }
}
