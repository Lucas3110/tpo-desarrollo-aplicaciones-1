package com.example.ronda.ui.ofertas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.TimeZone;

/** Fechas ISO del backend y cuenta regresiva del vencimiento de una oferta. */
public class FormatoOfertaTest {

    private static final String VENCE = "2026-09-17T22:50:37.000Z";

    private static long milis(String iso) {
        Long valor = FormatoOferta.milisDe(iso);
        assertNotNull("fecha invalida en el test: " + iso, valor);
        return valor;
    }

    @Test
    public void milisDe_leeLaFechaIsoEnUtc() {
        // 1970-01-01T00:00:00.000Z es el origen: cero milisegundos.
        assertEquals(Long.valueOf(0), FormatoOferta.milisDe("1970-01-01T00:00:00.000Z"));
        // Un dia entero despues.
        assertEquals(Long.valueOf(24L * 60 * 60 * 1000), FormatoOferta.milisDe("1970-01-02T00:00:00.000Z"));
    }

    @Test
    public void milisDe_devuelveNullSiNoEsUnaFechaDelBackend() {
        assertNull(FormatoOferta.milisDe(null));
        assertNull(FormatoOferta.milisDe(""));
        assertNull(FormatoOferta.milisDe("ayer"));
        assertNull(FormatoOferta.milisDe("2026-09-17"));
    }

    @Test
    public void fechaCorta_formateaEnLaZonaPedida() {
        assertEquals("17/09/2026 22:50", FormatoOferta.fechaCorta(VENCE, TimeZone.getTimeZone("UTC")));
        assertEquals("17/09/2026 19:50", FormatoOferta.fechaCorta(VENCE, TimeZone.getTimeZone("America/Argentina/Buenos_Aires")));
        assertEquals("", FormatoOferta.fechaCorta("nada", TimeZone.getTimeZone("UTC")));
    }

    @Test
    public void restante_dosDiasAntes_cuentaDiasHorasYMinutos() {
        FormatoOferta.Restante r = FormatoOferta.restante(VENCE, milis("2026-09-15T22:50:37.000Z"));

        assertNotNull(r);
        assertFalse(r.estaVencida());
        assertEquals(2, r.getDias());
        assertEquals(48, r.getHoras());
        assertEquals(2880, r.getMinutos());
    }

    @Test
    public void restante_menosDeUnDia_noTieneDiasPeroSiHoras() {
        FormatoOferta.Restante r = FormatoOferta.restante(VENCE, milis("2026-09-17T20:50:37.000Z"));

        assertEquals(0, r.getDias());
        assertEquals(2, r.getHoras());
    }

    @Test
    public void restante_redondeaHaciaArribaAlMinuto() {
        // Faltan 15 minutos y 37 segundos: se muestran 16.
        FormatoOferta.Restante r = FormatoOferta.restante(VENCE, milis("2026-09-17T22:35:00.000Z"));

        assertEquals(0, r.getHoras());
        assertEquals(16, r.getMinutos());
        assertFalse(r.estaVencida());
    }

    @Test
    public void restante_enElMomentoExactoODespues_estaVencida() {
        assertTrue(FormatoOferta.restante(VENCE, milis(VENCE)).estaVencida());
        assertTrue(FormatoOferta.restante(VENCE, milis("2026-09-18T00:00:00.000Z")).estaVencida());
        assertEquals(0, FormatoOferta.restante(VENCE, milis("2026-09-18T00:00:00.000Z")).getMinutos());
    }

    @Test
    public void restante_sinFecha_esNull() {
        assertNull(FormatoOferta.restante(null, 0));
        assertNull(FormatoOferta.restante("", 0));
    }
}
