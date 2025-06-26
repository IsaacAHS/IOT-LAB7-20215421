package com.example.telegesth;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

import com.example.telegesth.Transaccion;

public class ServicioAlmacenamiento {
    private static final String TAG = "ServicioAlmacenamiento";
    private static final String COLLECTION_TRANSACCIONES = "transacciones";
    private static final String STORAGE_COMPROBANTES = "comprobantes";

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;

    public ServicioAlmacenamiento() {
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
    }

    // Métodos para Firestore
    public Task<DocumentReference> guardarTransaccion(Transaccion transaccion) {
        return db.collection(COLLECTION_TRANSACCIONES).add(transaccion);
    }

    public Task<Void> actualizarTransaccion(String id, Transaccion transaccion) {
        return db.collection(COLLECTION_TRANSACCIONES).document(id).set(transaccion);
    }

    public Task<Void> eliminarTransaccion(String id) {
        return db.collection(COLLECTION_TRANSACCIONES).document(id).delete();
    }

    public Task<QuerySnapshot> obtenerTransacciones() {
        return db.collection(COLLECTION_TRANSACCIONES)
                .orderBy("fecha", Query.Direction.DESCENDING)
                .get();
    }

    public Task<QuerySnapshot> obtenerTransaccionesPorMes(long inicioMes, long finMes) {
        return db.collection(COLLECTION_TRANSACCIONES)
                .whereGreaterThanOrEqualTo("fecha", new java.util.Date(inicioMes))
                .whereLessThan("fecha", new java.util.Date(finMes))
                .orderBy("fecha", Query.Direction.DESCENDING)
                .get();
    }

    // Métodos para Storage
    public Task<UploadTask.TaskSnapshot> subirImagen(Uri imagenUri, String nombreArchivo) {
        StorageReference imageRef = storageRef.child(STORAGE_COMPROBANTES + "/" + nombreArchivo);
        return imageRef.putFile(imagenUri);
    }

    public Task<Uri> obtenerUrlImagen(String nombreArchivo) {
        StorageReference imageRef = storageRef.child(STORAGE_COMPROBANTES + "/" + nombreArchivo);
        return imageRef.getDownloadUrl();
    }

    public void descargarImagen(String nombreArchivo, Context context, OnDescargaCompletaListener listener) {
        StorageReference imageRef = storageRef.child(STORAGE_COMPROBANTES + "/" + nombreArchivo);

        try {
            File localFile = new File(context.getExternalFilesDir(null), nombreArchivo);
            imageRef.getFile(localFile).addOnSuccessListener(taskSnapshot -> {
                Log.d(TAG, "Imagen descargada: " + localFile.getPath());
                listener.onDescargaCompleta(localFile.getPath());
            }).addOnFailureListener(exception -> {
                Log.e(TAG, "Error al descargar imagen", exception);
                listener.onError(exception.getMessage());
            });
        } catch (Exception e) {
            Log.e(TAG, "Error al crear archivo local", e);
            listener.onError(e.getMessage());
        }
    }

    public String generarNombreArchivo() {
        return UUID.randomUUID().toString() + ".jpg";
    }

    public interface OnDescargaCompletaListener {
        void onDescargaCompleta(String rutaArchivo);
        void onError(String error);
    }
}