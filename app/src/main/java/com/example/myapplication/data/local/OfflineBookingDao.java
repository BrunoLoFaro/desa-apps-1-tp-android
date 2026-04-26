package com.example.myapplication.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import java.util.List;

@Dao
public interface OfflineBookingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<OfflineBookingEntity> bookings);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAllIgnore(List<OfflineBookingEntity> bookings);

    @Query("SELECT * FROM offline_bookings WHERE userId = :userId AND status = 'CONFIRMED' ORDER BY sessionStartTime ASC")
    List<OfflineBookingEntity> getConfirmedByUser(long userId);

    @Query("DELETE FROM offline_bookings WHERE userId = :userId AND status = 'CONFIRMED' AND pendingCancel = 0")
    void deleteConfirmedByUser(long userId);

    @Transaction
    default void replaceConfirmed(long userId, List<OfflineBookingEntity> bookings) {
        deleteConfirmedByUser(userId);
        if (!bookings.isEmpty()) insertAllIgnore(bookings);
    }

    @Query("UPDATE offline_bookings SET pendingCancel = 1, status = 'CANCELLED' WHERE id = :id")
    void markPendingCancel(long id);

    @Query("SELECT * FROM offline_bookings WHERE userId = :userId AND pendingCancel = 1")
    List<OfflineBookingEntity> getPendingCancellations(long userId);

    @Query("DELETE FROM offline_bookings WHERE id = :id")
    void deleteById(long id);

    @Query("DELETE FROM offline_bookings WHERE userId = :userId")
    void deleteAllByUser(long userId);

    @Query("SELECT * FROM offline_bookings WHERE id = :id LIMIT 1")
    OfflineBookingEntity getById(long id);
}
