package com.example.ronda.ui.ofertas;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.ronda.R;
import com.example.ronda.data.model.OfertaResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Filas de "Mis ofertas". La misma fila sirve para las enviadas y las
 * recibidas: cambia el texto de la otra parte, que el backend ya resuelve en
 * contraparte. Tocar una fila abre el detalle de esa publicacion.
 */
public class MisOfertasAdapter extends RecyclerView.Adapter<MisOfertasAdapter.ViewHolder> {

    public interface OnOfertaClickListener {
        void onOferta(OfertaResponse oferta);
    }

    private final OnOfertaClickListener listener;
    private List<OfertaResponse> ofertas = new ArrayList<>();
    /** Que pestaña se esta mostrando: cambia "Para ..." por "De ...". */
    private boolean enviadas = true;

    public MisOfertasAdapter(OnOfertaClickListener listener) {
        this.listener = listener;
    }

    public void mostrar(List<OfertaResponse> nuevas, boolean enviadas) {
        this.ofertas = nuevas != null ? nuevas : new ArrayList<OfertaResponse>();
        this.enviadas = enviadas;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mi_oferta, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OfertaResponse o = ofertas.get(position);
        Context ctx = holder.itemView.getContext();
        OfertaResponse.Publicacion pub = o.getPublicacion();

        holder.mostrarFoto(pub != null ? pub.getFotoPrincipal() : null);
        holder.tvTituloPublicacion.setText(pub != null ? pub.getTitulo() : "");
        holder.tvMonto.setText(FormatoOferta.precio(o.getMonto()));
        holder.tvEstado.setText(TextosOferta.estado(ctx, o));
        holder.tvEstado.setTextColor(ContextCompat.getColor(ctx, TextosOferta.colorDeEstado(o.getEstado())));
        holder.tvContraparte.setText(TextosOferta.contraparte(ctx, o, enviadas));

        mostrarOpcional(holder.tvMensaje,
                o.tieneMensaje() ? ctx.getString(R.string.oferta_mensaje_formato, o.getMensaje().trim()) : null);
        mostrarOpcional(holder.tvContraoferta, TextosOferta.contraoferta(ctx, o));
        holder.tvFecha.setText(FormatoOferta.fechaCorta(o.getCreadoEn()));
        mostrarOpcional(holder.tvVence, TextosOferta.vencimiento(ctx, o, System.currentTimeMillis()));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onOferta(o);
        });
    }

    @Override
    public int getItemCount() {
        return ofertas.size();
    }

    /** Un TextView que solo aparece cuando hay algo que decir. */
    private static void mostrarOpcional(TextView vista, String texto) {
        vista.setText(texto);
        vista.setVisibility(texto != null ? View.VISIBLE : View.GONE);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivFoto;
        final TextView tvTituloPublicacion, tvMonto, tvEstado, tvContraparte, tvMensaje,
                tvContraoferta, tvFecha, tvVence;

        ViewHolder(View v) {
            super(v);
            ivFoto = v.findViewById(R.id.ivFoto);
            tvTituloPublicacion = v.findViewById(R.id.tvTituloPublicacion);
            tvMonto = v.findViewById(R.id.tvMonto);
            tvEstado = v.findViewById(R.id.tvEstado);
            tvContraparte = v.findViewById(R.id.tvContraparte);
            tvMensaje = v.findViewById(R.id.tvMensaje);
            tvContraoferta = v.findViewById(R.id.tvContraoferta);
            tvFecha = v.findViewById(R.id.tvFecha);
            tvVence = v.findViewById(R.id.tvVence);
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
