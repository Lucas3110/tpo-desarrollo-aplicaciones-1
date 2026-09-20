package com.example.ronda.ui.historial;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.ronda.R;

/**
 * Historial de operaciones (Punto 9): compras y ventas concretadas, con
 * filtro por tipo y por fechas, y la opcion de calificar a la otra parte.
 *
 * Por ahora es solo la pantalla y su navegacion desde el Home. La carga de
 * datos, los filtros y el calificar se suman en los pasos siguientes.
 */
public class HistorialFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_historial, container, false);
    }
}
