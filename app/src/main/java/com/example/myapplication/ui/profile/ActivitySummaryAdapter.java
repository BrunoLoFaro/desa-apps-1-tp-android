package com.example.myapplication.ui.profile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
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
        holder.date.setText(item.getDate());
        holder.destination.setText(item.getDestination());
        boolean hasGuide = item.getGuideName() != null && !item.getGuideName().isEmpty();
        if (hasGuide) {
            holder.guide.setText(holder.itemView.getContext()
                    .getString(R.string.history_guide_prefix, item.getGuideName()));
            holder.guide.setVisibility(android.view.View.VISIBLE);
        } else {
            holder.guide.setVisibility(android.view.View.GONE);
        }
        holder.duration.setText(FormatUtils.formatDuration(item.getDurationMinutes()));
        holder.detailButton.setOnClickListener(
                listener != null ? v -> listener.onItemClick(item) : null);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, date, destination, guide, duration;
        MaterialButton detailButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name         = itemView.findViewById(R.id.summary_activity_name);
            date         = itemView.findViewById(R.id.summary_date);
            destination  = itemView.findViewById(R.id.summary_destination);
            guide        = itemView.findViewById(R.id.summary_guide);
            duration     = itemView.findViewById(R.id.summary_duration);
            detailButton = itemView.findViewById(R.id.summary_detail_button);
        }
    }
}
