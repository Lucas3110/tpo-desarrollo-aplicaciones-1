package com.example.ronda.di;

import android.content.Context;

import androidx.room.Room;

import com.example.ronda.data.local.db.AppDatabase;
import com.example.ronda.data.local.db.PublicacionCacheadaDao;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

/**
 * Le enseña a Hilt cómo construir el almacenamiento local (Punto 6).
 *
 * Mismo patrón que NetworkModule: en vez de que cada pantalla arme su propia
 * base con Room.databaseBuilder(), Hilt la crea una sola vez y la inyecta.
 * Eso importa más de lo que parece — abrir dos instancias de la misma base
 * de datos es una fuente clásica de bloqueos.
 */
@Module
@InstallIn(SingletonComponent.class)
public class StorageModule {

    private static final String NOMBRE_BASE = "ronda_cache";

    @Provides
    @Singleton
    public AppDatabase provideAppDatabase(@ApplicationContext Context contexto) {
        return Room.databaseBuilder(contexto, AppDatabase.class, NOMBRE_BASE)
                // Si cambia el esquema, se tira la caché y se vuelve a bajar.
                // Son datos descartables: no hay nada que migrar.
                .fallbackToDestructiveMigration(true)
                .build();
    }

    /**
     * El DAO se provee aparte para que las pantallas pidan sólo lo que usan y
     * no la base entera.
     */
    @Provides
    @Singleton
    public PublicacionCacheadaDao providePublicacionCacheadaDao(AppDatabase base) {
        return base.publicacionCacheadaDao();
    }
}
