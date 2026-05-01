package com.example.myapplication.ui.explore;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import com.example.myapplication.data.model.DestinationResponse;
import com.example.myapplication.util.ConnectivityUtils;
import com.example.myapplication.ui.explore.viewmodel.ExploreViewModel;
import com.example.myapplication.ui.home.TourAdapter;
import com.example.myapplication.ui.main.MainViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import dagger.hilt.android.AndroidEntryPoint;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

@AndroidEntryPoint
public class ExploreFragment extends Fragment {

    private ExploreViewModel viewModel;
    private boolean isLoading = false;

    private final Map<String, Long> destinationNameToId = new HashMap<>();
    private final Map<String, String> categoryLabelToValue = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_explore, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ExploreViewModel.class);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.nav_explore));

        MaterialAutoCompleteTextView destinationDropdown = view.findViewById(R.id.destination_dropdown);
        MaterialAutoCompleteTextView categoryDropdown = view.findViewById(R.id.category_dropdown);
        TextInputEditText dateInput = view.findViewById(R.id.date_input);
        TextInputEditText minPriceInput = view.findViewById(R.id.min_price_input);
        TextInputEditText maxPriceInput = view.findViewById(R.id.max_price_input);
        TextInputLayout minPriceLayout = view.findViewById(R.id.min_price_layout);
        TextInputLayout maxPriceLayout = view.findViewById(R.id.max_price_layout);
        MaterialButton applyButton = view.findViewById(R.id.apply_filters_button);
        MaterialButton clearButton = view.findViewById(R.id.clear_filters_button);

        ProgressBar loading = view.findViewById(R.id.loading_spinner);
        ProgressBar pagingLoading = view.findViewById(R.id.paging_loading_spinner);
        View emptyContainer = view.findViewById(R.id.empty_state_container);
        MaterialButton emptyClearButton = view.findViewById(R.id.empty_clear_filters_button);
        RecyclerView recycler = view.findViewById(R.id.activities_recycler_view);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        TourAdapter adapter = new TourAdapter(false, true);
        recycler.setAdapter(adapter);
        adapter.setOnFavoriteToggleListener((activity, targetFavorite) -> {
            if (activity.getId() == null) return;
            viewModel.toggleFavorite(activity.getId(), targetFavorite, null);
        });

        recycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                if (dy <= 0) return;
                LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                if (lm == null) return;
                int lastVisible = lm.findLastVisibleItemPosition();
                int total = lm.getItemCount();
                if (total > 0 && lastVisible >= total - 3) {
                    viewModel.loadNextPageIfAvailable();
                }
            }
        });

        // Date picker (ISO yyyy-MM-dd)
        SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        iso.setTimeZone(TimeZone.getTimeZone("UTC"));
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.explore_date_picker_title))
                .build();

        if (dateInput != null) {
            dateInput.setFocusable(false);
            dateInput.setOnClickListener(v -> datePicker.show(getParentFragmentManager(), "date_picker"));
        }
        datePicker.addOnPositiveButtonClickListener(selection -> {
            if (selection != null && dateInput != null) {
                dateInput.setText(iso.format(new Date(selection)));
            }
        });

        viewModel.getDestinations().observe(getViewLifecycleOwner(), destinations -> {
            destinationNameToId.clear();
            List<String> labels = new ArrayList<>();
            labels.add(getString(R.string.explore_all_destinations));
            if (destinations != null) {
                for (DestinationResponse d : destinations) {
                    if (d != null && d.name != null && d.id != null) {
                        labels.add(d.name);
                        destinationNameToId.put(d.name, d.id);
                    }
                }
            }
            ArrayAdapter<String> a = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_list_item_1, labels);
            if (destinationDropdown != null) {
                destinationDropdown.setAdapter(a);
                if (TextUtils.isEmpty(destinationDropdown.getText())) {
                    destinationDropdown.setText(getString(R.string.explore_all_destinations), false);
                }
            }
        });

        viewModel.getCategories().observe(getViewLifecycleOwner(), categories -> {
            categoryLabelToValue.clear();
            List<String> labels = new ArrayList<>();
            labels.add(getString(R.string.explore_all_categories));
            if (categories != null) {
                for (String value : categories) {
                    if (value == null) continue;
                    String label = toCategoryLabel(value);
                    labels.add(label);
                    categoryLabelToValue.put(label, value);
                }
            }
            ArrayAdapter<String> a = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_list_item_1, labels);
            if (categoryDropdown != null) {
                categoryDropdown.setAdapter(a);
                if (TextUtils.isEmpty(categoryDropdown.getText())) {
                    categoryDropdown.setText(getString(R.string.explore_all_categories), false);
                }
            }
        });

        viewModel.getActivities().observe(getViewLifecycleOwner(), activities -> {
            adapter.updateData(activities);
            boolean isEmpty = activities == null || activities.isEmpty();
            if (isLoading) {
                if (emptyContainer != null) emptyContainer.setVisibility(View.GONE);
                recycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            } else {
                if (emptyContainer != null) emptyContainer.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                recycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            }
        });

        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            boolean show = Boolean.TRUE.equals(isLoading);
            this.isLoading = show;
            boolean listHasItems = adapter.getItemCount() > 0;
            loading.setVisibility(show && !listHasItems ? View.VISIBLE : View.GONE);
            if (pagingLoading != null) {
                pagingLoading.setVisibility(show && listHasItems ? View.VISIBLE : View.GONE);
            }
            if (show && emptyContainer != null) emptyContainer.setVisibility(View.GONE);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && ConnectivityUtils.isOnline(requireContext())) {
                Toast.makeText(requireContext(), error.resolve(requireContext()), Toast.LENGTH_SHORT).show();
            }
        });

        applyButton.setOnClickListener(v -> {
            if (minPriceLayout != null) minPriceLayout.setError(null);
            if (maxPriceLayout != null) maxPriceLayout.setError(null);

            Long destinationId = null;
            String destinationLabel = destinationDropdown != null ? destinationDropdown.getText().toString() : null;
            if (!TextUtils.isEmpty(destinationLabel) && !"Todos".equalsIgnoreCase(destinationLabel)) {
                destinationId = destinationNameToId.get(destinationLabel);
            }

            String categoryValue = null;
            String categoryLabel = categoryDropdown != null ? categoryDropdown.getText().toString() : null;
            if (!TextUtils.isEmpty(categoryLabel) && !"Todas".equalsIgnoreCase(categoryLabel)) {
                categoryValue = categoryLabelToValue.get(categoryLabel);
            }

            String dateIso = dateInput != null && dateInput.getText() != null ? dateInput.getText().toString().trim() : null;
            if (TextUtils.isEmpty(dateIso)) dateIso = null;

            String minRaw = minPriceInput != null && minPriceInput.getText() != null
                    ? minPriceInput.getText().toString()
                    : null;
            String maxRaw = maxPriceInput != null && maxPriceInput.getText() != null
                    ? maxPriceInput.getText().toString()
                    : null;

            String minPrice = normalizePriceInput(minRaw);
            String maxPrice = normalizePriceInput(maxRaw);

            // basic validation: only send valid decimals (backend expects decimal with "." separator)
            if ((!TextUtils.isEmpty(minRaw) && minPrice == null) || (!TextUtils.isEmpty(maxRaw) && maxPrice == null)) {
                if (!TextUtils.isEmpty(minRaw) && minPrice == null && minPriceLayout != null) {
                    minPriceLayout.setError(getString(R.string.explore_invalid_price));
                }
                if (!TextUtils.isEmpty(maxRaw) && maxPrice == null && maxPriceLayout != null) {
                    maxPriceLayout.setError(getString(R.string.explore_invalid_price));
                }
                return;
            }
            if (minPrice != null && maxPrice != null) {
                try {
                    double min = Double.parseDouble(minPrice);
                    double max = Double.parseDouble(maxPrice);
                    if (min > max) {
                        if (minPriceLayout != null) minPriceLayout.setError(getString(R.string.explore_invalid_price_range));
                        if (maxPriceLayout != null) maxPriceLayout.setError(getString(R.string.explore_invalid_price_range));
                        return;
                    }
                } catch (NumberFormatException ignored) {
                    if (minPriceLayout != null) minPriceLayout.setError(getString(R.string.explore_invalid_price));
                    if (maxPriceLayout != null) maxPriceLayout.setError(getString(R.string.explore_invalid_price));
                    return;
                }
            }

            viewModel.applyFilters(destinationId, categoryValue, dateIso, minPrice, maxPrice);
        });

        View.OnClickListener clearAction = v -> {
            if (destinationDropdown != null) {
                destinationDropdown.setText(getString(R.string.explore_all_destinations), false);
            }
            if (categoryDropdown != null) {
                categoryDropdown.setText(getString(R.string.explore_all_categories), false);
            }
            if (dateInput != null) dateInput.setText("");
            if (minPriceInput != null) minPriceInput.setText("");
            if (maxPriceInput != null) maxPriceInput.setText("");
            if (minPriceLayout != null) minPriceLayout.setError(null);
            if (maxPriceLayout != null) maxPriceLayout.setError(null);
            viewModel.applyFilters(null, null, null, null, null);
        };

        clearButton.setOnClickListener(clearAction);
        if (emptyClearButton != null) emptyClearButton.setOnClickListener(clearAction);

        // initial load
        viewModel.loadMeta();
        viewModel.loadFirstPage();

        View offlineState = view.findViewById(R.id.offline_state);
        View filterContainer = view.findViewById(R.id.filter_container);
        boolean[] wasOffline = {false};
        new ViewModelProvider(requireActivity()).get(MainViewModel.class)
                .isOnline().observe(getViewLifecycleOwner(), online -> {
            boolean isOffline = !Boolean.TRUE.equals(online);
            if (offlineState != null) offlineState.setVisibility(isOffline ? View.VISIBLE : View.GONE);
            if (filterContainer != null) filterContainer.setVisibility(isOffline ? View.GONE : View.VISIBLE);
            if (!isOffline && wasOffline[0]) {
                viewModel.loadMeta();
                viewModel.loadFirstPage();
            }
            wasOffline[0] = isOffline;
        });
    }

    private static boolean isValidDecimal(String value) {
        if (value == null) return true;
        if (value.isEmpty()) return true;
        // allow "." decimal separator
        return value.matches("^\\d+(\\.\\d+)?$");
    }

    /**
     * Accepts common user formats:
     * - "1000" -> "1000"
     * - "1.000" (thousands) -> "1000"
     * - "1000,50" (es_AR decimal) -> "1000.50"
     * - "1.000,50" -> "1000.50"
     *
     * Returns null when invalid (non-empty input but not parseable).
     */
    private static String normalizePriceInput(String raw) {
        if (raw == null) return null;
        String value = raw.trim().replace(" ", "");
        if (value.isEmpty()) return null;

        // es_AR: thousands "." and decimal ","
        if (value.contains(",")) {
            value = value.replace(".", "");
            value = value.replace(",", ".");
        } else {
            // If it's only thousands grouping (e.g., 1.000 or 10.000.000), remove dots.
            if (value.matches("^\\d{1,3}(\\.\\d{3})+$")) {
                value = value.replace(".", "");
            }
        }

        return value.matches("^\\d+(\\.\\d+)?$") ? value : null;
    }

    private static String toCategoryLabel(String enumValue) {
        if (enumValue == null) return "";
        String lower = enumValue.toLowerCase(Locale.US).replace("_", " ");
        if (lower.isEmpty()) return "";
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
