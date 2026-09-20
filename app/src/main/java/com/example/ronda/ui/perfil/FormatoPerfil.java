package com.example.ronda.ui.perfil;

import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.ronda.R;

/**
 * Lo que comparten las pantallas de perfil (el propio y, mas adelante, el
 * publico): como se dibuja la foto de perfil.
 */
public final class FormatoPerfil {

    private FormatoPerfil() {
    }

    /**
     * Foto circular con Glide. Sin foto, o si la URL no se puede cargar
     * (por ejemplo un content:// al que se perdio el permiso), queda el
     * avatar por defecto en vez de un hueco.
     */
    public static void cargarAvatar(ImageView vista, @Nullable String url) {
        if (url == null || url.isEmpty()) {
            Glide.with(vista).clear(vista);
            vista.setImageResource(R.drawable.ic_persona);
            return;
        }
        Glide.with(vista)
                .load(url)
                .circleCrop()
                .placeholder(R.drawable.ic_persona)
                .error(R.drawable.ic_persona)
                .into(vista);
    }
}
