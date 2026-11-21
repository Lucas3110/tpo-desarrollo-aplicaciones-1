package com.example.ronda.ui.entrega;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.ronda.R;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

/**
 * Abre la camara para leer un QR y devuelve el texto leido (Punto 12).
 *
 * Son dos pasos encadenados con la API de resultados (registerForActivityResult),
 * la misma que usan Publicar y Perfil para elegir fotos:
 *
 *   1. RequestPermission: CAMERA es un permiso "peligroso" y declararlo en el
 *      Manifest no alcanza, hay que pedirselo a la persona en tiempo de
 *      ejecucion. Si ya lo dio, este paso se salta.
 *   2. ScanContract (ZXing Android Embedded): abre el lector a pantalla
 *      completa y vuelve con el contenido del QR, o con null si se cancelo.
 *
 * Los launchers tienen que registrarse antes de que el Fragment llegue a
 * STARTED, por eso esto se construye en onCreate del Fragment y no cuando se
 * toca el boton.
 */
public final class EscanerQr {

    public interface Listener {
        /** Se leyo un QR: el texto tal cual venia, sin limpiar. */
        void onCodigoLeido(@NonNull String contenido);

        /** La persona cerro la camara sin leer nada. */
        void onCancelado();

        /** No dio el permiso de camara (o lo nego antes de forma permanente). */
        void onSinPermiso();
    }

    private final Fragment fragment;
    private final Listener listener;
    private final ActivityResultLauncher<String> pedirPermiso;
    private final ActivityResultLauncher<ScanOptions> abrirLector;

    public EscanerQr(@NonNull Fragment fragment, @NonNull Listener listener) {
        this.fragment = fragment;
        this.listener = listener;
        pedirPermiso = fragment.registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                concedido -> {
                    if (concedido) abrirLector();
                    else listener.onSinPermiso();
                });
        abrirLector = fragment.registerForActivityResult(
                new ScanContract(),
                resultado -> {
                    if (resultado.getContents() == null) listener.onCancelado();
                    else listener.onCodigoLeido(resultado.getContents());
                });
    }

    /** Un emulador o una tablet barata pueden no tener ninguna camara. */
    public boolean hayCamara() {
        Context ctx = fragment.requireContext();
        return ctx.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
    }

    /** Pide el permiso si hace falta y abre el lector. */
    public void escanear() {
        Context ctx = fragment.requireContext();
        boolean concedido = ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
        if (concedido) abrirLector();
        else pedirPermiso.launch(Manifest.permission.CAMERA);
    }

    private void abrirLector() {
        ScanOptions opciones = new ScanOptions()
                // Solo QR: un codigo de barras de un producto no es una entrega.
                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                .setPrompt(fragment.getString(R.string.confirmar_entrega_prompt_camara))
                .setBeepEnabled(false)
                // Que respete la orientacion del celular en vez de forzar horizontal.
                .setOrientationLocked(false);
        abrirLector.launch(opciones);
    }
}
