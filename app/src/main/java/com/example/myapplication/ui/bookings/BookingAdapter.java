package com.example.myapplication.ui.bookings;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import com.example.myapplication.data.model.BookingResponse;
import com.google.android.material.button.MaterialButton;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

    private static final int REVIEW_WINDOW_HOURS = 48;

    public interface OnCancelClickListener {
        void onCancel(BookingResponse booking);
    }

    public interface OnReviewClickListener {
        void onReview(BookingResponse booking);
    }

    private List<BookingResponse> bookings = Collections.emptyList();
    private final OnCancelClickListener cancelClickListener;
    private final OnReviewClickListener reviewClickListener;

    public BookingAdapter(OnCancelClickListener cancelClickListener, OnReviewClickListener reviewClickListener) {
        this.cancelClickListener = cancelClickListener;
        this.reviewClickListener = reviewClickListener;
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
        holder.dateTime.setText(formatStartTime(booking.sessionStartTime));
        holder.status.setText(booking.status != null ? booking.status : "");
        holder.participants.setText("Participantes: " + booking.participants);
        holder.price.setText(formatPrice(booking.totalPrice, booking.currency));

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
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    private static String formatStartTime(String iso) {
        if (iso == null) return "";
        String value = iso.replace("T", " ");
        if (value.length() >= 16) return value.substring(0, 16);
        return value;
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

        // Accept "yyyy-MM-dd HH:mm" or "yyyy-MM-ddTHH:mm"
        String normalized = value.replace(" ", "T");
        try {
            return LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
        } catch (DateTimeParseException ignored) {
        }

        return null;
    }

    private static String formatPrice(double price, String currency) {
        if (price <= 0) return "Gratis";
        String symbol = "ARS".equals(currency) ? "$" : (currency == null ? "" : currency + " ");
        return symbol + String.format(Locale.US, "%.2f", price);
    }

    static class BookingViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView subtitle;
        final TextView dateTime;
        final TextView status;
        final TextView participants;
        final TextView price;
        final MaterialButton cancelButton;
        final MaterialButton reviewButton;

        BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.booking_title);
            subtitle = itemView.findViewById(R.id.booking_subtitle);
            dateTime = itemView.findViewById(R.id.booking_datetime);
            status = itemView.findViewById(R.id.booking_status);
            participants = itemView.findViewById(R.id.booking_participants);
            price = itemView.findViewById(R.id.booking_price);
            cancelButton = itemView.findViewById(R.id.booking_cancel_button);
            reviewButton = itemView.findViewById(R.id.booking_review_button);
        }
    }
}

