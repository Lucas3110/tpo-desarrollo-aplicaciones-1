package com.example.ronda.ui.home;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ronda.R;
import com.example.ronda.data.model.OfertaResponse;
import com.example.ronda.ui.ofertas.FormatoOferta;
import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;

/**
 * Historial de ofertas de una publicacion (bottom sheet del detalle).
 *
 * Quien puede responder sale de cruzar quien propuso el monto (origen) con
 * si quien mira es el vendedor: una oferta del comprador la responde el
 * vendedor y una contraoferta del vendedor la responde el comprador. Esa
 * regla vive en OfertaResponse para no repetirla en cada pantalla.
 */
public class OfertasAdapter extends RecyclerView.Adapter<OfertasAdapter.ViewHolder> {

    private List<OfertaResponse> ofertas = new ArrayList<>();
    private boolean esVendedor;
    private OnResponderOfertaClickListener listener;
    private OnContraofertarClickListener contraofertaListener;

    public interface OnResponderOfertaClickListener {
        void onResponder(int ofertaId, String estado);
    }

    public interface OnContraofertarClickListener {
        void onContraofertar(OfertaResponse oferta);
    }

    /** Tocar el nombre de quien oferto abre su perfil publico (solo para el vendedor). */
    public interface OnAutorClickListener {
        void onAutor(int usuarioId);
    }

    private OnAutorClickListener autorListener;

    public void setOnAutorClickListener(OnAutorClickListener listener) {
        this.autorListener = listener;
    }

    public void setOfertas(List<OfertaResponse> ofertas, boolean esVendedor,
                           OnResponderOfertaClickListener listener,
                           OnContraofertarClickListener contraofertaListener) {
        this.ofertas = ofertas != null ? ofertas : new ArrayList<OfertaResponse>();
        this.esVendedor = esVendedor;
        this.listener = listener;
        this.contraofertaListener = contraofertaListener;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_oferta, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OfertaResponse o = ofertas.get(position);
        Context ctx = holder.itemView.getContext();

        holder.tvMontoOferta.setText(FormatoOferta.precio(o.getMonto()));
        holder.tvEstadoOferta.setText(textoEstado(o));
        holder.tvEstadoOferta.setTextColor(ContextCompat.getColor(ctx, colorDeEstado(o.getEstado())));

        holder.tvAutorOferta.setText(textoAutor(ctx, o, esVendedor));
        // El vendedor puede mirar el perfil de quien le ofrecio antes de responder.
        boolean autorAbrible = esVendedor && !o.laPropusoElVendedor() && o.getAutor() != null
                && autorListener != null;
        holder.tvAutorOferta.setTextColor(autorAbrible
                ? ColorStateList.valueOf(MaterialColors.getColor(holder.tvAutorOferta,
                        com.google.android.material.R.attr.colorPrimary))
                : holder.colorAutor);
        holder.tvAutorOferta.setOnClickListener(autorAbrible
                ? v -> autorListener.onAutor(o.getAutor().getId())
                : null);
        holder.tvAutorOferta.setClickable(autorAbrible);

        if (o.tieneMensaje()) {
            holder.tvMensajeOferta.setText(ctx.getString(R.string.oferta_mensaje_formato, o.getMensaje().trim()));
            holder.tvMensajeOferta.setVisibility(View.VISIBLE);
        } else {
            holder.tvMensajeOferta.setVisibility(View.GONE);
        }

        holder.tvFechaOferta.setText(FormatoOferta.fechaCorta(o.getCreadoEn()));

        String vence = textoVencimiento(ctx, o);
        holder.tvVenceOferta.setText(vence);
        holder.tvVenceOferta.setVisibility(vence != null ? View.VISIBLE : View.GONE);

        boolean puedoResponder = o.puedoResponderla(esVendedor);
        boolean puedoContraofertar = o.puedoContraofertarla(esVendedor);
        holder.llAccionesOferta.setVisibility(puedoResponder ? View.VISIBLE : View.GONE);
        holder.btnContraofertarOferta.setVisibility(puedoContraofertar ? View.VISIBLE : View.GONE);
        holder.btnContraofertarOferta.setOnClickListener(v -> {
            if (contraofertaListener != null) contraofertaListener.onContraofertar(o);
        });
        holder.btnAceptarOferta.setOnClickListener(v -> {
            if (listener != null) listener.onResponder(o.getId(), OfertaResponse.ACEPTADA);
        });
        holder.btnRechazarOferta.setOnClickListener(v -> {
            if (listener != null) listener.onResponder(o.getId(), OfertaResponse.RECHAZADA);
        });
    }

