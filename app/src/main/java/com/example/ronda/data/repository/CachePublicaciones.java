package com.example.ronda.data.repository;

import androidx.annotation.Nullable;

import com.example.ronda.data.local.db.AppDatabase;
import com.example.ronda.data.local.db.PublicacionCacheada;
import com.example.ronda.data.local.db.PublicacionCacheadaDao;
import com.example.ronda.data.model.PublicacionDetalleResponse;
import com.example.ronda.data.model.PublicacionItemResponse;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Guarda y recupera publicaciones del celular (Punto 6).
 *
 * Está entre las pantallas y el DAO por tres motivos:
 *
 *   1. Room no deja tocar la base desde el hilo principal. Acá vive el
 *      ExecutorService, así que ningún Fragment tiene que acordarse.
 *   2. Traduce entre el JSON guardado y los objetos que la app ya sabe
 *      dibujar.
 *   3. Devuelve los resultados en el hilo principal, que es donde los
 *      necesita la UI.
 *
 * Nada de esto es crítico: si falla la caché, la app sigue andando con los
 * datos de la red. Por eso los errores se tragan en vez de propagarse — un
 * problema guardando una copia local no puede romper una pantalla que ya
 * tiene los datos buenos.
 */
@Singleton
public class CachePublicaciones {

    /** Tope de la caché. Con 60 alcanza para varias pantallas de scroll. */
    private static final int MAXIMO = 60;

    private final AppDatabase base;
    private final PublicacionCacheadaDao dao;
    private final Gson gson = new Gson();

    /** Un solo hilo: las escrituras quedan en orden y no compiten entre sí. */
    private final ExecutorService hilo = Executors.newSingleThreadExecutor();

    @Inject
    public CachePublicaciones(AppDatabase base, PublicacionCacheadaDao dao) {
        this.base = base;
        this.dao = dao;
    }

    /** Lo que necesita la pantalla cuando lee sin conexión. */
    public static class Instantanea {
        public final List<PublicacionItemResponse> items;
        /** Milisegundos del guardado más reciente. 0 si la caché está vacía. */
        public final long guardadoEn;

        Instantanea(List<PublicacionItemResponse> items, long guardadoEn) {
            this.items = items;
            this.guardadoEn = guardadoEn;
        }

        public boolean estaVacia() {
            return items.isEmpty();
        }
    }

    /** Lo que se recupera al abrir un detalle sin conexión. */
    public static class DetalleGuardado {
        public final PublicacionDetalleResponse.Publicacion publicacion;
        public final long guardadoEn;

        DetalleGuardado(PublicacionDetalleResponse.Publicacion publicacion, long guardadoEn) {
            this.publicacion = publicacion;
            this.guardadoEn = guardadoEn;
        }
    }

    public interface Respuesta<T> {
        void listo(@Nullable T resultado);
    }

    // -----------------------------------------------------------------
    // Guardar
    // -----------------------------------------------------------------

    /** Se llama cada vez que el Home trae una página con éxito. */
    public void guardarListado(List<PublicacionItemResponse> items) {
        if (items == null || items.isEmpty()) return;

        final List<PublicacionItemResponse> copia = new ArrayList<>(items);
        hilo.execute(() -> {
            try {
                final long ahora = System.currentTimeMillis();
                // Una sola transacción: sin esto serían 20 escrituras sueltas
                // a disco, y podría quedar media página guardada si algo falla.
                base.runInTransaction(() -> {
                    for (PublicacionItemResponse item : copia) {
                        dao.guardarListado(item.getId(), gson.toJson(item), ahora);
                    }
                    dao.podar(MAXIMO);
                });
            } catch (Exception e) {
                // La caché es una comodidad: que falle no puede romper la app.
            }
        });
    }

    /** Se llama al abrir el detalle: completa la fila con lo que faltaba. */
    public void guardarDetalle(PublicacionDetalleResponse.Publicacion pub) {
        if (pub == null) return;

        hilo.execute(() -> {
            try {
                dao.guardarDetalle(pub.getId(), gson.toJson(pub), System.currentTimeMillis());
                dao.podar(MAXIMO);
            } catch (Exception e) {
                // Ídem.
            }
        });
    }

    // -----------------------------------------------------------------
    // Leer
    // -----------------------------------------------------------------

    /**
     * Las últimas publicaciones que se cargaron bien, para el Home offline.
     * El callback llega en el hilo principal.
     */
    public void ultimasVistas(Respuesta<Instantanea> respuesta) {
        hilo.execute(() -> {
            List<PublicacionItemResponse> items = new ArrayList<>();
            long guardado = 0L;
            try {
                for (PublicacionCacheada fila : dao.ultimasVistas(MAXIMO)) {
                    PublicacionItemResponse item =
                            gson.fromJson(fila.jsonListado, PublicacionItemResponse.class);
                    if (item != null) items.add(item);
                }
                Long reciente = dao.guardadoMasReciente();
                if (reciente != null) guardado = reciente;
            } catch (Exception e) {
                // Devolvemos lo que haya, aunque sea nada.
            }
            responder(respuesta, new Instantanea(items, guardado));
        });
    }

    /**
     * El detalle guardado de una publicación, o null si nunca se abrió.
     *
     * Una fila que sólo vino del listado no tiene descripción ni galería;
     * mostrarla como si fuera el detalle completo sería peor que avisar que
     * no hay nada guardado.
     */
    public void detalleGuardado(int id, Respuesta<DetalleGuardado> respuesta) {
        hilo.execute(() -> {
            DetalleGuardado resultado = null;
            try {
                PublicacionCacheada fila = dao.porId(id);
                if (fila != null && fila.jsonDetalle != null) {
                    PublicacionDetalleResponse.Publicacion pub = gson.fromJson(
                            fila.jsonDetalle, PublicacionDetalleResponse.Publicacion.class);
                    if (pub != null) resultado = new DetalleGuardado(pub, fila.guardadoEn);
                }
            } catch (Exception e) {
                // Ídem: sin caché utilizable.
            }
            responder(respuesta, resultado);
        });
    }

    /** Al cerrar sesión: lo visto es de esa persona, no del dispositivo. */
    public void limpiar() {
        hilo.execute(() -> {
            try {
                dao.borrarTodo();
            } catch (Exception e) {
                // Ídem.
            }
        });
    }

    /**
     * Devuelve el resultado en el hilo principal.
     *
     * Sin esto, quien llame tendría que acordarse de hacer el salto de hilo
     * antes de tocar una vista, y alcanza con olvidarse una vez para que la
     * app crashee con CalledFromWrongThreadException.
     */
    private <T> void responder(Respuesta<T> respuesta, @Nullable T valor) {
        new android.os.Handler(android.os.Looper.getMainLooper())
                .post(() -> respuesta.listo(valor));
    }
}
