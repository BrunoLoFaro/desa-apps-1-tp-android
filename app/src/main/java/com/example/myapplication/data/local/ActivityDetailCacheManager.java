package com.example.myapplication.data.local;

import android.content.Context;
import com.example.myapplication.data.model.ActivityDetailResponse;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import dagger.hilt.android.qualifiers.ApplicationContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ActivityDetailCacheManager {

    private final File cacheDir;
    private final JsonAdapter<ActivityDetailResponse> adapter;

    @Inject
    public ActivityDetailCacheManager(@ApplicationContext Context context, Moshi moshi) {
        this.cacheDir = new File(context.getFilesDir(), "activity_detail_cache");
        this.cacheDir.mkdirs();
        this.adapter = moshi.adapter(ActivityDetailResponse.class);
    }

    public void save(long activityId, ActivityDetailResponse detail) {
        try {
            String json = adapter.toJson(detail);
            File tmp = new File(cacheDir, activityId + ".tmp");
            try (FileOutputStream fos = new FileOutputStream(tmp)) {
                fos.write(json.getBytes(StandardCharsets.UTF_8));
            }
            tmp.renameTo(getCacheFile(activityId));
        } catch (IOException ignored) {
        }
    }

    public ActivityDetailResponse load(long activityId) {
        File file = getCacheFile(activityId);
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

    public void clear(long activityId) {
        getCacheFile(activityId).delete();
    }

    private File getCacheFile(long activityId) {
        return new File(cacheDir, activityId + ".json");
    }
}
