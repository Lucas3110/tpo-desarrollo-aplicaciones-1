package com.example.ronda.data.local.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Una publicación guardada en el celular (Punto 6). Con @Entity, Room crea la
 * tabla publicaciones_cache: cada objeto de esta clase es una fila y cada
 * campo, una columna.
 *
 * Hay que conservar el detalle, las fotos, el precio y los datos del vendedor
 * de las publicaciones que la persona consultó, más lo último que se cargó
 * bien en el Home.
 *
 * Se guarda el JSON tal como lo mandó el backend, no columna por columna.
 * Suena raro la primera vez, pero tiene tres ventajas concretas:
 *
 *   - Lo que se lee sin conexión es idéntico a lo que se leería con conexión.
 *     No hay campos que se pierdan en la traducción.
 *   - Si el backend agrega un campo mañana, esto no se toca.
 *   - La app reconstruye el mismo objeto que ya sabe dibujar, así que las
 *     pantallas no necesitan un segundo camino para "datos de la caché".
 *
 * Lo que sí va en columnas es lo único por lo que consultamos: el id, la
 * fecha de guardado y el orden.
 *
 * Las fotos se guardan como URLs dentro de ese JSON, no como archivos: Glide
 * ya guarda las imágenes en disco por su cuenta, así que una foto que se vio
 * una vez se sigue viendo sin conexión sin que nosotros hagamos nada.
 *
 * Se usa Room y no SharedPreferences porque son muchos registros que hay que
 * ordenar, limitar a 60 y borrar por partes, y eso se resuelve con SQL.
 * SharedPreferences queda para datos sueltos, como el token.
 */
@Entity(tableName = "publicaciones_cache")
public class PublicacionCacheada {

    /** La clave primaria: el mismo id que en el backend, así una publicación nunca se duplica. */
    @PrimaryKey
    public int id;

    /**
     * El item tal como viene en el listado del Home.
     * Es lo que se muestra cuando no hay conexión.
     * (@ColumnInfo le pone el nombre a la columna.)
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
     * Cuándo se guardó, en milisegundos. Sirve para saber qué es lo más
     * reciente y para podar lo más viejo.
     */
    @ColumnInfo(name = "guardado_en")
    public long guardadoEn;

    /**
     * Qué carga del Home trajo esta fila, y en qué posición venía.
     *
     * Están para una sola cosa: que sin conexión el listado se vea en el
     * mismo orden en que lo mandó el servidor. Si se ordenara por
     * guardado_en, abrir el detalle de una publicación actualizaría esa fecha
     * y esa publicación saltaría sola al primer lugar de la lista.
     *
     * lote_carga es el momento en que empezó la carga (el mismo valor para
     * todas las páginas de un mismo scroll) y posicion es el índice absoluto
     * dentro de esa carga. Ordenando por lote descendente y posición
     * ascendente, lo último que se cargó bien queda primero y en su orden.
     */
    @ColumnInfo(name = "lote_carga")
    public long loteCarga;

    @ColumnInfo(name = "posicion")
    public int posicion;
}
