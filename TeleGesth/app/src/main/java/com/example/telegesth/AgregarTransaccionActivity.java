package com.example.telegesth;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.telegesth.databinding.ActivityAgregarTransaccionBinding;
import com.example.telegesth.Transaccion;
import com.example.telegesth.ServicioAlmacenamiento;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AgregarTransaccionActivity extends AppCompatActivity {
    private static final String TAG = "AgregarTransaccion";

    private ActivityAgregarTransaccionBinding binding;
    private ServicioAlmacenamiento servicioAlmacenamiento;
    private Uri imagenSeleccionada;
    private Date fechaSeleccionada;
    private Calendar calendar;
    private SimpleDateFormat dateFormat;
    private Transaccion transaccionEditar;
    private boolean esEdicion = false;

    private ActivityResultLauncher<Intent> seleccionarImagenLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    imagenSeleccionada = result.getData().getData();
                    mostrarImagenSeleccionada();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAgregarTransaccionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        inicializarComponentes();
        configurarSpinner();
        configurarEventos();
        verificarEdicion();
    }

    private void inicializarComponentes() {
        servicioAlmacenamiento = new ServicioAlmacenamiento();
        calendar = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        fechaSeleccionada = new Date();
        binding.etFecha.setText(dateFormat.format(fechaSeleccionada));
    }

    private void configurarSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.tipos_transaccion, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerTipo.setAdapter(adapter);
    }

    private void configurarEventos() {
        binding.etFecha.setOnClickListener(v -> mostrarDatePicker());
        binding.btnSeleccionarImagen.setOnClickListener(v -> seleccionarImagen());
        binding.btnGuardar.setOnClickListener(v -> guardarTransaccion());
        binding.btnCancelar.setOnClickListener(v -> finish());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void verificarEdicion() {
        Intent intent = getIntent();
        if (intent.hasExtra("transaccion_id")) {
            esEdicion = true;
            // Aquí cargarías los datos de la transacción para editar
            setTitle("Editar Transacción");
        } else {
            setTitle("Nueva Transacción");
        }
    }

    private void mostrarDatePicker() {
        calendar.setTime(fechaSeleccionada);
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    fechaSeleccionada = calendar.getTime();
                    binding.etFecha.setText(dateFormat.format(fechaSeleccionada));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void seleccionarImagen() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        seleccionarImagenLauncher.launch(intent);
    }

    private void mostrarImagenSeleccionada() {
        if (imagenSeleccionada != null) {
            binding.ivImagenSeleccionada.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(imagenSeleccionada)
                    .into(binding.ivImagenSeleccionada);
        }
    }

    private void guardarTransaccion() {
        if (!validarCampos()) return;

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnGuardar.setEnabled(false);

        String titulo = binding.etTitulo.getText().toString().trim();
        double monto = Double.parseDouble(binding.etMonto.getText().toString().trim());
        String descripcion = binding.etDescripcion.getText().toString().trim();
        String tipo = binding.spinnerTipo.getSelectedItem().toString();

        Transaccion transaccion = new Transaccion(titulo, monto, descripcion, fechaSeleccionada, tipo);

        if (imagenSeleccionada != null) {
            subirImagenYGuardar(transaccion);
        } else {
            guardarEnFirestore(transaccion);
        }
    }

    private boolean validarCampos() {
        if (binding.etTitulo.getText().toString().trim().isEmpty()) {
            binding.etTitulo.setError("Campo requerido");
            return false;
        }
        if (binding.etMonto.getText().toString().trim().isEmpty()) {
            binding.etMonto.setError("Campo requerido");
            return false;
        }
        if (imagenSeleccionada == null) {
            Toast.makeText(this, "Debe seleccionar una imagen como comprobante", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private void subirImagenYGuardar(Transaccion transaccion) {
        String nombreArchivo = servicioAlmacenamiento.generarNombreArchivo();

        servicioAlmacenamiento.subirImagen(imagenSeleccionada, nombreArchivo)
                .addOnSuccessListener(taskSnapshot -> {
                    transaccion.setImagenNombre(nombreArchivo);
                    servicioAlmacenamiento.obtenerUrlImagen(nombreArchivo)
                            .addOnSuccessListener(uri -> {
                                transaccion.setImagenUrl(uri.toString());
                                guardarEnFirestore(transaccion);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error al obtener URL de imagen", e);
                                mostrarError("Error al procesar imagen");
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al subir imagen", e);
                    mostrarError("Error al subir imagen");
                });
    }

    private void guardarEnFirestore(Transaccion transaccion) {
        servicioAlmacenamiento.guardarTransaccion(transaccion)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Transacción guardada con ID: " + documentReference.getId());
                    Toast.makeText(this, "Transacción guardada exitosamente", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al guardar transacción", e);
                    mostrarError("Error al guardar transacción");
                });
    }

    private void mostrarError(String mensaje) {
        binding.progressBar.setVisibility(View.GONE);
        binding.btnGuardar.setEnabled(true);
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}