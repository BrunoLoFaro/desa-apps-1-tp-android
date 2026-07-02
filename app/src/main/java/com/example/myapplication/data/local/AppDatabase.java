package com.example.myapplication.data.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(
    entities = {OfflineBookingEntity.class, CachedActivityEntity.class},
    version = 6,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    public abstract OfflineBookingDao offlineBookingDao();
    public abstract CachedActivityDao cachedActivityDao();

    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `cached_activities` (" +
                "`id` INTEGER NOT NULL, " +
                "`name` TEXT, " +
                "`destinationName` TEXT, " +
                "`category` TEXT, " +
                "`description` TEXT, " +
                "`imageUrl` TEXT, " +
                "`durationMinutes` INTEGER NOT NULL DEFAULT 0, " +
                "`basePrice` REAL NOT NULL DEFAULT 0, " +
                "`currency` TEXT, " +
                "`guideName` TEXT, " +
                "`meetingPoint` TEXT, " +
                "`language` TEXT, " +
                "`includesText` TEXT, " +
                "`cancellationPolicy` TEXT, " +
                "`avgRating` REAL NOT NULL DEFAULT 0, " +
                "`reviewCount` INTEGER NOT NULL DEFAULT 0, " +
                "`availableSpots` INTEGER NOT NULL DEFAULT 0, " +
                "PRIMARY KEY(`id`))"
            );
        }
    };

    public static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `cached_activities` ADD COLUMN `itineraryJson` TEXT");
            database.execSQL("ALTER TABLE `cached_activities` ADD COLUMN `galleryJson` TEXT");
        }
    };
}
