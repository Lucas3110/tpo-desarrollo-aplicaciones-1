package com.example.ronda.ui.home;

import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import com.example.ronda.R;
import com.example.ronda.data.model.CategoriaResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * El panel plegable de filtros del Home: sus widgets y como se pasan a y
 * desde FiltrosPublicaciones.
 *
 * No es un Fragment ni hace red: HomeFragment le da la vista ya inflada y
 * el catalogo de categorias cuando llega, y le pide "volca lo aplicado" o
 * "lee lo que la persona puso". Existe para que HomeFragment no tenga que
 * saber de casillas y campos de precio ademas de la lista y la red.
 */
class PanelFiltros {

    private final View panel;
    private final Button btnFiltros;
    private final Spinner spCategoria;
    private final EditText etPrecioMin;
    private final EditText etPrecioMax;
    private final CheckBox cbNuevo;
    private final CheckBox cbComoNuevo;
    private final CheckBox cbUsado;
    private final CheckBox cbSoloMiZona;

    /** Catalogo para el Spinner; null hasta que llega GET /categorias. */
    private List<CategoriaResponse> categorias;

    PanelFiltros(View raiz) {
        panel = raiz.findViewById(R.id.panelFiltros);
        btnFiltros = raiz.findViewById(R.id.btnFiltros);
        spCategoria = raiz.findViewById(R.id.spCategoria);
        etPrecioMin = raiz.findViewById(R.id.etPrecioMin);
        etPrecioMax = raiz.findViewById(R.id.etPrecioMax);
        cbNuevo = raiz.findViewById(R.id.cbNuevo);
        cbComoNuevo = raiz.findViewById(R.id.cbComoNuevo);
        cbUsado = raiz.findViewById(R.id.cbUsado);
        cbSoloMiZona = raiz.findViewById(R.id.cbSoloMiZona);
        armarSpinnerCategorias();
    }

    /**
     * Nombre de la zona de la persona, o null si no configuro ninguna: en
     * ese caso la casilla queda deshabilitada y explica que hacer.
     */
    void setZonaDelUsuario(String nombreZona) {
        if (nombreZona == null) {
            cbSoloMiZona.setText(R.string.home_solo_mi_zona_sin_zona);
            cbSoloMiZona.setEnabled(false);
            cbSoloMiZona.setChecked(false);
        } else {
            cbSoloMiZona.setText(cbSoloMiZona.getContext().getString(R.string.home_solo_mi_zona, nombreZona));
            cbSoloMiZona.setEnabled(true);
        }
    }

    // ---------------------------------------------------------------
    // Abrir / cerrar
    // ---------------------------------------------------------------

    void mostrar(boolean abierto) {
        panel.setVisibility(abierto ? View.VISIBLE : View.GONE);
    }

    boolean estaAbierto() {
        return panel.getVisibility() == View.VISIBLE;
    }

    /** "Filtros" o "Filtros (n)" segun cuantos haya aplicados. */
    void actualizarBoton(FiltrosPublicaciones filtros) {
        int cantidad = filtros.contarFiltros();
        btnFiltros.setText(cantidad == 0
                ? btnFiltros.getContext().getString(R.string.home_filtros)
                : btnFiltros.getContext().getString(R.string.home_filtros_con_cantidad, cantidad));
    }

    // ---------------------------------------------------------------
    // Pasar datos entre el panel y FiltrosPublicaciones
    // ---------------------------------------------------------------

    /** Pone en los widgets lo que esta aplicado. */
    void volcar(FiltrosPublicaciones filtros) {
        etPrecioMin.setText(formatearPrecioParaCampo(filtros.getPrecioMin()));
        etPrecioMax.setText(formatearPrecioParaCampo(filtros.getPrecioMax()));
        etPrecioMax.setError(null);
        cbNuevo.setChecked(filtros.isNuevo());
        cbComoNuevo.setChecked(filtros.isComoNuevo());
        cbUsado.setChecked(filtros.isUsado());
        cbSoloMiZona.setChecked(cbSoloMiZona.isEnabled() && filtros.isSoloMiZona());
        seleccionarCategoria(filtros.getCategoriaId());
    }

