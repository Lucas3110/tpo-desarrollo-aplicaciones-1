package com.example.ronda.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.ronda.R;
import com.example.ronda.data.model.PublicacionItemResponse;

import java.text.NumberFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Convierte cada publicacion en una fila del ListView (patron Adapter, como
 * en el apunte "Primeros pasos": el ListView le pide una View por item y el
 * adapter la arma a partir del dato).
 *
 * NO tiene su propia copia de los datos: recibe la lista del Fragment por
 * referencia y trabaja siempre sobre esa. Asi hay un solo dueño de "lo que
 * se ve en pantalla" y no se desincronizan al paginar o al volver del detalle.
 */
public class PublicacionAdapter extends BaseAdapter {

    public interface OnItemClickListener {
        void onPublicacionClick(PublicacionItemResponse item);
        void onFavoritoClick(PublicacionItemResponse item);
    }

    private final List<PublicacionItemResponse> items;
    private final OnItemClickListener listener;
    private final Integer usuarioIdLogueado;

    /** Ids que ya estan en la lista, para no repetir una publicacion al paginar. */
    private final Set<Integer> idsVistos = new HashSet<>();

    public PublicacionAdapter(List<PublicacionItemResponse> items) {
        this(items, null, null);
    }

    public PublicacionAdapter(List<PublicacionItemResponse> items, Integer usuarioIdLogueado, OnItemClickListener listener) {
        this.items = items;
        this.usuarioIdLogueado = usuarioIdLogueado;
        this.listener = listener;
        for (PublicacionItemResponse item : items) {
            idsVistos.add(item.getId());
        }
    }

    /** Descarta lo que habia y muestra la lista nueva (pagina 1). */
    public void reemplazar(List<PublicacionItemResponse> nuevos) {
        items.clear();
        idsVistos.clear();
        agregar(nuevos);
    }

    /**
     * Suma publicaciones al final (paginas siguientes), ignorando las que ya
     * estan: si alguien publica entre la pagina 1 y la 2, el backend corre
     * todo un lugar y el ultimo item de la 1 vuelve a aparecer en la 2.
     */
    public void agregar(List<PublicacionItemResponse> nuevos) {
        for (PublicacionItemResponse item : nuevos) {
            if (idsVistos.add(item.getId())) {
                items.add(item);
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public PublicacionItemResponse getItem(int posicion) {
        return items.get(posicion);
    }

    @Override
    public long getItemId(int posicion) {
        return items.get(posicion).getId();
    }

    @Override
    public View getView(int posicion, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            // Primera vez: inflamos la fila y guardamos las referencias a sus
            // vistas en el tag, para no volver a hacer findViewById al scrollear.
            convertView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_publicacion, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            // El ListView nos devuelve una fila que salio de pantalla: la reusamos.
            holder = (ViewHolder) convertView.getTag();
        }

        holder.mostrar(getItem(posicion), listener, usuarioIdLogueado);
        
        PublicacionItemResponse item = getItem(posicion);
        convertView.setOnClickListener(v -> {
            if (listener != null) listener.onPublicacionClick(item);
        });
        
        return convertView;
    }

    /** Referencias a las vistas de una fila, para reciclarla sin buscarlas de nuevo. */
    private static class ViewHolder {
        final TextView tvTitulo;
        final TextView tvPrecio;
        final TextView tvEstado;
        final TextView tvZona;
        final ImageView ivFoto;
        final ImageButton btnFavorito;

        ViewHolder(View fila) {
            tvTitulo = fila.findViewById(R.id.tvTitulo);
            tvPrecio = fila.findViewById(R.id.tvPrecio);
            tvEstado = fila.findViewById(R.id.tvEstado);
            tvZona = fila.findViewById(R.id.tvZona);
            ivFoto = fila.findViewById(R.id.ivFoto);
            btnFavorito = fila.findViewById(R.id.btnFavorito);
        }

        void mostrar(PublicacionItemResponse item, OnItemClickListener listener, Integer usuarioIdLogueado) {
            tvTitulo.setText(item.getTitulo());
            tvPrecio.setText(formatearPrecio(item.getPrecio()));
            // Ya viene traducido del backend ("Como nuevo"), no hace falta mapear.
            tvEstado.setText(item.getEstadoArticuloTexto());
            tvZona.setText(item.getZona() != null ? item.getZona().getNombre() : "");
            mostrarFoto(item.getFotoPrincipal());

            btnFavorito.setImageResource(item.isFavorito() ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
            
            if (java.util.Objects.equals(item.getVendedorId(), usuarioIdLogueado)) {
                btnFavorito.setVisibility(View.GONE);
            } else {
                btnFavorito.setVisibility(View.VISIBLE);
            }

            btnFavorito.setOnClickListener(v -> {
                if (listener != null) listener.onFavoritoClick(item);
            });
        }

        /**
         * La foto es opcional: si la publicacion no tiene, el espacio se
         * oculta. Glide descarga en segundo plano, cachea y, como la fila se
         * recicla, cancela solo la carga anterior de este ImageView.
         */
        private void mostrarFoto(String url) {
            ivFoto.setVisibility(View.VISIBLE);

            if (url == null || url.isEmpty()) {
                // El hueco se conserva con un marcador: escondiendolo, la fila
                // sin foto quedaba descolgada respecto de las que si tienen.
                Glide.with(ivFoto).clear(ivFoto);
                ivFoto.setScaleType(ImageView.ScaleType.FIT_CENTER);
                ivFoto.setImageResource(R.drawable.bg_sin_foto);
                return;
            }

            ivFoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Glide.with(ivFoto)
                    .load(url)
                    .placeholder(R.drawable.bg_sin_foto)
                    .centerCrop()
                    .into(ivFoto);
        }
    }

    /**
     * "$ 175.000" para los precios enteros y "$ 175.000,50" si alguna vez
     * viene con centavos. Formato de Argentina: punto de miles, coma decimal.
     */
    static String formatearPrecio(double precio) {
        NumberFormat formato = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-AR"));
        formato.setMinimumFractionDigits(0);
        formato.setMaximumFractionDigits(2);
        return formato.format(precio);
    }
}
