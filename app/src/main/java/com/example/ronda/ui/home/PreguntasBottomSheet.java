package com.example.ronda.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ronda.R;
import com.example.ronda.data.model.ListaPreguntasResponse;
import com.example.ronda.data.model.PreguntaUnicaResponse;
import com.example.ronda.data.model.PreguntarRequest;
import com.example.ronda.data.model.ResponderRequest;
import com.example.ronda.data.network.PublicacionApiService;
import com.example.ronda.di.NetworkModule;
import com.example.ronda.data.repository.SessionRepository;
import com.example.ronda.data.network.ApiErrorParser;
import com.example.ronda.data.model.ErrorResponse;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class PreguntasBottomSheet extends BottomSheetDialogFragment {
    private int publicacionId;
    private boolean esVendedor;
    private boolean puedePreguntar;

    private RecyclerView rvPreguntas;
    private ProgressBar pbLoading;
    private TextView tvSinPreguntas;
    private LinearLayout llHacerPregunta;
    private EditText etNuevaPregunta;
    private ImageButton btnEnviarPregunta;

    private PreguntasAdapter adapter;
    @Inject
    PublicacionApiService apiService;
    private SessionRepository sessionRepository;

    public PreguntasBottomSheet(int publicacionId, boolean esVendedor, boolean puedePreguntar) {
        this.publicacionId = publicacionId;
        this.esVendedor = esVendedor;
        this.puedePreguntar = puedePreguntar;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_preguntas, container, false);
        
        rvPreguntas = view.findViewById(R.id.rvPreguntas);
        pbLoading = view.findViewById(R.id.pbLoadingPreguntas);
        tvSinPreguntas = view.findViewById(R.id.tvSinPreguntas);
        llHacerPregunta = view.findViewById(R.id.llHacerPregunta);
        etNuevaPregunta = view.findViewById(R.id.etNuevaPregunta);
        btnEnviarPregunta = view.findViewById(R.id.btnEnviarPregunta);

        sessionRepository = new SessionRepository(requireContext());
        

        rvPreguntas.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new PreguntasAdapter();
        rvPreguntas.setAdapter(adapter);

        if (puedePreguntar) {
            llHacerPregunta.setVisibility(View.VISIBLE);
            btnEnviarPregunta.setOnClickListener(v -> hacerPregunta());
        }

        cargarPreguntas();

        return view;
    }

    private void cargarPreguntas() {
        pbLoading.setVisibility(View.VISIBLE);
        tvSinPreguntas.setVisibility(View.GONE);
        
        apiService.listarPreguntas(publicacionId).enqueue(new Callback<ListaPreguntasResponse>() {
            @Override
            public void onResponse(Call<ListaPreguntasResponse> call, Response<ListaPreguntasResponse> response) {
                pbLoading.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().getPreguntas().isEmpty()) {
                        tvSinPreguntas.setVisibility(View.VISIBLE);
                    } else {
                        adapter.setPreguntas(response.body().getPreguntas(), esVendedor, (preguntaId, respuesta) -> responderPregunta(preguntaId, respuesta));
                    }
                } else {
                    ErrorResponse.Detalle err = ApiErrorParser.parse(response);
                    String msg = ApiErrorParser.mensaje(err, "Error del servidor");
                    Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ListaPreguntasResponse> call, Throwable t) {
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(getContext(), getString(R.string.error_sin_conexion), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void hacerPregunta() {
        String texto = etNuevaPregunta.getText().toString();
        if (texto.trim().isEmpty()) return;

        btnEnviarPregunta.setEnabled(false);
        apiService.hacerPregunta("Bearer " + sessionRepository.getToken(), publicacionId, new PreguntarRequest(texto)).enqueue(new Callback<PreguntaUnicaResponse>() {
            @Override
            public void onResponse(Call<PreguntaUnicaResponse> call, Response<PreguntaUnicaResponse> response) {
                btnEnviarPregunta.setEnabled(true);
                if (response.isSuccessful()) {
                    etNuevaPregunta.setText("");
                    cargarPreguntas();
                } else {
                    ErrorResponse.Detalle err = ApiErrorParser.parse(response);
                    String msg = ApiErrorParser.mensaje(err, "Error del servidor");
                    Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PreguntaUnicaResponse> call, Throwable t) {
                btnEnviarPregunta.setEnabled(true);
                Toast.makeText(getContext(), getString(R.string.error_sin_conexion), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void responderPregunta(int preguntaId, String respuesta) {
        apiService.responderPregunta("Bearer " + sessionRepository.getToken(), preguntaId, new ResponderRequest(respuesta)).enqueue(new Callback<PreguntaUnicaResponse>() {
            @Override
            public void onResponse(Call<PreguntaUnicaResponse> call, Response<PreguntaUnicaResponse> response) {
                if (response.isSuccessful()) {
                    cargarPreguntas();
                } else {
                    ErrorResponse.Detalle err = ApiErrorParser.parse(response);
                    String msg = ApiErrorParser.mensaje(err, "Error del servidor");
                    Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PreguntaUnicaResponse> call, Throwable t) {
                Toast.makeText(getContext(), getString(R.string.error_sin_conexion), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