    /**
     * Lee los widgets y los guarda en filtros. Devuelve false, con el error
     * marcado en el campo, si el minimo es mayor que el maximo.
     */
    boolean leerEn(FiltrosPublicaciones filtros) {
        Double precioMin = leerPrecio(etPrecioMin);
        Double precioMax = leerPrecio(etPrecioMax);
        if (precioMin != null && precioMax != null && precioMin > precioMax) {
            mostrarErrorPrecioMax(etPrecioMax.getContext().getString(R.string.error_rango_precio));
            return false;
        }
        etPrecioMax.setError(null);

        filtros.setPrecioMin(precioMin);
        filtros.setPrecioMax(precioMax);
        filtros.setCategoriaId(categoriaSeleccionada(filtros.getCategoriaId()));
        filtros.setEstados(cbNuevo.isChecked(), cbComoNuevo.isChecked(), cbUsado.isChecked());
        filtros.setSoloMiZona(cbSoloMiZona.isEnabled() && cbSoloMiZona.isChecked());
        return true;
    }

    /** Para el RANGO_PRECIO_INVALIDO que pueda devolver el backend. */
    void mostrarErrorPrecioMax(String mensaje) {
        etPrecioMax.setError(mensaje);
        etPrecioMax.requestFocus();
    }

    // ---------------------------------------------------------------
    // Categorias
    // ---------------------------------------------------------------

    /** Llego el catalogo: se rearma el Spinner y se vuelve a marcar la aplicada. */
    void setCategorias(List<CategoriaResponse> categorias, Integer categoriaAplicada) {
        this.categorias = categorias;
        armarSpinnerCategorias();
        seleccionarCategoria(categoriaAplicada);
    }

    /** Posicion 0 = "Todas"; la posicion p > 0 es categorias.get(p - 1). */
    private void armarSpinnerCategorias() {
        List<String> nombres = new ArrayList<>();
        nombres.add(spCategoria.getContext().getString(R.string.home_categoria_todas));
        if (categorias != null) {
            for (CategoriaResponse categoria : categorias) {
                nombres.add(categoria.getNombre());
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(spCategoria.getContext(),
                android.R.layout.simple_spinner_item, nombres);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategoria.setAdapter(adapter);
    }

    private void seleccionarCategoria(Integer categoriaId) {
        int posicion = 0;
        if (categoriaId != null && categorias != null) {
            for (int i = 0; i < categorias.size(); i++) {
                if (categorias.get(i).getId() == categoriaId) {
                    posicion = i + 1;
                    break;
                }
            }
        }
        spCategoria.setSelection(posicion, false);
    }

    private Integer categoriaSeleccionada(Integer aplicadaHoy) {
        if (categorias == null) {
            // El catalogo no llego: el Spinner solo tiene "Todas" y no dice
            // nada de lo que la persona quiere. Se conserva lo aplicado.
            return aplicadaHoy;
        }
        int posicion = spCategoria.getSelectedItemPosition();
        if (posicion <= 0 || posicion > categorias.size()) return null;   // "Todas"
        return categorias.get(posicion - 1).getId();
    }

    // ---------------------------------------------------------------
    // Precios
    // ---------------------------------------------------------------

    /**
     * Numero del campo, o null si esta vacio o no se entiende. Los campos son
     * inputType="number" (enteros, como los precios del seed): asi "150000" no
     * se confunde con "150.000" ni con "150,50".
     */
    private static Double leerPrecio(EditText campo) {
        String texto = campo.getText().toString().trim().replace(',', '.');
        if (texto.isEmpty()) return null;
        try {
            return Double.parseDouble(texto);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** "175000" para el campo (sin ".0" si es entero), o vacio si no hay valor. */
    private static String formatearPrecioParaCampo(Double precio) {
        if (precio == null) return "";
        if (precio == Math.rint(precio)) return String.valueOf(precio.longValue());
        return String.valueOf(precio);
    }
}
