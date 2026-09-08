package com.example.ronda.ui.perfil;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.ronda.R;
import com.example.ronda.data.model.EditarPerfilRequest;
import com.example.ronda.data.model.ErrorResponse;
import com.example.ronda.data.model.PerfilResponse;
import com.example.ronda.data.model.UsuarioResponse;
import com.example.ronda.data.model.ZonaResponse;
import com.example.ronda.data.model.ZonasResponse;
import com.example.ronda.data.network.ApiErrorParser;
import com.example.ronda.data.network.PublicacionApiService;
import com.example.ronda.data.network.UsuarioApiService;
import com.example.ronda.data.repository.SessionRepository;
import com.example.ronda.ui.auth.ValidadorRegistro;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Mi perfil (Punto 2): ver y editar los datos personales (nombre, telefono y
 * zona; el email es de solo lectura). La reputacion se muestra fija.
 *
 * GET /usuarios/me trae los datos y PUT /usuarios/me los guarda. El catalogo
 * de zonas del Spinner sale de GET /zonas (misma interfaz del Punto 3). Se
 * sigue el patron de las pantallas del Punto 1: enqueue, estaVivo() antes de
 * tocar la UI y ApiErrorParser.parse() una sola vez.
 *
 * Cada campo arranca bloqueado y se habilita tocando el lapiz que tiene al
 * lado. "Guardar cambios" hace el PUT y vuelve a bloquear todo.
 */
@AndroidEntryPoint
public class PerfilFragment extends Fragment {

    @Inject
    UsuarioApiService usuarioApi;

    /** Solo para el catalogo de zonas (GET /zonas vive en esta interfaz). */
    @Inject
    PublicacionApiService publicacionApi;

    @Inject
    SessionRepository sesion;

    private ProgressBar progressBar;
    private View grupoContenido;
    private View grupoError;
    private TextView tvError;
    private Button btnReintentar;
    private EditText etNombre;
    private TextView tvEmail;
    private EditText etTelefono;
    private Spinner spZona;
    private ImageButton btnEditarNombre;
    private ImageButton btnEditarTelefono;
    private ImageButton btnEditarZona;
    private Button btnGuardar;

    private UsuarioResponse datos;
    /** Paralelo al adapter del Spinner. La posicion 0 es "Sin zona" (null). */
    private final List<ZonaResponse> zonasSpinner = new ArrayList<>();
    private Call<PerfilResponse> llamadaDatos;
    private Call<ZonasResponse> llamadaZonas;
    private Call<PerfilResponse> llamadaGuardar;
    private boolean guardando = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_perfil, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressBar = view.findViewById(R.id.progressBar);
        grupoContenido = view.findViewById(R.id.grupoContenido);
        grupoError = view.findViewById(R.id.grupoError);
        tvError = view.findViewById(R.id.tvError);
        btnReintentar = view.findViewById(R.id.btnReintentar);
        etNombre = view.findViewById(R.id.etNombre);
        tvEmail = view.findViewById(R.id.tvEmail);
        etTelefono = view.findViewById(R.id.etTelefono);
        spZona = view.findViewById(R.id.spZona);
        btnEditarNombre = view.findViewById(R.id.btnEditarNombre);
        btnEditarTelefono = view.findViewById(R.id.btnEditarTelefono);
        btnEditarZona = view.findViewById(R.id.btnEditarZona);
        btnGuardar = view.findViewById(R.id.btnGuardar);

        btnReintentar.setOnClickListener(v -> cargar());
        btnGuardar.setOnClickListener(v -> guardar());
        btnEditarNombre.setOnClickListener(v -> habilitarEdicion(etNombre));
        btnEditarTelefono.setOnClickListener(v -> habilitarEdicion(etTelefono));
        btnEditarZona.setOnClickListener(v -> {
            spZona.setEnabled(true);
            spZona.performClick();
        });

