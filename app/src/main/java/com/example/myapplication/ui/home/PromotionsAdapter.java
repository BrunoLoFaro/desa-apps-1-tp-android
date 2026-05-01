package com.example.myapplication.ui.home;

import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StrikethroughSpan;
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
import com.example.myapplication.data.model.NewsItem;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PromotionsAdapter extends RecyclerView.Adapter<PromotionsAdapter.PromotionViewHolder> {

    private List<NewsItem> promotionItems;

    public PromotionsAdapter() {
        this.promotionItems = java.util.Collections.emptyList();
    }

    public void updateData(List<NewsItem> newData) {
        this.promotionItems = newData;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PromotionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_news, parent, false);
        return new PromotionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PromotionViewHolder holder, int position) {
        NewsItem promotion = promotionItems.get(position);
        
        holder.title.setText(promotion.title);
        
        // Process description to add strikethrough to prices (e.g., "$100.00" appears as strikethrough)
        String description = promotion.description;
        holder.description.setText(applyStrikethroughToPrice(description));
        
        // Set type badge
        String typeText = getPromotionTypeText(promotion.type);
        holder.type.setText(typeText);
        
        // Promotions should not show published date in card.
        holder.publishedAt.setVisibility(View.GONE);
        
        // Load image
        String imageUrl = promotion.imageUrl;
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .centerCrop()
                    .into(holder.image);
        } else {
            holder.image.setImageResource(android.R.drawable.ic_menu_gallery);
        }
        
        // Click listener - Navigate directly to DetailFragment with activity_id
        // relatedActivityId is the activity ID for promotions
        holder.itemView.setOnClickListener(v -> {
            if (promotion.relatedActivityId != null) {
                Bundle bundle = new Bundle();
                bundle.putLong("activity_id", promotion.relatedActivityId);
                Navigation.findNavController(v).navigate(R.id.detailFragment, bundle);
            }
        });
    }

    @Override
    public int getItemCount() {
        return promotionItems.size();
    }

    private String getPromotionTypeText(String type) {
        if (type == null) return "OFERTA";
        switch (type.toUpperCase()) {
            case "OFFER":
                return "OFERTA";
            default:
                return "OFERTA";
        }
    }

    private String formatDate(String dateString) {
        try {
            Date date = null;
            String[] patterns = new String[] {
                    "yyyy-MM-dd'T'HH:mm:ss",
                    "yyyy-MM-dd'T'HH:mm:ss'Z'",
                    "yyyy-MM-dd'T'HH:mm:ssX",
                    "yyyy-MM-dd"
            };
            for (String pattern : patterns) {
                try {
                    SimpleDateFormat inputFormat = new SimpleDateFormat(pattern, Locale.US);
                    date = inputFormat.parse(dateString);
                    if (date != null) break;
                } catch (Exception ignored) {
                }
            }
            if (date == null) return dateString;

            SimpleDateFormat outputFormat = new SimpleDateFormat("dd 'de' MMMM", new Locale("es", "AR"));
            String formatted = outputFormat.format(date);
            int monthIndex = formatted.lastIndexOf(" de ");
            if (monthIndex >= 0 && monthIndex + 4 < formatted.length()) {
                char first = Character.toUpperCase(formatted.charAt(monthIndex + 4));
                return formatted.substring(0, monthIndex + 4) + first + formatted.substring(monthIndex + 5);
            }
            return formatted;
        } catch (Exception e) {
            return dateString;
        }
    }

    /**
     * Applies strikethrough to price patterns in the text (e.g., "$100.00")
     * Useful for promotional descriptions that show old vs new prices
     * Example: "20% OFF - $200.00" -> the "$200.00" will show with strikethrough
     */
    private SpannableString applyStrikethroughToPrice(String text) {
        SpannableString spannableString = new SpannableString(text);
        
        // Pattern to match prices like $123.45 or $100
        // We look for prices that might appear in "X% OFF - $price" format
        Pattern pricePattern = Pattern.compile("\\$\\d+(\\.\\d{2})?");
        Matcher matcher = pricePattern.matcher(text);
        
        // Collect all matches
        int matchCount = 0;
        while (matcher.find()) {
            matchCount++;
        }
        
        // Reset matcher and apply strikethrough only to prices (usually the original price before discount)
        // In promo text like "20% OFF - $200.00", we want the price to show strikethrough
        matcher.reset();
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            spannableString.setSpan(new StrikethroughSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        
        return spannableString;
    }

    public static class PromotionViewHolder extends RecyclerView.ViewHolder {
        public TextView title;
        public TextView description;
        public TextView type;
        public TextView publishedAt;
        public ImageView image;

        public PromotionViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.news_title);
            description = itemView.findViewById(R.id.news_description);
            type = itemView.findViewById(R.id.news_type);
            publishedAt = itemView.findViewById(R.id.news_published_at);
            image = itemView.findViewById(R.id.news_image);
        }
    }
}
