package com.example.ronda.ui.publicar;

import android.net.Uri;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.ronda.R;
import com.example.ronda.data.model.BorradorResponse;
import com.example.ronda.data.model.CategoriaResponse;
import com.example.ronda.data.model.CategoriasResponse;
import com.example.ronda.data.model.ErrorResponse;
import com.example.ronda.data.model.GuardarBorradorRequest;
import com.example.ronda.data.model.PublicacionRequest;
import com.example.ronda.data.model.PublicacionResponse;
import com.example.ronda.data.model.ZonaResponse;
import com.example.ronda.data.model.ZonasResponse;
import com.example.ronda.data.network.ApiErrorParser;
import com.example.ronda.data.network.PublicacionApiService;
import com.example.ronda.data.repository.SessionRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.io.IOException;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Asistente del Punto 5. Guarda el avance en el borrador del backend. */
@AndroidEntryPoint
public class PublicarFragment extends Fragment {
    @Inject PublicacionApiService api;
    @Inject SessionRepository sesion;

    private int paso = 1;
    private Double latitud, longitud;
    private String direccionResuelta = "";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int consultaDireccion = 0;
    private final List<Uri> fotos = new ArrayList<>();
    private final List<CategoriaResponse> categorias = new ArrayList<>();
    private final List<ZonaResponse> zonas = new ArrayList<>();
    private Call<?> llamada;
    /** Evita recrear el borrador al salir después de publicar o descartarlo. */
    private boolean salidaDefinitiva = false;

    private View pasoFotos, pasoDatos, pasoRevision;
    private TextView tvPaso, tvTituloPaso, tvFotosElegidas, tvResumen;
    private LinearLayout contenedorFotos;
    private EditText etTitulo, etDescripcion, etPrecio, etDireccion;
    private Spinner spCategoria, spEstado, spZona;
    private Button btnAnterior, btnSiguiente, btnDescartar;
    private ProgressBar progreso;

