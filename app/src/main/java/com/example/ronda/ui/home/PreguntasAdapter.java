package com.example.ronda.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ronda.R;
import com.example.ronda.data.model.PreguntaResponse;
import java.util.ArrayList;
import java.util.List;

public class PreguntasAdapter extends RecyclerView.Adapter<PreguntasAdapter.ViewHolder> {
    private List<PreguntaResponse> preguntas = new ArrayList<>();
    private boolean esVendedor;
    private OnResponderClickListener listener;

    public interface OnResponderClickListener {
        void onResponder(int preguntaId, String respuesta);
    }

    public void setPreguntas(List<PreguntaResponse> preguntas, boolean esVendedor, OnResponderClickListener listener) {
        this.preguntas = preguntas;
        this.esVendedor = esVendedor;
        this.listener = listener;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pregunta, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PreguntaResponse p = preguntas.get(position);
        holder.tvPregunta.setText(p.getTexto());
        holder.tvAutorPregunta.setText(p.getAutor().getNombre() + " - " + p.getCreadoEn().substring(0, 10));

        if (p.isRespondida()) {
            holder.llRespuesta.setVisibility(View.VISIBLE);
            holder.llResponder.setVisibility(View.GONE);
            holder.tvRespuesta.setText(p.getRespuesta());
            holder.tvFechaRespuesta.setText(p.getRespondidaEn().substring(0, 10));
        } else {
            holder.llRespuesta.setVisibility(View.GONE);
            if (esVendedor) {
                holder.llResponder.setVisibility(View.VISIBLE);
                holder.btnResponder.setOnClickListener(v -> {
                    String respuesta = holder.etRespuesta.getText().toString();
                    if (!respuesta.isEmpty() && listener != null) {
                        listener.onResponder(p.getId(), respuesta);
                    }
                });
            } else {
                holder.llResponder.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return preguntas.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPregunta, tvAutorPregunta, tvRespuesta, tvFechaRespuesta;
        LinearLayout llRespuesta, llResponder;
        EditText etRespuesta;
        Button btnResponder;

        ViewHolder(View v) {
            super(v);
            tvPregunta = v.findViewById(R.id.tvPregunta);
            tvAutorPregunta = v.findViewById(R.id.tvAutorPregunta);
            tvRespuesta = v.findViewById(R.id.tvRespuesta);
            tvFechaRespuesta = v.findViewById(R.id.tvFechaRespuesta);
            llRespuesta = v.findViewById(R.id.llRespuesta);
            llResponder = v.findViewById(R.id.llResponder);
            etRespuesta = v.findViewById(R.id.etRespuesta);
            btnResponder = v.findViewById(R.id.btnResponder);
        }
    }
}
