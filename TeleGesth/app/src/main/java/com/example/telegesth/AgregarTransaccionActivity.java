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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AgregarTransaccionActivity extends AppCompatActivity {
    private static final String TAG = "AgregarTransaccion";

    private ActivityAgregarTransaccionBinding binding;
    private ServicioAlmacenamiento servicioAlmacenamiento;
    private CloudinaryService cloudinaryService;
    private Uri imagenSeleccionada;
    private String urlImagenSubida;
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
        cloudinaryService = new CloudinaryService();
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
            Glide.with(this)
                    .load(imagenSeleccionada)
                    .centerCrop()
                    .into(binding.imageViewFoto);
            binding.imageViewFoto.setVisibility(View.VISIBLE);
        }
    }

    private void guardarTransaccion() {
        if (!validarCampos()) {
            return;
        }

        binding.btnGuardar.setEnabled(false);
        binding.progressBar.setVisibility(View.VISIBLE);

        if (imagenSeleccionada != null) {
            // Primero subir imagen a Cloudinary
            String tipoTransaccion = binding.spinnerTipo.getSelectedItem().toString();
            cloudinaryService.subirImagen(this, imagenSeleccionada, tipoTransaccion, new CloudinaryService.OnUploadCompleteListener() {
                @Override
                public void onSuccess(String imageUrl) {
                    urlImagenSubida = imageUrl;
                    guardarTransaccionEnFirestore();
                }

                @Override
                public void onError(String error) {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnGuardar.setEnabled(true);
                    Toast.makeText(AgregarTransaccionActivity.this,
                            "Error al subir imagen: " + error, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error Cloudinary: " + error);
                }
            });
        } else {
            // Sin imagen, guardar directamente
            guardarTransaccionEnFirestore();
        }
    }

    private void guardarTransaccionEnFirestore() {
        String titulo = binding.etTitulo.getText().toString().trim();
        String montoTexto = binding.etMonto.getText().toString().trim();
        String descripcion = binding.etDescripcion.getText().toString().trim();
        String tipo = binding.spinnerTipo.getSelectedItem().toString();

        double monto = Double.parseDouble(montoTexto);

        Transaccion transaccion = new Transaccion(titulo, monto, descripcion, tipo, fechaSeleccionada, urlImagenSubida);

        servicioAlmacenamiento.guardarTransaccion(transaccion)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Transacción guardada con ID: " + documentReference.getId());
                    Toast.makeText(this, "Transacción guardada exitosamente", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al guardar transacción", e);
                    Toast.makeText(this, "Error al guardar transacción", Toast.LENGTH_SHORT).show();
                })
                .addOnCompleteListener(task -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnGuardar.setEnabled(true);
                });
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

        try {
            Double.parseDouble(binding.etMonto.getText().toString().trim());
        } catch (NumberFormatException e) {
            binding.etMonto.setError("Monto inválido");
            return false;
        }

        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}