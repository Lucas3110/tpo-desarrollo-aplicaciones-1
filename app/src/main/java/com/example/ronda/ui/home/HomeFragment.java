package com.example.ronda.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.ronda.R;
import com.example.ronda.data.repository.SessionRepository;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Pantalla a la que se llega con la sesion ya iniciada, por login con
 * contrasena o por login con codigo. Los Puntos 3 a 6 cuelgan sus pantallas
 * del home_nav_graph.
 *
 * La bienvenida que se muestra al confirmar una cuenta nueva NO pasa por aca:
 * vive en ui/auth/BienvenidaFragment, dentro del flujo de autenticacion.
 */
@AndroidEntryPoint
public class HomeFragment extends Fragment {

    @Inject
    SessionRepository sesion;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvEmail = view.findViewById(R.id.tvEmail);
        Button btnCerrarSesion = view.findViewById(R.id.btnCerrarSesion);

        tvEmail.setText(sesion.getEmail());

        btnCerrarSesion.setOnClickListener(v -> {
            // Borrar el token es lo que corta la sesion: sin el, el
            // auto-login del LoginFragment no se dispara.
            sesion.cerrarSesion();
            Navigation.findNavController(view).navigate(R.id.action_home_to_auth);
        });
    }
}
