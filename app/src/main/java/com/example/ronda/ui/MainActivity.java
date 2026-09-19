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
import com.example.ronda.di.AuthEventBus;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Unica Activity de la app (Single Activity Architecture).
 * No dibuja pantallas: solo hospeda el NavHostFragment, y cada pantalla
 * es un Fragment que el Navigation Component va intercambiando ahi adentro.
 */
@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    /**
     * No se usa Manifest.permission.ACCESS_LOCAL_NETWORK porque esa constante
     * solo existe compilando contra API 36+. Con el texto plano, el codigo
     * sigue compilando con cualquier compileSdk.
     */
    private static final String PERMISO_RED_LOCAL = "android.permission.ACCESS_LOCAL_NETWORK";
    private static final int PEDIDO_RED_LOCAL = 1001;

    @Inject
    AuthEventBus eventosDeAuth;

    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Con edge-to-edge el contenido se dibuja debajo de la barra de estado y
        // de la de navegacion. Le agregamos padding al contenedor para que las
        // pantallas no queden tapadas.
        View contenedor = findViewById(R.id.nav_host_fragment);
        ViewCompat.setOnApplyWindowInsetsListener(contenedor, (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            observarSesionExpirada();
        }

        pedirAccesoALaRedLocal();
    }

    /**
     * Android 16 (API 36) estrenó la "Local Network Protection": el permiso
     * INTERNET dejó de alcanzar para conectarse a una dirección de la red
     * local, y hace falta ACCESS_LOCAL_NETWORK concedido en runtime.
     *
     * Nos pega de lleno porque el backend de desarrollo vive en la PC, que
     * desde el emulador es 10.0.2.2 y desde un celular es una IP 192.168.x.x.
     *
     * Lo feo del caso es cómo falla si no está: las requests no dan un error
     * de permisos, se quedan colgadas hasta el timeout como si el servidor no
     * existiera. Por eso conviene pedirlo apenas arranca la app y no cuando
     * ya hay una pantalla esperando datos.
     *
     * En Android 15 o anterior el permiso no existe y esto no hace nada.
     */
    private void pedirAccesoALaRedLocal() {
        if (Build.VERSION.SDK_INT < 36) return;

        if (ContextCompat.checkSelfPermission(this, PERMISO_RED_LOCAL)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        ActivityCompat.requestPermissions(this, new String[]{PERMISO_RED_LOCAL}, PEDIDO_RED_LOCAL);
    }

    /**
     * Punto 1: cuando el backend responde 401, el interceptor de red limpia la
     * sesion y avisa por el AuthEventBus. Aca se escucha ese aviso y se vuelve
     * al login.
     *
     * Tiene que estar en la Activity y no en un Fragment: el 401 puede llegar
     * estando en cualquier pantalla, y la Activity es la unica que vive
     * siempre y puede navegar.
     */
    private void observarSesionExpirada() {
        eventosDeAuth.getSesionExpirada().observe(this, expirada -> {
            if (!Boolean.TRUE.equals(expirada)) return;

            // Se limpia antes de navegar: el LiveData guarda el ultimo valor y
            // se lo volveria a entregar al proximo observador, pateando al
            // login apenas la persona vuelve a entrar.
            eventosDeAuth.limpiar();

            Toast.makeText(this, R.string.sesion_expirada, Toast.LENGTH_LONG).show();

            // popUpTo con inclusive borra todo el back stack: sin esto, el
            // boton "atras" devolveria a la pantalla privada que se acaba de
            // cerrar.
            navController.navigate(
                    R.id.auth_nav_graph,
                    null,
                    new NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build());
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController != null && navController.navigateUp() || super.onSupportNavigateUp();
    }
}
