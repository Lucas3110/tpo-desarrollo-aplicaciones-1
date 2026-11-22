package com.example.ronda.data.local.db;

import androidx.room.Dao;
import androidx.room.Query;

import java.util.List;

/**
 * El DAO (Data Access Object) de la caché (Punto 6): las consultas a la
 * tabla publicaciones_cache. Es abstracto porque Room escribe el código de
 * cada método a partir del SQL de su @Query (la clase generada es
 * PublicacionCacheadaDao_Impl). Los ":nombre" del SQL se reemplazan por el
 * parámetro del método que se llama igual.
 *
 * Nada de esto corre solo en segundo plano: son métodos sincrónicos, y quien
 * los llama (CachePublicaciones) tiene que hacerlo en un hilo aparte. Room
 * directamente tira una excepción si se los invoca en el hilo principal, que
 * es su forma de impedir que una consulta a disco trabe la pantalla.
 */
@Dao
public abstract class PublicacionCacheadaDao {

    /**
     * Guarda o actualiza el resumen que viene del listado.
     *
     * Es un UPSERT a mano (INSERT ... ON CONFLICT DO UPDATE) en vez de un
     * @Insert(REPLACE) por un motivo: si la publicación ya estaba con su
     * detalle guardado, un REPLACE le pisaría el json_detalle con null y la
     * persona perdería lo que ya había abierto. Acá el detalle solo se toca
     * si viene uno nuevo.
     */
    @Query("INSERT INTO publicaciones_cache "
            + "(id, json_listado, json_detalle, guardado_en, lote_carga, posicion) "
            + "VALUES (:id, :jsonListado, NULL, :guardadoEn, :lote, :posicion) "
            + "ON CONFLICT(id) DO UPDATE SET json_listado = :jsonListado, "
            + "guardado_en = :guardadoEn, lote_carga = :lote, posicion = :posicion")
    public abstract void guardarListado(int id, String jsonListado, long guardadoEn,
                                        long lote, int posicion);

    /** Lo mismo, del otro lado: guardar el detalle no borra el resumen del listado. */
    @Query("INSERT INTO publicaciones_cache "
            + "(id, json_listado, json_detalle, guardado_en, lote_carga, posicion) "
            + "VALUES (:id, NULL, :jsonDetalle, :guardadoEn, 0, 0) "
            + "ON CONFLICT(id) DO UPDATE SET json_detalle = :jsonDetalle, guardado_en = :guardadoEn")
    public abstract void guardarDetalle(int id, String jsonDetalle, long guardadoEn);

    /**
     * Lo último que se cargó bien en el Home, en el mismo orden en que lo
     * mandó el servidor: primero la carga más reciente y, dentro de ella, la
     * posición original. Abrir un detalle no reordena nada.
     */
    @Query("SELECT * FROM publicaciones_cache WHERE json_listado IS NOT NULL "
            + "ORDER BY lote_carga DESC, posicion ASC, id ASC LIMIT :limite")
    public abstract List<PublicacionCacheada> ultimasVistas(int limite);

    /** Una publicación por id (para el detalle sin conexión). */
    @Query("SELECT * FROM publicaciones_cache WHERE id = :id LIMIT 1")
    public abstract PublicacionCacheada porId(int id);

    /** La fecha del guardado más reciente. */
    @Query("SELECT MAX(guardado_en) FROM publicaciones_cache")
    public abstract Long guardadoMasReciente();

    /**
     * La caché no puede crecer para siempre: se conservan las N más recientes
     * (CachePublicaciones pasa 60).
     */
    @Query("DELETE FROM publicaciones_cache WHERE id NOT IN "
            + "(SELECT id FROM publicaciones_cache ORDER BY guardado_en DESC LIMIT :cuantasConservar)")
    public abstract void podar(int cuantasConservar);

    /** Al cerrar sesión o cuando vence: lo visto es de esa persona, no del celular. */
    @Query("DELETE FROM publicaciones_cache")
    public abstract void borrarTodo();
}
