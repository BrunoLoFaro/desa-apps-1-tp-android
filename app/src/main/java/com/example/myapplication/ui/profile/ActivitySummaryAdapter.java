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

    public interface OnReviewClickListener {
        void onReview(BookingSummaryItem item);
    }

    private List<BookingSummaryItem> items = Collections.emptyList();
    private OnItemClickListener listener;
    private OnReviewClickListener reviewListener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnReviewClickListener(OnReviewClickListener listener) {
        this.reviewListener = listener;
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

        // Eliminamos la validación de 48h del frontend para confiar plenamente en el flag 'canReview' del backend.
        // Si el historial muestra el item, usualmente es porque está COMPLETED.
        boolean canReview = item.isCanReview();

        if (canReview) {
            holder.reviewButton.setVisibility(View.VISIBLE);
            holder.reviewButton.setOnClickListener(v -> {
                if (reviewListener != null) reviewListener.onReview(item);
            });
        } else {
            holder.reviewButton.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView name, date, destination, guide, duration;
        View guideRow;
        MaterialButton detailButton;
        MaterialButton reviewButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            image        = itemView.findViewById(R.id.summary_image);
            name         = itemView.findViewById(R.id.summary_activity_name);
            date         = itemView.findViewById(R.id.summary_date);
            destination  = itemView.findViewById(R.id.summary_destination);
            guide        = itemView.findViewById(R.id.summary_guide);
            guideRow     = itemView.findViewById(R.id.summary_guide_row);
            duration     = itemView.findViewById(R.id.summary_duration);
            detailButton = itemView.findViewById(R.id.summary_detail_button);
            reviewButton = itemView.findViewById(R.id.summary_review_button);
        }
    }
}
