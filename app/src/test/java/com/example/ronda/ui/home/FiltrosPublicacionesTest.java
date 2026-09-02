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

    @Test
    public void orden_porDefectoEsRecientesYNoCuentaComoFiltro() {
        FiltrosPublicaciones filtros = new FiltrosPublicaciones();

        assertEquals("recientes", filtros.getOrden());

        filtros.setOrden("precio_desc");
        assertEquals("precio_desc", filtros.getOrden());
        assertFalse(filtros.hayAlgoAplicado());
    }

    @Test
    public void orden_nuloOVacioVuelveAlPorDefecto_yLimpiarTodoLoConserva() {
        FiltrosPublicaciones filtros = new FiltrosPublicaciones();

        filtros.setOrden(null);
        assertEquals("recientes", filtros.getOrden());

        filtros.setOrden("precio_asc");
        filtros.setQ("bici");
        filtros.limpiarTodo();
        assertEquals("precio_asc", filtros.getOrden());
    }

    @Test
    public void ordenEnum_traduceValoresDeLaApi() {
        assertEquals(Orden.PRECIO_ASC, Orden.desdeValorApi("precio_asc"));
        assertEquals(Orden.PRECIO_DESC, Orden.desdeValorApi("precio_desc"));
        assertEquals(Orden.RECIENTES, Orden.desdeValorApi("recientes"));
        assertEquals(Orden.RECIENTES, Orden.desdeValorApi("cualquier-cosa"));
        assertEquals("precio_desc", Orden.PRECIO_DESC.getValorApi());
    }
}
