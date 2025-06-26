package com.example.telegesth.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.telegesth.R;
import com.example.telegesth.Transaccion;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TransaccionesAdapter extends RecyclerView.Adapter<TransaccionesAdapter.TransaccionViewHolder> {

    private List<Transaccion> transacciones;
    private OnTransaccionClickListener listener;
    private SimpleDateFormat dateFormat;

    public interface OnTransaccionClickListener {
        void onTransaccionClick(Transaccion transaccion);
        void onEliminarClick(Transaccion transaccion);
        void onDescargarImagenClick(Transaccion transaccion);
    }

    public TransaccionesAdapter(List<Transaccion> transacciones, OnTransaccionClickListener listener) {
        this.transacciones = transacciones;
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    }

    @NonNull
    @Override
    public TransaccionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaccion, parent, false);
        return new TransaccionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransaccionViewHolder holder, int position) {
        Transaccion transaccion = transacciones.get(position);

        holder.tvTitulo.setText(transaccion.getTitulo());
        holder.tvMonto.setText(String.format("$%.2f", transaccion.getMonto()));
        holder.tvDescripcion.setText(transaccion.getDescripcion());
        holder.tvFecha.setText(dateFormat.format(transaccion.getFecha()));
        holder.tvTipo.setText(transaccion.getTipo());

        // Cambiar color según el tipo
        int color = "Ingreso".equals(transaccion.getTipo()) ?
                holder.itemView.getContext().getColor(android.R.color.holo_green_dark) :
                holder.itemView.getContext().getColor(android.R.color.holo_red_dark);
        holder.tvMonto.setTextColor(color);
        holder.tvTipo.setTextColor(color);

        // Cargar imagen si existe
        if (transaccion.getImagenUrl() != null && !transaccion.getImagenUrl().isEmpty()) {
            holder.ivComprobante.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext())
                    .load(transaccion.getImagenUrl())
                    .placeholder(R.drawable.ic_placeholder)
                    .into(holder.ivComprobante);
        } else {
            holder.ivComprobante.setVisibility(View.GONE);
        }

        // Configurar clicks
        holder.itemView.setOnClickListener(v -> listener.onTransaccionClick(transaccion));
        holder.btnEliminar.setOnClickListener(v -> listener.onEliminarClick(transaccion));
        holder.btnDescargar.setOnClickListener(v -> listener.onDescargarImagenClick(transaccion));
    }

    @Override
    public int getItemCount() {
        return transacciones.size();
    }

    static class TransaccionViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitulo, tvMonto, tvDescripcion, tvFecha, tvTipo;
        ImageView ivComprobante;
        ImageButton btnEliminar, btnDescargar;

        public TransaccionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tv_titulo);
            tvMonto = itemView.findViewById(R.id.tv_monto);
            tvDescripcion = itemView.findViewById(R.id.tv_descripcion);
            tvFecha = itemView.findViewById(R.id.tv_fecha);
            tvTipo = itemView.findViewById(R.id.tv_tipo);
            ivComprobante = itemView.findViewById(R.id.iv_comprobante);
            btnEliminar = itemView.findViewById(R.id.btn_eliminar);
            btnDescargar = itemView.findViewById(R.id.btn_descargar);
        }
    }
}