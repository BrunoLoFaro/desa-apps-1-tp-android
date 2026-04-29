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

    public interface OnFavoriteToggleListener {
        void onFavoriteToggle(TourActivity activity, boolean targetFavorite);
    }

    private List<TourActivity> tourActivities;
    private final boolean isHorizontal;
    private final boolean isCompact;
    private OnFavoriteToggleListener favoriteToggleListener;

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

    public void setOnFavoriteToggleListener(OnFavoriteToggleListener listener) {
        this.favoriteToggleListener = listener;
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
        boolean soldOut = activity.getAvailableSlots() <= 0;
        
        // Handle discount pricing
        // API price is the ORIGINAL price. Discounted price = original * (1 - discount/100)
        if (activity.getDiscountPercentage() != null && activity.getDiscountPercentage() > 0) {
            String priceStr = activity.getPrice();
            if (priceStr != null && priceStr.startsWith("$")) {
                try {
                    double originalPrice = Double.parseDouble(priceStr.substring(1));
                    double discountedPrice = originalPrice * (1 - activity.getDiscountPercentage() / 100.0);

                    holder.originalPrice.setText(String.format("$%.2f", originalPrice));
                    holder.originalPrice.setVisibility(View.VISIBLE);
                    holder.price.setText(String.format("$%.2f", discountedPrice));

                    holder.discountBadge.setText(activity.getDiscountPercentage() + "% OFF");
                    holder.discountBadge.setVisibility(View.VISIBLE);
                } catch (NumberFormatException e) {
                    holder.originalPrice.setVisibility(View.GONE);
                    holder.discountBadge.setVisibility(View.GONE);
                    holder.price.setText(activity.getPrice());
                }
            } else {
                holder.originalPrice.setVisibility(View.GONE);
                holder.discountBadge.setVisibility(View.GONE);
                holder.price.setText(activity.getPrice());
            }
        } else {
            holder.originalPrice.setVisibility(View.GONE);
            holder.discountBadge.setVisibility(View.GONE);
            holder.price.setText(activity.getPrice());
        }
        holder.slots.setText(soldOut
                ? holder.itemView.getContext().getString(R.string.sold_out)
                : holder.itemView.getContext().getString(R.string.slots_available, activity.getAvailableSlots()));
        holder.itemView.setAlpha(soldOut ? 0.65f : 1f);

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

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .centerCrop()
                    .into(holder.image);
        } else {
            holder.image.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        if (holder.favoriteBtn != null) {
            holder.favoriteBtn.setImageResource(activity.isFavorite() ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
            holder.favoriteBtn.setOnClickListener(v -> {
                boolean targetFavorite = !activity.isFavorite();
                activity.setFavorite(targetFavorite);
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    notifyItemChanged(adapterPosition);
                }
                if (favoriteToggleListener != null) {
                    favoriteToggleListener.onFavoriteToggle(activity, targetFavorite);
                }
            });
        }

        if (holder.favoriteUpdateBadge != null) {
            boolean hasUpdates = activity.hasFavoriteUpdate() || activity.isPriceChanged() || activity.isSlotsChanged();
            holder.favoriteUpdateBadge.setVisibility(hasUpdates ? View.VISIBLE : View.GONE);
        }

        View.OnClickListener listener = v -> {
            Bundle bundle = new Bundle();
            bundle.putSerializable("activity_data", activity);
            Navigation.findNavController(v).navigate(R.id.detailFragment, bundle);
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
        TextView originalPrice, discountBadge;
        TextView description, rating, language, guide, meetingPoint, includes, cancellation;
        TextView favoriteUpdateBadge;
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
            originalPrice = itemView.findViewById(R.id.original_price);
            discountBadge = itemView.findViewById(R.id.discount_badge);
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
            favoriteUpdateBadge = itemView.findViewById(R.id.favorite_update_badge);
        }
    }
}
