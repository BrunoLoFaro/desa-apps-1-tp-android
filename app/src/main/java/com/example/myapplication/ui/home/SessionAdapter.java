package com.example.myapplication.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import com.example.myapplication.data.model.ActivitySessionResponse;
import com.google.android.material.card.MaterialCardView;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.SessionViewHolder> {

    public interface OnSessionClickListener {
        void onSessionSelected(ActivitySessionResponse session);
    }

    private List<ActivitySessionResponse> sessions = Collections.emptyList();
    private int selectedPosition = RecyclerView.NO_POSITION;
    private final OnSessionClickListener clickListener;

    public SessionAdapter() {
        this(null);
    }

    public SessionAdapter(OnSessionClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void updateData(List<ActivitySessionResponse> newData) {
        sessions = newData != null ? newData : Collections.emptyList();
        selectedPosition = RecyclerView.NO_POSITION;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_session, parent, false);
        return new SessionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        ActivitySessionResponse session = sessions.get(position);

        holder.dateTime.setText(formatStartTime(session.startTime));
        holder.spots.setText(holder.itemView.getContext().getString(
                R.string.slots_available, Math.max(0, session.availableSpots)));
        holder.price.setText(formatPrice(session.price));

        holder.itemView.setSelected(position == selectedPosition);
        if (holder.card != null) {
            boolean selected = position == selectedPosition;
            holder.card.setStrokeWidth(selected ? 4 : 0);
            holder.card.setStrokeColor(selected
                    ? holder.itemView.getContext().getColor(R.color.md_theme_primary)
                    : holder.itemView.getContext().getColor(android.R.color.transparent));
        }
        holder.itemView.setOnClickListener(v -> {
            int old = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            if (old != RecyclerView.NO_POSITION) notifyItemChanged(old);
            if (selectedPosition != RecyclerView.NO_POSITION) notifyItemChanged(selectedPosition);
            if (clickListener != null && selectedPosition != RecyclerView.NO_POSITION) {
                clickListener.onSessionSelected(sessions.get(selectedPosition));
            }
        });
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    private static String formatStartTime(String iso) {
        if (iso == null) return "";
        String value = iso.replace("T", " ");
        if (value.length() >= 16) return value.substring(0, 16);
        return value;
    }

    private static String formatPrice(double price) {
        if (price <= 0) return "Gratis";
        return String.format(Locale.US, "$%.2f", price);
    }

    static class SessionViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final TextView dateTime;
        final TextView spots;
        final TextView price;

        SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView instanceof MaterialCardView ? (MaterialCardView) itemView : null;
            dateTime = itemView.findViewById(R.id.session_datetime);
            spots = itemView.findViewById(R.id.session_spots);
            price = itemView.findViewById(R.id.session_price);
        }
    }
}
