package com.example.telegesth;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

public class TeleGesthApplication extends Application {
    private static final String TAG = "TeleGesthApp";

    @Override
    public void onCreate() {
        super.onCreate();

        // Inicializar Firebase
        FirebaseApp.initializeApp(this);

        // Configurar Firestore
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        firestore.setFirestoreSettings(settings);

        Log.d(TAG, "Firebase inicializado correctamente");
    }
}