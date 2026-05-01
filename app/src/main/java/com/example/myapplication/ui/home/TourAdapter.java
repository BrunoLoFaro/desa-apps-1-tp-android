package com.example.myapplication.ui.home;

import android.annotation.SuppressLint;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.data.model.TourActivity;
import com.example.myapplication.util.FormatUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.util.TypedValue;
import java.util.List;

public class TourAdapter extends RecyclerView.Adapter<TourAdapter.TourViewHolder> {

    public interface OnFavoriteToggleListener {
        void onFavoriteToggle(TourActivity activity, boolean targetFavorite);
    }

    private List<TourActivity> tourActivities;
    private final boolean isHorizontal;
    private final boolean isCompact;
    private final boolean isFavoritesSection;
    private OnFavoriteToggleListener favoriteToggleListener;

    public TourAdapter(boolean isHorizontal) {
        this(isHorizontal, isHorizontal, false);
    }

    public TourAdapter(boolean isHorizontal, boolean isCompact) {
        this(isHorizontal, isCompact, false);
    }

    public TourAdapter(boolean isHorizontal, boolean isCompact, boolean isFavoritesSection) {
        this.tourActivities = java.util.Collections.emptyList();
        this.isHorizontal = isHorizontal;
        this.isCompact = isCompact;
        this.isFavoritesSection = isFavoritesSection;
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
            // Ensure fixed height for horizontal carousel items to keep uniform card heights
            int heightDp = 680; // matches item_activity root FrameLayout height
            int heightPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, heightDp, parent.getContext().getResources().getDisplayMetrics());
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(width, heightPx);
            params.setMargins(0, 0, 32, 0);
            view.setLayoutParams(params);
        }
        return new TourViewHolder(view);
    }

    @Override
    @SuppressLint("SetTextI18n")
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

                    holder.originalPrice.setText(FormatUtils.formatPrice(originalPrice, "ARS"));
                    holder.originalPrice.setPaintFlags(holder.originalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    holder.originalPrice.setVisibility(View.VISIBLE);
                    holder.price.setText(FormatUtils.formatPrice(discountedPrice, "ARS"));

                    holder.discountBadge.setText(activity.getDiscountPercentage() + "% OFF");
                    holder.discountBadge.setVisibility(View.VISIBLE);
                } catch (NumberFormatException e) {
                    holder.originalPrice.setText(activity.getPrice());
                    holder.originalPrice.setVisibility(View.INVISIBLE);
                    holder.discountBadge.setText("");
                    holder.discountBadge.setVisibility(View.INVISIBLE);
                    holder.price.setText(activity.getPrice());
                }
            } else {
                holder.originalPrice.setText(activity.getPrice());
                holder.originalPrice.setVisibility(View.INVISIBLE);
                holder.discountBadge.setText("");
                holder.discountBadge.setVisibility(View.INVISIBLE);
                holder.price.setText(activity.getPrice());
            }
        } else {
            holder.originalPrice.setText(activity.getPrice());
            holder.originalPrice.setVisibility(View.INVISIBLE);
            holder.discountBadge.setText("");
            holder.discountBadge.setVisibility(View.INVISIBLE);
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
            if (holder.language != null) {
                String langText = "Idioma: " + activity.getLanguage();
                holder.language.setText(langText);
            }
            if (holder.guide != null) {
                String guideText = "Guía: " + activity.getGuideName();
                holder.guide.setText(guideText);
            }
            if (holder.meetingPoint != null) {
                String meetingText = "Encuentro: " + activity.getMeetingPoint();
                holder.meetingPoint.setText(meetingText);
            }
            if (holder.includes != null) {
                holder.includes.setText(formatList(activity.getWhatIncluded()));
            }
            if (holder.cancellation != null) holder.cancellation.setText(activity.getCancellationPolicy());
        }

        Glide.with(holder.itemView.getContext())
                .load(activity.getImageUrl() != null && !activity.getImageUrl().isEmpty() ? activity.getImageUrl() : null)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .centerCrop()
                .into(holder.image);

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

        bindChips(holder, activity, soldOut);

        if (holder.bookButton != null) {
            if (isFavoritesSection) {
                holder.bookButton.setVisibility(View.VISIBLE);
                holder.bookButton.setEnabled(!soldOut);
                holder.bookButton.setAlpha(soldOut ? 0.5f : 1f);
            } else {
                holder.bookButton.setVisibility(View.GONE);
            }
        }

        View.OnClickListener detailListener = v -> {
            Bundle bundle = new Bundle();
            bundle.putSerializable("activity_data", activity);
            Navigation.findNavController(v).navigate(R.id.detailFragment, bundle);
        };

        holder.itemView.setOnClickListener(detailListener);
        holder.image.setOnClickListener(detailListener);
        holder.name.setOnClickListener(detailListener);
        if (holder.bookButton != null && isFavoritesSection) {
            holder.bookButton.setOnClickListener(detailListener);
        }
    }

    private void bindChips(TourViewHolder holder, TourActivity activity, boolean soldOut) {
        if (holder.chipsContainer == null) return;

        if (!isFavoritesSection) {
            holder.chipsContainer.setVisibility(View.GONE);
            return;
        }

        if (holder.chipSoldOut != null) holder.chipSoldOut.setVisibility(View.GONE);
        if (holder.chipSlotsAvailable != null) holder.chipSlotsAvailable.setVisibility(View.GONE);
        if (holder.chipNewPrice != null) holder.chipNewPrice.setVisibility(View.GONE);

        boolean anyChipVisible = false;

        if (soldOut) {
            if (holder.chipSoldOut != null) holder.chipSoldOut.setVisibility(View.VISIBLE);
            anyChipVisible = true;
        } else {
            if (activity.isSlotsChanged() && holder.chipSlotsAvailable != null) {
                holder.chipSlotsAvailable.setVisibility(View.VISIBLE);
                anyChipVisible = true;
            }
            if (activity.isPriceChanged() && holder.chipNewPrice != null) {
                holder.chipNewPrice.setVisibility(View.VISIBLE);
                anyChipVisible = true;
            }
        }

        holder.chipsContainer.setVisibility(anyChipVisible ? View.VISIBLE : View.GONE);
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
        LinearLayout chipsContainer;
        TextView chipSoldOut, chipSlotsAvailable, chipNewPrice;
        View detailedContainer;
        ImageView favoriteBtn;
        MaterialButton bookButton;

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
            chipsContainer = itemView.findViewById(R.id.chips_container);
            chipSoldOut = itemView.findViewById(R.id.chip_sold_out);
            chipSlotsAvailable = itemView.findViewById(R.id.chip_slots_available);
            chipNewPrice = itemView.findViewById(R.id.chip_new_price);
            bookButton = itemView.findViewById(R.id.btn_book_now);
        }
    }
}