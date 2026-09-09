package com.example.ronda.ui.home;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ronda.R;
import com.example.ronda.data.model.OfertaResponse;
import java.util.ArrayList;
import java.util.List;

public class OfertasAdapter extends RecyclerView.Adapter<OfertasAdapter.ViewHolder> {
    private List<OfertaResponse> ofertas = new ArrayList<>();
    private boolean esVendedor;
    private OnResponderOfertaClickListener listener;

    public interface OnResponderOfertaClickListener {
        void onResponder(int ofertaId, String estado);
    }

    public void setOfertas(List<OfertaResponse> ofertas, boolean esVendedor, OnResponderOfertaClickListener listener) {
        this.ofertas = ofertas;
        this.esVendedor = esVendedor;
        this.listener = listener;
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
        holder.tvMontoOferta.setText(String.format("$ %.2f", o.getMonto()));
        holder.tvEstadoOferta.setText(o.getEstado());
        
        switch (o.getEstado()) {
            case "ACEPTADA": holder.tvEstadoOferta.setTextColor(Color.parseColor("#4CAF50")); break;
            case "RECHAZADA": holder.tvEstadoOferta.setTextColor(Color.parseColor("#F44336")); break;
            default: holder.tvEstadoOferta.setTextColor(Color.parseColor("#FF9800")); break;
        }

        if (o.getAutor() != null) {
            holder.tvAutorOferta.setText(o.getAutor().getNombre());
            holder.tvAutorOferta.setVisibility(View.VISIBLE);
        } else {
            holder.tvAutorOferta.setVisibility(View.GONE);
        }
        
        holder.tvFechaOferta.setText(o.getCreadoEn().substring(0, 10));

        if (esVendedor && "PENDIENTE".equals(o.getEstado())) {
            holder.llAccionesOferta.setVisibility(View.VISIBLE);
            holder.btnAceptarOferta.setOnClickListener(v -> {
                if (listener != null) listener.onResponder(o.getId(), "ACEPTADA");
            });
            holder.btnRechazarOferta.setOnClickListener(v -> {
                if (listener != null) listener.onResponder(o.getId(), "RECHAZADA");
            });
        } else {
            holder.llAccionesOferta.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return ofertas.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMontoOferta, tvEstadoOferta, tvAutorOferta, tvFechaOferta;
        LinearLayout llAccionesOferta;
        Button btnAceptarOferta, btnRechazarOferta;

        ViewHolder(View v) {
            super(v);
            tvMontoOferta = v.findViewById(R.id.tvMontoOferta);
            tvEstadoOferta = v.findViewById(R.id.tvEstadoOferta);
            tvAutorOferta = v.findViewById(R.id.tvAutorOferta);
            tvFechaOferta = v.findViewById(R.id.tvFechaOferta);
            llAccionesOferta = v.findViewById(R.id.llAccionesOferta);
            btnAceptarOferta = v.findViewById(R.id.btnAceptarOferta);
            btnRechazarOferta = v.findViewById(R.id.btnRechazarOferta);
        }
    }
}
