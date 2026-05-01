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
import com.example.myapplication.data.model.NewsItem;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {

    private List<NewsItem> newsItems;

    public NewsAdapter() {
        this.newsItems = java.util.Collections.emptyList();
    }

    public void updateData(List<NewsItem> newData) {
        this.newsItems = newData;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_news, parent, false);
        return new NewsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        NewsItem news = newsItems.get(position);
        
        holder.title.setText(news.title);
        holder.description.setText(news.description);
        
        // Set type badge
        String typeText = getNewsTypeText(news.type);
        holder.type.setText(typeText);
        
        // Set published date
        if (news.publishedAt != null && !news.publishedAt.isEmpty()) {
            holder.publishedAt.setText(formatDate(news.publishedAt));
        } else {
            holder.publishedAt.setVisibility(View.GONE);
        }
        
        // Load image
        String imageUrl = news.imageUrl;
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .centerCrop()
                    .into(holder.image);
        } else {
            holder.image.setImageResource(android.R.drawable.ic_menu_gallery);
        }
        
        // Click listener
        holder.itemView.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putLong("news_id", news.id);
            if (news.relatedActivityId != null) {
                bundle.putLong("related_activity_id", news.relatedActivityId);
            }
            Navigation.findNavController(v).navigate(R.id.newsDetailFragment, bundle);
        });
    }

    @Override
    public int getItemCount() {
        return newsItems.size();
    }

    private String getNewsTypeText(String type) {
        if (type == null) return "NOTICIA";
        switch (type.toUpperCase()) {
            case "NEWS":
                return "NOTICIA";
            case "OFFER":
                return "OFERTA";
            case "FEATURED_DESTINATION":
                return "DESTACADO";
            default:
                return "NOTICIA";
        }
    }

    private String formatDate(String dateString) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date date = inputFormat.parse(dateString);
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd 'de' MMMM 'de' yyyy", new Locale("es", "AR"));
            return outputFormat.format(date);
        } catch (Exception e) {
            return dateString;
        }
    }

    static class NewsViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView type, title, description, publishedAt;

        public NewsViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.news_image);
            type = itemView.findViewById(R.id.news_type);
            title = itemView.findViewById(R.id.news_title);
            description = itemView.findViewById(R.id.news_description);
            publishedAt = itemView.findViewById(R.id.news_published_at);
        }
    }
}
