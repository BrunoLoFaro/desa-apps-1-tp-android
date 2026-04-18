package com.example.myapplication.data.local;

import android.content.Context;
import com.example.myapplication.data.model.BookingResponse;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import dagger.hilt.android.qualifiers.ApplicationContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class BookingCacheManager {

    private final File cacheDir;
    private final JsonAdapter<List<BookingResponse>> adapter;

    @Inject
    public BookingCacheManager(@ApplicationContext Context context, Moshi moshi) {
        this.cacheDir = new File(context.getFilesDir(), "booking_cache");
        this.cacheDir.mkdirs();
        Type listType = Types.newParameterizedType(List.class, BookingResponse.class);
        this.adapter = moshi.adapter(listType);
    }

    public void save(long userId, List<BookingResponse> bookings) {
        try {
            String json = adapter.toJson(bookings);
            File tmp = new File(cacheDir, userId + ".tmp");
            try (FileOutputStream fos = new FileOutputStream(tmp)) {
                fos.write(json.getBytes(StandardCharsets.UTF_8));
            }
            tmp.renameTo(getCacheFile(userId));
        } catch (IOException ignored) {
        }
    }

    public List<BookingResponse> load(long userId) {
        File file = getCacheFile(userId);
        if (!file.exists()) return null;
        try {
            byte[] bytes = new byte[(int) file.length()];
            try (FileInputStream fis = new FileInputStream(file)) {
                //noinspection ResultOfMethodCallIgnored
                fis.read(bytes);
            }
            return adapter.fromJson(new String(bytes, StandardCharsets.UTF_8));
        } catch (IOException e) {
            return null;
        }
    }

    public void clear(long userId) {
        getCacheFile(userId).delete();
    }

    private File getCacheFile(long userId) {
        return new File(cacheDir, userId + ".json");
    }
}
