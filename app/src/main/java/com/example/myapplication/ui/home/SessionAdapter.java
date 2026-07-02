package com.example.myapplication.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import com.example.myapplication.data.model.ActivitySessionResponse;
import com.example.myapplication.util.FormatUtils;
import com.google.android.material.card.MaterialCardView;
import java.util.Collections;
import java.util.List;

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
        if (newData == null) {
            sessions = Collections.emptyList();
        } else {
            java.util.ArrayList<ActivitySessionResponse> filtered = new java.util.ArrayList<>();
            java.util.Date now = new java.util.Date();
            String[] patterns = new String[] {"yyyy-MM-dd'T'HH:mm:ss","yyyy-MM-dd'T'HH:mm:ss'Z'","yyyy-MM-dd'T'HH:mm:ssX","yyyy-MM-dd"};
            for (ActivitySessionResponse s : newData) {
                if (s == null) continue;
                if (s.availableSpots <= 0) continue; // skip no spots
                java.util.Date start = null;
                for (String p : patterns) {
                    try {
                        java.text.SimpleDateFormat f = new java.text.SimpleDateFormat(p, java.util.Locale.US);
                        start = f.parse(s.startTime);
                        if (start != null) break;
                    } catch (Exception ignored) {}
                }
                if (start == null) continue; // skip if cannot parse
                if (!start.after(now)) continue; // skip past or current
                filtered.add(s);
            }
            sessions = filtered;
        }
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

        String date = FormatUtils.formatShortDate(session.startTime);
        String time = FormatUtils.extractTime(session.startTime);
        holder.dateTime.setText(date + " | " + time);
        holder.spots.setText(holder.itemView.getContext().getString(
            R.string.slots_available, Math.max(0, session.availableSpots)));
        // price intentionally hidden in session cards

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
