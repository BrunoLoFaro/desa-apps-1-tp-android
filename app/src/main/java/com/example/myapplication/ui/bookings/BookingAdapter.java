package com.example.myapplication.ui.bookings;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
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

public class BookingAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_BOOKING = 1;
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

    public interface OnVoucherClickListener {
        void onVoucher(BookingResponse booking);
    }

    private List<BookingListItem> items = Collections.emptyList();
    private boolean offline = false;
    private final OnCancelClickListener cancelClickListener;
    private OnDetailClickListener detailClickListener;
    private OnVoucherClickListener voucherClickListener;
    private final OnReviewClickListener reviewClickListener;

    public BookingAdapter(OnCancelClickListener cancelClickListener, OnReviewClickListener reviewClickListener) {
        this.cancelClickListener = cancelClickListener;
        this.reviewClickListener = reviewClickListener;
    }

    public void setOnDetailClickListener(OnDetailClickListener listener) {
        this.detailClickListener = listener;
    }

    public void setOnVoucherClickListener(OnVoucherClickListener listener) {
        this.voucherClickListener = listener;
    }

    public void updateData(List<BookingListItem> newItems) {
        items = newItems != null ? newItems : Collections.emptyList();
        notifyDataSetChanged();
    }

    public void setOffline(boolean offline) {
        if (this.offline != offline) {
            this.offline = offline;
            notifyDataSetChanged();
        }
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof BookingListItem.SectionHeader
                ? VIEW_TYPE_HEADER : VIEW_TYPE_BOOKING;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_HEADER) {
            View view = inflater.inflate(R.layout.item_booking_section_header, parent, false);
            return new HeaderViewHolder(view);
        }
        View view = inflater.inflate(R.layout.item_booking, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            bindHeader((HeaderViewHolder) holder, (BookingListItem.SectionHeader) items.get(position));
        } else {
            bindBooking((BookingViewHolder) holder, (BookingListItem.BookingItem) items.get(position));
        }
    }

    private void bindHeader(HeaderViewHolder holder, BookingListItem.SectionHeader header) {
        holder.title.setText(header.title);
        int color = header.isToday
                ? ContextCompat.getColor(holder.itemView.getContext(), R.color.success)
                : ContextCompat.getColor(holder.itemView.getContext(), R.color.md_theme_primary);
        holder.dot.setBackgroundTintList(ColorStateList.valueOf(color));
    }

    private void bindBooking(BookingViewHolder holder, BookingListItem.BookingItem item) {
        BookingResponse booking = item.booking;

        holder.day.setText(item.dayNumber);
        holder.month.setText(item.monthAbbr);

        bindStatusBadge(holder.statusBadge, booking.status);

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


        // Estados
        final boolean isPendingCancel = "PENDING_CANCEL".equalsIgnoreCase(booking.status);
        final boolean isConfirmed = "CONFIRMED".equalsIgnoreCase(booking.status);
        final boolean isCompleted = "COMPLETED".equalsIgnoreCase(booking.status);
        final boolean canCancel = isConfirmed;
        final boolean canVoucher = isConfirmed && !isPendingCancel;
        final boolean canReview = booking.canReview && isCompleted && isWithinReviewWindow(booking.sessionStartTime, booking.durationMinutes);

        // Utilidad para configurar botones
        setButtonState(holder.cancelButton, (canCancel && !isPendingCancel), !isPendingCancel, isPendingCancel ? 0.5f : 1f);
        setButtonState(holder.voucherButton, canVoucher, !isPendingCancel, isPendingCancel ? 0.5f : 1f);
        setButtonState(holder.reviewButton, canReview, !offline, offline ? 0.45f : 1f);

        holder.cancelButton.setOnClickListener(v -> {
            if (!isPendingCancel && cancelClickListener != null) {
                cancelClickListener.onCancel(booking);
            } else if (isPendingCancel) {
                Toast.makeText(v.getContext(), R.string.cancel_booking_pending_alert, Toast.LENGTH_SHORT).show();
            }
        });

        holder.voucherButton.setOnClickListener(v -> {
            if (!isPendingCancel && voucherClickListener != null) {
                voucherClickListener.onVoucher(booking);
            } else if (isPendingCancel) {
                Toast.makeText(v.getContext(), R.string.cancel_booking_pending_alert, Toast.LENGTH_SHORT).show();
            }
        });

        holder.reviewButton.setOnClickListener(v -> {
            if (offline) {
                Toast.makeText(v.getContext(), R.string.review_offline_error, Toast.LENGTH_SHORT).show();
            } else if (reviewClickListener != null) {
                reviewClickListener.onReview(booking);
            }
        });

        holder.detailButton.setOnClickListener(v -> {
            if (detailClickListener != null) detailClickListener.onDetail(booking);
        });
    }

    /**
     * Configura visibilidad, habilitación y alpha de un botón de forma DRY.
     */
    private void setButtonState(View button, boolean visible, boolean enabled, float alpha) {
        button.setVisibility(visible ? View.VISIBLE : View.GONE);
        button.setEnabled(enabled);
        button.setAlpha(alpha);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static void bindStatusBadge(TextView badge, String status) {
        if (status == null) {
            badge.setVisibility(View.GONE);
            return;
        }
        android.content.Context ctx = badge.getContext();
        String label;
        int bgColor;
        int textColor;
            switch (status.toUpperCase()) {
                case "CONFIRMED":
                    label = ctx.getString(R.string.status_confirmed);
                    bgColor = 0xFFE8F5E9;
                    textColor = 0xFF2E7D32;
                    break;
                case "COMPLETED":
                    label = ctx.getString(R.string.status_completed);
                    bgColor = 0xFFE8F5E9;
                    textColor = 0xFF2E7D32;
                break;
            case "CANCELLED":
                label = ctx.getString(R.string.status_cancelled);
                bgColor = 0xFFFFEBEE;
                textColor = 0xFFB71C1C;
                break;
            case "PENDING_CANCEL":
                label = ctx.getString(R.string.status_pending_cancel);
                bgColor = 0xFFFFF8E1;
                textColor = 0xFFE65100;
                break;
            case "PENDING":
                label = ctx.getString(R.string.status_pending);
                bgColor = 0xFFFFF8E1;
                textColor = 0xFFE65100;
                break;
            default:
                label = status;
                bgColor = 0xFFF5F5F5;
                textColor = 0xFF616161;
                break;
        }
        badge.setText(label);
        badge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bgColor));
        badge.setTextColor(textColor);
        badge.setVisibility(View.VISIBLE);
    }

    private static boolean isWithinReviewWindow(String sessionStartIso, int durationMinutes) {
        LocalDateTime start = tryParseLocalDateTime(sessionStartIso);
        if (start == null) return true;
        LocalDateTime end = start.plusMinutes(Math.max(0, durationMinutes));
        return !LocalDateTime.now().isAfter(end.plusHours(REVIEW_WINDOW_HOURS));
    }

    private static LocalDateTime tryParseLocalDateTime(String raw) {
        if (raw == null) return null;
        String value = raw.trim();
        if (value.isEmpty()) return null;
        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (DateTimeParseException ignored) {}
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignored) {}
        String normalized = value.replace(" ", "T");
        try {
            return LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
        } catch (DateTimeParseException ignored) {}
        return null;
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        final View dot;
        final TextView title;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            dot   = itemView.findViewById(R.id.section_dot);
            title = itemView.findViewById(R.id.section_title);
        }
    }

    static class BookingViewHolder extends RecyclerView.ViewHolder {
        final TextView day;
        final TextView month;
        final TextView statusBadge;
        final TextView title;
        final TextView subtitle;
        final TextView dateTime;
        final TextView duration;
        final TextView guide;
        final MaterialButton detailButton;
        final MaterialButton cancelButton;
        final MaterialButton reviewButton;
        final MaterialButton voucherButton;

        BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            day           = itemView.findViewById(R.id.booking_day);
            month         = itemView.findViewById(R.id.booking_month);
            statusBadge   = itemView.findViewById(R.id.booking_status_badge);
            title         = itemView.findViewById(R.id.booking_title);
            subtitle      = itemView.findViewById(R.id.booking_subtitle);
            dateTime      = itemView.findViewById(R.id.booking_datetime);
            duration      = itemView.findViewById(R.id.booking_duration);
            guide         = itemView.findViewById(R.id.booking_guide);
            detailButton  = itemView.findViewById(R.id.booking_detail_button);
            cancelButton  = itemView.findViewById(R.id.booking_cancel_button);
            reviewButton  = itemView.findViewById(R.id.booking_review_button);
            voucherButton = itemView.findViewById(R.id.booking_voucher_button);
        }
    }
}
