package com.example.myapplication.ui.home;

import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.data.model.ActivitySessionResponse;
import com.example.myapplication.data.model.TourActivity;
import com.example.myapplication.data.model.ReviewResponse;
import com.example.myapplication.ui.home.viewmodel.CreateBookingViewModel;
import com.example.myapplication.ui.home.viewmodel.DetailViewModel;
import com.example.myapplication.ui.home.viewmodel.HistoryReviewViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.List;

@AndroidEntryPoint
public class DetailFragment extends Fragment {

    private View rootView;

    private TourActivity tourActivity;
    private boolean fromHistory;
    private String bookingStatus;
    private Long bookingId;
    private String bookingDate;
    private DetailViewModel detailViewModel;
    private CreateBookingViewModel createBookingViewModel;
    private HistoryReviewViewModel historyReviewViewModel;
    private SessionAdapter sessionAdapter;
    private ActivitySessionResponse selectedSession;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            tourActivity = (TourActivity) getArguments().getSerializable("activity_data");
            fromHistory = getArguments().getBoolean("from_history", false);
            bookingStatus = getArguments().getString("booking_status");
            bookingDate = getArguments().getString("booking_date");
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
        toolbar.setTitle("");
        toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(view).navigateUp());

        RecyclerView sessionsRecycler = view.findViewById(R.id.sessions_recycler_view);
        sessionsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        View bookingCard = view.findViewById(R.id.booking_card);
        MaterialButton bookButton = view.findViewById(R.id.book_button);
        TextView participantsCountView = view.findViewById(R.id.participants_count);
        MaterialButton btnDecrease = view.findViewById(R.id.btn_decrease);
        MaterialButton btnIncrease = view.findViewById(R.id.btn_increase);
        final int[] count = {1};

        if (btnDecrease != null) {
            btnDecrease.setOnClickListener(v -> {
                if (count[0] > 1) {
                    count[0]--;
                    if (participantsCountView != null) participantsCountView.setText(String.valueOf(count[0]));
                }
            });
        }

        if (btnIncrease != null) {
            btnIncrease.setOnClickListener(v -> {
                int max = selectedSession != null && selectedSession.availableSpots > 0
                        ? selectedSession.availableSpots : Integer.MAX_VALUE;
                if (count[0] < max) {
                    count[0]++;
                    if (participantsCountView != null) participantsCountView.setText(String.valueOf(count[0]));
                }
            });
        }

        sessionAdapter = new SessionAdapter(session -> {
            selectedSession = session;
            if (bookingCard != null) bookingCard.setVisibility(View.VISIBLE);
            if (bookButton != null) {
                bookButton.setEnabled(session.availableSpots > 0);
            }
        });
        sessionsRecycler.setAdapter(sessionAdapter);

        View sessionsTitle = view.findViewById(R.id.sessions_title);
        ProgressBar loading = view.findViewById(R.id.detail_loading_spinner);

        detailViewModel = new ViewModelProvider(this).get(DetailViewModel.class);
        createBookingViewModel = new ViewModelProvider(this).get(CreateBookingViewModel.class);
        historyReviewViewModel = new ViewModelProvider(this).get(HistoryReviewViewModel.class);

        if (bookButton != null) {
            bookButton.setOnClickListener(v -> {
                if (selectedSession == null || selectedSession.id == null) {
                    android.widget.Toast.makeText(requireContext(), "Selecciona un horario", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }

                int participants = count[0];
                if (participants < 1) {
                    android.widget.Toast.makeText(requireContext(), "Participantes invalidos", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                if (selectedSession.availableSpots > 0 && participants > selectedSession.availableSpots) {
                    android.widget.Toast.makeText(requireContext(), "No hay cupos suficientes", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }

                createBookingViewModel.create(selectedSession.id, participants);
            });
        }

        createBookingViewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (bookButton != null) {
                bookButton.setEnabled(!Boolean.TRUE.equals(isLoading));
            }
        });
        createBookingViewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                android.widget.Toast.makeText(requireContext(), error.resolve(requireContext()),
                        android.widget.Toast.LENGTH_SHORT).show();
            }
        });
        createBookingViewModel.getBooking().observe(getViewLifecycleOwner(), booking -> {
            if (booking != null) {
                android.widget.Toast.makeText(requireContext(), "Reserva creada", android.widget.Toast.LENGTH_SHORT).show();
                // refresca cupos/sesiones
                if (tourActivity != null && tourActivity.getId() != null) {
                    detailViewModel.load(tourActivity.getId());
                }
                selectedSession = null;
                count[0] = 1;
                if (participantsCountView != null) participantsCountView.setText("1");
                if (bookingCard != null) bookingCard.setVisibility(View.GONE);
                createBookingViewModel.clearBooking();
            }
        });

        View experienceSection = view.findViewById(R.id.experience_section);
        View historyContent = view.findViewById(R.id.history_content);

        if (fromHistory) {
            if (sessionsTitle != null) sessionsTitle.setVisibility(View.GONE);
            sessionsRecycler.setVisibility(View.GONE);
            if (experienceSection != null) experienceSection.setVisibility(View.GONE);
            if (bookingCard != null) bookingCard.setVisibility(View.GONE);

            View detailContent = view.findViewById(R.id.detail_content);
            if (detailContent != null) detailContent.setVisibility(View.GONE);
            if (historyContent != null) historyContent.setVisibility(View.VISIBLE);

            if (bookingId != null) {
                historyReviewViewModel.loadReview(bookingId);
                historyReviewViewModel.getReview().observe(getViewLifecycleOwner(),
                        review -> populateHistoryReview(historyContent, review));
            }
        }

        if (tourActivity != null) {
            toolbar.setTitle("");

            View content = view.findViewById(R.id.detail_content);
            if (fromHistory) {
                populateHistoryDetails(historyContent);
            } else if (content != null) {
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
                        toolbar.setTitle("");
                        if (fromHistory) {
                            populateHistoryDetails(historyContent);
                        } else if (content != null) {
                            populateDetails(content);
                        }
                    }
                });
                detailViewModel.getSessions().observe(getViewLifecycleOwner(), sessions -> {
                    if (fromHistory) return;
                    populateSessions(sessions);
                    selectedSession = null;
                    count[0] = 1;
                    if (participantsCountView != null) participantsCountView.setText("1");
                    if (bookingCard != null) bookingCard.setVisibility(View.GONE);
                    boolean hasSessions = sessions != null && !sessions.isEmpty();
                    boolean hasAvailableSpots = false;
                    if (sessions != null) {
                        for (ActivitySessionResponse session : sessions) {
                            if (session.availableSpots > 0) {
                                hasAvailableSpots = true;
                                break;
                            }
                        }
                    }
                    if (bookButton != null) {
                        bookButton.setEnabled(hasAvailableSpots);
                    }
                    if (sessionsTitle != null) sessionsTitle.setVisibility(hasSessions ? View.VISIBLE : View.GONE);
                    sessionsRecycler.setVisibility(hasSessions ? View.VISIBLE : View.GONE);
                });

                detailViewModel.load(id);
            } else {
                if (sessionsTitle != null) sessionsTitle.setVisibility(View.GONE);
                sessionsRecycler.setVisibility(View.GONE);
            }
        }
    }

    private void populateSessions(List<ActivitySessionResponse> sessions) {
        if (sessionAdapter != null) {
            sessionAdapter.updateData(sessions);
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
        com.google.android.material.floatingactionbutton.FloatingActionButton favoriteButton = root.findViewById(R.id.favorite_button);

        // Forzar visibilidad del contenedor de detalles
        if (detailedContainer != null) {
            detailedContainer.setVisibility(View.VISIBLE);
        }

        name.setText(tourActivity.getName());
        destination.setText(tourActivity.getDestination());
        category.setText(tourActivity.getCategory().toUpperCase());
        duration.setText(tourActivity.getDuration());
        price.setText(tourActivity.getPrice());
        boolean soldOut = tourActivity.getAvailableSlots() <= 0;
        slots.setText(soldOut
                ? getString(R.string.sold_out)
                : getString(R.string.slots_available, tourActivity.getAvailableSlots()));
        root.setAlpha(soldOut ? 0.65f : 1f);
        slots.setVisibility(fromHistory ? View.GONE : View.VISIBLE);

        if (favoriteButton != null) {
            favoriteButton.setImageResource(tourActivity.isFavorite() ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
            favoriteButton.setOnClickListener(v -> {
                if (tourActivity == null || tourActivity.getId() == null) return;
                boolean previous = tourActivity.isFavorite();
                boolean targetFavorite = !previous;
                tourActivity.setFavorite(targetFavorite);
                favoriteButton.setImageResource(targetFavorite ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
                detailViewModel.toggleFavorite(tourActivity.getId(), targetFavorite, (success, error) -> {
                    if (!success) {
                        tourActivity.setFavorite(previous);
                        favoriteButton.setImageResource(previous ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
                    }
                });
            });
        }

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

    private void populateHistoryDetails(View root) {
        if (root == null || tourActivity == null) return;

        TextView histTitle = root.findViewById(R.id.hist_title);
        TextView histDate = root.findViewById(R.id.hist_date);
        TextView histDuration = root.findViewById(R.id.hist_duration);
        TextView histPrice = root.findViewById(R.id.hist_price);
        TextView histDescription = root.findViewById(R.id.hist_description);
        View histGalleryBtn = root.findViewById(R.id.hist_gallery_btn);
        com.google.android.material.chip.Chip histStatusChip = root.findViewById(R.id.hist_status_chip);
        com.google.android.material.chip.Chip histCompletedChip = root.findViewById(R.id.hist_completed_chip);
        ImageView histImage = root.findViewById(R.id.hist_image);

        if (histTitle != null) histTitle.setText(tourActivity.getName());
        if (histDuration != null && tourActivity.getDuration() != null) {
            histDuration.setText("Duración: " + tourActivity.getDuration());
        }
        if (histPrice != null && tourActivity.getPrice() != null) {
            histPrice.setText(tourActivity.getPrice() + " por persona");
        }
        if (histDescription != null) histDescription.setText(tourActivity.getDescription());

        if (histDate != null) {
            if (bookingDate != null && !bookingDate.isEmpty()) {
                histDate.setText(bookingDate);
                histDate.setVisibility(View.VISIBLE);
            } else {
                histDate.setVisibility(View.GONE);
            }
        }

        if (histStatusChip != null) {
            if ("CANCELLED".equals(bookingStatus)) {
                histStatusChip.setText("✗ ACTIVIDAD CANCELADA");
            } else {
                histStatusChip.setText("✓ ACTIVIDAD FINALIZADA");
            }
        }
        if (histCompletedChip != null) {
            histCompletedChip.setVisibility("COMPLETED".equals(bookingStatus) ? View.VISIBLE : View.GONE);
        }

        if (histGalleryBtn != null) {
            boolean hasGallery = tourActivity.getGalleryUrls() != null && !tourActivity.getGalleryUrls().isEmpty();
            histGalleryBtn.setVisibility(hasGallery ? View.VISIBLE : View.GONE);
        }

        if (histImage != null) {
            Glide.with(this)
                    .load(tourActivity.getImageUrl())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .centerCrop()
                    .into(histImage);
        }
    }

    private void populateHistoryReview(View root, ReviewResponse review) {
        if (root == null) return;
        View histReviewCard = root.findViewById(R.id.hist_review_card);
        View histNoReview = root.findViewById(R.id.hist_no_review);
        TextView histComment = root.findViewById(R.id.hist_review_comment);
        RatingBar histRating = root.findViewById(R.id.hist_review_rating);

        if (review == null || review.activityRating == null) {
            if (histReviewCard != null) histReviewCard.setVisibility(View.GONE);
            if (histNoReview != null) histNoReview.setVisibility(View.VISIBLE);
            return;
        }

        if (histReviewCard != null) histReviewCard.setVisibility(View.VISIBLE);
        if (histNoReview != null) histNoReview.setVisibility(View.GONE);

        if (histRating != null) histRating.setRating(review.activityRating);

        if (histComment != null) {
            String comment = review.comment != null ? review.comment.trim() : "";
            if (review.activityRating >= 4) {
                String suffix = "¡Recomendado!";
                String full = comment.isEmpty() ? suffix : comment + " " + suffix;
                SpannableString span = new SpannableString(full);
                int start = full.lastIndexOf(suffix);
                span.setSpan(
                        new ForegroundColorSpan(requireContext().getColor(R.color.md_theme_primary)),
                        start, full.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                histComment.setText(span);
            } else {
                histComment.setText(comment.isEmpty() ? getString(R.string.experience_no_rating) : comment);
            }
        }
    }

    @Override
    public void onDestroyView() {
        rootView = null;
        sessionAdapter = null;
        detailViewModel = null;
        historyReviewViewModel = null;
        super.onDestroyView();
    }
}
