package com.example.ronda.ui.auth;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.ronda.R;

/**
 * Confirmacion que se muestra despues de verificar una cuenta recien creada.
 *
 * Es una pantalla de paso: anima tres textos y a los pocos segundos vuelve
 * sola al login para que la persona ingrese con sus credenciales. No crea
 * sesion ni necesita nada inyectado: recibe el email por argumento porque
 * en este punto todavia no hay nada guardado en el celular.
 *
 * Antes esta logica vivia adentro de HomeFragment como un "modo bienvenida";
 * se separo para que el Home sea solo el Home (listado del Punto 3).
 */
public class BienvenidaFragment extends Fragment {

    /** Cuanto dura la bienvenida antes de volver al login. */
    private static final long DEMORA_REDIRECCION_MS = 2600L;

    /** Desde donde "sube" cada texto al aparecer. */
    private static final float DESPLAZAMIENTO_DP = 32f;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable tareaRedireccion;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bienvenida, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String email = getArguments() != null ? getArguments().getString("email", "") : "";

        TextView tvBienvenida = view.findViewById(R.id.tvBienvenida);
        TextView tvEmail = view.findViewById(R.id.tvEmail);
        TextView tvSubtitulo = view.findViewById(R.id.tvSubtitulo);

        tvEmail.setText(email);

        aparecer(tvBienvenida, 0);
        aparecer(tvEmail, 120);
        aparecer(tvSubtitulo, 240);

        tareaRedireccion = () -> {
            // La vista pudo destruirse mientras corria la espera.
            if (!isAdded() || getView() == null) return;
            // La accion que trajo aca dejo al login justo debajo en la pila,
            // asi que con volver un paso alcanza.
            Navigation.findNavController(view).popBackStack();
        };
        handler.postDelayed(tareaRedireccion, DEMORA_REDIRECCION_MS);
    }

    /** Fade-in con un leve desplazamiento hacia arriba. Sin librerias extra. */
    private void aparecer(View v, long demoraMs) {
        // setTranslationY recibe pixeles: se convierte desde dp para que el
        // desplazamiento mida lo mismo en cualquier densidad de pantalla.
        float desplazamiento = DESPLAZAMIENTO_DP * getResources().getDisplayMetrics().density;
        v.setAlpha(0f);
        v.setTranslationY(desplazamiento);
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
