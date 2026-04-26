package com.example.myapplication.ui.profile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.data.model.BookingSummaryItem;
import com.example.myapplication.util.FormatUtils;
import com.google.android.material.button.MaterialButton;
import java.util.Collections;
import java.util.List;

public class ActivitySummaryAdapter extends RecyclerView.Adapter<ActivitySummaryAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(BookingSummaryItem item);
    }

    private List<BookingSummaryItem> items = Collections.emptyList();
    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void updateData(List<BookingSummaryItem> newItems) {
        this.items = newItems != null ? newItems : Collections.emptyList();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_activity_summary, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BookingSummaryItem item = items.get(position);
        holder.name.setText(item.getActivityName());
        bindStatusBadge(holder.statusBadge, item.getStatus());
        holder.date.setText(item.getDate());
        holder.destination.setText(item.getDestination());
        holder.duration.setText(FormatUtils.formatDuration(item.getDurationMinutes()));

        String imageUrl = item.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .centerCrop()
                    .placeholder(R.color.md_theme_primaryContainer)
                    .into(holder.image);
        } else {
            holder.image.setImageDrawable(null);
        }

        boolean hasGuide = item.getGuideName() != null && !item.getGuideName().isEmpty();
        if (hasGuide) {
            holder.guide.setText(holder.itemView.getContext()
                    .getString(R.string.history_guide_prefix, item.getGuideName()));
            holder.guideRow.setVisibility(View.VISIBLE);
        } else {
            holder.guideRow.setVisibility(View.GONE);
        }

        holder.detailButton.setOnClickListener(
                listener != null ? v -> listener.onItemClick(item) : null);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static void bindStatusBadge(TextView badge, String status) {
        if (status == null) { badge.setVisibility(View.GONE); return; }
        android.content.Context ctx = badge.getContext();
        String label;
        int bgColor, textColor;
        switch (status.toUpperCase()) {
            case "CONFIRMED":
                label = ctx.getString(R.string.status_confirmed);
                bgColor = 0xFFE3F2FD; textColor = 0xFF1565C0; break;
            case "COMPLETED":
                label = ctx.getString(R.string.status_completed);
                bgColor = 0xFFE8F5E9; textColor = 0xFF2E7D32; break;
            case "CANCELLED":
                label = ctx.getString(R.string.status_cancelled);
                bgColor = 0xFFFFEBEE; textColor = 0xFFB71C1C; break;
            case "PENDING":
                label = ctx.getString(R.string.status_pending);
                bgColor = 0xFFFFF8E1; textColor = 0xFFE65100; break;
            case "PENDING_CANCEL":
                label = ctx.getString(R.string.status_pending_cancel);
                bgColor = 0xFFFFF3E0; textColor = 0xFFBF360C; break;
            default:
                label = status; bgColor = 0xFFF5F5F5; textColor = 0xFF616161; break;
        }
        badge.setText(label);
        badge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bgColor));
        badge.setTextColor(textColor);
        badge.setVisibility(View.VISIBLE);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView name, date, destination, guide, duration, statusBadge;
        View guideRow;
        MaterialButton detailButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            image        = itemView.findViewById(R.id.summary_image);
            name         = itemView.findViewById(R.id.summary_activity_name);
            statusBadge  = itemView.findViewById(R.id.summary_status_badge);
            date         = itemView.findViewById(R.id.summary_date);
            destination  = itemView.findViewById(R.id.summary_destination);
            guide        = itemView.findViewById(R.id.summary_guide);
            guideRow     = itemView.findViewById(R.id.summary_guide_row);
            duration     = itemView.findViewById(R.id.summary_duration);
            detailButton = itemView.findViewById(R.id.summary_detail_button);
        }
    }
}
