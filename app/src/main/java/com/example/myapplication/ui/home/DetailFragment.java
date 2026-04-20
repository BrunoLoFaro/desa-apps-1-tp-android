package com.example.myapplication.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.data.model.TourActivity;
import com.example.myapplication.data.model.ReviewResponse;
import com.example.myapplication.ui.home.viewmodel.DetailViewModel;
import com.example.myapplication.ui.home.viewmodel.HistoryReviewViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class DetailFragment extends Fragment {

    private View rootView;

    private TourActivity tourActivity;
    private boolean fromHistory;
    private String bookingStatus;
    private Long bookingId;
    private DetailViewModel detailViewModel;
    private HistoryReviewViewModel historyReviewViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            tourActivity = (TourActivity) getArguments().getSerializable("activity_data");
            fromHistory = getArguments().getBoolean("from_history", false);
            bookingStatus = getArguments().getString("booking_status");
            if (getArguments().containsKey("booking_id")) {
                bookingId = getArguments().getLong("booking_id");
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootView = view;

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(view).navigateUp());

        ProgressBar loading = view.findViewById(R.id.detail_loading_spinner);

        detailViewModel = new ViewModelProvider(this).get(DetailViewModel.class);
        historyReviewViewModel = new ViewModelProvider(this).get(HistoryReviewViewModel.class);

        View experienceSection = view.findViewById(R.id.experience_section);

        if (fromHistory) {
            boolean isCompleted = "COMPLETED".equals(bookingStatus);
            if (experienceSection != null) {
                experienceSection.setVisibility(isCompleted ? View.VISIBLE : View.GONE);
            }
            if (isCompleted && bookingId != null) {
                historyReviewViewModel.loadReview(bookingId);
                historyReviewViewModel.getReview().observe(getViewLifecycleOwner(),
                        review -> populateExperienceSection(experienceSection, review));
            }
        }

        if (tourActivity != null) {
            toolbar.setTitle(tourActivity.getName());

            // Buscamos la vista incluida
            View content = view.findViewById(R.id.detail_content);
            if (content != null) {
                populateDetails(content);
            }

            Long id = tourActivity.getId();
            if (id != null && id > 0) {
                detailViewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
                    if (loading != null) {
                        loading.setVisibility(Boolean.TRUE.equals(isLoading) ? View.VISIBLE : View.GONE);
                    }
                });
                detailViewModel.getError().observe(getViewLifecycleOwner(), error -> {
                    if (error != null) {
                        android.widget.Toast.makeText(requireContext(), error.resolve(requireContext()),
                                android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
                detailViewModel.getActivity().observe(getViewLifecycleOwner(), activity -> {
                    if (activity != null) {
                        tourActivity = activity;
                        toolbar.setTitle(activity.getName());
                        if (content != null) {
                            populateDetails(content);
                        }
                    }
                });

                detailViewModel.load(id);
            }
        }
    }

    private void populateDetails(View root) {
        ImageView image = root.findViewById(R.id.activity_image);
        TextView category = root.findViewById(R.id.activity_category);
        TextView name = root.findViewById(R.id.activity_name);
        TextView destination = root.findViewById(R.id.activity_destination);
        TextView duration = root.findViewById(R.id.activity_duration);
        TextView price = root.findViewById(R.id.activity_price);
        TextView slots = root.findViewById(R.id.activity_slots);
        
        View detailedContainer = root.findViewById(R.id.detailed_info_container);
        TextView description = root.findViewById(R.id.activity_description);
        TextView rating = root.findViewById(R.id.activity_rating);
        TextView language = root.findViewById(R.id.activity_language);
        TextView guide = root.findViewById(R.id.activity_guide);
        TextView meetingPoint = root.findViewById(R.id.activity_meeting_point);
        TextView includes = root.findViewById(R.id.activity_includes);
        TextView cancellation = root.findViewById(R.id.activity_cancellation);
        
        Button viewSessionsBtn = root.findViewById(R.id.btn_book_now_item);

        // Forzar visibilidad del contenedor de detalles
        if (detailedContainer != null) {
            detailedContainer.setVisibility(View.VISIBLE);
        }

        name.setText(tourActivity.getName());
        destination.setText(tourActivity.getDestination());
        category.setText(tourActivity.getCategory().toUpperCase());
        duration.setText(tourActivity.getDuration());
        price.setText(tourActivity.getPrice());
        slots.setText(getString(R.string.slots_available, tourActivity.getAvailableSlots()));
        slots.setVisibility(fromHistory ? View.GONE : View.VISIBLE);
        
        if (description != null) description.setText(tourActivity.getDescription());
        if (rating != null) {
            if (tourActivity.getReviewsCount() <= 0) {
                rating.setText(getString(R.string.no_reviews));
            } else {
                rating.setText(String.valueOf(tourActivity.getRating()));
            }
        }
        if (language != null) language.setText("Idioma: " + tourActivity.getLanguage());
        if (guide != null) guide.setText("Guía: " + tourActivity.getGuideName());
        if (meetingPoint != null) meetingPoint.setText("Encuentro: " + tourActivity.getMeetingPoint());
        if (includes != null) includes.setText(tourActivity.getWhatIncluded());
        if (cancellation != null) cancellation.setText(tourActivity.getCancellationPolicy());

        if (image != null) {
            // Ajustar altura de imagen para detalle (opcional, como tenías en tu Activity)
            ViewGroup.LayoutParams lp = image.getLayoutParams();
            lp.height = (int) (240 * getResources().getDisplayMetrics().density);
            image.setLayoutParams(lp);

            Glide.with(this)
                    .load(tourActivity.getImageUrl())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .centerCrop()
                    .into(image);
        }

        if (viewSessionsBtn != null) {
            viewSessionsBtn.setVisibility(fromHistory ? View.GONE : View.VISIBLE);
            viewSessionsBtn.setOnClickListener(v -> {
                Bundle bundle = new Bundle();
                bundle.putSerializable("activity_data", tourActivity);
                Navigation.findNavController(v).navigate(R.id.action_detailFragment_to_sessionsFragment, bundle);
            });
        }
    }

    private void populateExperienceSection(View section, ReviewResponse review) {
        if (section == null) return;
        TextView ratingView = section.findViewById(R.id.experience_rating);
        TextView commentView = section.findViewById(R.id.experience_comment);
        if (review == null || review.activityRating == null) {
            if (ratingView != null) ratingView.setText(R.string.experience_no_rating);
            if (commentView != null) commentView.setVisibility(View.GONE);
        } else {
            if (ratingView != null) {
                ratingView.setText(getString(R.string.experience_rating_format, review.activityRating));
            }
            if (commentView != null) {
                if (review.comment != null && !review.comment.isEmpty()) {
                    commentView.setText(review.comment);
                    commentView.setVisibility(View.VISIBLE);
                } else {
                    commentView.setVisibility(View.GONE);
                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        rootView = null;
        detailViewModel = null;
        historyReviewViewModel = null;
        super.onDestroyView();
    }
}
