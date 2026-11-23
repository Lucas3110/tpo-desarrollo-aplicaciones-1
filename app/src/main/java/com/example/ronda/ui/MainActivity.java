package com.example.ronda.ui;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.example.ronda.R;
import com.example.ronda.data.repository.CachePublicaciones;
import com.example.ronda.di.AuthEventBus;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Única Activity de la app (Single Activity Architecture).
 *
 * No dibuja pantallas: su layout (activity_main.xml) tiene solo el
 * NavHostFragment, el marco donde el Navigation Component va poniendo y
 * sacando las pantallas, que son todas Fragments. Qué pantallas hay y cómo
 * se pasa de una a otra está en res/navigation (nav_graph.xml y los dos
 * grafos que incluye).
 *
 * Como es la única que vive mientras la app está abierta, hace además dos
 * cosas:
 *   - pide el permiso de red local al arrancar (pedirAccesoALaRedLocal)
 *   - escucha si la sesión venció y vuelve al login (observarSesionExpirada)
 *
 * La anotación @AndroidEntryPoint le avisa a Hilt que esta Activity recibe
 * dependencias con @Inject.
 */
@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    /**
     * No se usa Manifest.permission.ACCESS_LOCAL_NETWORK porque esa constante
     * solo existe compilando contra API 36+. Escrito como texto, el código
     * sigue compilando con cualquier compileSdk.
     */
    private static final String PERMISO_RED_LOCAL = "android.permission.ACCESS_LOCAL_NETWORK";
    private static final int PEDIDO_RED_LOCAL = 1001;

    /** Por acá llega el aviso de que el backend respondió 401. Lo pone Hilt. */
    @Inject
    AuthEventBus eventosDeAuth;

    /** Punto 6: lo guardado para ver sin conexión se borra al vencer la sesión. */
    @Inject
    CachePublicaciones cache;

    /** El que sabe navegar entre las pantallas del nav graph. */
    private NavController navController;

    /** Ciclo de vida: es lo primero que corre cuando se abre la Activity. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        // Infla activity_main.xml, que tiene solo el NavHostFragment.
        setContentView(R.layout.activity_main);

        // Con edge-to-edge el contenido se dibuja debajo de la barra de estado y
        // de la de navegación. Le agregamos padding al contenedor para que las
        // pantallas no queden tapadas.
        View contenedor = findViewById(R.id.nav_host_fragment);
        ViewCompat.setOnApplyWindowInsetsListener(contenedor, (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });

        // Se busca el NavHostFragment del layout para sacarle el NavController.
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            observarSesionExpirada();
        }

        pedirAccesoALaRedLocal();
    }

    /**
     * Android 16 (API 36) trajo la "Local Network Protection": el permiso
     * INTERNET dejó de alcanzar para conectarse a una dirección de la red
     * local, y hace falta ACCESS_LOCAL_NETWORK concedido en tiempo de
     * ejecución.
     *
     * Nos afecta porque el backend de desarrollo corre en la PC, que desde el
     * emulador es 10.0.2.2 y desde un celular es una IP 192.168.x.x.
     *
     * Si falta, los pedidos no dan un error de permisos: se quedan colgados
     * hasta el timeout, como si el servidor no existiera. Por eso se pide
     * apenas arranca la app y no cuando ya hay una pantalla esperando datos.
     *
     * En Android 15 o anterior el permiso no existe y esto no hace nada.
     */
    private void pedirAccesoALaRedLocal() {
        if (Build.VERSION.SDK_INT < 36) return;

        // Si ya lo tiene, no se vuelve a preguntar.
        if (ContextCompat.checkSelfPermission(this, PERMISO_RED_LOCAL)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        ActivityCompat.requestPermissions(this, new String[]{PERMISO_RED_LOCAL}, PEDIDO_RED_LOCAL);
    }

    /**
     * Punto 1: cuando el backend responde 401, el interceptor de red limpia la
     * sesión y avisa por el AuthEventBus. Acá se escucha ese aviso y se vuelve
     * al login.
     *
     * Punto 6: también se borra lo guardado para ver sin conexión, igual que
     * con el botón "Cerrar sesión" del Home. Todo 401 con sesión pasa por acá,
     * así que alcanza con hacerlo en un solo lugar.
     *
     * Tiene que estar en la Activity y no en un Fragment: el 401 puede llegar
     * estando en cualquier pantalla, y la Activity es la única que vive
     * siempre y puede navegar.
     */
    private void observarSesionExpirada() {
        // observe(this, ...) se suscribe al LiveData mientras la Activity está
        // viva: cada vez que cambia el valor, corre este bloque.
        eventosDeAuth.getSesionExpirada().observe(this, expirada -> {
            if (!Boolean.TRUE.equals(expirada)) return;

            // Se limpia antes de navegar: el LiveData guarda el último valor y
            // se lo volvería a entregar al próximo observador, pateando al
            // login apenas la persona vuelve a entrar.
            eventosDeAuth.limpiar();

            // Lo que quedó guardado es de la cuenta que se acaba de cerrar
            // (por ejemplo, cuáles tenía en favoritos): no tiene que verlo
            // quien entre después.
            cache.limpiar();

            Toast.makeText(this, R.string.sesion_expirada, Toast.LENGTH_LONG).show();

            // popUpTo con inclusive vacía la pila de pantallas: sin esto, el
            // botón "atrás" devolvería a la pantalla privada que se acaba de
            // cerrar.
            navController.navigate(
                    R.id.auth_nav_graph,
                    null,
                    new NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build());
        });
    }

    /** La flecha "atrás" de la barra de arriba también navega con el NavController. */
    @Override
    public boolean onSupportNavigateUp() {
        return navController != null && navController.navigateUp() || super.onSupportNavigateUp();
    }
}
