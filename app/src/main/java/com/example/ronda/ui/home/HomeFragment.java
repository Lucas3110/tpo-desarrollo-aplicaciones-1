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

/**
 * Pantalla a la que se llega con la sesion ya iniciada.
 * En el Punto 1 solo confirma que el ingreso funciono y permite cerrar
 * sesion. Los Puntos 3 a 6 cuelgan sus pantallas del home_nav_graph.
 */
public class HomeFragment extends Fragment {

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

        SessionRepository sesion = new SessionRepository(requireContext());

        TextView tvEmail = view.findViewById(R.id.tvEmail);
        tvEmail.setText(sesion.getEmail());

        Button btnCerrarSesion = view.findViewById(R.id.btnCerrarSesion);
        btnCerrarSesion.setOnClickListener(v -> {
            // Borrar el token es lo que corta la sesion: sin el, el
            // auto-login del LoginFragment no se dispara.
            sesion.cerrarSesion();
            Navigation.findNavController(view).navigate(R.id.action_home_to_auth);
        });
    }
}
