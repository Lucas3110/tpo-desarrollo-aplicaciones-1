package com.example.ronda.data.local.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

/**
 * La base de datos local de la app, hecha con Room (Punto 6: modo sin
 * conexión).
 *
 * Room es una capa sobre SQLite: nosotros escribimos esta clase abstracta con
 * la anotación @Database, y Room genera el código que la implementa
 * (AppDatabase_Impl, que aparece en la carpeta "java (generated)").
 *
 * entities son las tablas (acá, una sola) y version se sube cuando cambia el
 * esquema. exportSchema = false porque no versionamos migraciones de Room: si
 * cambia el esquema se sube la versión y se descarta la caché, que son datos
 * descartables por definición: siempre se pueden volver a bajar del backend.
 *
 * La crea StorageModule, una sola vez para toda la app.
 */
@Database(entities = {PublicacionCacheada.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    /** Room implementa este método: devuelve el DAO de la tabla. */
    public abstract PublicacionCacheadaDao publicacionCacheadaDao();
}
