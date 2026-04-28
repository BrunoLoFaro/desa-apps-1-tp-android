package com.example.myapplication.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
import com.google.android.material.textfield.TextInputEditText;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.List;

@AndroidEntryPoint
public class DetailFragment extends Fragment {

    private View rootView;

    private TourActivity tourActivity;
    private boolean fromHistory;
    private String bookingStatus;
    private Long bookingId;
    private DetailViewModel detailViewModel;
    private CreateBookingViewModel createBookingViewModel;
    private HistoryReviewViewModel historyReviewViewModel;
    private SessionAdapter sessionAdapter;
    private ActivitySessionResponse selectedSession;

    private Long activityIdFromArgs;

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
            if (getArguments().containsKey("activity_id")) {
                activityIdFromArgs = getArguments().getLong("activity_id");
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

        RecyclerView sessionsRecycler = view.findViewById(R.id.sessions_recycler_view);
        sessionsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        View bookingCard = view.findViewById(R.id.booking_card);
        TextInputEditText participantsInput = view.findViewById(R.id.participants_input);
        MaterialButton bookButton = view.findViewById(R.id.book_button);

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

                int participants = 1;
                if (participantsInput != null && participantsInput.getText() != null) {
                    String value = participantsInput.getText().toString().trim();
                    if (!value.isEmpty()) {
                        try {
                            participants = Integer.parseInt(value);
                        } catch (NumberFormatException ignored) {
                            participants = 1;
                        }
                    }
                }
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
                // ocultar card hasta una nueva seleccion (updateData resetea la seleccion)
                selectedSession = null;
                if (bookingCard != null) bookingCard.setVisibility(View.GONE);
                createBookingViewModel.clearBooking();
            }
        });

        View experienceSection = view.findViewById(R.id.experience_section);

        if (fromHistory) {
            if (sessionsTitle != null) sessionsTitle.setVisibility(View.GONE);
            sessionsRecycler.setVisibility(View.GONE);
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

        // If navigated from a promotion with only activity_id, load from API
        if (tourActivity == null && activityIdFromArgs != null) {
            tourActivity = new TourActivity("", "", "", "", "", 0, null);
            tourActivity.setId(activityIdFromArgs);
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
                detailViewModel.getSessions().observe(getViewLifecycleOwner(), sessions -> {
                    if (fromHistory) return;
                    populateSessions(sessions);
                    selectedSession = null;
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

        // Handle discount pricing in detail view
        TextView originalPrice = root.findViewById(R.id.original_price);
        TextView discountBadge = root.findViewById(R.id.discount_badge);
        if (tourActivity.getDiscountPercentage() != null && tourActivity.getDiscountPercentage() > 0) {
            String priceStr = tourActivity.getPrice();
            if (priceStr != null && priceStr.startsWith("$")) {
                try {
                    double basePrice = Double.parseDouble(priceStr.substring(1));
                    double discountedPrice = basePrice * (1 - tourActivity.getDiscountPercentage() / 100.0);
                    if (originalPrice != null) {
                        originalPrice.setText(String.format("$%.2f", basePrice));
                        originalPrice.setVisibility(View.VISIBLE);
                    }
                    price.setText(String.format("$%.2f", discountedPrice));
                    if (discountBadge != null) {
                        discountBadge.setText(tourActivity.getDiscountPercentage() + "% OFF");
                        discountBadge.setVisibility(View.VISIBLE);
                    }
                } catch (NumberFormatException e) {
                    if (originalPrice != null) originalPrice.setVisibility(View.GONE);
                    if (discountBadge != null) discountBadge.setVisibility(View.GONE);
                    price.setText(tourActivity.getPrice());
                }
            } else {
                if (originalPrice != null) originalPrice.setVisibility(View.GONE);
                if (discountBadge != null) discountBadge.setVisibility(View.GONE);
                price.setText(tourActivity.getPrice());
            }
        } else {
            if (originalPrice != null) originalPrice.setVisibility(View.GONE);
            if (discountBadge != null) discountBadge.setVisibility(View.GONE);
            price.setText(tourActivity.getPrice());
        }
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

    @Override
    public void onDestroyView() {
        rootView = null;
        sessionAdapter = null;
        detailViewModel = null;
        historyReviewViewModel = null;
        super.onDestroyView();
    }
}