    private final ActivityResultLauncher<String[]> selectorFotos = registerForActivityResult(
            new ActivityResultContracts.OpenMultipleDocuments(), uris -> {
                if (!estaVivo() || uris.isEmpty()) return;
                fotos.clear();
                int limite = Math.min(uris.size(), 10);
                boolean permisoFallido = false;
                for (int i = 0; i < limite; i++) {
                    Uri uri = uris.get(i);
                    try {
                        requireContext().getContentResolver().takePersistableUriPermission(
                                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (SecurityException e) { permisoFallido = true; }
                    fotos.add(uri);
                }
                if (permisoFallido) Toast.makeText(requireContext(), R.string.publicar_foto_permiso, Toast.LENGTH_LONG).show();
                mostrarFotos();
                guardarBorrador(false);
                if (uris.size() > 10) Toast.makeText(requireContext(), R.string.publicar_limite_fotos, Toast.LENGTH_SHORT).show();
            });

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_publicar, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        pasoFotos = view.findViewById(R.id.pasoFotos); pasoDatos = view.findViewById(R.id.pasoDatos);
        pasoRevision = view.findViewById(R.id.pasoRevision); tvPaso = view.findViewById(R.id.tvPaso);
        tvTituloPaso = view.findViewById(R.id.tvTituloPaso); tvFotosElegidas = view.findViewById(R.id.tvFotosElegidas);
        tvResumen = view.findViewById(R.id.tvResumenPublicacion); contenedorFotos = view.findViewById(R.id.contenedorFotos);
        etTitulo = view.findViewById(R.id.etTituloPublicacion); etDescripcion = view.findViewById(R.id.etDescripcion);
        etPrecio = view.findViewById(R.id.etPrecio); spCategoria = view.findViewById(R.id.spCategoriaPublicar);
        etDireccion = view.findViewById(R.id.etDireccionEntrega);
        spEstado = view.findViewById(R.id.spEstadoArticulo); spZona = view.findViewById(R.id.spZonaPublicar);
        btnAnterior = view.findViewById(R.id.btnAnterior); btnSiguiente = view.findViewById(R.id.btnSiguiente);
        btnDescartar = view.findViewById(R.id.btnDescartar); progreso = view.findViewById(R.id.pbPublicar);

        spEstado.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Nuevo", "Como nuevo", "Usado"}));
        view.findViewById(R.id.btnElegirFotos).setOnClickListener(v -> selectorFotos.launch(new String[]{"image/*"}));
        btnAnterior.setOnClickListener(v -> { if (paso > 1) { paso--; guardarBorrador(false); mostrarPaso(); } });
        btnSiguiente.setOnClickListener(v -> avanzar());
        btnDescartar.setOnClickListener(v -> confirmarDescartar());
        mostrarFotos(); mostrarPaso(); cargarCatalogosYBorrador();
    }

    private void cargarCatalogosYBorrador() {
        api.categorias().enqueue(new Callback<CategoriasResponse>() {
            @Override public void onResponse(@NonNull Call<CategoriasResponse> c, @NonNull Response<CategoriasResponse> r) {
                if (!estaVivo()) return;
                if (r.isSuccessful() && r.body() != null && r.body().getCategorias() != null) {
                    categorias.clear(); categorias.addAll(r.body().getCategorias());
                    spCategoria.setAdapter(adapterNombresCategorias());
                }
                cargarZonas();
            }
            @Override public void onFailure(@NonNull Call<CategoriasResponse> c, @NonNull Throwable t) { if (estaVivo()) cargarZonas(); }
        });
    }

    private void cargarZonas() {
        api.zonas().enqueue(new Callback<ZonasResponse>() {
            @Override public void onResponse(@NonNull Call<ZonasResponse> c, @NonNull Response<ZonasResponse> r) {
                if (!estaVivo()) return;
                if (r.isSuccessful() && r.body() != null && r.body().getZonas() != null) {
                    zonas.clear(); zonas.addAll(r.body().getZonas()); spZona.setAdapter(adapterNombresZonas());
                }
                recuperarBorrador();
            }
            @Override public void onFailure(@NonNull Call<ZonasResponse> c, @NonNull Throwable t) { if (estaVivo()) recuperarBorrador(); }
        });
    }

    private ArrayAdapter<String> adapterNombresCategorias() {
        List<String> nombres = new ArrayList<>(); for (CategoriaResponse c : categorias) nombres.add(c.getNombre());
        return new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, nombres);
    }
    private ArrayAdapter<String> adapterNombresZonas() {
        List<String> nombres = new ArrayList<>(); for (ZonaResponse z : zonas) nombres.add(z.getNombre());
        return new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, nombres);
    }

    private void recuperarBorrador() {
        llamada = api.obtenerBorrador(sesion.getBearer());
        ((Call<BorradorResponse>) llamada).enqueue(new Callback<BorradorResponse>() {
            @Override public void onResponse(@NonNull Call<BorradorResponse> c, @NonNull Response<BorradorResponse> r) {
                if (!estaVivo()) return;
                if (r.code() == 401) { irAlLogin(); return; }
                BorradorResponse.Borrador b = r.isSuccessful() && r.body() != null ? r.body().getBorrador() : null;
                if (b != null) { restaurar(b); Toast.makeText(requireContext(), R.string.publicar_borrador_recuperado, Toast.LENGTH_SHORT).show(); }
            }
            @Override public void onFailure(@NonNull Call<BorradorResponse> c, @NonNull Throwable t) { }
        });
    }

    private void restaurar(BorradorResponse.Borrador borrador) {
        paso = Math.max(1, Math.min(3, borrador.getPaso())); Map<String, Object> d = borrador.getDatos();
        if (d == null) return;
        etTitulo.setText(texto(d.get("titulo"))); etDescripcion.setText(texto(d.get("descripcion")));
        etPrecio.setText(texto(d.get("precio")));
        etDireccion.setText(texto(d.get("direccion")));
        direccionResuelta = texto(etDireccion);
        Double lat = d.get("latitud") instanceof Number ? ((Number) d.get("latitud")).doubleValue() : null;
        Double lng = d.get("longitud") instanceof Number ? ((Number) d.get("longitud")).doubleValue() : null;
        latitud = PublicacionRequest.coordenadasValidas(lat, lng) ? lat : null;
        longitud = PublicacionRequest.coordenadasValidas(lat, lng) ? lng : null;
        seleccionarPorId(spCategoria, categorias, entero(d.get("categoriaId")));
        seleccionarPorId(spZona, zonas, entero(d.get("zonaId")));
        String estado = texto(d.get("estadoArticulo"));
        if ("COMO_NUEVO".equals(estado)) spEstado.setSelection(1); else if ("USADO".equals(estado)) spEstado.setSelection(2);
        Object lista = d.get("fotosLocales");
        fotos.clear();
        if (lista instanceof List<?>) for (Object item : (List<?>) lista) if (item != null) fotos.add(Uri.parse(item.toString()));
        mostrarFotos(); mostrarPaso();
    }

    private void avanzar() {
        if (paso == 1) { paso = 2; guardarBorrador(false); mostrarPaso(); return; }
        if (paso == 2) {
            if (!datosValidos()) return;
            resolverDireccion(); return;
        }
        publicar();
    }

    private boolean datosValidos() {
        if (texto(etDireccion).length() > 255) {
            etDireccion.setError(getString(R.string.publicar_direccion_larga)); return false;
        }
        if (texto(etTitulo).isEmpty() || texto(etDescripcion).isEmpty() || categorias.isEmpty() || zonas.isEmpty()) {
            Toast.makeText(requireContext(), R.string.publicar_campos_requeridos, Toast.LENGTH_SHORT).show(); return false;
        }
        try { if (Double.parseDouble(texto(etPrecio)) <= 0) throw new NumberFormatException(); }
        catch (NumberFormatException e) { etPrecio.setError(getString(R.string.publicar_precio_invalido)); return false; }
        return true;
    }

    private void mostrarPaso() {
        if (!estaVivo()) return;
        pasoFotos.setVisibility(paso == 1 ? View.VISIBLE : View.GONE);
        pasoDatos.setVisibility(paso == 2 ? View.VISIBLE : View.GONE);
        pasoRevision.setVisibility(paso == 3 ? View.VISIBLE : View.GONE);
        tvPaso.setText(getString(R.string.publicar_paso, paso));
        tvTituloPaso.setText(paso == 1 ? R.string.publicar_paso_fotos : paso == 2 ? R.string.publicar_paso_datos : R.string.publicar_paso_revision);
        btnAnterior.setVisibility(paso == 1 ? View.GONE : View.VISIBLE);
        btnSiguiente.setText(paso == 3 ? R.string.publicar_confirmar : R.string.publicar_siguiente);
        if (paso == 3) tvResumen.setText(resumen());
    }

    /** El precio como se ve en el resto de la app: $ 45.000, no $ 45000. */
    private String precioDelResumen() {
        String crudo = texto(etPrecio);
        try {
            java.text.NumberFormat formato =
                    java.text.NumberFormat.getCurrencyInstance(java.util.Locale.forLanguageTag("es-AR"));
            formato.setMinimumFractionDigits(0);
            formato.setMaximumFractionDigits(2);
            return formato.format(Double.parseDouble(crudo));
        } catch (NumberFormatException e) {
            // Todavia no escribio un numero: se muestra tal cual lo tipeo.
            return getString(R.string.publicar_resumen_precio, crudo);
        }
    }

    private String textoDeFotos() {
        int cuantas = fotos.size();
        if (cuantas == 0) return getString(R.string.publicar_fotos_ninguna);
        return getResources().getQuantityString(R.plurals.publicar_fotos_cantidad, cuantas, cuantas);
    }

    private String resumen() {
        String cat = categorias.isEmpty() ? "-" : categorias.get(spCategoria.getSelectedItemPosition()).getNombre();
        String zona = zonas.isEmpty() ? "-" : zonas.get(spZona.getSelectedItemPosition()).getNombre();
        return texto(etTitulo) + "\n\n" + texto(etDescripcion) + "\n\n" + precioDelResumen()
                + "\n" + cat + " · " + spEstado.getSelectedItem() + "\nEntrega en " + zona
                + "\n" + textoDeFotos()
                + (texto(etDireccion).isEmpty() ? "" : "\n" + getString(R.string.publicar_direccion_resumen, texto(etDireccion)));
    }

    private void mostrarFotos() {
        if (contenedorFotos == null) return; contenedorFotos.removeAllViews();
        tvFotosElegidas.setText(textoDeFotos());
        int px = (int) (110 * getResources().getDisplayMetrics().density);
        for (Uri uri : fotos) {
            ImageView imagen = new ImageView(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(px, px); lp.setMarginEnd(8); imagen.setLayoutParams(lp);
            imagen.setScaleType(ImageView.ScaleType.CENTER_CROP); Glide.with(this).load(uri).into(imagen); contenedorFotos.addView(imagen);
        }
    }

    private void guardarBorrador(boolean avisar) {
        if (!sesion.haySesion()) return;
        api.guardarBorrador(sesion.getBearer(), new GuardarBorradorRequest(paso, datosBorrador()))
                .enqueue(new Callback<BorradorResponse>() {
                    @Override public void onResponse(@NonNull Call<BorradorResponse> c, @NonNull Response<BorradorResponse> r) {
                        if (avisar && estaVivo() && r.isSuccessful()) Toast.makeText(requireContext(), R.string.publicar_borrador_guardado, Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onFailure(@NonNull Call<BorradorResponse> c, @NonNull Throwable t) { }
                });
    }

    private Map<String, Object> datosBorrador() {
        Map<String, Object> d = new HashMap<>(); d.put("titulo", texto(etTitulo)); d.put("descripcion", texto(etDescripcion));
        d.put("precio", texto(etPrecio)); d.put("estadoArticulo", estadoCodigo());
        d.put("direccion", texto(etDireccion));
        if (texto(etDireccion).equals(direccionResuelta)
                && PublicacionRequest.coordenadasValidas(latitud, longitud)) {
            d.put("latitud", latitud); d.put("longitud", longitud);
        }
        if (!categorias.isEmpty()) d.put("categoriaId", categorias.get(spCategoria.getSelectedItemPosition()).getId());
        if (!zonas.isEmpty()) d.put("zonaId", zonas.get(spZona.getSelectedItemPosition()).getId());
        List<String> locales = new ArrayList<>(); for (Uri uri : fotos) locales.add(uri.toString()); d.put("fotosLocales", locales);
        return d;
    }

    private void publicar() {
        if (!datosValidos()) { paso = 2; mostrarPaso(); return; }
        bloquear(true);
        // Según el contrato de la demo, estas URI sólo sirven en el dispositivo de origen.
        List<String> fotosLocales = new ArrayList<>();
        for (Uri uri : fotos) fotosLocales.add(uri.toString());
        boolean mismaDireccion = texto(etDireccion).equals(direccionResuelta);
        PublicacionRequest req = new PublicacionRequest(texto(etTitulo), texto(etDescripcion),
                categorias.get(spCategoria.getSelectedItemPosition()).getId(), Double.parseDouble(texto(etPrecio)),
                estadoCodigo(), zonas.get(spZona.getSelectedItemPosition()).getId(), fotosLocales,
                texto(etDireccion), mismaDireccion ? latitud : null, mismaDireccion ? longitud : null);
        llamada = api.crear(sesion.getBearer(), req);
        ((Call<PublicacionResponse>) llamada).enqueue(new Callback<PublicacionResponse>() {
            @Override public void onResponse(@NonNull Call<PublicacionResponse> c, @NonNull Response<PublicacionResponse> r) {
                if (!estaVivo()) return; bloquear(false);
                if (r.code() == 401) { irAlLogin(); return; }
                if (r.isSuccessful()) {
                    salidaDefinitiva = true;
                    Toast.makeText(requireContext(), R.string.publicar_exito, Toast.LENGTH_LONG).show();
                    Navigation.findNavController(requireView()).navigate(R.id.action_publicar_to_mis_publicaciones);
                } else {
                    ErrorResponse.Detalle e = ApiErrorParser.parse(r);
                    Toast.makeText(requireContext(), ApiErrorParser.mensaje(e, "No pudimos publicar el artículo"), Toast.LENGTH_LONG).show();
                }
            }
            @Override public void onFailure(@NonNull Call<PublicacionResponse> c, @NonNull Throwable t) {
                if (estaVivo() && !c.isCanceled()) { bloquear(false); Toast.makeText(requireContext(), R.string.publicar_error_conexion, Toast.LENGTH_LONG).show(); }
            }
        });
    }

    private void confirmarDescartar() {
        new MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.publicar_descartar_titulo)
                .setMessage(R.string.publicar_descartar_confirmacion).setNegativeButton(R.string.accion_cancelar, null)
                .setPositiveButton(R.string.publicar_descartar_si, (d, w) -> descartar()).show();
    }
    private void descartar() {
        api.descartarBorrador(sesion.getBearer()).enqueue(new Callback<Void>() {
            @Override public void onResponse(@NonNull Call<Void> c, @NonNull Response<Void> r) { if (estaVivo()) { salidaDefinitiva = true; Navigation.findNavController(requireView()).popBackStack(); } }
            @Override public void onFailure(@NonNull Call<Void> c, @NonNull Throwable t) { if (estaVivo()) Toast.makeText(requireContext(), R.string.publicar_error_conexion, Toast.LENGTH_SHORT).show(); }
        });
    }
    private void bloquear(boolean valor) { progreso.setVisibility(valor ? View.VISIBLE : View.GONE); btnSiguiente.setEnabled(!valor); btnAnterior.setEnabled(!valor); btnDescartar.setEnabled(!valor); }
    private String estadoCodigo() { int p = spEstado.getSelectedItemPosition(); return p == 1 ? "COMO_NUEVO" : p == 2 ? "USADO" : "NUEVO"; }
    private String texto(EditText e) { return e.getText().toString().trim(); }
    private String texto(Object o) { if (o == null) return ""; if (o instanceof Double && ((Double)o) % 1 == 0) return String.valueOf(((Double)o).intValue()); return o.toString(); }
    private Integer entero(Object o) { return o instanceof Number ? ((Number)o).intValue() : null; }
    private void seleccionarPorId(Spinner s, List<?> lista, Integer id) { if (id == null) return; for (int i=0;i<lista.size();i++) { int actual = lista.get(i) instanceof CategoriaResponse ? ((CategoriaResponse)lista.get(i)).getId() : ((ZonaResponse)lista.get(i)).getId(); if (actual == id) { s.setSelection(i); return; } } }
    private boolean estaVivo() { return isAdded() && getView() != null; }
    private void irAlLogin() { sesion.cerrarSesion(); Navigation.findNavController(requireView()).navigate(R.id.action_publicar_to_auth); }
    @Override public void onPause() { super.onPause(); if (getView() != null && !salidaDefinitiva) guardarBorrador(false); }
    @Override public void onDestroyView() {
        consultaDireccion++;
        handler.removeCallbacksAndMessages(null);
        if (llamada != null) llamada.cancel();
        super.onDestroyView();
    }

    private void resolverDireccion() {
        String direccion = texto(etDireccion);
        latitud = null; longitud = null; direccionResuelta = direccion;
        if (direccion.isEmpty() || !Geocoder.isPresent()) {
            paso = 3; guardarBorrador(false); mostrarPaso(); return;
        }
        bloquear(true);
        etDireccion.setEnabled(false);
        int consulta = ++consultaDireccion;
        Context contexto = requireContext().getApplicationContext();
        // No se bloquea la UI ni se impide publicar cuando el proveedor no responde.
        Runnable timeout = () -> terminarDireccion(consulta, direccion, null, null);
        handler.postDelayed(timeout, 8000);
        new Thread(() -> {
            Double lat = null, lng = null;
            try {
                List<Address> resultados = new Geocoder(contexto, new Locale("es", "AR"))
                        .getFromLocationName(direccion + ", Argentina", 1);
                if (resultados != null && !resultados.isEmpty()) {
                    Address resultado = resultados.get(0);
                    if (resultado.hasLatitude() && resultado.hasLongitude()) {
                        lat = resultado.getLatitude(); lng = resultado.getLongitude();
                    }
                }
            } catch (IOException | IllegalArgumentException | SecurityException e) {
                // La dirección textual sigue siendo válida sin coordenadas.
            }
            final Double latFinal = lat, lngFinal = lng;
            handler.post(() -> {
                handler.removeCallbacks(timeout);
                terminarDireccion(consulta, direccion, latFinal, lngFinal);
            });
        }, "direccion-entrega").start();
    }

    private void terminarDireccion(int consulta, String direccion, Double lat, Double lng) {
        if (!estaVivo() || consulta != consultaDireccion) return;
        consultaDireccion++;
        bloquear(false);
        etDireccion.setEnabled(true);
        if (!direccion.equals(texto(etDireccion))) return;
        if (PublicacionRequest.coordenadasValidas(lat, lng)) { latitud = lat; longitud = lng; }
        paso = 3; guardarBorrador(false); mostrarPaso();
    }
}
