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
    public void estadoArticulo_seMandaSoloSiHayAlgunosMarcadosPeroNoTodos() {
        FiltrosPublicaciones filtros = new FiltrosPublicaciones();

        assertNull(filtros.getEstadoArticuloParam());

        filtros.setEstados(true, false, true);
        assertEquals("NUEVO,USADO", filtros.getEstadoArticuloParam());

        filtros.setEstados(false, true, false);
        assertEquals("COMO_NUEVO", filtros.getEstadoArticuloParam());

        // Los tres marcados es lo mismo que ninguno: no viaja el parametro.
        filtros.setEstados(true, true, true);
        assertNull(filtros.getEstadoArticuloParam());
        assertEquals(0, filtros.contarFiltros());
    }

    @Test
    public void contarFiltros_cuentaCategoriaRangoYEstadoComoUnoCadaUno() {
        FiltrosPublicaciones filtros = new FiltrosPublicaciones();
        assertEquals(0, filtros.contarFiltros());

        filtros.setCategoriaId(2);
        assertEquals(1, filtros.contarFiltros());

        filtros.setPrecioMin(1000.0);
        filtros.setPrecioMax(5000.0);
        assertEquals(2, filtros.contarFiltros());

        filtros.setEstados(false, false, true);
        assertEquals(3, filtros.contarFiltros());
        assertTrue(filtros.hayAlgoAplicado());
    }

    @Test
    public void rangoPrecio_esInvalidoSoloConLosDosExtremosYMinMayorQueMax() {
        FiltrosPublicaciones filtros = new FiltrosPublicaciones();

        filtros.setPrecioMin(5000.0);
        assertFalse(filtros.rangoPrecioInvalido());

        filtros.setPrecioMax(1000.0);
        assertTrue(filtros.rangoPrecioInvalido());

        filtros.setPrecioMax(5000.0);
        assertFalse(filtros.rangoPrecioInvalido());
    }

    @Test
    public void limpiarFiltros_conservaBusquedaYOrden_limpiarTodoSoloElOrden() {
        FiltrosPublicaciones filtros = new FiltrosPublicaciones();
        filtros.setQ("bici");
        filtros.setOrden("precio_asc");
        filtros.setCategoriaId(7);
        filtros.setPrecioMax(90000.0);
        filtros.setEstados(true, false, false);

        filtros.limpiarFiltros();
        assertEquals("bici", filtros.getQ());
        assertEquals("precio_asc", filtros.getOrden());
        assertNull(filtros.getCategoriaId());
        assertNull(filtros.getPrecioMax());
        assertNull(filtros.getEstadoArticuloParam());
        assertEquals(0, filtros.contarFiltros());

        filtros.limpiarTodo();
        assertNull(filtros.getQ());
        assertEquals("precio_asc", filtros.getOrden());
        assertFalse(filtros.hayAlgoAplicado());
    }

    @Test
    public void claveDeFiltros_cambiaConCadaFiltroYNoConBusquedaNiOrden() {
        FiltrosPublicaciones filtros = new FiltrosPublicaciones();
        String base = filtros.claveDeFiltros();

        filtros.setQ("bici");
        filtros.setOrden("precio_asc");
        assertEquals(base, filtros.claveDeFiltros());

        filtros.setCategoriaId(3);
        String conCategoria = filtros.claveDeFiltros();
        assertFalse(base.equals(conCategoria));

        filtros.setPrecioMin(1000.0);
        String conMin = filtros.claveDeFiltros();
        assertFalse(conCategoria.equals(conMin));

        filtros.setPrecioMax(9000.0);
        String conMax = filtros.claveDeFiltros();
        assertFalse(conMin.equals(conMax));

        filtros.setEstados(true, false, false);
        assertFalse(conMax.equals(filtros.claveDeFiltros()));

        filtros.limpiarFiltros();
        assertEquals(base, filtros.claveDeFiltros());
    }

    @Test
    public void soloMiZona_cambiaLaClaveDeFiltros() {
        FiltrosPublicaciones filtros = new FiltrosPublicaciones();
        String sinZona = filtros.claveDeFiltros();

        filtros.setSoloMiZona(true);

        assertFalse(sinZona.equals(filtros.claveDeFiltros()));
    }

    @Test
    public void soloMiZona_cuentaComoFiltroYSeLimpiaConLosDemas() {
        FiltrosPublicaciones filtros = new FiltrosPublicaciones();

        filtros.setSoloMiZona(true);
        assertTrue(filtros.isSoloMiZona());
        assertEquals(1, filtros.contarFiltros());
        assertTrue(filtros.hayAlgoAplicado());

        filtros.limpiarFiltros();
        assertFalse(filtros.isSoloMiZona());
        assertEquals(0, filtros.contarFiltros());
    }

    @Test
    public void ordenEnum_incluyeCercania() {
        assertEquals(Orden.CERCANIA, Orden.desdeValorApi("cercania"));
        assertEquals("cercania", Orden.CERCANIA.getValorApi());
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
