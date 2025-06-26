package com.example.telegesth;

import android.app.Application;
import android.util.Log;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

public class TeleGesthApplication extends Application {
    private static final String TAG = "TeleGesthApp";

    @Override
    public void onCreate() {
        super.onCreate();

        // FORZAR TEMA CLARO A NIVEL DE APLICACIÓN
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        // Inicializar Firebase
        FirebaseApp.initializeApp(this);

        // Configurar Firestore
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        firestore.setFirestoreSettings(settings);

        Log.d(TAG, "Firebase inicializado correctamente");
        Log.d(TAG, "Tema claro forzado a nivel de aplicación");
    }
}