package com.example.ronda;

import android.app.Application;

import dagger.hilt.android.HiltAndroidApp;

/**
 * La Application de RONDA: el primer objeto que crea Android al abrir la app,
 * antes que cualquier Activity o Fragment.
 *
 * La anotación @HiltAndroidApp arranca acá el contenedor de Hilt, que después
 * arma y reparte las dependencias (Retrofit, SessionRepository, la base de
 * Room...) a quien las pida con @Inject. Tiene que ser en la Application
 * porque es el único punto de entrada que siempre corre primero: si el
 * contenedor arrancara en una Activity y hubiera varias, no habría forma de
 * saber cuál corre antes.
 *
 * Queda registrada en el AndroidManifest con android:name=".MyApp".
 */
@HiltAndroidApp
public class MyApp extends Application {
}
