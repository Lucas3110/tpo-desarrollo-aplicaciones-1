package com.example.ronda.ui.home;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pruebas unitarias de FiltrosPublicaciones. Corren en la JVM de la PC con
 * ./gradlew test, sin emulador, porque la clase no depende de Android.
 */
public class FiltrosPublicacionesTest {

    @Test
    public void sinBusqueda_qEsNullYNoHayNadaAplicado() {
        FiltrosPublicaciones filtros = new FiltrosPublicaciones();

        assertNull(filtros.getQ());
        assertFalse(filtros.hayAlgoAplicado());
    }

    @Test
    public void setQ_recortaEspaciosYMarcaAplicado() {
        FiltrosPublicaciones filtros = new FiltrosPublicaciones();

        filtros.setQ("  monitor curvo ");

        assertEquals("monitor curvo", filtros.getQ());
        assertTrue(filtros.hayAlgoAplicado());
    }

    @Test
    public void setQ_vacioOSoloEspaciosEquivaleASinBusqueda() {
        FiltrosPublicaciones filtros = new FiltrosPublicaciones();

        filtros.setQ("   ");
        assertNull(filtros.getQ());

        filtros.setQ(null);
        assertNull(filtros.getQ());
        assertFalse(filtros.hayAlgoAplicado());
    }

    @Test
    public void limpiarTodo_vuelveAlEstadoInicial() {
        FiltrosPublicaciones filtros = new FiltrosPublicaciones();
        filtros.setQ("bici");

        filtros.limpiarTodo();

        assertNull(filtros.getQ());
        assertFalse(filtros.hayAlgoAplicado());
    }
}
