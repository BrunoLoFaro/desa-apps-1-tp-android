package com.example.myapplication.ui.bookings;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import com.example.myapplication.data.model.BookingResponse;
import com.example.myapplication.util.FormatUtils;
import com.google.android.material.button.MaterialButton;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

    private static final int REVIEW_WINDOW_HOURS = 48;

    public interface OnCancelClickListener {
        void onCancel(BookingResponse booking);
    }

    public interface OnReviewClickListener {
        void onReview(BookingResponse booking);
    }

    public interface OnDetailClickListener {
        void onDetail(BookingResponse booking);
    }

    private List<BookingResponse> bookings = Collections.emptyList();
    private final OnCancelClickListener cancelClickListener;
    private OnDetailClickListener detailClickListener;
    private final OnReviewClickListener reviewClickListener;

    public BookingAdapter(OnCancelClickListener cancelClickListener, OnReviewClickListener reviewClickListener) {
        this.cancelClickListener = cancelClickListener;
        this.reviewClickListener = reviewClickListener;
    }

    public void setOnDetailClickListener(OnDetailClickListener listener) {
        this.detailClickListener = listener;
    }

    public void updateData(List<BookingResponse> newData) {
        bookings = newData != null ? newData : Collections.emptyList();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        BookingResponse booking = bookings.get(position);

        holder.title.setText(booking.activityName != null ? booking.activityName : "");

        String destination = booking.destination != null ? booking.destination.name : "";
        holder.subtitle.setText(destination);

        holder.dateTime.setText(FormatUtils.formatStartTime(booking.sessionStartTime));

        holder.duration.setText(FormatUtils.formatDuration(booking.durationMinutes));

        if (booking.guideName != null && !booking.guideName.isEmpty()) {
            holder.guide.setText(holder.itemView.getContext()
                    .getString(R.string.history_guide_prefix, booking.guideName));
            holder.guide.setVisibility(View.VISIBLE);
        } else {
            holder.guide.setVisibility(View.GONE);
        }

        boolean canCancel = "CONFIRMED".equalsIgnoreCase(booking.status);
        holder.cancelButton.setVisibility(canCancel ? View.VISIBLE : View.GONE);
        holder.cancelButton.setOnClickListener(v -> {
            if (cancelClickListener != null) cancelClickListener.onCancel(booking);
        });

        boolean canReview = booking.canReview
                && "COMPLETED".equalsIgnoreCase(booking.status)
                && isWithinReviewWindow(booking.sessionStartTime, booking.durationMinutes);
        holder.reviewButton.setVisibility(canReview ? View.VISIBLE : View.GONE);
        holder.reviewButton.setOnClickListener(v -> {
            if (reviewClickListener != null) reviewClickListener.onReview(booking);
        });

        holder.detailButton.setOnClickListener(v -> {
            if (detailClickListener != null) detailClickListener.onDetail(booking);
        });
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    private static boolean isWithinReviewWindow(String sessionStartIso, int durationMinutes) {
        LocalDateTime start = tryParseLocalDateTime(sessionStartIso);
        if (start == null) {
            return true;
        }
        LocalDateTime end = start.plusMinutes(Math.max(0, durationMinutes));
        return !LocalDateTime.now().isAfter(end.plusHours(REVIEW_WINDOW_HOURS));
    }

    private static LocalDateTime tryParseLocalDateTime(String raw) {
        if (raw == null) return null;
        String value = raw.trim();
        if (value.isEmpty()) return null;

        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
        }

        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignored) {
        }

        String normalized = value.replace(" ", "T");
        try {
            return LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
        } catch (DateTimeParseException ignored) {
        }

        return null;
    }

    static class BookingViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView subtitle;
        final TextView dateTime;
        final TextView duration;
        final TextView guide;
        final MaterialButton detailButton;
        final MaterialButton cancelButton;
        final MaterialButton reviewButton;

        BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            title        = itemView.findViewById(R.id.booking_title);
            subtitle     = itemView.findViewById(R.id.booking_subtitle);
            dateTime     = itemView.findViewById(R.id.booking_datetime);
            duration     = itemView.findViewById(R.id.booking_duration);
            guide        = itemView.findViewById(R.id.booking_guide);
            detailButton = itemView.findViewById(R.id.booking_detail_button);
            cancelButton = itemView.findViewById(R.id.booking_cancel_button);
            reviewButton = itemView.findViewById(R.id.booking_review_button);
        }
    }
}
