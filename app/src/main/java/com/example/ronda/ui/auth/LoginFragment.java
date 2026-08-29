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
 * Pantalla de ingreso.
 * Por ahora solo navega: la llamada real a POST /auth/login se agrega
 * en la branch feature/app-login.
 */
public class LoginFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // En onCreateView SOLO se infla el layout. Las vistas se buscan
        // en onViewCreated, cuando ya existen.
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText etEmail = view.findViewById(R.id.etEmail);
        Button btnIngresar = view.findViewById(R.id.btnIngresar);
        TextView tvIngresarConCodigo = view.findViewById(R.id.tvIngresarConCodigo);
        TextView tvCrearCuenta = view.findViewById(R.id.tvCrearCuenta);

        btnIngresar.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_auth_to_home));

        tvIngresarConCodigo.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("email", etEmail.getText().toString().trim());
            args.putString("proposito", "LOGIN");
            Navigation.findNavController(view).navigate(R.id.action_login_to_otp, args);
        });

        tvCrearCuenta.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_login_to_registro));
    }
}
