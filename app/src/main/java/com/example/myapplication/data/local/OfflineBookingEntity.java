package com.example.myapplication.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "offline_bookings")
public class OfflineBookingEntity {
    @PrimaryKey
    public long id;
    public long userId;
    public Long sessionId;
    public Long activityId;
    public String activityName;
    public String destinationName;
    public String guideName;
    public String sessionStartTime;
    public int durationMinutes;
    public int participants;
    public double totalPrice;
    public String currency;
    public String status;
    public String cancellationPolicy;
    public String createdAt;
    public String cancelledAt;
    public boolean canReview;
    public boolean pendingCancel;
    public String voucherCode;
    public String meetingPoint;
}
