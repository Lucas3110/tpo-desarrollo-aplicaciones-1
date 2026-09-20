package com.example.ronda.ui.favoritos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.ronda.R;
import com.example.ronda.data.model.PublicacionItemResponse;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

public class FavoritosAdapter extends ListAdapter<PublicacionItemResponse, FavoritosAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onPublicacionClick(int id);
        void onFavoritoClick(PublicacionItemResponse item);
    }

    private final OnItemClickListener listener;

    public FavoritosAdapter(OnItemClickListener listener) {
        super(new DiffCallback());
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_publicacion_favorita, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitulo;
        private final TextView tvPrecio;
        private final TextView tvEstado;
        private final TextView tvZona;
        private final TextView tvNovedad;
        private final ImageView ivFoto;
        private final ImageButton btnFavorito;

        ViewHolder(View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvPrecio = itemView.findViewById(R.id.tvPrecio);
            tvEstado = itemView.findViewById(R.id.tvEstado);
            tvZona = itemView.findViewById(R.id.tvZona);
            tvNovedad = itemView.findViewById(R.id.tvNovedad);
            ivFoto = itemView.findViewById(R.id.ivFoto);
            btnFavorito = itemView.findViewById(R.id.btnFavorito);
        }

        void bind(PublicacionItemResponse item, OnItemClickListener listener) {
            tvTitulo.setText(item.getTitulo());
            tvPrecio.setText(formatearPrecio(item.getPrecio()));
            tvEstado.setText(item.getEstadoArticuloTexto());
            tvZona.setText(item.getZona() != null ? item.getZona().getNombre() : "");

            if (item.getNovedad() != null && item.getNovedad().isCambioDePrecio()) {
                tvNovedad.setVisibility(View.VISIBLE);
                int plantilla = item.getNovedad().isBajoDePrecio()
                        ? R.string.favoritos_bajo_de_precio
                        : R.string.favoritos_subio_de_precio;
                tvNovedad.setText(itemView.getContext().getString(plantilla,
                        formatearPrecio(item.getNovedad().getPrecioAnterior())));
            } else {
                tvNovedad.setVisibility(View.GONE);
            }

            if (item.getFotoPrincipal() != null && !item.getFotoPrincipal().isEmpty()) {
                ivFoto.setVisibility(View.VISIBLE);
                Glide.with(ivFoto.getContext())
                        .load(item.getFotoPrincipal())
                        .placeholder(R.drawable.bg_estado_articulo)
                        .centerCrop()
                        .into(ivFoto);
            } else {
                ivFoto.setVisibility(View.GONE);
                Glide.with(ivFoto.getContext()).clear(ivFoto);
            }

            btnFavorito.setImageResource(item.isFavorito() ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);

            itemView.setOnClickListener(v -> listener.onPublicacionClick(item.getId()));
            btnFavorito.setOnClickListener(v -> listener.onFavoritoClick(item));
        }

        private String formatearPrecio(double precio) {
            NumberFormat formato = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-AR"));
            formato.setMinimumFractionDigits(0);
            formato.setMaximumFractionDigits(2);
            return formato.format(precio);
        }
    }

    private static class DiffCallback extends DiffUtil.ItemCallback<PublicacionItemResponse> {
        @Override
        public boolean areItemsTheSame(@NonNull PublicacionItemResponse oldItem, @NonNull PublicacionItemResponse newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull PublicacionItemResponse oldItem, @NonNull PublicacionItemResponse newItem) {
            return oldItem.isFavorito() == newItem.isFavorito() &&
                   Double.compare(oldItem.getPrecio(), newItem.getPrecio()) == 0 &&
                   Objects.equals(oldItem.getTitulo(), newItem.getTitulo());
        }
    }
}
