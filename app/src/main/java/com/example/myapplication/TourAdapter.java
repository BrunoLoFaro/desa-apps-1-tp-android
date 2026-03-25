package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.myapplication.data.model.TourActivity;
import java.util.List;

public class TourAdapter extends RecyclerView.Adapter<TourAdapter.TourViewHolder> {

    private List<TourActivity> tourActivities;
    private boolean isHorizontal;

    public TourAdapter(List<TourActivity> tourActivities, boolean isHorizontal) {
        this.tourActivities = tourActivities;
        this.isHorizontal = isHorizontal;
    }

    @NonNull
    @Override
    public TourViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_activity, parent, false);
        if (isHorizontal) {
            // Ajustamos el ancho al 80% de la pantalla
            int width = (int) (parent.getContext().getResources().getDisplayMetrics().widthPixels * 0.80);
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            // Añadimos margen a la derecha para separar las cards
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
        
        // Imágenes reales de prueba basadas en la categoría
        String imageUrl = "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=500"; // Default
        if (activity.getCategory().equalsIgnoreCase("Gastronomía")) {
            imageUrl = "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=500";
        } else if (activity.getCategory().equalsIgnoreCase("Excursión")) {
            imageUrl = "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=500";
        } else if (activity.getCategory().equalsIgnoreCase("Free Tour")) {
            imageUrl = "https://images.openqa.com/photo-1467269204594-9661b134dd2b?w=500";
        } else if (activity.getCategory().equalsIgnoreCase("Visita Guiada")) {
            imageUrl = "https://images.unsplash.com/photo-1552832230-c0197dd311b5?w=500";
        }

        Glide.with(holder.itemView.getContext())
                .load(imageUrl)
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