        cargar();
    }

    // -----------------------------------------------------------------
    // Cargar
    // -----------------------------------------------------------------

    private void cargar() {
        mostrarEstado(true, false);

        llamadaDatos = usuarioApi.misDatos(sesion.getBearer());
        llamadaDatos.enqueue(new Callback<PerfilResponse>() {
            @Override
            public void onResponse(@NonNull Call<PerfilResponse> call,
                                   @NonNull Response<PerfilResponse> response) {
                if (call.isCanceled() || !estaVivo()) return;

                if (response.code() == 401) {
                    volverAlLogin();
                    return;
                }
                if (!response.isSuccessful() || response.body() == null
                        || response.body().getUsuario() == null) {
                    ErrorResponse.Detalle error = ApiErrorParser.parse(response);
                    mostrarError(ApiErrorParser.mensaje(error, getString(R.string.perfil_error_carga)));
                    return;
                }

                datos = response.body().getUsuario();
                pintarDatos();
                cargarZonas();
                mostrarEstado(false, true);
            }

            @Override
            public void onFailure(@NonNull Call<PerfilResponse> call, @NonNull Throwable t) {
                if (call.isCanceled() || !estaVivo()) return;
                mostrarError(getString(R.string.error_sin_conexion));
            }
        });
    }

    private void pintarDatos() {
        etNombre.setText(datos.getNombre());
        tvEmail.setText(datos.getEmail());
        etTelefono.setText(datos.getTelefono() != null ? datos.getTelefono() : "");
        bloquearCampos();
    }

    /** Deja los campos en modo lectura: se editan tocando el lapiz de al lado. */
    private void bloquearCampos() {
        etNombre.setEnabled(false);
        etTelefono.setEnabled(false);
        spZona.setEnabled(false);
        etNombre.setError(null);
        etTelefono.setError(null);
        ocultarTeclado();
    }

    /** Habilita un campo de texto y le pasa el foco con el teclado abierto. */
    private void habilitarEdicion(EditText campo) {
        campo.setEnabled(true);
        campo.requestFocus();
        campo.setSelection(campo.getText().length());
        InputMethodManager imm = (InputMethodManager)
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(campo, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void ocultarTeclado() {
        View foco = requireActivity().getCurrentFocus();
        InputMethodManager imm = (InputMethodManager)
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && foco != null) {
            imm.hideSoftInputFromWindow(foco.getWindowToken(), 0);
        }
    }

    /**
     * GET /zonas para el Spinner. Si falla no bloquea: el Spinner queda con la
     * zona actual y "Sin zona".
     */
    private void cargarZonas() {
        llamadaZonas = publicacionApi.zonas();
        llamadaZonas.enqueue(new Callback<ZonasResponse>() {
            @Override
            public void onResponse(@NonNull Call<ZonasResponse> call,
                                   @NonNull Response<ZonasResponse> response) {
                if (call.isCanceled() || !estaVivo()) return;
                List<ZonaResponse> zonas = response.isSuccessful() && response.body() != null
                        ? response.body().getZonas() : null;
                poblarSpinnerZonas(zonas);
            }

            @Override
            public void onFailure(@NonNull Call<ZonasResponse> call, @NonNull Throwable t) {
                if (call.isCanceled() || !estaVivo()) return;
                poblarSpinnerZonas(null);
            }
        });
    }

    private void poblarSpinnerZonas(@Nullable List<ZonaResponse> zonas) {
        zonasSpinner.clear();
        zonasSpinner.add(null); // "Sin zona"

        ZonaResponse zonaActual = datos != null ? datos.getZona() : null;
        boolean actualIncluida = false;
        if (zonas != null) {
            for (ZonaResponse z : zonas) {
                zonasSpinner.add(z);
                if (zonaActual != null && z.getId() == zonaActual.getId()) {
                    actualIncluida = true;
                }
            }
        }
        // Si el catalogo no cargo pero la persona ya tiene zona, la agregamos
        // para no perderla al guardar sin querer.
        if (zonaActual != null && !actualIncluida) {
            zonasSpinner.add(zonaActual);
        }

        List<String> labels = new ArrayList<>();
        for (ZonaResponse z : zonasSpinner) {
            labels.add(z == null ? getString(R.string.perfil_zona_sin_elegir) : z.getNombre());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spZona.setAdapter(adapter);

        int seleccion = 0;
        if (zonaActual != null) {
            for (int i = 1; i < zonasSpinner.size(); i++) {
                ZonaResponse z = zonasSpinner.get(i);
                if (z != null && z.getId() == zonaActual.getId()) {
                    seleccion = i;
                    break;
                }
            }
        }
        spZona.setSelection(seleccion);
    }

    // -----------------------------------------------------------------
    // Guardar
    // -----------------------------------------------------------------

    private void guardar() {
        if (guardando) return;

        String nombre = etNombre.getText().toString().trim();
        String telefono = etTelefono.getText().toString().trim();

        ValidadorRegistro.Resultado nombreOk = ValidadorRegistro.validarNombre(nombre);
        if (!nombreOk.esValido()) {
            etNombre.setError(getString(nombreOk.getMensajeError()));
            etNombre.requestFocus();
            return;
        }
        ValidadorRegistro.Resultado telOk = ValidadorRegistro.validarTelefono(telefono);
        if (!telOk.esValido()) {
            etTelefono.setError(getString(telOk.getMensajeError()));
            etTelefono.requestFocus();
            return;
        }

        Integer zonaId = null;
        int pos = spZona.getSelectedItemPosition();
        if (pos >= 0 && pos < zonasSpinner.size() && zonasSpinner.get(pos) != null) {
            zonaId = zonasSpinner.get(pos).getId();
        }

        guardando = true;
        btnGuardar.setEnabled(false);

        EditarPerfilRequest body = new EditarPerfilRequest(
                nombre, telefono.isEmpty() ? null : telefono, zonaId);
        llamadaGuardar = usuarioApi.actualizarMisDatos(sesion.getBearer(), body);
        llamadaGuardar.enqueue(new Callback<PerfilResponse>() {
            @Override
            public void onResponse(@NonNull Call<PerfilResponse> call,
                                   @NonNull Response<PerfilResponse> response) {
                if (call.isCanceled() || !estaVivo()) return;
                guardando = false;
                btnGuardar.setEnabled(true);

                if (response.code() == 401) {
                    volverAlLogin();
                    return;
                }
                if (response.isSuccessful() && response.body() != null
                        && response.body().getUsuario() != null) {
                    datos = response.body().getUsuario();
                    // El Home usa la zona guardada para "Solo mi zona" y la
                    // cercania: hay que refrescarla.
                    sesion.guardarZona(datos.getZona());
                    pintarDatos();
                    Toast.makeText(requireContext(), R.string.perfil_guardado,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                mostrarErrorDeGuardado(response);
            }

            @Override
            public void onFailure(@NonNull Call<PerfilResponse> call, @NonNull Throwable t) {
                if (call.isCanceled() || !estaVivo()) return;
                guardando = false;
                btnGuardar.setEnabled(true);
                Toast.makeText(requireContext(), R.string.error_sin_conexion,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void mostrarErrorDeGuardado(Response<PerfilResponse> response) {
        ErrorResponse.Detalle error = ApiErrorParser.parse(response);
        String codigo = ApiErrorParser.codigo(error);
        String mensaje = ApiErrorParser.mensaje(error, getString(R.string.perfil_error_guardar));

        if (codigo != null) {
            switch (codigo) {
                case "NOMBRE_REQUERIDO":
                case "NOMBRE_LARGO":
                case "NOMBRE_CON_NUMEROS":
                case "NOMBRE_INVALIDO":
                    etNombre.setError(mensaje);
                    etNombre.requestFocus();
                    return;
                case "TELEFONO_INVALIDO":
                case "TELEFONO_LARGO":
                    etTelefono.setError(mensaje);
                    etTelefono.requestFocus();
                    return;
                default:
                    break;
            }
        }
        Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show();
    }

    // -----------------------------------------------------------------
    // Estados de la pantalla
    // -----------------------------------------------------------------

    private void mostrarEstado(boolean cargando, boolean contenido) {
        progressBar.setVisibility(cargando ? View.VISIBLE : View.GONE);
        grupoContenido.setVisibility(contenido ? View.VISIBLE : View.GONE);
        grupoError.setVisibility(View.GONE);
    }

    private void mostrarError(String mensaje) {
        tvError.setText(mensaje);
        progressBar.setVisibility(View.GONE);
        grupoContenido.setVisibility(View.GONE);
        grupoError.setVisibility(View.VISIBLE);
    }

    private void volverAlLogin() {
        sesion.cerrarSesion();
        Toast.makeText(requireContext(), R.string.home_sesion_vencida, Toast.LENGTH_LONG).show();
        Navigation.findNavController(requireView()).navigate(R.id.action_perfil_to_auth);
    }

    // -----------------------------------------------------------------
    // Ciclo de vida
    // -----------------------------------------------------------------

    @Override
    public void onDestroyView() {
        cancelar(llamadaDatos);
        cancelar(llamadaZonas);
        cancelar(llamadaGuardar);
        super.onDestroyView();
    }

    private void cancelar(@Nullable Call<?> call) {
        if (call != null) call.cancel();
    }

    private boolean estaVivo() {
        return isAdded() && getView() != null;
    }
}
