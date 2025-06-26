package com.example.telegesth;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

public class ServicioAlmacenamiento {
    private static final String TAG = "ServicioAlmacenamiento";
    private static final String COLLECTION_TRANSACCIONES = "transacciones";

    private FirebaseFirestore db;

    public ServicioAlmacenamiento() {
        db = FirebaseFirestore.getInstance();
    }

    // ✅ MÉTODOS DE FIRESTORE QUE SE MANTIENEN
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
                .orderBy("fechaCreacion", Query.Direction.DESCENDING)
                .get();
    }

    public Task<QuerySnapshot> obtenerTransaccionesPorMes(long inicioMes, long finMes) {
        return db.collection(COLLECTION_TRANSACCIONES)
                .whereGreaterThanOrEqualTo("fecha", new java.util.Date(inicioMes))
                .whereLessThan("fecha", new java.util.Date(finMes))
                .orderBy("fecha", Query.Direction.DESCENDING)
                .get();
    }

    // ✅ MÉTODO NUEVO: Obtener transacción específica (útil para edición)
    public Task<com.google.firebase.firestore.DocumentSnapshot> obtenerTransaccion(String id) {
        return db.collection(COLLECTION_TRANSACCIONES).document(id).get();
    }

    // ✅ MÉTODO NUEVO: Buscar transacciones por tipo
    public Task<QuerySnapshot> obtenerTransaccionesPorTipo(String tipo) {
        return db.collection(COLLECTION_TRANSACCIONES)
                .whereEqualTo("tipo", tipo)
                .orderBy("fechaCreacion", Query.Direction.DESCENDING)
                .get();
    }
}