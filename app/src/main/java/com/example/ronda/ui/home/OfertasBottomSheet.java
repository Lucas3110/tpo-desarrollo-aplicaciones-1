package com.example.ronda.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ronda.R;
import com.example.ronda.data.model.EstadoOfertaRequest;
import com.example.ronda.data.model.ListaOfertasResponse;
import com.example.ronda.data.model.OfertaUnicaResponse;
import com.example.ronda.data.model.OfertarRequest;
import com.example.ronda.data.network.PublicacionApiService;
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
public class OfertasBottomSheet extends BottomSheetDialogFragment {
    private int publicacionId;
    private boolean esVendedor;
    private boolean puedeOfertar;

    private RecyclerView rvOfertas;
    private ProgressBar pbLoading;
    private TextView tvSinOfertas;
    private LinearLayout llHacerOferta;
    private EditText etMontoOferta;
    private Button btnEnviarOferta;

    private OfertasAdapter adapter;
    @Inject
    PublicacionApiService apiService;
    /**
     * Se pide con @Inject en vez de hacer new SessionRepository(context):
     * Hilt entrega el mismo singleton que usan el resto de las pantallas y
     * le pasa el contexto de la aplicacion, no el del dialogo (clase 4, DI).
     */
    @Inject
    SessionRepository sessionRepository;

    private static final String ARG_PUBLICACION_ID = "publicacionId";
    private static final String ARG_ES_VENDEDOR = "esVendedor";
    private static final String ARG_PUEDE_OFERTAR = "puedeOfertar";

    /**
     * Los Fragments (y este dialogo lo es) los recrea el sistema con el
     * constructor vacio, por ejemplo al rotar la pantalla: si recibieran los
     * datos por constructor, al volver quedarian en cero o directamente no
     * se podrian instanciar. Por eso viajan en el Bundle de argumentos.
     */
    public static OfertasBottomSheet newInstance(int publicacionId, boolean esVendedor, boolean puedeOfertar) {
        Bundle args = new Bundle();
        args.putInt(ARG_PUBLICACION_ID, publicacionId);
        args.putBoolean(ARG_ES_VENDEDOR, esVendedor);
        args.putBoolean(ARG_PUEDE_OFERTAR, puedeOfertar);
        OfertasBottomSheet dialogo = new OfertasBottomSheet();
        dialogo.setArguments(args);
        return dialogo;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = requireArguments();
        publicacionId = args.getInt(ARG_PUBLICACION_ID);
        esVendedor = args.getBoolean(ARG_ES_VENDEDOR);
        puedeOfertar = args.getBoolean(ARG_PUEDE_OFERTAR);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_ofertas, container, false);
        
        rvOfertas = view.findViewById(R.id.rvOfertas);
        pbLoading = view.findViewById(R.id.pbLoadingOfertas);
        tvSinOfertas = view.findViewById(R.id.tvSinOfertas);
        llHacerOferta = view.findViewById(R.id.llHacerOferta);
        etMontoOferta = view.findViewById(R.id.etMontoOferta);
        btnEnviarOferta = view.findViewById(R.id.btnEnviarOferta);

        rvOfertas.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new OfertasAdapter();
        rvOfertas.setAdapter(adapter);

        if (puedeOfertar) {
            llHacerOferta.setVisibility(View.VISIBLE);
            btnEnviarOferta.setOnClickListener(v -> hacerOferta());
        }

        cargarOfertas();

        return view;
    }

    private void cargarOfertas() {
        pbLoading.setVisibility(View.VISIBLE);
        tvSinOfertas.setVisibility(View.GONE);
        
        apiService.listarOfertas("Bearer " + sessionRepository.getToken(), publicacionId).enqueue(new Callback<ListaOfertasResponse>() {
            @Override
            public void onResponse(Call<ListaOfertasResponse> call, Response<ListaOfertasResponse> response) {
                pbLoading.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().getOfertas().isEmpty()) {
                        tvSinOfertas.setVisibility(View.VISIBLE);
                    } else {
                        adapter.setOfertas(response.body().getOfertas(), response.body().isEsVendedor(), (ofertaId, estado) -> responderOferta(ofertaId, estado));
                    }
                } else {
                    ErrorResponse.Detalle err = ApiErrorParser.parse(response);
                    String msg = ApiErrorParser.mensaje(err, "Error del servidor");
                    Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ListaOfertasResponse> call, Throwable t) {
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(getContext(), getString(R.string.error_sin_conexion), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void hacerOferta() {
        String texto = etMontoOferta.getText().toString();
        if (texto.trim().isEmpty()) return;
        
        double monto = 0;
        try {
            monto = Double.parseDouble(texto);
        } catch (NumberFormatException e) {
            return;
        }

        btnEnviarOferta.setEnabled(false);
        apiService.hacerOferta("Bearer " + sessionRepository.getToken(), publicacionId, new OfertarRequest(monto)).enqueue(new Callback<OfertaUnicaResponse>() {
            @Override
            public void onResponse(Call<OfertaUnicaResponse> call, Response<OfertaUnicaResponse> response) {
                btnEnviarOferta.setEnabled(true);
                if (response.isSuccessful()) {
                    etMontoOferta.setText("");
                    cargarOfertas();
                } else {
                    ErrorResponse.Detalle err = ApiErrorParser.parse(response);
                    String msg = ApiErrorParser.mensaje(err, "Error del servidor");
                    Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<OfertaUnicaResponse> call, Throwable t) {
                btnEnviarOferta.setEnabled(true);
                Toast.makeText(getContext(), getString(R.string.error_sin_conexion), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void responderOferta(int ofertaId, String estado) {
        apiService.responderOferta("Bearer " + sessionRepository.getToken(), ofertaId, new EstadoOfertaRequest(estado)).enqueue(new Callback<OfertaUnicaResponse>() {
            @Override
            public void onResponse(Call<OfertaUnicaResponse> call, Response<OfertaUnicaResponse> response) {
                if (response.isSuccessful()) {
                    cargarOfertas();
                } else {
                    ErrorResponse.Detalle err = ApiErrorParser.parse(response);
                    String msg = ApiErrorParser.mensaje(err, "Error del servidor");
                    Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<OfertaUnicaResponse> call, Throwable t) {
                Toast.makeText(getContext(), getString(R.string.error_sin_conexion), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
