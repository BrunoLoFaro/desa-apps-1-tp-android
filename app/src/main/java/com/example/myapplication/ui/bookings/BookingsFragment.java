package com.example.myapplication.ui.bookings;

import android.app.DatePickerDialog;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import com.example.myapplication.data.model.BookingResponse;
import com.example.myapplication.data.model.BookingSummaryItem;
import com.example.myapplication.data.model.TourActivity;
import com.example.myapplication.ui.bookings.viewmodel.BookingsViewModel;
import com.example.myapplication.ui.profile.ActivitySummaryAdapter;
import com.example.myapplication.util.MainThreadUtils;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import dagger.hilt.android.AndroidEntryPoint;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@AndroidEntryPoint
public class BookingsFragment extends Fragment {

    private static final String[] MONTHS =
            {"ENE","FEB","MAR","ABR","MAY","JUN","JUL","AGO","SEP","OCT","NOV","DIC"};

    private BookingsViewModel viewModel;
    private View offlineBanner;
    private ConnectivityManager.NetworkCallback networkCallback;
    private SwipeRefreshLayout swipeRefresh;

    // Activas views
    private View sectionActivas;
    private RecyclerView activasRecycler;
    private ProgressBar activasLoading;
    private TextView activasEmpty;
    private BookingAdapter bookingAdapter;

    // Historial views
    private View sectionHistorial;
    private RecyclerView historialRecycler;
    private ProgressBar historialLoading;
    private TextView historialEmpty;
    private ActivitySummaryAdapter summaryAdapter;
    private AutoCompleteTextView filterDestination;
    private TextInputEditText filterFromDate;
    private TextInputEditText filterToDate;
    private MaterialButton btnBuscar;
    private MaterialButton btnLimpiar;

