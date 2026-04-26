package com.example.myapplication.data.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {OfflineBookingEntity.class}, version = 4, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract OfflineBookingDao offlineBookingDao();
}
