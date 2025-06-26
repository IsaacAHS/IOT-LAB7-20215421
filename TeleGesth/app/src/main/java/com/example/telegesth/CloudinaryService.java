package com.example.telegesth;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CloudinaryService {
    private static final String TAG = "CloudinaryService";

    // ⚠️ REEMPLAZAR CON TUS CREDENCIALES REALES DE CLOUDINARY
    private static final String CLOUD_NAME = "dxxpzevgs";
    private static final String API_KEY = "643927143231768";
    private static final String API_SECRET = "06BGuKsOMVcYSHvnkZAJPlKAPRI";

    private Cloudinary cloudinary;
    private ExecutorService executor;

    public CloudinaryService() {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", CLOUD_NAME);
        config.put("api_key", API_KEY);
        config.put("api_secret", API_SECRET);
        config.put("secure", "true"); // Usar HTTPS

        cloudinary = new Cloudinary(config);
        executor = Executors.newFixedThreadPool(3);
    }

    public interface OnUploadCompleteListener {
        void onSuccess(String imageUrl);
        void onError(String error);
    }

    public void subirImagen(Context context, Uri imageUri, String tipoTransaccion, OnUploadCompleteListener listener) {
        executor.execute(() -> {
            try {
                InputStream inputStream = context.getContentResolver().openInputStream(imageUri);

                // Determinar carpeta según tipo de transacción
                String folder = "telegesth_transactions/" +
                        (tipoTransaccion != null ? tipoTransaccion.toLowerCase() : "otros");

                Map<String, Object> uploadParams = ObjectUtils.asMap(
                        "folder", folder,
                        "resource_type", "image",
                        "quality", "auto:good",
                        "format", "jpg",
                        "transformation", ObjectUtils.asMap(
                                "width", 800,
                                "height", 600,
                                "crop", "limit"
                        ),
                        "tags", "telegesth,transaction," + tipoTransaccion
                );

                Map uploadResult = cloudinary.uploader().upload(inputStream, uploadParams);
                String imageUrl = (String) uploadResult.get("secure_url");
                String publicId = (String) uploadResult.get("public_id");

                Log.d(TAG, "Imagen subida exitosamente: " + publicId);

                // Llamar al listener en el hilo principal
                if (context instanceof android.app.Activity) {
                    ((android.app.Activity) context).runOnUiThread(() ->
                            listener.onSuccess(imageUrl)
                    );
                }

            } catch (IOException e) {
                Log.e(TAG, "Error al subir imagen a Cloudinary", e);
                if (context instanceof android.app.Activity) {
                    ((android.app.Activity) context).runOnUiThread(() ->
                            listener.onError("Error al subir imagen: " + e.getMessage())
                    );
                }
            }
        });
    }

    // Método sobrecargado para compatibilidad
    public void subirImagen(Context context, Uri imageUri, OnUploadCompleteListener listener) {
        subirImagen(context, imageUri, "otros", listener);
    }

    public void eliminarImagen(String imageUrl, OnUploadCompleteListener listener) {
        executor.execute(() -> {
            try {
                String publicId = extraerPublicId(imageUrl);
                if (publicId != null) {
                    Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                    Log.d(TAG, "Imagen eliminada: " + result.get("result"));
                    listener.onSuccess("Imagen eliminada");
                } else {
                    listener.onError("No se pudo extraer el ID de la imagen");
                }
            } catch (IOException e) {
                Log.e(TAG, "Error al eliminar imagen de Cloudinary", e);
                listener.onError("Error al eliminar imagen: " + e.getMessage());
            }
        });
    }

    private String extraerPublicId(String imageUrl) {
        try {
            // URL ejemplo: https://res.cloudinary.com/cloud_name/image/upload/v1234567890/folder/image_id.jpg
            String[] parts = imageUrl.split("/");
            if (parts.length >= 2) {
                // Buscar la parte después de "upload/"
                boolean foundUpload = false;
                StringBuilder publicId = new StringBuilder();

                for (String part : parts) {
                    if (foundUpload && !part.startsWith("v")) {
                        if (publicId.length() > 0) publicId.append("/");
                        publicId.append(part);
                    }
                    if ("upload".equals(part)) {
                        foundUpload = true;
                    }
                }

                // Remover extensión del último segmento
                String result = publicId.toString();
                int lastDot = result.lastIndexOf('.');
                if (lastDot > 0) {
                    result = result.substring(0, lastDot);
                }

                return result;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al extraer public_id", e);
        }
        return null;
    }

    // Método para obtener URL optimizada
    public String getOptimizedUrl(String originalUrl, int width, int height) {
        try {
            String publicId = extraerPublicId(originalUrl);
            if (publicId != null) {
                return cloudinary.url()
                        .transformation(
                                ObjectUtils.asMap(
                                        "width", width,
                                        "height", height,
                                        "crop", "fill",
                                        "quality", "auto"
                                )
                        )
                        .generate(publicId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al generar URL optimizada", e);
        }
        return originalUrl; // Retornar URL original si falla
    }
}