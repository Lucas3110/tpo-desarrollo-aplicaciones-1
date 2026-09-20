package com.example.ronda.data.local.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Una publicación guardada en el celular (Punto 6).
 *
 * El enunciado pide conservar "detalle, fotos, precio y datos del vendedor"
 * de las publicaciones que la persona consultó, más lo último que se cargó
 * bien en el Home.
 *
 * Se guarda **el JSON tal como lo mandó el backend**, no columna por columna.
 * Suena raro la primera vez, pero tiene tres ventajas concretas:
 *
 *   - Lo que se lee sin conexión es idéntico a lo que se leería con conexión.
 *     No hay campos que se pierdan en la traducción.
 *   - Si el backend agrega un campo mañana, esto no se toca.
 *   - La app reconstruye el mismo objeto que ya sabe dibujar, así que las
 *     pantallas no necesitan un segundo camino para "datos de la caché".
 *
 * Lo que sí va en columnas es lo único por lo que consultamos: el id y la
 * fecha de guardado.
 *
 * Las fotos se guardan como URLs dentro de ese JSON, no como archivos: Glide
 * ya cachea las imágenes en disco por su cuenta, así que una foto que se vio
 * una vez se sigue viendo sin conexión sin que nosotros hagamos nada.
 */
@Entity(tableName = "publicaciones_cache")
public class PublicacionCacheada {

    /** El mismo id que en el backend, así una publicación nunca se duplica. */
    @PrimaryKey
    public int id;

    /**
     * El item tal como viene en el listado del Home.
     * Es lo que se repinta cuando no hay conexión.
     */
    @ColumnInfo(name = "json_listado")
    public String jsonListado;

    /**
     * El detalle completo, con descripción, galería y vendedor.
     * Queda en null mientras la persona no haya abierto esa publicación.
     */
    @ColumnInfo(name = "json_detalle")
    public String jsonDetalle;

    /**
     * Cuándo se guardó, en milisegundos. Es lo que permite decirle a la
     * persona "estos datos son de las 14:30" en vez de mostrarle información
     * vieja como si fuera de ahora.
     */
    @ColumnInfo(name = "guardado_en")
    public long guardadoEn;

    /**
     * Que carga del Home trajo esta fila, y en que posicion venia.
     *
     * Estan para una sola cosa: que sin conexion el listado se vea en el
     * mismo orden en que lo mando el servidor. Antes se ordenaba por
     * guardado_en, y como abrir el detalle de una publicacion actualiza esa
     * fecha, esa publicacion se saltaba sola al primer lugar de la lista.
     *
     * lote_carga es el momento en que empezo la carga (el mismo valor para
     * todas las paginas de un mismo scroll) y posicion es el indice absoluto
     * dentro de esa carga. Ordenando por lote descendente y posicion
     * ascendente, lo ultimo que se cargo bien queda primero y en su orden.
     */
    @ColumnInfo(name = "lote_carga")
    public long loteCarga;

    @ColumnInfo(name = "posicion")
    public int posicion;
}
