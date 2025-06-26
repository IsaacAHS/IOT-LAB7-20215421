package com.example.telegesth;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.example.telegesth.R;
import com.example.telegesth.Transaccion;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TransaccionesAdapter extends RecyclerView.Adapter<TransaccionesAdapter.TransaccionViewHolder> {

    private List<Transaccion> transacciones;
    private OnTransaccionClickListener listener;
    private Context context;
    private SimpleDateFormat dateFormat;
    private NumberFormat currencyFormat;

    public interface OnTransaccionClickListener {
        void onTransaccionClick(Transaccion transaccion);
        void onEliminarClick(Transaccion transaccion);
        void onCompartirClick(Transaccion transaccion);
    }

    public TransaccionesAdapter(List<Transaccion> transacciones, OnTransaccionClickListener listener) {
        this.transacciones = transacciones;
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "PE"));
    }

    @NonNull
    @Override
    public TransaccionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_transaccion, parent, false);
        return new TransaccionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransaccionViewHolder holder, int position) {
        Transaccion transaccion = transacciones.get(position);
        holder.bind(transaccion);
    }

    @Override
    public int getItemCount() {
        return transacciones.size();
    }

    public class TransaccionViewHolder extends RecyclerView.ViewHolder {
        private TextView tvTitulo;
        private TextView tvMonto;
        private TextView tvDescripcion;
        private TextView tvTipo;
        private TextView tvFecha;
        private ImageView ivFoto;
        private ImageButton btnEliminar;
        private ImageButton btnCompartir;
        private View contenedorImagen;

        public TransaccionViewHolder(@NonNull View itemView) {
            super(itemView);

            tvTitulo = itemView.findViewById(R.id.tv_titulo);
            tvMonto = itemView.findViewById(R.id.tv_monto);
            tvDescripcion = itemView.findViewById(R.id.tv_descripcion);
            tvTipo = itemView.findViewById(R.id.tv_tipo);
            tvFecha = itemView.findViewById(R.id.tv_fecha);
            ivFoto = itemView.findViewById(R.id.iv_foto);
            btnEliminar = itemView.findViewById(R.id.btn_eliminar);
            btnCompartir = itemView.findViewById(R.id.btn_compartir);
            contenedorImagen = itemView.findViewById(R.id.contenedor_imagen);

            // Click en el item completo
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onTransaccionClick(transacciones.get(position));
                }
            });

            // Click en eliminar
            if (btnEliminar != null) {
                btnEliminar.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && listener != null) {
                        listener.onEliminarClick(transacciones.get(position));
                    }
                });
            }

            // Click en compartir
            if (btnCompartir != null) {
                btnCompartir.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && listener != null) {
                        listener.onCompartirClick(transacciones.get(position));
                    }
                });
            }
        }

        public void bind(Transaccion transaccion) {
            // Información básica
            tvTitulo.setText(transaccion.getTitulo());
            tvMonto.setText(currencyFormat.format(transaccion.getMonto()));
            tvTipo.setText(transaccion.getTipo());
            tvFecha.setText(dateFormat.format(transaccion.getFecha()));

            // Descripción
            if (transaccion.getDescripcion() != null && !transaccion.getDescripcion().isEmpty()) {
                tvDescripcion.setText(transaccion.getDescripcion());
                tvDescripcion.setVisibility(View.VISIBLE);
            } else {
                tvDescripcion.setVisibility(View.GONE);
            }

            // Color del monto según el tipo
            configurarColorMonto(transaccion);

            // Cargar imagen desde Cloudinary
            cargarImagen(transaccion);
        }

        private void configurarColorMonto(Transaccion transaccion) {
            int color;
            if ("Ingreso".equals(transaccion.getTipo())) {
                color = context.getResources().getColor(android.R.color.holo_green_dark, null);
            } else if ("Egreso".equals(transaccion.getTipo())) {
                color = context.getResources().getColor(android.R.color.holo_red_dark, null);
            } else {
                color = context.getResources().getColor(android.R.color.holo_blue_dark, null);
            }
            tvMonto.setTextColor(color);
        }

        private void cargarImagen(Transaccion transaccion) {
            if (transaccion.getFoto() != null && !transaccion.getFoto().isEmpty()) {
                // Mostrar contenedor de imagen
                if (contenedorImagen != null) {
                    contenedorImagen.setVisibility(View.VISIBLE);
                }
                ivFoto.setVisibility(View.VISIBLE);

                // Cargar imagen desde URL de Cloudinary usando Glide
                Glide.with(context)
                        .load(transaccion.getFoto())
                        .placeholder(R.drawable.ic_launcher_foreground) // Placeholder temporal
                        .error(R.drawable.ic_launcher_foreground) // Error temporal
                        .transform(new CenterCrop(), new RoundedCorners(16))
                        .into(ivFoto);

                // Click en imagen para mostrar opciones
                ivFoto.setOnClickListener(v -> mostrarOpcionesImagen(transaccion.getFoto()));

            } else {
                // Ocultar contenedor de imagen
                if (contenedorImagen != null) {
                    contenedorImagen.setVisibility(View.GONE);
                }
                ivFoto.setVisibility(View.GONE);
            }
        }

        private void mostrarOpcionesImagen(String imageUrl) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Imagen del comprobante")
                    .setItems(new CharSequence[]{"Ver imagen completa", "Compartir imagen", "Copiar enlace"},
                            (dialog, which) -> {
                                switch (which) {
                                    case 0: // Ver imagen completa
                                        abrirImagenEnNavegador(imageUrl);
                                        break;
                                    case 1: // Compartir imagen
                                        compartirImagen(imageUrl);
                                        break;
                                    case 2: // Copiar enlace
                                        copiarEnlace(imageUrl);
                                        break;
                                }
                            })
                    .setNegativeButton("Cancelar", null)
                    .show();
        }

        private void abrirImagenEnNavegador(String imageUrl) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(imageUrl));
            context.startActivity(browserIntent);
        }

        private void compartirImagen(String imageUrl) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Comprobante: " + imageUrl);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Comprobante de transacción");
            context.startActivity(Intent.createChooser(shareIntent, "Compartir imagen"));
        }

        private void copiarEnlace(String imageUrl) {
            android.content.ClipboardManager clipboard =
                    (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("URL Imagen", imageUrl);
            clipboard.setPrimaryClip(clip);

            android.widget.Toast.makeText(context, "Enlace copiado", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    // Método para actualizar la lista
    public void actualizarTransacciones(List<Transaccion> nuevasTransacciones) {
        this.transacciones.clear();
        this.transacciones.addAll(nuevasTransacciones);
        notifyDataSetChanged();
    }

    // Método para agregar transacción
    public void agregarTransaccion(Transaccion transaccion) {
        this.transacciones.add(0, transaccion);
        notifyItemInserted(0);
    }

    // Método para eliminar transacción
    public void eliminarTransaccion(int position) {
        if (position >= 0 && position < transacciones.size()) {
            transacciones.remove(position);
            notifyItemRemoved(position);
        }
    }
}