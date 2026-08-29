package com.example.ronda.ui.auth;

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

/**
 * Pantalla del codigo de verificacion.
 * Se usa para los dos caminos del enunciado, segun el argumento "proposito":
 *   REGISTRO -> confirmar la cuenta recien creada
 *   LOGIN    -> ingresar sin contrasena
 * Por ahora solo navega: las llamadas reales se agregan en feature/app-otp.
 */
public class OtpFragment extends Fragment {

    private String email = "";
    private String proposito = "REGISTRO";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_otp, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Argumentos que mandaron LoginFragment o RegistroFragment
        if (getArguments() != null) {
            email = getArguments().getString("email", "");
            proposito = getArguments().getString("proposito", "REGISTRO");
        }

        TextView tvDestino = view.findViewById(R.id.tvDestino);
        Button btnVerificar = view.findViewById(R.id.btnVerificar);
        Button btnReenviar = view.findViewById(R.id.btnReenviar);

        tvDestino.setText(getString(R.string.otp_enviado_a, email));

        btnVerificar.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_otp_to_home));

        btnReenviar.setOnClickListener(v -> { /* feature/app-otp */ });
    }
}
