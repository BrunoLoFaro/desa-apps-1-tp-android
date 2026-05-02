package com.example.myapplication.ui.bookings;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import com.example.myapplication.data.model.ReviewResponse;
import com.example.myapplication.util.FormatUtils;
import java.util.Collections;
import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {

    private List<ReviewResponse> items = Collections.emptyList();

    public void updateData(List<ReviewResponse> newItems) {
        this.items = newItems != null ? newItems : Collections.emptyList();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ReviewResponse review = items.get(position);
        holder.activityName.setText(review.activityName);
        holder.destination.setText(review.destinationName);
        
        // Redondear calificación de actividad a un decimal
        float activityRatingValue = review.activityRating != null ? review.activityRating : 0;
        float roundedActivityRating = Math.round(activityRatingValue * 10f) / 10f;
        holder.activityRating.setRating(roundedActivityRating);
        
        if (review.guideRating != null && review.guideRating > 0) {
            // Redondear calificación de guía a un decimal
            float roundedGuideRating = Math.round(review.guideRating * 10f) / 10f;
            holder.guideRating.setRating(roundedGuideRating);
            holder.guideRow.setVisibility(View.VISIBLE);
        } else {
            holder.guideRow.setVisibility(View.GONE);
        }

        if (review.comment != null && !review.comment.trim().isEmpty()) {
            holder.comment.setText("\"" + review.comment + "\"");
            holder.comment.setVisibility(View.VISIBLE);
        } else {
            holder.comment.setVisibility(View.GONE);
        }

        holder.date.setText(FormatUtils.formatStartTimeWithUTC3(review.createdAt));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView activityName, destination, comment, date;
        RatingBar activityRating, guideRating;
        View guideRow;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            activityName = itemView.findViewById(R.id.review_activity_name);
            destination = itemView.findViewById(R.id.review_destination);
            comment = itemView.findViewById(R.id.review_comment);
            date = itemView.findViewById(R.id.review_date);
            activityRating = itemView.findViewById(R.id.review_activity_rating);
            guideRating = itemView.findViewById(R.id.review_guide_rating);
            guideRow = itemView.findViewById(R.id.review_guide_row);
        }
    }
}
