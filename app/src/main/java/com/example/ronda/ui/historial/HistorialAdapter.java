package com.example.ronda.ui.historial;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.ronda.R;
import com.example.ronda.data.model.OperacionResponse;
import com.example.ronda.ui.ofertas.FormatoOferta;

import java.util.ArrayList;
import java.util.List;

/**
 * Filas del historial (Punto 9). La lista mezcla dos tipos de fila: los
 * encabezados de seccion ("Compras (2)", un String) y las operaciones. Asi
 * la separacion en compras y ventas que pide la consigna vive en una sola
 * lista con un solo scroll.
 */
public class HistorialAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Listener {
        /** Tocar la fila: abre el articulo. */
        void onArticulo(OperacionResponse operacion);

        /** Tocar el nombre de la contraparte: abre su perfil publico. */
        void onContraparte(OperacionResponse operacion);
    }

    private static final int VISTA_SECCION = 0;
    private static final int VISTA_OPERACION = 1;

    private final Listener listener;
    /** String = encabezado de seccion; OperacionResponse = una operacion. */
    private List<Object> filas = new ArrayList<>();

    public HistorialAdapter(Listener listener) {
        this.listener = listener;
    }

    public void mostrar(List<Object> nuevas) {
        this.filas = nuevas != null ? nuevas : new ArrayList<>();
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return filas.get(position) instanceof String ? VISTA_SECCION : VISTA_OPERACION;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VISTA_SECCION) {
            return new SeccionHolder(inflater.inflate(R.layout.item_historial_seccion, parent, false));
        }
        return new OperacionHolder(inflater.inflate(R.layout.item_operacion, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object fila = filas.get(position);
        if (holder instanceof SeccionHolder) {
            ((SeccionHolder) holder).tvSeccion.setText((String) fila);
        } else {
            enlazar((OperacionHolder) holder, (OperacionResponse) fila);
        }
    }

    private void enlazar(OperacionHolder h, OperacionResponse o) {
        Context ctx = h.itemView.getContext();
        OperacionResponse.Articulo articulo = o.getArticulo();

        h.mostrarFoto(articulo != null ? articulo.getFotoPrincipal() : null);
        // Si la publicacion se borro despues de la operacion no hay titulo.
        h.tvTitulo.setText(articulo != null && articulo.getTitulo() != null
                ? articulo.getTitulo() : ctx.getString(R.string.historial_articulo_eliminado));
        h.tvMonto.setText(FormatoOferta.precio(o.getMontoFinal()));
        h.tvFecha.setText(FormatoOferta.fechaCorta(o.getFecha()));

        String nombre = o.getContraparte() != null && o.getContraparte().getNombre() != null
                ? o.getContraparte().getNombre() : "";
        // En una compra la contraparte es quien vendio, y al reves.
        h.tvContraparte.setText(ctx.getString(
                o.esCompra() ? R.string.historial_vendedor : R.string.historial_comprador, nombre));

        h.itemView.setOnClickListener(v -> listener.onArticulo(o));
        h.tvContraparte.setOnClickListener(v -> listener.onContraparte(o));
    }

    @Override
    public int getItemCount() {
        return filas.size();
    }

    static class SeccionHolder extends RecyclerView.ViewHolder {
        final TextView tvSeccion;

        SeccionHolder(View v) {
            super(v);
            tvSeccion = v.findViewById(R.id.tvSeccion);
        }
    }

    static class OperacionHolder extends RecyclerView.ViewHolder {
        final ImageView ivFoto;
        final TextView tvTitulo, tvMonto, tvFecha, tvContraparte;

        OperacionHolder(View v) {
            super(v);
            ivFoto = v.findViewById(R.id.ivFoto);
            tvTitulo = v.findViewById(R.id.tvTitulo);
            tvMonto = v.findViewById(R.id.tvMonto);
            tvFecha = v.findViewById(R.id.tvFecha);
            tvContraparte = v.findViewById(R.id.tvContraparte);
        }

        /** Igual que en el Home: con foto se muestra via Glide, sin foto se oculta. */
        void mostrarFoto(String url) {
            if (url == null || url.isEmpty()) {
                Glide.with(ivFoto).clear(ivFoto);
                ivFoto.setVisibility(View.GONE);
                return;
            }
            ivFoto.setVisibility(View.VISIBLE);
            Glide.with(ivFoto)
                    .load(url)
                    .placeholder(R.drawable.bg_estado_articulo)
                    .centerCrop()
                    .into(ivFoto);
        }
    }
}
