package com.example.myapplication.ui.bookings;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
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
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

@AndroidEntryPoint
public class BookingsFragment extends Fragment {

    private BookingsViewModel viewModel;

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
    private MaterialButton btnClearFilters;

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

        // Carga inicial: Activas (CONFIRMED)
        viewModel.loadMyBookings("CONFIRMED");
    }

    private void bindViews(@NonNull View view) {
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
        btnClearFilters   = view.findViewById(R.id.btn_clear_filters);
    }

    private void setupAdapters(@NonNull View view) {
        bookingAdapter = new BookingAdapter(viewModel::cancelBooking);
        bookingAdapter.setOnDetailClickListener(this::navigateToDetail);
        activasRecycler.setAdapter(bookingAdapter);

        summaryAdapter = new ActivitySummaryAdapter();
        summaryAdapter.setOnItemClickListener(this::navigateToHistoryDetail);
        historialRecycler.setAdapter(summaryAdapter);
    }

    private void setupFilters() {
        filterDestination.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setFilterDestination(s.toString());
                updateClearButtonVisibility();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        filterFromDate.setOnClickListener(v -> showDatePicker(true));
        filterToDate.setOnClickListener(v -> showDatePicker(false));

        btnClearFilters.setOnClickListener(v -> {
            filterDestination.setText("");
            filterFromDate.setText("");
            filterToDate.setText("");
            viewModel.clearFilters();
            updateClearButtonVisibility();
        });
    }

    private void updateClearButtonVisibility() {
        boolean hasFilter =
                (filterDestination != null && filterDestination.getText() != null
                        && !filterDestination.getText().toString().isEmpty())
                || (filterFromDate != null && filterFromDate.getText() != null
                        && !filterFromDate.getText().toString().isEmpty())
                || (filterToDate != null && filterToDate.getText() != null
                        && !filterToDate.getText().toString().isEmpty());
        if (btnClearFilters != null) {
            btnClearFilters.setVisibility(hasFilter ? View.VISIBLE : View.GONE);
        }
    }

    private void setupTabs(@NonNull View view) {
        TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText(R.string.bookings_tab_activas));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.bookings_tab_historial));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewModel.setSelectedTab(tab.getPosition());
                if (tab.getPosition() == 0) {
                    sectionActivas.setVisibility(View.VISIBLE);
                    sectionHistorial.setVisibility(View.GONE);
                } else {
                    sectionActivas.setVisibility(View.GONE);
                    sectionHistorial.setVisibility(View.VISIBLE);
                    viewModel.loadHistorialIfNeeded();
                }
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Restaurar tab al volver de otra pantalla
        int savedTab = viewModel.getSelectedTab();
        if (savedTab == 1) {
            TabLayout.Tab tab = tabLayout.getTabAt(1);
            if (tab != null) tab.select();
        }
    }

    private void observeViewModel(@NonNull View view) {
        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            activasLoading.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
            if (!Boolean.TRUE.equals(loading)) {
                // empty state handled in bookings observer
            }
        });

        viewModel.getBookings().observe(getViewLifecycleOwner(), bookings -> {
            bookingAdapter.updateData(bookings);
            boolean empty = bookings == null || bookings.isEmpty();
            activasEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            activasRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
        });

        viewModel.isHistorialLoading().observe(getViewLifecycleOwner(), loading -> {
            historialLoading.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
        });

        viewModel.getHistorial().observe(getViewLifecycleOwner(), items -> {
            summaryAdapter.updateData(items);
            boolean empty = items == null || items.isEmpty();
            historialEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            historialRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
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
    }

    private void navigateToDetail(BookingResponse booking) {
        if (booking.activityId == null) return;
        TourActivity activity = new TourActivity(
                booking.activityName != null ? booking.activityName : "",
                "", "", "", "", 0, null);
        activity.setId(booking.activityId);
        Bundle args = new Bundle();
        args.putSerializable("activity_data", activity);
        args.putBoolean("from_history", false);
        Navigation.findNavController(requireView())
                .navigate(R.id.action_bookingsFragment_to_detailFragment, args);
    }

    private void navigateToHistoryDetail(BookingSummaryItem item) {
        if (item.getActivityId() == null) return;
        TourActivity activity = new TourActivity(
                item.getActivityName(), "", "", "", "", 0, null);
        activity.setId(item.getActivityId());
        Bundle args = new Bundle();
        args.putSerializable("activity_data", activity);
        args.putBoolean("from_history", true);
        args.putString("booking_status", "COMPLETED");
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
                        viewModel.setFilterFrom(date);
                    } else {
                        filterToDate.setText(date);
                        viewModel.setFilterTo(date);
                    }
                    updateClearButtonVisibility();
                },
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    @Override
    public void onDestroyView() {
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
        btnClearFilters   = null;
        super.onDestroyView();
    }
}
