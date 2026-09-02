package com.example.ronda.ui.home;

import java.io.Serializable;

/**
 * Lo que la persona tiene APLICADO en el Home: por ahora el texto del
 * buscador; los filtros y el orden se suman en las proximas entregas.
 *
 * Es Java puro (sin imports de Android) a proposito: se puede probar con
 * JUnit sin emulador y se guarda en el Bundle al rotar porque es Serializable.
 * Los nombres de los campos son los mismos que los parametros de la query
 * de GET /publicaciones, asi no hay que traducir nada al armar el request.
 */
public class FiltrosPublicaciones implements Serializable {

    private String q;

    /** Texto a buscar, o null si no hay busqueda (asi Retrofit no manda el parametro). */
    public String getQ() {
        return q;
    }

    /** Guarda el texto recortado; vacio o solo espacios equivale a "sin busqueda". */
    public void setQ(String texto) {
        String limpio = texto == null ? "" : texto.trim();
        this.q = limpio.isEmpty() ? null : limpio;
    }

    /** true si lo que se ve en pantalla esta acotado por algo que eligio la persona. */
    public boolean hayAlgoAplicado() {
        return q != null;
    }

    /** Vuelve al listado sin acotar. */
    public void limpiarTodo() {
        q = null;
    }
}
