package com.example.myapplication.ui.favorites;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.data.model.TourActivity;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.ViewHolder> {

    public interface OnFavoriteRemoveListener {
        void onRemoveRequested(TourActivity activity);
    }

    private List<TourActivity> items = new ArrayList<>();
    private final OnFavoriteRemoveListener removeListener;

    public FavoritesAdapter(OnFavoriteRemoveListener removeListener) {
        this.removeListener = removeListener;
    }

    public void updateData(List<TourActivity> newData) {
        List<TourActivity> oldList = this.items;
        List<TourActivity> newList = newData != null ? new ArrayList<>(newData) : new ArrayList<>();
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new FavoritesDiffCallback(oldList, newList));
        this.items = newList;
        result.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_favorite_activity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TourActivity activity = items.get(position);
        boolean soldOut = activity.getAvailableSlots() <= 0;

        holder.name.setText(activity.getName());
        holder.destination.setText(activity.getDestination());

        String date = activity.getStartDate();
        String duration = activity.getDuration();
        boolean hasDate = date != null && !date.isEmpty();
        boolean hasDuration = duration != null && !duration.isEmpty();
        if (hasDate || hasDuration) {
            holder.dateTimeRow.setVisibility(View.VISIBLE);
            holder.dateText.setText(hasDate ? date : "");
            holder.timeText.setText(hasDuration ? duration : "");
        } else {
            holder.dateTimeRow.setVisibility(View.GONE);
        }

        holder.heartIcon.setImageResource(R.drawable.ic_favorite);
        holder.heartIcon.setOnClickListener(v -> {
            if (removeListener != null) {
                removeListener.onRemoveRequested(activity);
            }
        });

        Glide.with(holder.itemView.getContext())
                .load(activity.getImageUrl() != null && !activity.getImageUrl().isEmpty()
                        ? activity.getImageUrl() : null)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .centerCrop()
                .into(holder.image);

        bindChips(holder, activity);

        holder.bookButton.setEnabled(!soldOut);
        holder.bookButton.setAlpha(soldOut ? 0.5f : 1f);

        View.OnClickListener goToDetail = v -> {
            Bundle bundle = new Bundle();
            bundle.putSerializable("activity_data", activity);
            Navigation.findNavController(v).navigate(R.id.detailFragment, bundle);
        };

        holder.bookButton.setOnClickListener(soldOut ? null : v -> {
            Bundle bundle = new Bundle();
            bundle.putSerializable("activity_data", activity);
            bundle.putBoolean("scroll_to_booking", true);
            Navigation.findNavController(v).navigate(R.id.detailFragment, bundle);
        });
        holder.detailButton.setOnClickListener(goToDetail);
        holder.itemView.setOnClickListener(goToDetail);
    }

    private void bindChips(ViewHolder holder, TourActivity activity) {
        boolean showSoldOut = activity.getAvailableSlots() <= 0;
        boolean showSlotsAvailable = !showSoldOut && activity.isSlotsChanged();
        boolean showNewPrice = !showSoldOut && activity.isPriceChanged();

        holder.chipSoldOut.setVisibility(showSoldOut ? View.VISIBLE : View.GONE);
        holder.chipSlotsAvailable.setVisibility(showSlotsAvailable ? View.VISIBLE : View.GONE);
        holder.chipNewPrice.setVisibility(showNewPrice ? View.VISIBLE : View.GONE);

        boolean anyVisible = showSoldOut || showSlotsAvailable || showNewPrice;
        holder.chipsContainer.setVisibility(anyVisible ? View.VISIBLE : View.GONE);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image, heartIcon;
        TextView name, destination, dateText, timeText;
        LinearLayout chipsContainer, dateTimeRow;
        TextView chipSoldOut, chipSlotsAvailable, chipNewPrice;
        MaterialButton bookButton, detailButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.fav_image);
            heartIcon = itemView.findViewById(R.id.fav_heart);
            name = itemView.findViewById(R.id.fav_name);
            destination = itemView.findViewById(R.id.fav_destination);
            dateTimeRow = itemView.findViewById(R.id.fav_date_time_row);
            dateText = itemView.findViewById(R.id.fav_date);
            timeText = itemView.findViewById(R.id.fav_time);
            chipsContainer = itemView.findViewById(R.id.chips_container);
            chipSoldOut = itemView.findViewById(R.id.chip_sold_out);
            chipSlotsAvailable = itemView.findViewById(R.id.chip_slots_available);
            chipNewPrice = itemView.findViewById(R.id.chip_new_price);
            bookButton = itemView.findViewById(R.id.btn_book_now);
            detailButton = itemView.findViewById(R.id.btn_view_detail);
        }
    }

    private static class FavoritesDiffCallback extends DiffUtil.Callback {
        private final List<TourActivity> oldList;
        private final List<TourActivity> newList;

        FavoritesDiffCallback(List<TourActivity> oldList, List<TourActivity> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() { return oldList.size(); }

        @Override
        public int getNewListSize() { return newList.size(); }

        @Override
        public boolean areItemsTheSame(int oldPos, int newPos) {
            Long oldId = oldList.get(oldPos).getId();
            Long newId = newList.get(newPos).getId();
            return oldId != null && oldId.equals(newId);
        }

        @Override
        public boolean areContentsTheSame(int oldPos, int newPos) {
            if (!areItemsTheSame(oldPos, newPos)) return false;
            TourActivity o = oldList.get(oldPos);
            TourActivity n = newList.get(newPos);
            return o.getAvailableSlots() == n.getAvailableSlots()
                    && o.isPriceChanged() == n.isPriceChanged()
                    && o.isSlotsChanged() == n.isSlotsChanged();
        }
    }
}
