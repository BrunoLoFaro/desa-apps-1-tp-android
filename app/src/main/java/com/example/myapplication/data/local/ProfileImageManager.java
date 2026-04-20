package com.example.myapplication.data.local;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import dagger.hilt.android.qualifiers.ApplicationContext;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Gestiona la imagen de perfil en almacenamiento interno del dispositivo.
 * Ruta: filesDir/profile_photos/{userId}.jpg
 *
 * La imagen vive únicamente en Android — el backend nunca guarda ni sirve imágenes.
 * Siempre se carga desde el archivo local; si no existe se muestra el placeholder.
 */
@Singleton
public class ProfileImageManager {

    private static final String PHOTO_DIR = "profile_photos";

    private final Context context;

    @Inject
    public ProfileImageManager(@ApplicationContext Context context) {
        this.context = context;
    }

    /** Devuelve el File esperado para el usuario dado (puede no existir). */
    public File getLocalFile(long userId) {
        return new File(getPhotoDir(), userId + ".jpg");
    }

    /** true si existe un archivo no vacío para este usuario. */
    public boolean hasLocalImage(long userId) {
        File f = getLocalFile(userId);
        return f.exists() && f.length() > 0;
    }

    /** Persiste bytes crudos (p.ej. decodificados de base64) como imagen local. */
    public void saveBytes(long userId, byte[] bytes) throws IOException {
        File dir = getPhotoDir();
        if (!dir.exists()) dir.mkdirs();
        try (FileOutputStream out = new FileOutputStream(getLocalFile(userId))) {
            out.write(bytes);
        }
    }

    /** Lee desde una URI content:// y persiste el resultado localmente. */
    public void saveFromUri(long userId, Uri uri, ContentResolver cr) throws IOException {
        try (InputStream is = cr.openInputStream(uri)) {
            if (is == null) throw new IOException("No se pudo abrir URI: " + uri);
            saveBytes(userId, is.readAllBytes());
        }
    }

    /** Elimina la imagen local del usuario (p.ej. en logout). */
    public void delete(long userId) {
        getLocalFile(userId).delete();
    }

    private File getPhotoDir() {
        return new File(context.getFilesDir(), PHOTO_DIR);
    }
}
