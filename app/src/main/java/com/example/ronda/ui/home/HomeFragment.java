package com.example.ronda.ui.home;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
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
 *
 * Tiene dos modos, segun el argumento "volverAlLogin":
 *
 *   false (por defecto) -> se llego por login con contrasena o por login con
 *          codigo. Es el Home de verdad: se queda ahi, con el boton de cerrar
 *          sesion. Los Puntos 3 a 6 cuelgan sus pantallas del home_nav_graph.
 *
 *   true  -> se llego de confirmar la cuenta recien creada. Se muestra una
 *          bienvenida animada y a los pocos segundos se vuelve al login para
 *          que la persona ingrese con sus credenciales.
 */
public class HomeFragment extends Fragment {

    /** Cuanto dura la bienvenida antes de volver al login. */
    private static final long DEMORA_REDIRECCION_MS = 2600L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable tareaRedireccion;

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

        TextView tvBienvenida = view.findViewById(R.id.tvBienvenida);
        TextView tvEmail = view.findViewById(R.id.tvEmail);
        TextView tvSubtitulo = view.findViewById(R.id.tvSubtitulo);
        Button btnCerrarSesion = view.findViewById(R.id.btnCerrarSesion);

        tvEmail.setText(sesion.getEmail());

        boolean volverAlLogin = getArguments() != null
                && getArguments().getBoolean("volverAlLogin", false);

        if (volverAlLogin) {
            mostrarBienvenidaYVolver(view, sesion, tvBienvenida, tvEmail, tvSubtitulo,
                    btnCerrarSesion);
        } else {
            tvSubtitulo.setVisibility(View.GONE);
            btnCerrarSesion.setVisibility(View.VISIBLE);
            btnCerrarSesion.setOnClickListener(v -> {
                // Borrar el token es lo que corta la sesion: sin el, el
                // auto-login del LoginFragment no se dispara.
                sesion.cerrarSesion();
                Navigation.findNavController(view).navigate(R.id.action_home_to_auth);
            });
        }
    }

    private void mostrarBienvenidaYVolver(View view,
                                          SessionRepository sesion,
                                          TextView tvBienvenida,
                                          TextView tvEmail,
                                          TextView tvSubtitulo,
                                          Button btnCerrarSesion) {
        // En este modo la pantalla es solo una confirmacion: no tiene sentido
        // ofrecer "cerrar sesion" si ya nos vamos solos.
        btnCerrarSesion.setVisibility(View.GONE);
        tvSubtitulo.setText(R.string.home_cuenta_verificada);
        tvSubtitulo.setVisibility(View.VISIBLE);

        aparecer(tvBienvenida, 0);
        aparecer(tvEmail, 120);
        aparecer(tvSubtitulo, 240);

        tareaRedireccion = () -> {
            // La vista pudo destruirse mientras corria la espera.
            if (!isAdded() || getView() == null) return;
            // La cuenta ya quedo verificada; que ingrese con sus credenciales.
            sesion.cerrarSesion();
            Navigation.findNavController(view).navigate(R.id.action_home_to_auth);
        };
        handler.postDelayed(tareaRedireccion, DEMORA_REDIRECCION_MS);
    }

    /** Fade-in con un leve desplazamiento hacia arriba. Sin librerias extra. */
    private void aparecer(View v, long demoraMs) {
        v.setAlpha(0f);
        v.setTranslationY(32f);
        v.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(demoraMs)
                .setDuration(400)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    @Override
    public void onDestroyView() {
        // Si no lo cancelamos, la redireccion se dispara sobre una vista que
        // ya no existe (por ejemplo si el usuario toca "atras" antes de tiempo).
        if (tareaRedireccion != null) {
            handler.removeCallbacks(tareaRedireccion);
            tareaRedireccion = null;
        }
        super.onDestroyView();
    }
}
