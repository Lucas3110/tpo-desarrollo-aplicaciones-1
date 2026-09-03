package com.example.ronda;

import android.app.Application;

import dagger.hilt.android.HiltAndroidApp;

/**
 * Clase Application de la app.
 *
 * Android siempre crea un objeto Application al arrancar, ANTES que cualquier
 * Activity o Fragment. Hasta ahora usábamos el que trae el sistema por defecto
 * y por eso no existía este archivo.
 *
 * @HiltAndroidApp le dice a Hilt que arranque acá el grafo de dependencias.
 * Tiene que ser en la Application porque es el único punto de entrada
 * garantizado: si el grafo se inicializara en una Activity y hubiera varias,
 * no habría forma de saber cuál corre primero.
 *
 * Queda registrada en el AndroidManifest con android:name=".MyApp".
 */
@HiltAndroidApp
public class MyApp extends Application {
}
