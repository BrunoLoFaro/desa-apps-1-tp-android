package com.example.myapplication.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.data.model.TourActivity;
import java.util.List;

public class TourAdapter extends RecyclerView.Adapter<TourAdapter.TourViewHolder> {

    private List<TourActivity> tourActivities;
    private final boolean isHorizontal;

    public TourAdapter(boolean isHorizontal) {
        this.tourActivities = java.util.Collections.emptyList();
        this.isHorizontal = isHorizontal;
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
            int width = (int) (parent.getContext().getResources().getDisplayMetrics().widthPixels * 0.80);
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 24, 0);
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

        String imageUrl = activity.getImageUrl();

        Glide.with(holder.itemView.getContext())
                .load(imageUrl != null && !imageUrl.isEmpty() ? imageUrl : null)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .centerCrop()
                .into(holder.image);
    }

    @Override
    public int getItemCount() {
        return tourActivities.size();
    }

    static class TourViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView category, name, destination, duration, price, slots;

        public TourViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.activity_image);
            category = itemView.findViewById(R.id.activity_category);
            name = itemView.findViewById(R.id.activity_name);
            destination = itemView.findViewById(R.id.activity_destination);
            duration = itemView.findViewById(R.id.activity_duration);
            price = itemView.findViewById(R.id.activity_price);
            slots = itemView.findViewById(R.id.activity_slots);
        }
    }
}
