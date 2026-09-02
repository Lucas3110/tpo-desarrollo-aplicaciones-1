package com.example.ronda.ui.home;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Lo que la persona tiene APLICADO en el Home: texto del buscador, orden y
 * filtros (categoria, rango de precio, estado del articulo y "solo mi
 * zona"). Todo se combina en un solo request.
 *
 * Es Java puro (sin imports de Android) a proposito: se puede probar con
 * JUnit sin emulador y se guarda en el Bundle al rotar porque es Serializable.
 * Los nombres de los campos son los mismos que los parametros de la query
 * de GET /publicaciones, asi no hay que traducir nada al armar el request.
 */
public class FiltrosPublicaciones implements Serializable {

    /** Valor por defecto del backend, para arrancar igual que sin parametro. */
    public static final String ORDEN_POR_DEFECTO = "recientes";

    private String q;
    private String orden = ORDEN_POR_DEFECTO;

    private Integer categoriaId;
    private Double precioMin;
    private Double precioMax;
    private boolean nuevo;
    private boolean comoNuevo;
    private boolean usado;
    /** Filtrar por la zona de la persona (zonaId sale de la sesion, no de aca). */
    private boolean soloMiZona;

    // ---------------------------------------------------------------
    // Busqueda
    // ---------------------------------------------------------------

    /** Texto a buscar, o null si no hay busqueda (asi Retrofit no manda el parametro). */
    public String getQ() {
        return q;
    }

    /** Guarda el texto recortado; vacio o solo espacios equivale a "sin busqueda". */
    public void setQ(String texto) {
        String limpio = texto == null ? "" : texto.trim();
        this.q = limpio.isEmpty() ? null : limpio;
    }

    // ---------------------------------------------------------------
    // Orden
    // ---------------------------------------------------------------

    /** Valor de la API: recientes, precio_asc o precio_desc. Nunca null. */
    public String getOrden() {
        return orden;
    }

    public void setOrden(String orden) {
        this.orden = (orden == null || orden.trim().isEmpty()) ? ORDEN_POR_DEFECTO : orden.trim();
    }

    // ---------------------------------------------------------------
    // Filtros
    // ---------------------------------------------------------------

    /** Id de categoria, o null para "todas". */
    public Integer getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(Integer categoriaId) {
        this.categoriaId = categoriaId;
    }

    public Double getPrecioMin() {
        return precioMin;
    }

    public void setPrecioMin(Double precioMin) {
        this.precioMin = precioMin;
    }

    public Double getPrecioMax() {
        return precioMax;
    }

    public void setPrecioMax(Double precioMax) {
        this.precioMax = precioMax;
    }

    /** false si falta alguno de los dos extremos: sin los dos no hay rango que validar. */
    public boolean rangoPrecioInvalido() {
        return precioMin != null && precioMax != null && precioMin > precioMax;
    }

    public boolean isNuevo() {
        return nuevo;
    }

    public boolean isComoNuevo() {
        return comoNuevo;
    }

    public boolean isUsado() {
        return usado;
    }

    public void setEstados(boolean nuevo, boolean comoNuevo, boolean usado) {
        this.nuevo = nuevo;
        this.comoNuevo = comoNuevo;
        this.usado = usado;
    }

    /**
     * Valor del parametro estadoArticulo: "NUEVO,COMO_NUEVO" para los
     * marcados. Null si no hay ninguno marcado o estan los tres (en los dos
     * casos se quiere todo, y asi no viaja el parametro).
     */
    public String getEstadoArticuloParam() {
        if (!hayEstadoFiltrado()) return null;
        List<String> estados = new ArrayList<>();
        if (nuevo) estados.add("NUEVO");
        if (comoNuevo) estados.add("COMO_NUEVO");
        if (usado) estados.add("USADO");
        return String.join(",", estados);
    }

    private boolean hayEstadoFiltrado() {
        boolean alguno = nuevo || comoNuevo || usado;
        boolean todos = nuevo && comoNuevo && usado;
        return alguno && !todos;
    }

    public boolean isSoloMiZona() {
        return soloMiZona;
    }

    public void setSoloMiZona(boolean soloMiZona) {
        this.soloMiZona = soloMiZona;
    }

    /**
     * Resumen de TODO lo que el panel manda en la request. Se compara antes y
     * despues de "Aplicar" para pedir de nuevo solo si algo cambio. Vive aca,
     * al lado de los campos, para que un filtro nuevo no quede afuera.
     */
    public String claveDeFiltros() {
        return categoriaId + "|" + precioMin + "|" + precioMax + "|" + getEstadoArticuloParam()
                + "|" + soloMiZona;
    }

    /** Cuantos filtros hay puestos, para el boton "Filtros (n)". */
    public int contarFiltros() {
        int cantidad = 0;
        if (categoriaId != null) cantidad++;
        if (precioMin != null || precioMax != null) cantidad++;
        if (hayEstadoFiltrado()) cantidad++;
        if (soloMiZona) cantidad++;
        return cantidad;
    }

    /** Saca solo los filtros del panel; conserva la busqueda y el orden. */
    public void limpiarFiltros() {
        categoriaId = null;
        precioMin = null;
        precioMax = null;
        nuevo = false;
        comoNuevo = false;
        usado = false;
        soloMiZona = false;
    }

    // ---------------------------------------------------------------

    /**
     * true si lo que se ve en pantalla esta acotado por algo que eligio la
     * persona. El orden no cuenta: cambia como se ve, no cuanto se ve.
     */
    public boolean hayAlgoAplicado() {
        return q != null || contarFiltros() > 0;
    }

    /** Vuelve al listado sin acotar. Mantiene el orden elegido. */
    public void limpiarTodo() {
        q = null;
        limpiarFiltros();
    }
}
