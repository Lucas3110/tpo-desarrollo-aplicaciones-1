package com.example.ronda.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
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