    // Mis Calificaciones views
    private View sectionReviews;
    private RecyclerView reviewsRecycler;
    private ProgressBar reviewsLoading;
    private TextView reviewsEmpty;
    private ReviewAdapter reviewAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bookings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(BookingsViewModel.class);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(view).navigateUp());

        bindViews(view);
        setupAdapters(view);
        setupFilters();
        setupTabs(view);
        observeViewModel(view);
        registerNetworkCallback();

        viewModel.loadMyBookings("CONFIRMED");
    }

    private void bindViews(@NonNull View view) {
        offlineBanner     = view.findViewById(R.id.offline_banner);
        swipeRefresh      = view.findViewById(R.id.swipe_refresh);
        sectionActivas    = view.findViewById(R.id.section_activas);
        activasRecycler   = view.findViewById(R.id.bookings_recycler_view);
        activasLoading    = view.findViewById(R.id.bookings_loading_spinner);
        activasEmpty      = view.findViewById(R.id.activas_empty_text);

        sectionHistorial  = view.findViewById(R.id.section_historial);
        historialRecycler = view.findViewById(R.id.historial_recycler_view);
        historialLoading  = view.findViewById(R.id.historial_loading_spinner);
        historialEmpty    = view.findViewById(R.id.historial_empty_text);

        filterDestination = view.findViewById(R.id.filter_destination);
        filterFromDate    = view.findViewById(R.id.filter_from_date);
        filterToDate      = view.findViewById(R.id.filter_to_date);
        btnBuscar         = view.findViewById(R.id.btn_buscar);
        btnLimpiar        = view.findViewById(R.id.btn_limpiar);

        sectionReviews    = view.findViewById(R.id.section_mis_calificaciones);
        reviewsRecycler   = view.findViewById(R.id.reviews_recycler_view);
        reviewsLoading    = view.findViewById(R.id.reviews_loading_spinner);
        reviewsEmpty      = view.findViewById(R.id.reviews_empty_text);
    }

    private void setupAdapters(@NonNull View view) {
        bookingAdapter = new BookingAdapter(this::showCancelDialog, b -> showReviewDialog(b.id, b.activityName));
        bookingAdapter.setOnDetailClickListener(this::navigateToDetail);
        bookingAdapter.setOnVoucherClickListener(this::navigateToVoucher);
        activasRecycler.setAdapter(bookingAdapter);

        summaryAdapter = new ActivitySummaryAdapter();
        summaryAdapter.setOnItemClickListener(this::navigateToHistoryDetail);
        summaryAdapter.setOnReviewClickListener(this::showReviewDialogForSummary);
        historialRecycler.setAdapter(summaryAdapter);

        reviewAdapter = new ReviewAdapter();
        reviewsRecycler.setAdapter(reviewAdapter);

        swipeRefresh.setOnRefreshListener(() -> viewModel.loadMyBookings("CONFIRMED"));
    }

    private void setupFilters() {
        filterDestination.addTextChangedListener(new com.example.myapplication.util.SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateBuscarState();
            }
        });

        filterFromDate.setOnClickListener(v -> showDatePicker(true));
        filterToDate.setOnClickListener(v -> showDatePicker(false));

        btnBuscar.setOnClickListener(v -> {
            String dest = filterDestination.getText() != null
                    ? filterDestination.getText().toString() : "";
            String from = filterFromDate.getText() != null
                    ? filterFromDate.getText().toString() : "";
            String to = filterToDate.getText() != null
                    ? filterToDate.getText().toString() : "";
            viewModel.setFilterDestination(dest);
            viewModel.setFilterFrom(from);
            viewModel.setFilterTo(to);
            btnLimpiar.setVisibility(hasAnyFilter() ? View.VISIBLE : View.GONE);
        });

        btnLimpiar.setOnClickListener(v -> {
            filterDestination.setText("");
            filterFromDate.setText("");
            filterToDate.setText("");
            viewModel.clearFilters();
            btnLimpiar.setVisibility(View.GONE);
            updateBuscarState();
        });
    }

    private boolean hasAnyFilter() {
        return (filterDestination != null && filterDestination.getText() != null
                && !filterDestination.getText().toString().isEmpty())
            || (filterFromDate != null && filterFromDate.getText() != null
                && !filterFromDate.getText().toString().isEmpty())
            || (filterToDate != null && filterToDate.getText() != null
                && !filterToDate.getText().toString().isEmpty());
    }

    private void updateBuscarState() {
        if (btnBuscar != null) btnBuscar.setEnabled(hasAnyFilter());
    }

    private void setupTabs(@NonNull View view) {
        TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText(R.string.bookings_tab_activas));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.bookings_tab_historial));
        tabLayout.addTab(tabLayout.newTab().setText("Mis Calificaciones"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int pos = tab.getPosition();
                viewModel.setSelectedTab(pos);
                
                sectionActivas.setVisibility(pos == 0 ? View.VISIBLE : View.GONE);
                sectionHistorial.setVisibility(pos == 1 ? View.VISIBLE : View.GONE);
                sectionReviews.setVisibility(pos == 2 ? View.VISIBLE : View.GONE);

                if (pos == 1) {
                    viewModel.loadHistorialIfNeeded();
                } else if (pos == 2) {
                    viewModel.loadMyReviewsIfNeeded();
                }
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        int savedTab = viewModel.getSelectedTab();
        if (savedTab >= 0 && savedTab < tabLayout.getTabCount()) {
            TabLayout.Tab tab = tabLayout.getTabAt(savedTab);
            if (tab != null) tab.select();
        }
    }

    private void observeViewModel(@NonNull View view) {
        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            boolean isLoading = Boolean.TRUE.equals(loading);
            activasLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            if (!isLoading && swipeRefresh != null) swipeRefresh.setRefreshing(false);
        });

        viewModel.getBookings().observe(getViewLifecycleOwner(), bookings -> {
            List<BookingListItem> grouped = buildGroupedList(bookings);
            bookingAdapter.updateData(grouped);
            boolean empty = bookings == null || bookings.isEmpty();
            if (empty) {
                boolean offline = Boolean.TRUE.equals(viewModel.isOffline().getValue());
                activasEmpty.setText(offline
                        ? R.string.bookings_empty_offline
                        : R.string.bookings_empty_activas);
            }
            activasEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            activasRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
        });

        viewModel.isHistorialLoading().observe(getViewLifecycleOwner(), loading -> {
            historialLoading.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
        });

        viewModel.getHistorial().observe(getViewLifecycleOwner(), items -> {
            if (items == null) return;
            summaryAdapter.updateData(items);
            boolean empty = items.isEmpty();
            // El mensaje se decide abajo según neverSynced
            historialRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
        });

        viewModel.isMyReviewsLoading().observe(getViewLifecycleOwner(), loading -> {
            reviewsLoading.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
        });

        viewModel.getMyReviews().observe(getViewLifecycleOwner(), reviews -> {
            reviewAdapter.updateData(reviews);
            boolean empty = reviews == null || reviews.isEmpty();
            reviewsEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            reviewsRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
        });

        viewModel.isHistorialNeverSynced().observe(getViewLifecycleOwner(), neverSynced -> {
            boolean show = false;
            if (Boolean.TRUE.equals(neverSynced)) {
                List<BookingSummaryItem> items = viewModel.getHistorial().getValue();
                show = (items == null || items.isEmpty());
            }
            if (show) {
                historialEmpty.setText(R.string.bookings_empty_historial_never_synced);
                historialEmpty.setVisibility(View.VISIBLE);
                historialRecycler.setVisibility(View.GONE);
            } else {
                List<BookingSummaryItem> items = viewModel.getHistorial().getValue();
                boolean empty = (items == null || items.isEmpty());
                historialEmpty.setText(R.string.bookings_empty_historial);
                historialEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                historialRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
            }
        });

        viewModel.getAvailableDestinations().observe(getViewLifecycleOwner(), destinations -> {
            if (destinations == null || filterDestination == null) return;
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    requireContext(), android.R.layout.simple_dropdown_item_1line, destinations);
            filterDestination.setAdapter(adapter);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error.resolve(requireContext()),
                        Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) {
                Toast.makeText(requireContext(), msg.resolve(requireContext()), Toast.LENGTH_SHORT).show();
                viewModel.clearMessage();
            }
        });

        viewModel.isShowOfflineCancelModal().observe(getViewLifecycleOwner(), show -> {
            if (Boolean.TRUE.equals(show)) {
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                        .setMessage(R.string.cancel_booking_offline_modal)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                viewModel.clearOfflineCancelModal();
            }
        });

        viewModel.isOffline().observe(getViewLifecycleOwner(), offline -> {
            boolean isOffline = Boolean.TRUE.equals(offline);
            if (offlineBanner != null) {
                offlineBanner.setVisibility(isOffline ? View.VISIBLE : View.GONE);
            }
            if (bookingAdapter != null) {
                bookingAdapter.setOffline(isOffline);
            }
            // Si la lista ya está vacía, actualizar el texto según el nuevo estado de red
            if (activasEmpty != null && activasEmpty.getVisibility() == View.VISIBLE) {
                activasEmpty.setText(isOffline
                        ? R.string.bookings_empty_offline
                        : R.string.bookings_empty_activas);
            }
        });
    }

    private void registerNetworkCallback() {
        ConnectivityManager cm = (ConnectivityManager)
                requireContext().getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                MainThreadUtils.post(() -> {
                    if (isAdded()) viewModel.onConnectivityChanged(true);
                });
            }

            @Override
            public void onLost(Network network) {
                MainThreadUtils.post(() -> {
                    if (isAdded()) viewModel.onConnectivityChanged(false);
                });
            }
        };
        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        cm.registerNetworkCallback(request, networkCallback);
    }

    // ── Grouping ─────────────────────────────────────────────────────────────

    private List<BookingListItem> buildGroupedList(List<BookingResponse> bookings) {
        if (bookings == null || bookings.isEmpty()) return Collections.emptyList();

        String todayStr = LocalDate.now().toString();

        List<BookingResponse> todayItems = new ArrayList<>();
        List<BookingResponse> upcomingItems = new ArrayList<>();

        for (BookingResponse b : bookings) {
            String dateStr = b.sessionStartTime != null && b.sessionStartTime.length() >= 10
                    ? b.sessionStartTime.substring(0, 10) : null;
            if (todayStr.equals(dateStr)) {
                todayItems.add(b);
            } else {
                upcomingItems.add(b);
            }
        }

        List<BookingListItem> result = new ArrayList<>();

        // Hoy first
        if (!todayItems.isEmpty()) {
            result.add(new BookingListItem.SectionHeader(getString(R.string.bookings_section_hoy), true));
            for (BookingResponse b : todayItems) result.add(toBookingItem(b, true));
        }

        // Próximas after
        if (!upcomingItems.isEmpty()) {
            result.add(new BookingListItem.SectionHeader(getString(R.string.bookings_section_proximas), false));
            for (BookingResponse b : upcomingItems) result.add(toBookingItem(b, false));
        }

        return result;
    }

    private BookingListItem.BookingItem toBookingItem(BookingResponse booking, boolean isToday) {
        String dayNumber = "";
        String monthAbbr = "";
        if (booking.sessionStartTime != null && booking.sessionStartTime.length() >= 10) {
            try {
                String[] parts = booking.sessionStartTime.substring(0, 10).split("-");
                dayNumber = parts[2];
                monthAbbr = MONTHS[Integer.parseInt(parts[1]) - 1];
            } catch (Exception ignored) {}
        }
        return new BookingListItem.BookingItem(booking, dayNumber, monthAbbr, isToday);
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private void navigateToDetail(BookingResponse booking) {
        String destination = booking.destination != null ? booking.destination.name : "";
        String duration = booking.durationMinutes > 0 ? booking.durationMinutes + " min" : "";
        String price = booking.currency != null
                ? booking.totalPrice + " " + booking.currency : String.valueOf(booking.totalPrice);
        TourActivity activity = new TourActivity(
                booking.activityName != null ? booking.activityName : "",
                destination, "", duration, price, 1, null,
                null, 0f, 0, null, booking.meetingPoint,
                booking.guideName, null, booking.cancellationPolicy, null);
        if (booking.activityId != null) activity.setId(booking.activityId);
        Bundle args = new Bundle();
        args.putSerializable("activity_data", activity);
        args.putBoolean("from_history", false);
        args.putBoolean("from_booking", true);
        args.putString("booking_status", booking.status != null ? booking.status : "CONFIRMED");
        if (booking.id != null) args.putLong("booking_id", booking.id);
        Navigation.findNavController(requireView())
                .navigate(R.id.action_bookingsFragment_to_detailFragment, args);
    }

    private void navigateToVoucher(BookingResponse booking) {
        if (booking.id == null) return;
        Bundle args = new Bundle();
        args.putLong("bookingId", booking.id);
        Navigation.findNavController(requireView())
                .navigate(R.id.action_bookingsFragment_to_voucherFragment, args);
    }

    private void navigateToHistoryDetail(BookingSummaryItem item) {
        if (item.getActivityId() == null) return;
        String duration = item.getDurationMinutes() > 0 ? item.getDurationMinutes() + " min" : "";
        TourActivity activity = new TourActivity(
                item.getActivityName() != null ? item.getActivityName() : "",
                item.getDestination() != null ? item.getDestination() : "",
                "", duration,
                item.getPrice() != null ? item.getPrice() : "",
                0, item.getImageUrl(),
                null, 0f, 0, null, null,
                item.getGuideName(), null, null, null);
        activity.setId(item.getActivityId());
        Bundle args = new Bundle();
        args.putSerializable("activity_data", activity);
        args.putBoolean("from_history", true);
        args.putBoolean("from_booking", true);
        args.putString("booking_status", item.getStatus() != null ? item.getStatus() : "");
        if (item.getId() != null) args.putLong("booking_id", item.getId());
        Navigation.findNavController(requireView())
                .navigate(R.id.action_bookingsFragment_to_detailFragment, args);
    }

    private void showDatePicker(boolean isFrom) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(
                requireContext(),
                (dp, year, month, day) -> {
                    String date = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day);
                    if (isFrom) {
                        filterFromDate.setText(date);
                    } else {
                        filterToDate.setText(date);
                    }
                    updateBuscarState();
                },
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    @Override
    public void onDestroyView() {
        if (networkCallback != null) {
            ConnectivityManager cm = (ConnectivityManager)
                    requireContext().getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
            if (cm != null) cm.unregisterNetworkCallback(networkCallback);
            networkCallback = null;
        }
        offlineBanner     = null;
        swipeRefresh      = null;
        sectionActivas    = null;
        activasRecycler   = null;
        activasLoading    = null;
        activasEmpty      = null;
        bookingAdapter    = null;
        sectionHistorial  = null;
        historialRecycler = null;
        historialLoading  = null;
        historialEmpty    = null;
        summaryAdapter    = null;
        filterDestination = null;
        filterFromDate    = null;
        filterToDate      = null;
        btnBuscar         = null;
        btnLimpiar        = null;
        sectionReviews    = null;
        reviewsRecycler   = null;
        reviewsLoading    = null;
        reviewsEmpty      = null;
        reviewAdapter     = null;
        super.onDestroyView();
    }

    private void showCancelDialog(BookingResponse booking) {
        if (booking == null || booking.id == null) return;
        String policy = (booking.cancellationPolicy != null && !booking.cancellationPolicy.isEmpty())
                ? booking.cancellationPolicy
                : getString(R.string.cancel_booking_policy_unknown);
        String name = booking.activityName != null ? booking.activityName : "";
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.cancel_booking_title)
                .setMessage(getString(R.string.cancel_booking_message, name, policy))
                .setPositiveButton(R.string.cancel_booking_confirm, (d, w) -> viewModel.cancelBooking(booking.id))
                .setNegativeButton(R.string.cancel_booking_back, null)
                .show();
    }

    private void showReviewDialogForSummary(BookingSummaryItem item) {
        if (item == null) return;
        showReviewDialog(item.getId(), item.getActivityName());
    }

    private void showReviewDialog(Long bookingId, String name) {
        if (bookingId == null) return;

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_review, null);
        RatingBar activityRating = dialogView.findViewById(R.id.review_activity_rating);
        RatingBar guideRating = dialogView.findViewById(R.id.review_guide_rating);
        TextInputEditText commentInput = dialogView.findViewById(R.id.review_comment_input);

        String title = getString(R.string.review_title);
        String activityName = name != null ? name.trim() : "";
        if (!activityName.isEmpty()) title = activityName;

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setView(dialogView)
                .setNegativeButton(R.string.review_cancel, (d, which) -> d.dismiss())
                .setPositiveButton(R.string.review_send, null)
                .show();

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            int a = Math.round(activityRating.getRating());
            if (a < 1) {
                Toast.makeText(requireContext(), getString(R.string.review_error_activity_required),
                        Toast.LENGTH_SHORT).show();
                return;
            }
            int g = Math.round(guideRating.getRating());
            Integer guide = g >= 1 ? g : null;

            String comment = null;
            if (commentInput.getText() != null) {
                String raw = commentInput.getText().toString().trim();
                if (!raw.isEmpty()) comment = raw;
            }

            viewModel.submitReview(bookingId, a, guide, comment);
            dialog.dismiss();
        });
    }
}
