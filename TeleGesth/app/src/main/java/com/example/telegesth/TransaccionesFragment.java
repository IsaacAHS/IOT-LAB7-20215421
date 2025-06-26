package com.example.telegesth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.telegesth.AgregarTransaccionActivity;
import com.example.telegesth.adapters.TransaccionesAdapter;
import com.example.telegesth.databinding.FragmentTransaccionesBinding;
import com.example.telegesth.Transaccion;
import com.example.telegesth.ServicioAlmacenamiento;

import java.util.ArrayList;
import java.util.List;

public class TransaccionesFragment extends Fragment implements TransaccionesAdapter.OnTransaccionClickListener {
    private static final String TAG = "TransaccionesFragment";

    private FragmentTransaccionesBinding binding;
    private ServicioAlmacenamiento servicioAlmacenamiento;
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
        listaTransacciones = new ArrayList<>();
    }

    private void configurarRecyclerView() {
        adapter = new TransaccionesAdapter(listaTransacciones, this);
        binding.recyclerViewTransacciones.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewTransacciones.setAdapter(adapter);
    }

    private void cargarTransacciones() {
        binding.progressBar.setVisibility(View.VISIBLE);

        servicioAlmacenamiento.obtenerTransacciones()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listaTransacciones.clear();
                    queryDocumentSnapshots.forEach(doc -> {
                        Transaccion transaccion = doc.toObject(Transaccion.class);
                        transaccion.setId(doc.getId());
                        listaTransacciones.add(transaccion);
                    });

                    adapter.notifyDataSetChanged();
                    binding.progressBar.setVisibility(View.GONE);

                    if (listaTransacciones.isEmpty()) {
                        binding.tvSinTransacciones.setVisibility(View.VISIBLE);
                    } else {
                        binding.tvSinTransacciones.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al cargar transacciones", e);
                    binding.progressBar.setVisibility(View.GONE);
                });
    }

    @Override
    public void onTransaccionClick(Transaccion transaccion) {
        Intent intent = new Intent(getContext(), AgregarTransaccionActivity.class);
        intent.putExtra("transaccion_id", transaccion.getId());
        startActivity(intent);
    }

    @Override
    public void onEliminarClick(Transaccion transaccion) {
        servicioAlmacenamiento.eliminarTransaccion(transaccion.getId())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Transacción eliminada");
                    cargarTransacciones(); // Recargar lista
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al eliminar transacción", e);
                });
    }

    @Override
    public void onDescargarImagenClick(Transaccion transaccion) {
        if (transaccion.getImagenNombre() != null && !transaccion.getImagenNombre().isEmpty()) {
            servicioAlmacenamiento.descargarImagen(transaccion.getImagenNombre(),
                    getContext(), new ServicioAlmacenamiento.OnDescargaCompletaListener() {
                        @Override
                        public void onDescargaCompleta(String rutaArchivo) {
                            // Mostrar notificación de descarga exitosa
                            Log.d(TAG, "Imagen descargada en: " + rutaArchivo);
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Error al descargar imagen: " + error);
                        }
                    });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        cargarTransacciones(); // Recargar cuando se regrese a este fragment
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}