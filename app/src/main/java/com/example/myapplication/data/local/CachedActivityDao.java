package com.example.myapplication.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface CachedActivityDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertAll(List<CachedActivityEntity> activities);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(CachedActivityEntity activity);

    @Query("SELECT * FROM cached_activities WHERE id = :id")
    CachedActivityEntity getById(long id);

    @Query("SELECT * FROM cached_activities")
    List<CachedActivityEntity> getAll();

    @Query("SELECT COUNT(*) FROM cached_activities")
    int count();
}
