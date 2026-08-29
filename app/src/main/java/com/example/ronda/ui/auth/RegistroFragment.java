package com.example.ronda.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.ronda.R;

/**
 * Pantalla de alta de cuenta.
 * Por ahora solo navega: la llamada real a POST /auth/registro se agrega
 * en la branch feature/app-registro.
 */
public class RegistroFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_registro, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText etEmail = view.findViewById(R.id.etEmail);
        Button btnRegistrarme = view.findViewById(R.id.btnRegistrarme);
        TextView tvYaTengoCuenta = view.findViewById(R.id.tvYaTengoCuenta);

        btnRegistrarme.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("email", etEmail.getText().toString().trim());
            args.putString("proposito", "REGISTRO");
            Navigation.findNavController(view).navigate(R.id.action_registro_to_otp, args);
        });

        tvYaTengoCuenta.setOnClickListener(v ->
                Navigation.findNavController(view).popBackStack());
    }
}
