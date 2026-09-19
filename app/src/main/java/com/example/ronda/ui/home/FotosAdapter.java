package com.example.ronda.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.ronda.R;
import com.example.ronda.data.model.PublicacionDetalleResponse;

import java.util.List;

public class FotosAdapter extends RecyclerView.Adapter<FotosAdapter.FotoViewHolder> {

    private List<PublicacionDetalleResponse.Foto> fotos;

    public FotosAdapter(List<PublicacionDetalleResponse.Foto> fotos) {
        this.fotos = fotos;
    }

    @NonNull
    @Override
    public FotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_foto, parent, false);
        
        return new FotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FotoViewHolder holder, int position) {
        PublicacionDetalleResponse.Foto foto = fotos.get(position);
        Glide.with(holder.itemView.getContext())
                .load(foto.getUrl())
                .centerCrop().placeholder(android.R.drawable.ic_menu_gallery).error(android.R.drawable.ic_dialog_alert).into(holder.ivFoto);
    }

    @Override
    public int getItemCount() {
        return fotos != null ? fotos.size() : 0;
    }

    static class FotoViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFoto;
        public FotoViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFoto = itemView.findViewById(R.id.ivFoto);
        }
    }
}