package com.example.ronda.ui.home;

import java.io.Serializable;

/**
 * Lo que la persona tiene APLICADO en el Home: el texto del buscador y el
 * orden; los filtros se suman en las proximas entregas.
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

    /** Texto a buscar, o null si no hay busqueda (asi Retrofit no manda el parametro). */
    public String getQ() {
        return q;
    }

    /** Guarda el texto recortado; vacio o solo espacios equivale a "sin busqueda". */
    public void setQ(String texto) {
        String limpio = texto == null ? "" : texto.trim();
        this.q = limpio.isEmpty() ? null : limpio;
    }

    /** Valor de la API: recientes, precio_asc o precio_desc. Nunca null. */
    public String getOrden() {
        return orden;
    }

    public void setOrden(String orden) {
        this.orden = (orden == null || orden.trim().isEmpty()) ? ORDEN_POR_DEFECTO : orden.trim();
    }

    /**
     * true si lo que se ve en pantalla esta acotado por algo que eligio la
     * persona. El orden no cuenta: cambia como se ve, no cuanto se ve.
     */
    public boolean hayAlgoAplicado() {
        return q != null;
    }

    /** Vuelve al listado sin acotar. Mantiene el orden elegido. */
    public void limpiarTodo() {
        q = null;
    }
}
