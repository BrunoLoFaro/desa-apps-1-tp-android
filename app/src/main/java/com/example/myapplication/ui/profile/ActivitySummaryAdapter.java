package com.example.myapplication.ui.profile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import com.example.myapplication.data.model.BookingSummaryItem;
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
        holder.status.setText(localizedStatus(holder.itemView, item.getStatus()));
        holder.date.setText(item.getDate());
        holder.price.setText(item.getPrice());
        holder.itemView.setOnClickListener(listener != null ? v -> listener.onItemClick(item) : null);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static String localizedStatus(View view, String status) {
        if (status == null) return "";
        switch (status) {
            case "CONFIRMED":  return view.getContext().getString(R.string.booking_status_confirmed);
            case "COMPLETED":  return view.getContext().getString(R.string.booking_status_completed);
            case "CANCELLED":  return view.getContext().getString(R.string.booking_status_cancelled);
            default:           return status;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, status, date, price;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name   = itemView.findViewById(R.id.summary_activity_name);
            status = itemView.findViewById(R.id.summary_status);
            date   = itemView.findViewById(R.id.summary_date);
            price  = itemView.findViewById(R.id.summary_price);
        }
    }
}
