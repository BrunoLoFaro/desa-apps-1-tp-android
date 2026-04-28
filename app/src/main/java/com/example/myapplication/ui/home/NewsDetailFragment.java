package com.example.myapplication.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.data.model.NewsDetailResponse;
import com.example.myapplication.ui.home.viewmodel.NewsViewModel;
import com.google.android.material.button.MaterialButton;
import dagger.hilt.android.AndroidEntryPoint;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@AndroidEntryPoint
public class NewsDetailFragment extends Fragment {

    private NewsViewModel newsViewModel;
    private ProgressBar progressBar;
    private ImageView newsImage;
    private TextView typeText, titleText, publishedAtText, validUntilText, contentText;
    private LinearLayout relatedActivitySection;
    private TextView relatedActivityName;
    private MaterialButton ctaButton;
    private Long newsId;
    private Long relatedActivityId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_news_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        newsViewModel = new ViewModelProvider(this).get(NewsViewModel.class);

        progressBar = view.findViewById(R.id.progress_bar);
        newsImage = view.findViewById(R.id.news_detail_image);
        typeText = view.findViewById(R.id.news_detail_type);
        titleText = view.findViewById(R.id.news_detail_title);
        publishedAtText = view.findViewById(R.id.news_detail_published_at);
        validUntilText = view.findViewById(R.id.news_detail_valid_until);
        contentText = view.findViewById(R.id.news_detail_content);
        relatedActivitySection = view.findViewById(R.id.related_activity_section);
        relatedActivityName = view.findViewById(R.id.related_activity_name);
        ctaButton = view.findViewById(R.id.cta_button);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            Navigation.findNavController(v).navigateUp();
        });

        // Get news ID from arguments
        if (getArguments() != null) {
            newsId = getArguments().getLong("news_id");
            relatedActivityId = getArguments().getLong("related_activity_id", -1);
        }

        if (newsId != null) {
            loadNewsDetail();
        } else {
            Toast.makeText(requireContext(), "Error: ID de noticia no proporcionado", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).navigateUp();
        }

        newsViewModel.getNewsDetail().observe(getViewLifecycleOwner(), this::bindNewsDetail);

        newsViewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error.resolve(requireContext()), Toast.LENGTH_SHORT).show();
            }
        });

        newsViewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        });
    }

    private void loadNewsDetail() {
        if (newsId != null) {
            newsViewModel.loadNewsDetail(newsId);
        }
    }

    private void bindNewsDetail(NewsDetailResponse news) {
        if (news == null) return;

        // Set type badge
        String typeLabelText = getNewsTypeText(news.type);
        typeText.setText(typeLabelText);

        // Set title
        titleText.setText(news.title);

        // Set published date
        if (news.publishedAt != null && !news.publishedAt.isEmpty()) {
            publishedAtText.setText("Publicado: " + formatDate(news.publishedAt));
        }

        // Set valid until
        if (news.validUntil != null && !news.validUntil.isEmpty()) {
            validUntilText.setText("Válido hasta: " + formatDate(news.validUntil));
            validUntilText.setVisibility(View.VISIBLE);
        }

        // Set content
        String content = news.fullContent != null && !news.fullContent.isEmpty() 
                ? news.fullContent 
                : news.description;
        contentText.setText(content);

        // Load image
        String imageUrl = news.imageUrl;
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(requireContext())
                    .load(imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .centerCrop()
                    .into(newsImage);
        } else {
            newsImage.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // Related activity
        if (news.relatedActivityId != null) {
            relatedActivitySection.setVisibility(View.VISIBLE);
            relatedActivityName.setText(news.relatedActivityName != null && !news.relatedActivityName.isEmpty()
                    ? news.relatedActivityName
                    : "Actividad relacionada");
            this.relatedActivityId = news.relatedActivityId;
        } else {
            relatedActivitySection.setVisibility(View.GONE);
        }

        // CTA button
        if (news.relatedActivityId != null) {
            ctaButton.setVisibility(View.VISIBLE);
            ctaButton.setText(news.ctaText != null && !news.ctaText.isEmpty()
                    ? news.ctaText
                    : "Explorar actividades");
            ctaButton.setOnClickListener(v -> {
                Bundle bundle = new Bundle();
                bundle.putLong("activity_id", news.relatedActivityId);
                Navigation.findNavController(v).navigate(R.id.detailFragment, bundle);
            });
        } else {
            ctaButton.setVisibility(View.GONE);
            ctaButton.setOnClickListener(null);
        }
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
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
            return outputFormat.format(date);
        } catch (Exception e) {
            return dateString;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        newsViewModel = null;
    }
}
