package com.example.ronda.ui.favoritos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ronda.R;
import com.example.ronda.data.model.BusquedaGuardadaDto;


public class BusquedasGuardadasAdapter extends ListAdapter<BusquedaGuardadaDto, BusquedasGuardadasAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onBusquedaClick(BusquedaGuardadaDto item);
    }

    private final OnItemClickListener listener;

    public BusquedasGuardadasAdapter(OnItemClickListener listener) {
        super(new DiffCallback());
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_busqueda_guardada, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvNombreBusqueda;
        private final TextView tvFiltros;
        private final TextView tvNovedadesBusqueda;

        ViewHolder(View itemView) {
            super(itemView);
            tvNombreBusqueda = itemView.findViewById(R.id.tvNombreBusqueda);
            tvFiltros = itemView.findViewById(R.id.tvFiltros);
            tvNovedadesBusqueda = itemView.findViewById(R.id.tvNovedadesBusqueda);
        }

        void bind(BusquedaGuardadaDto item, OnItemClickListener listener) {
            tvNombreBusqueda.setText(item.getNombre());
            
            // El resumen lo arma el backend, que es el unico que puede traducir
            // el id de la categoria a su nombre sin pedir el catalogo entero.
            String resumen = item.getResumen();
            if (resumen != null && !resumen.trim().isEmpty()) {
                tvFiltros.setText(resumen);
                tvFiltros.setVisibility(View.VISIBLE);
            } else {
                tvFiltros.setVisibility(View.GONE);
            }

            int novedades = item.getNovedades();
            if (novedades > 0) {
                tvNovedadesBusqueda.setVisibility(View.VISIBLE);
                tvNovedadesBusqueda.setText(itemView.getResources()
                        .getQuantityString(R.plurals.favoritos_novedades, novedades, novedades));
            } else {
                tvNovedadesBusqueda.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> listener.onBusquedaClick(item));
        }
    }

    private static class DiffCallback extends DiffUtil.ItemCallback<BusquedaGuardadaDto> {
        @Override
        public boolean areItemsTheSame(@NonNull BusquedaGuardadaDto oldItem, @NonNull BusquedaGuardadaDto newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull BusquedaGuardadaDto oldItem, @NonNull BusquedaGuardadaDto newItem) {
            return oldItem.getNovedades() == newItem.getNovedades() &&
                   oldItem.getNombre().equals(newItem.getNombre());
        }
    }
}
