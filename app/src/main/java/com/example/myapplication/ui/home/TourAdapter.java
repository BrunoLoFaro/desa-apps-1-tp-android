package com.example.myapplication.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.data.model.TourActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.List;

public class TourAdapter extends RecyclerView.Adapter<TourAdapter.TourViewHolder> {

    private List<TourActivity> tourActivities;
    private final boolean isHorizontal;
    private final boolean isCompact;

    public TourAdapter(boolean isHorizontal) {
        this(isHorizontal, isHorizontal);
    }

    public TourAdapter(boolean isHorizontal, boolean isCompact) {
        this.tourActivities = java.util.Collections.emptyList();
        this.isHorizontal = isHorizontal;
        this.isCompact = isCompact;
    }

    public void updateData(List<TourActivity> newData) {
        this.tourActivities = newData;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TourViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_activity, parent, false);
        if (isHorizontal) {
            int width = (int) (parent.getContext().getResources().getDisplayMetrics().widthPixels * 0.85);
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 32, 0);
            view.setLayoutParams(params);
        }
        return new TourViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TourViewHolder holder, int position) {
        TourActivity activity = tourActivities.get(position);
        holder.name.setText(activity.getName());
        holder.destination.setText(activity.getDestination());
        holder.category.setText(activity.getCategory().toUpperCase());
        holder.duration.setText(activity.getDuration());
        holder.price.setText(activity.getPrice());
        holder.slots.setText(holder.itemView.getContext().getString(R.string.slots_available, activity.getAvailableSlots()));

        if (holder.rating != null) {
            if (activity.getReviewsCount() <= 0) {
                holder.rating.setText(holder.itemView.getContext().getString(R.string.no_reviews));
            } else {
                holder.rating.setText(String.valueOf(activity.getRating()));
            }
        }

        if (isCompact) {
            if (holder.detailedContainer != null) holder.detailedContainer.setVisibility(View.GONE);
        } else {
            if (holder.detailedContainer != null) holder.detailedContainer.setVisibility(View.VISIBLE);
            if (holder.description != null) holder.description.setText(activity.getDescription());
            if (holder.language != null) holder.language.setText("Idioma: " + activity.getLanguage());
            if (holder.guide != null) holder.guide.setText("Guía: " + activity.getGuideName());
            if (holder.meetingPoint != null) holder.meetingPoint.setText("Encuentro: " + activity.getMeetingPoint());
            if (holder.includes != null) {
                holder.includes.setText(formatList(activity.getWhatIncluded()));
            }
            if (holder.cancellation != null) holder.cancellation.setText(activity.getCancellationPolicy());
        }

        String imageUrl = activity.getImageUrl();

        Glide.with(holder.itemView.getContext())
                .load(imageUrl != null && !imageUrl.isEmpty() ? imageUrl : null)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .centerCrop()
                .into(holder.image);

        View.OnClickListener listener = v -> {
            Bundle bundle = new Bundle();
            bundle.putSerializable("activity_data", activity);
            Navigation.findNavController(v).navigate(R.id.action_homeFragment_to_detailFragment, bundle);
        };

        holder.itemView.setOnClickListener(listener);
        holder.image.setOnClickListener(listener);
        holder.name.setOnClickListener(listener);
    }

    private String formatList(String input) {
        if (input == null || input.isEmpty()) return "";
        String[] items = input.split("[,\\n]+");
        StringBuilder sb = new StringBuilder();
        for (String item : items) {
            String trimmed = item.trim();
            if (!trimmed.isEmpty()) {
                if (sb.length() > 0) sb.append("\n");
                sb.append("• ").append(trimmed);
            }
        }
        return sb.toString();
    }

    @Override
    public int getItemCount() {
        return tourActivities.size();
    }

    static class TourViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView category, name, destination, duration, price, slots;
        TextView description, rating, language, guide, meetingPoint, includes, cancellation;
        View detailedContainer;
        FloatingActionButton favoriteBtn;

        public TourViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.activity_image);
            category = itemView.findViewById(R.id.activity_category);
            name = itemView.findViewById(R.id.activity_name);
            destination = itemView.findViewById(R.id.activity_destination);
            duration = itemView.findViewById(R.id.activity_duration);
            price = itemView.findViewById(R.id.activity_price);
            slots = itemView.findViewById(R.id.activity_slots);

            detailedContainer = itemView.findViewById(R.id.detailed_info_container);
            description = itemView.findViewById(R.id.activity_description);
            rating = itemView.findViewById(R.id.activity_rating);
            language = itemView.findViewById(R.id.activity_language);
            guide = itemView.findViewById(R.id.activity_guide);
            meetingPoint = itemView.findViewById(R.id.activity_meeting_point);
            includes = itemView.findViewById(R.id.activity_includes);
            cancellation = itemView.findViewById(R.id.activity_cancellation);
            favoriteBtn = itemView.findViewById(R.id.favorite_button);
        }
    }
}