    @Override
    public int getItemCount() {
        return ofertas.size();
    }

    /** estadoTexto ya viene traducido; el codigo es el respaldo si faltara. */
    static String textoEstado(OfertaResponse o) {
        return o.getEstadoTexto() != null ? o.getEstadoTexto() : o.getEstado();
    }

    static int colorDeEstado(String estado) {
        if (OfertaResponse.ACEPTADA.equals(estado)) return R.color.estado_aceptada;
        if (OfertaResponse.RECHAZADA.equals(estado)) return R.color.estado_rechazada;
        if (OfertaResponse.VENCIDA.equals(estado)) return R.color.estado_vencida;
        return R.color.estado_pendiente;
    }

    /**
     * Quien propuso el monto. Se decide por origen y no por "autor": el
     * backend manda como autor a la persona interesada del hilo tambien en
     * las contraofertas, que son del vendedor.
     */
    static String textoAutor(Context ctx, OfertaResponse o, boolean esVendedor) {
        if (o.laPropusoElVendedor()) {
            return ctx.getString(esVendedor
                    ? R.string.oferta_contraoferta_tuya : R.string.oferta_contraoferta_del_vendedor);
        }
        if (!esVendedor) return ctx.getString(R.string.oferta_tuya);
        String nombre = o.getAutor() != null && o.getAutor().getNombre() != null ? o.getAutor().getNombre() : "";
        return ctx.getString(R.string.oferta_de, nombre);
    }

    /**
     * Cuanto falta para que venza. Solo para pendientes: a las demas ya no
     * les corre el plazo. Null si no corresponde mostrar nada.
     */
    static String textoVencimiento(Context ctx, OfertaResponse o) {
        if (!o.isPendiente()) return null;
        FormatoOferta.Restante r = FormatoOferta.restante(o.getExpiraEn(), System.currentTimeMillis());
        if (r == null) return null;
        if (r.estaVencida()) return ctx.getString(R.string.oferta_vencida);
        if (r.getDias() >= 1) {
            int dias = (int) r.getDias();
            return ctx.getResources().getQuantityString(R.plurals.oferta_vence_en_dias, dias, dias);
        }
        if (r.getHoras() >= 1) return ctx.getString(R.string.oferta_vence_en_horas, r.getHoras());
        return ctx.getString(R.string.oferta_vence_en_minutos, r.getMinutos());
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvMontoOferta, tvEstadoOferta, tvAutorOferta, tvMensajeOferta, tvFechaOferta, tvVenceOferta;
        final LinearLayout llAccionesOferta;
        final Button btnAceptarOferta, btnRechazarOferta, btnContraofertarOferta;
        /** Color original del autor, para devolverselo a las filas que se reciclan. */
        final ColorStateList colorAutor;

        ViewHolder(View v) {
            super(v);
            tvMontoOferta = v.findViewById(R.id.tvMontoOferta);
            tvEstadoOferta = v.findViewById(R.id.tvEstadoOferta);
            tvAutorOferta = v.findViewById(R.id.tvAutorOferta);
            colorAutor = tvAutorOferta.getTextColors();
            tvMensajeOferta = v.findViewById(R.id.tvMensajeOferta);
            tvFechaOferta = v.findViewById(R.id.tvFechaOferta);
            tvVenceOferta = v.findViewById(R.id.tvVenceOferta);
            llAccionesOferta = v.findViewById(R.id.llAccionesOferta);
            btnAceptarOferta = v.findViewById(R.id.btnAceptarOferta);
            btnRechazarOferta = v.findViewById(R.id.btnRechazarOferta);
            btnContraofertarOferta = v.findViewById(R.id.btnContraofertarOferta);
        }
    }
}
