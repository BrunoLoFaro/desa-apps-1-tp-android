package com.example.myapplication.ui.bookings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import com.example.myapplication.ui.bookings.viewmodel.BookingsViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.ChipGroup;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class BookingsFragment extends Fragment {

    private BookingsViewModel viewModel;

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
        toolbar.setTitle(getString(R.string.nav_bookings));

        ProgressBar loading = view.findViewById(R.id.bookings_loading_spinner);
        RecyclerView recycler = view.findViewById(R.id.bookings_recycler_view);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));

        BookingAdapter adapter = new BookingAdapter(viewModel::cancelBooking);
        recycler.setAdapter(adapter);

        ChipGroup filters = view.findViewById(R.id.bookings_filter_group);
        if (filters != null) {
            filters.setOnCheckedChangeListener((group, checkedId) -> {
                String status = null;
                if (checkedId == R.id.chip_filter_active) {
                    status = "CONFIRMED";
                } else if (checkedId == R.id.chip_filter_completed) {
                    status = "COMPLETED";
                } else if (checkedId == R.id.chip_filter_cancelled) {
                    status = "CANCELLED";
                }
                viewModel.loadMyBookings(status);
            });
        }

        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            boolean show = Boolean.TRUE.equals(isLoading);
            loading.setVisibility(show ? View.VISIBLE : View.GONE);
            recycler.setVisibility(show ? View.GONE : View.VISIBLE);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error.resolve(requireContext()), Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getBookings().observe(getViewLifecycleOwner(), adapter::updateData);

        // initial load: "Todas"
        viewModel.loadMyBookings(null);
    }
}
