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
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

    public interface OnCancelClickListener {
        void onCancel(Long bookingId);
    }

    private List<BookingResponse> bookings = Collections.emptyList();
    private final OnCancelClickListener cancelClickListener;

    public BookingAdapter(OnCancelClickListener cancelClickListener) {
        this.cancelClickListener = cancelClickListener;
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
            if (cancelClickListener != null) cancelClickListener.onCancel(booking.id);
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

        BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.booking_title);
            subtitle = itemView.findViewById(R.id.booking_subtitle);
            dateTime = itemView.findViewById(R.id.booking_datetime);
            status = itemView.findViewById(R.id.booking_status);
            participants = itemView.findViewById(R.id.booking_participants);
            price = itemView.findViewById(R.id.booking_price);
            cancelButton = itemView.findViewById(R.id.booking_cancel_button);
        }
    }
}

