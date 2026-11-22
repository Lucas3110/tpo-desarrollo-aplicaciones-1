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
 * Bienvenida que se muestra después de confirmar una cuenta nueva (Punto 1).
 *
 * Es una pantalla de paso: anima tres textos y a los 2,6 segundos entra sola
 * al Home. La sesión ya la creó OtpFragment al verificar el código, así que
 * acá no se inyecta nada (por eso no lleva @AndroidEntryPoint): el email
 * llega por argumento solo para mostrarlo.
 */
public class BienvenidaFragment extends Fragment {

    /** Cuánto dura la bienvenida antes de entrar al Home. */
    private static final long DEMORA_REDIRECCION_MS = 2600L;

    /** Desde dónde "sube" cada texto al aparecer. */
    private static final float DESPLAZAMIENTO_DP = 32f;

    /** Sirve para correr algo más tarde en el hilo principal (postDelayed). */
    private final Handler handler = new Handler(Looper.getMainLooper());
    /** La entrada al Home programada. Se guarda para poder cancelarla. */
    private Runnable tareaRedireccion;

    /** Ciclo de vida: infla el XML de la pantalla. */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bienvenida, container, false);
    }

    /** Ciclo de vida: muestra el email, anima los textos y programa la entrada al Home. */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String email = getArguments() != null ? getArguments().getString("email", "") : "";

        TextView tvBienvenida = view.findViewById(R.id.tvBienvenida);
        TextView tvEmail = view.findViewById(R.id.tvEmail);
        TextView tvSubtitulo = view.findViewById(R.id.tvSubtitulo);

        tvEmail.setText(email);

        // Los tres textos aparecen uno detrás del otro.
        aparecer(tvBienvenida, 0);
        aparecer(tvEmail, 120);
        aparecer(tvSubtitulo, 240);

        tareaRedireccion = () -> {
            // La vista pudo destruirse mientras corría la espera.
            if (!isAdded() || getView() == null) return;
            // La cuenta nueva ya tiene sesión: se entra al Home y se saca de la
            // pila todo lo de autenticación, igual que al ingresar.
            Navigation.findNavController(view).navigate(R.id.action_bienvenida_to_home);
        };
        handler.postDelayed(tareaRedireccion, DEMORA_REDIRECCION_MS);
    }

    /** Fade-in con un leve desplazamiento hacia arriba. Sin librerías extra. */
    private void aparecer(View v, long demoraMs) {
        // setTranslationY recibe píxeles: se convierte desde dp para que el
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

    /** Ciclo de vida: la vista se está por destruir. */
    @Override
    public void onDestroyView() {
        // Si no lo cancelamos, la redirección se dispara sobre una vista que
        // ya no existe (por ejemplo, si la persona toca "atrás" antes de tiempo).
        if (tareaRedireccion != null) {
            handler.removeCallbacks(tareaRedireccion);
            tareaRedireccion = null;
        }
        super.onDestroyView();
    }
}
