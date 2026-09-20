package com.example.ronda.data.local.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

/**
 * Base de datos local de la app (Punto 6: modo sin conexión).
 *
 * exportSchema = false porque no versionamos migraciones de Room: si cambia
 * el esquema se sube `version` y se descarta la caché, que son datos
 * descartables por definición — siempre se pueden volver a bajar del backend.
 */
@Database(entities = {PublicacionCacheada.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract PublicacionCacheadaDao publicacionCacheadaDao();
}
