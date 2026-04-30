package com.example.myapplication.ui.home;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
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
import com.example.myapplication.data.model.ItineraryPoint;
import com.example.myapplication.data.model.TourActivity;
import com.example.myapplication.data.model.ReviewResponse;
import com.example.myapplication.ui.home.viewmodel.CreateBookingViewModel;
import com.example.myapplication.ui.home.viewmodel.DetailViewModel;
import com.example.myapplication.ui.home.viewmodel.HistoryReviewViewModel;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import dagger.hilt.android.AndroidEntryPoint;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@AndroidEntryPoint
public class DetailFragment extends Fragment {

    private View rootView;

    private TourActivity tourActivity;
    private boolean fromHistory;
    private boolean fromBooking;
    private String bookingStatus;
    private Long bookingId;
    private DetailViewModel detailViewModel;
    private CreateBookingViewModel createBookingViewModel;
    private HistoryReviewViewModel historyReviewViewModel;
    private SessionAdapter sessionAdapter;
    private ActivitySessionResponse selectedSession;

    private View meetingMapSection;
    private FrameLayout meetingMapContainer;
    private TextView meetingMapError;
    private MaterialButton directionsButton;
    private SupportMapFragment mapFragment;
    private GoogleMap googleMap;
    private int mapRequestId = 0;
    private Long activityIdFromArgs;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            tourActivity = (TourActivity) getArguments().getSerializable("activity_data");
            fromHistory = getArguments().getBoolean("from_history", false);
            fromBooking = getArguments().getBoolean("from_booking", false);
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
            if (fromHistory || fromBooking) return;
            selectedSession = session;
            if (bookingCard != null) bookingCard.setVisibility(View.VISIBLE);
            if (bookButton != null) {
                bookButton.setEnabled(session.availableSpots > 0);
            }
        });
        sessionsRecycler.setAdapter(sessionAdapter);

        View sessionsTitle = view.findViewById(R.id.sessions_title);
        ProgressBar loading = view.findViewById(R.id.detail_loading_spinner);

        meetingMapSection = view.findViewById(R.id.meeting_map_section);
        meetingMapContainer = view.findViewById(R.id.meeting_map_container);
        meetingMapError = view.findViewById(R.id.meeting_map_error);
        directionsButton = view.findViewById(R.id.meeting_directions_button);

        if (fromBooking) {
            if (meetingMapSection != null) meetingMapSection.setVisibility(View.VISIBLE);
            if (sessionsTitle != null) sessionsTitle.setVisibility(View.GONE);
            sessionsRecycler.setVisibility(View.GONE);
            if (bookingCard != null) bookingCard.setVisibility(View.GONE);
            setupMapIfNeeded();
            setupDirectionsButton();
        }

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
                selectedSession = null;
                if (bookingCard != null) bookingCard.setVisibility(View.GONE);
                createBookingViewModel.clearBooking();
                if (booking.id != null && booking.id > 0) {
                    Bundle voucherArgs = new Bundle();
                    voucherArgs.putLong("bookingId", booking.id);
                    androidx.navigation.Navigation.findNavController(requireView())
                            .navigate(R.id.action_detailFragment_to_voucherFragment, voucherArgs);
                } else {
                    android.widget.Toast.makeText(requireContext(),
                            getString(R.string.voucher_confirmed), android.widget.Toast.LENGTH_SHORT).show();
                    if (tourActivity != null && tourActivity.getId() != null) {
                        detailViewModel.load(tourActivity.getId());
                    }
                }
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
                        refreshMeetingMapIfReady();
                    }
                });
                detailViewModel.getSessions().observe(getViewLifecycleOwner(), sessions -> {
                    if (fromHistory || fromBooking) return;
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

                detailViewModel.isOfflineCacheMiss().observe(getViewLifecycleOwner(), miss -> {
                    if (Boolean.TRUE.equals(miss)) {
                        android.widget.Toast.makeText(requireContext(),
                                getString(R.string.detail_offline_cache_miss),
                                android.widget.Toast.LENGTH_LONG).show();
                    }
                });

                if (isOnline()) {
                    detailViewModel.load(id);
                } else {
                    if (loading != null) loading.setVisibility(View.GONE);
                    if (sessionsTitle != null) sessionsTitle.setVisibility(View.GONE);
                    sessionsRecycler.setVisibility(View.GONE);
                    if (bookingCard != null) bookingCard.setVisibility(View.GONE);
                    detailViewModel.loadFromCache(id);
                    android.widget.Toast.makeText(requireContext(),
                            "Sin conexión. Mostrando datos guardados.", android.widget.Toast.LENGTH_SHORT).show();
                }
            } else {
                if (sessionsTitle != null) sessionsTitle.setVisibility(View.GONE);
                sessionsRecycler.setVisibility(View.GONE);
            }
        }
    }

    private void setupDirectionsButton() {
        if (directionsButton == null) return;
        directionsButton.setOnClickListener(v -> openDirectionsToMeetingPoint());
    }

    private void setupMapIfNeeded() {
        if (!fromBooking || meetingMapContainer == null) return;
        if (mapFragment == null) {
            mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.meeting_map_container);
            if (mapFragment == null) {
                mapFragment = SupportMapFragment.newInstance();
                try {
                    getChildFragmentManager().beginTransaction()
                            .replace(R.id.meeting_map_container, mapFragment)
                            .commitNow();
                } catch (IllegalStateException e) {
                    getChildFragmentManager().beginTransaction()
                            .replace(R.id.meeting_map_container, mapFragment)
                            .commit();
                }
            }
        }

        mapFragment.getMapAsync(map -> {
            googleMap = map;
            googleMap.getUiSettings().setMapToolbarEnabled(false);
            refreshMeetingMapIfReady();
        });
    }

    private void refreshMeetingMapIfReady() {
        if (!fromBooking) return;
        if (googleMap == null || tourActivity == null) return;
        if (meetingMapContainer == null) return;

        final int requestId = ++mapRequestId;
        final String destination = safeTrim(tourActivity.getDestination());

        final String meeting = safeTrim(tourActivity.getMeetingPoint());
        final List<ItineraryPoint> itinerary = tourActivity.getItineraryPoints();

        if (meeting.isEmpty()) {
            if (meetingMapError != null) meetingMapError.setVisibility(View.VISIBLE);
            return;
        }

        if (!Geocoder.isPresent()) {
            if (meetingMapError != null) meetingMapError.setVisibility(View.VISIBLE);
            return;
        }

        final Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        new Thread(() -> {
            List<MarkerData> markers = new ArrayList<>();
            try {
                LatLng meetingLatLng = geocodeFirst(geocoder, withDestination(meeting, destination));
                if (meetingLatLng != null) {
                    markers.add(new MarkerData(meetingLatLng, "Punto de encuentro", meeting));
                }

                if (itinerary != null) {
                    for (ItineraryPoint p : itinerary) {
                        if (p == null) continue;
                        String label = safeTrim(p.getName());
                        String address = safeTrim(p.getAddress());
                        String query = !address.isEmpty() ? address : label;
                        if (query.isEmpty()) continue;
                        LatLng ll = geocodeFirst(geocoder, withDestination(query, destination));
                        if (ll != null) {
                            String title = (p.getPosition() > 0 ? (p.getPosition() + ". ") : "") + (label.isEmpty() ? query : label);
                            markers.add(new MarkerData(ll, title, query));
                        }
                    }
                }
            } catch (Exception ignored) {
            }

            new Handler(Looper.getMainLooper()).post(() -> {
                if (!isAdded()) return;
                if (requestId != mapRequestId) return;
                renderMarkers(markers);
            });
        }).start();
    }

    private void renderMarkers(List<MarkerData> markers) {
        if (googleMap == null) return;
        googleMap.clear();

        if (markers == null || markers.isEmpty()) {
            if (meetingMapError != null) meetingMapError.setVisibility(View.VISIBLE);
            return;
        }

        if (meetingMapError != null) meetingMapError.setVisibility(View.GONE);

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (MarkerData m : markers) {
            googleMap.addMarker(new MarkerOptions()
                    .position(m.latLng)
                    .title(m.title)
                    .snippet(m.snippet));
            boundsBuilder.include(m.latLng);
        }

        if (markers.size() == 1) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(markers.get(0).latLng, 15f));
            return;
        }

        LatLngBounds bounds = boundsBuilder.build();
        meetingMapContainer.post(() -> {
            try {
                googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 80));
            } catch (Exception ignored) {
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(markers.get(0).latLng, 12f));
            }
        });
    }

    @Nullable
    private static LatLng geocodeFirst(Geocoder geocoder, String query) throws IOException {
        if (query == null || query.trim().isEmpty()) return null;
        List<Address> results = geocoder.getFromLocationName(query, 1);
        if (results == null || results.isEmpty()) return null;
        Address a = results.get(0);
        return new LatLng(a.getLatitude(), a.getLongitude());
    }

    private static String withDestination(String query, String destination) {
        String q = safeTrim(query);
        String d = safeTrim(destination);
        if (q.isEmpty() || d.isEmpty()) return q;
        String qLower = q.toLowerCase(Locale.ROOT);
        String dLower = d.toLowerCase(Locale.ROOT);
        return qLower.contains(dLower) ? q : (q + ", " + d);
    }

    private void openDirectionsToMeetingPoint() {
        if (tourActivity == null) return;
        String meeting = safeTrim(tourActivity.getMeetingPoint());
        if (meeting.isEmpty()) {
            android.widget.Toast.makeText(requireContext(), "Punto de encuentro no disponible", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        String destination = safeTrim(tourActivity.getDestination());
        String query = withDestination(meeting, destination);

        try {
            Uri navUri = Uri.parse("google.navigation:q=" + Uri.encode(query));
            Intent googleMapsIntent = new Intent(Intent.ACTION_VIEW, navUri);
            googleMapsIntent.setPackage("com.google.android.apps.maps");

            if (googleMapsIntent.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivity(googleMapsIntent);
                return;
            }

            Uri geoUri = Uri.parse("geo:0,0?q=" + Uri.encode(query));
            Intent fallbackIntent = new Intent(Intent.ACTION_VIEW, geoUri);
            if (fallbackIntent.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivity(fallbackIntent);
                return;
            }

            android.widget.Toast.makeText(requireContext(), "No hay una app de mapas instalada", android.widget.Toast.LENGTH_SHORT).show();
        } catch (ActivityNotFoundException e) {
            android.widget.Toast.makeText(requireContext(), "No hay una app de mapas instalada", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class MarkerData {
        final LatLng latLng;
        final String title;
        final String snippet;

        MarkerData(LatLng latLng, String title, String snippet) {
            this.latLng = latLng;
            this.title = title;
            this.snippet = snippet;
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

        if (detailedContainer != null) {
            detailedContainer.setVisibility(View.VISIBLE);
        }

        // Base fields (null-safe)
        name.setText(nd(tourActivity.getName()));
        destination.setText(nd(tourActivity.getDestination()));
        String cat = tourActivity.getCategory();
        category.setText(cat != null && !cat.isEmpty() ? cat.toUpperCase() : getString(R.string.no_data));
        duration.setText(nd(tourActivity.getDuration()));
        price.setText(nd(tourActivity.getPrice()));

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
                    price.setText(nd(tourActivity.getPrice()));
                }
            } else {
                if (originalPrice != null) originalPrice.setVisibility(View.GONE);
                if (discountBadge != null) discountBadge.setVisibility(View.GONE);
                price.setText(nd(tourActivity.getPrice()));
            }
        } else {
            if (originalPrice != null) originalPrice.setVisibility(View.GONE);
            if (discountBadge != null) discountBadge.setVisibility(View.GONE);
            price.setText(nd(tourActivity.getPrice()));
        }
        boolean soldOut = tourActivity.getAvailableSlots() <= 0;
        slots.setText(soldOut
                ? getString(R.string.sold_out)
                : getString(R.string.slots_available, tourActivity.getAvailableSlots()));
        root.setAlpha((!fromHistory && soldOut) ? 0.65f : 1f);
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

        if (description != null) description.setText(nd(tourActivity.getDescription()));
        if (rating != null) {
            if (tourActivity.getReviewsCount() <= 0) {
                rating.setText(getString(R.string.no_reviews));
            } else {
                rating.setText(String.valueOf(tourActivity.getRating()));
            }
        }
        if (language != null) language.setText("Idioma: " + nd(tourActivity.getLanguage()));
        if (guide != null) guide.setText("Guía: " + nd(tourActivity.getGuideName()));
        if (meetingPoint != null) meetingPoint.setText("Encuentro: " + nd(tourActivity.getMeetingPoint()));
        if (includes != null) includes.setText(nd(tourActivity.getWhatIncluded()));
        if (cancellation != null) cancellation.setText(nd(tourActivity.getCancellationPolicy()));

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

    /** Campos requeridos: siempre muestran "No disponible" si están vacíos. */
    private String nd(String value) {
        return (value != null && !value.trim().isEmpty()) ? value : getString(R.string.no_data);
    }

    /**
     * Campos opcionales: visibles con su texto cuando hay dato, ocultos cuando no.
     * @param prefix prefijo a mostrar antes del valor (ej. "Idioma: "), o null si no hay.
     */
    private void setOptionalText(TextView view, String value, String prefix) {
        if (view == null) return;
        if (value != null && !value.trim().isEmpty()) {
            view.setText(prefix != null ? prefix + value : value);
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) requireContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        android.net.Network network = cm.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    @Override
    public void onDestroyView() {
        rootView = null;
        sessionAdapter = null;
        detailViewModel = null;
        historyReviewViewModel = null;
        googleMap = null;
        mapFragment = null;
        meetingMapSection = null;
        meetingMapContainer = null;
        meetingMapError = null;
        directionsButton = null;
        super.onDestroyView();
    }
}
