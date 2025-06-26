package com.example.telegesth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.telegesth.TransaccionesAdapter;
import com.example.telegesth.databinding.FragmentTransaccionesBinding;

import java.util.ArrayList;
import java.util.List;

public class TransaccionesFragment extends Fragment implements TransaccionesAdapter.OnTransaccionClickListener {
    private static final String TAG = "TransaccionesFragment";

    private FragmentTransaccionesBinding binding;
    private ServicioAlmacenamiento servicioAlmacenamiento;
    private CloudinaryService cloudinaryService;
    private TransaccionesAdapter adapter;
    private List<Transaccion> listaTransacciones;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTransaccionesBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        inicializarComponentes();
        configurarRecyclerView();
        cargarTransacciones();

        return root;
    }

    private void inicializarComponentes() {
        servicioAlmacenamiento = new ServicioAlmacenamiento();
        cloudinaryService = new CloudinaryService();
        listaTransacciones = new ArrayList<>();
    }

    private void configurarRecyclerView() {
        adapter = new TransaccionesAdapter(listaTransacciones, this);
        binding.recyclerViewTransacciones.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewTransacciones.setAdapter(adapter);
    }

    private void cargarTransacciones() {
        mostrarCargando(true);

        servicioAlmacenamiento.obtenerTransacciones()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listaTransacciones.clear();
                    queryDocumentSnapshots.forEach(doc -> {
                        Transaccion transaccion = doc.toObject(Transaccion.class);
                        transaccion.setId(doc.getId());
                        listaTransacciones.add(transaccion);
                    });

                    adapter.notifyDataSetChanged();
                    mostrarCargando(false);
                    actualizarEstadoVista();

                    Log.d(TAG, "Transacciones cargadas: " + listaTransacciones.size());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al cargar transacciones", e);
                    mostrarCargando(false);
                    Toast.makeText(getContext(), "Error al cargar transacciones", Toast.LENGTH_SHORT).show();
                });
    }

    private void mostrarCargando(boolean mostrar) {
        if (binding.progressBar != null) {
            binding.progressBar.setVisibility(mostrar ? View.VISIBLE : View.GONE);
        }
    }

    private void actualizarEstadoVista() {
        if (listaTransacciones.isEmpty()) {
            binding.tvSinTransacciones.setVisibility(View.VISIBLE);
            binding.recyclerViewTransacciones.setVisibility(View.GONE);
        } else {
            binding.tvSinTransacciones.setVisibility(View.GONE);
            binding.recyclerViewTransacciones.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onTransaccionClick(Transaccion transaccion) {
        Intent intent = new Intent(getContext(), AgregarTransaccionActivity.class);
        intent.putExtra("transaccion_id", transaccion.getId());
        startActivity(intent);
    }

    @Override
    public void onEliminarClick(Transaccion transaccion) {
        // Confirmar eliminación
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Eliminar Transacción")
                .setMessage("¿Estás seguro de que deseas eliminar esta transacción?")
                .setPositiveButton("Eliminar", (dialog, which) -> eliminarTransaccion(transaccion))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarTransaccion(Transaccion transaccion) {
        // Primero eliminar imagen de Cloudinary si existe
        if (transaccion.getFoto() != null && !transaccion.getFoto().isEmpty()) {
            cloudinaryService.eliminarImagen(transaccion.getFoto(), new CloudinaryService.OnUploadCompleteListener() {
                @Override
                public void onSuccess(String result) {
                    Log.d(TAG, "Imagen eliminada de Cloudinary");
                    eliminarTransaccionDeFirestore(transaccion);
                }

                @Override
                public void onError(String error) {
                    Log.w(TAG, "Error al eliminar imagen de Cloudinary: " + error);
                    // Continuar eliminando la transacción aunque falle la imagen
                    eliminarTransaccionDeFirestore(transaccion);
                }
            });
        } else {
            // No hay imagen, eliminar directamente
            eliminarTransaccionDeFirestore(transaccion);
        }
    }

    private void eliminarTransaccionDeFirestore(Transaccion transaccion) {
        servicioAlmacenamiento.eliminarTransaccion(transaccion.getId())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Transacción eliminada exitosamente");
                    Toast.makeText(getContext(), "Transacción eliminada", Toast.LENGTH_SHORT).show();
                    cargarTransacciones(); // Recargar lista
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al eliminar transacción", e);
                    Toast.makeText(getContext(), "Error al eliminar transacción", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onCompartirClick(Transaccion transaccion) {
        compartirTransaccion(transaccion);
    }

    private void compartirTransaccion(Transaccion transaccion) {
        StringBuilder contenido = new StringBuilder();
        contenido.append("💰 TRANSACCIÓN\n");
        contenido.append("────────────────\n");
        contenido.append("📝 Título: ").append(transaccion.getTitulo()).append("\n");
        contenido.append("💵 Monto: $").append(String.format("%.2f", transaccion.getMonto())).append("\n");
        contenido.append("📊 Tipo: ").append(transaccion.getTipo()).append("\n");
        contenido.append("📅 Fecha: ").append(android.text.format.DateFormat.format("dd/MM/yyyy", transaccion.getFecha())).append("\n");

        if (transaccion.getDescripcion() != null && !transaccion.getDescripcion().isEmpty()) {
            contenido.append("📄 Descripción: ").append(transaccion.getDescripcion()).append("\n");
        }

        if (transaccion.getFoto() != null && !transaccion.getFoto().isEmpty()) {
            contenido.append("🖼️ Imagen: ").append(transaccion.getFoto()).append("\n");
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, contenido.toString());
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Transacción: " + transaccion.getTitulo());

        startActivity(Intent.createChooser(shareIntent, "Compartir transacción"));
    }

    @Override
    public void onResume() {
        super.onResume();
        // Recargar transacciones cuando se regrese a este fragment
        cargarTransacciones();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}