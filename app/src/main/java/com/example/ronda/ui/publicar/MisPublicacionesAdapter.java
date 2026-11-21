package com.example.ronda.ui.publicar;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.example.ronda.R;
import com.example.ronda.data.model.PublicacionItemResponse;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

class MisPublicacionesAdapter extends BaseAdapter {
    interface AlCambiarEstado { void cambiar(PublicacionItemResponse item); }
    private final List<PublicacionItemResponse> items; private final AlCambiarEstado listener;
    MisPublicacionesAdapter(List<PublicacionItemResponse> items, AlCambiarEstado listener) { this.items = items; this.listener = listener; }
    @Override public int getCount() { return items.size(); }
    @Override public PublicacionItemResponse getItem(int p) { return items.get(p); }
    @Override public long getItemId(int p) { return getItem(p).getId(); }
    @Override public View getView(int p, View convertida, ViewGroup parent) {
        View v = convertida != null ? convertida : LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mi_publicacion, parent, false);
        PublicacionItemResponse item = getItem(p);
        ((TextView)v.findViewById(R.id.tvTituloMia)).setText(item.getTitulo());
        NumberFormat formato = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-AR"));
        // Mismo formato que el listado: sin los ,00 cuando el precio es redondo.
        formato.setMinimumFractionDigits(0);
        formato.setMaximumFractionDigits(2);
        String precio = formato.format(item.getPrecio());
        String estado = item.getEstado();
        // Punto 12: una reservada tiene la venta en curso. Se aclara por que no
        // hay boton: el estado lo cambia el QR del comprador, no el vendedor.
        boolean reservada = "RESERVADA".equals(estado);
        String datos = precio + " · " + estado
                + (reservada ? "\n" + parent.getContext().getString(R.string.mias_reservada_detalle) : "");
        ((TextView)v.findViewById(R.id.tvDatosMia)).setText(datos);
        Button boton = v.findViewById(R.id.btnCambiarEstado);
        boolean cerrada = "VENDIDA".equals(estado) || reservada; boton.setVisibility(cerrada ? View.GONE : View.VISIBLE);
        boton.setText("ACTIVA".equals(item.getEstado()) ? R.string.mias_pausar : R.string.mias_reactivar);
        boton.setOnClickListener(x -> listener.cambiar(item)); return v;
    }
}
