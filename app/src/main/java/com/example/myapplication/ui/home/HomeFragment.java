package com.example.myapplication.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import com.example.myapplication.ui.home.viewmodel.HomeViewModel;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import com.google.android.material.appbar.MaterialToolbar;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class HomeFragment extends androidx.fragment.app.Fragment {

    private HomeViewModel homeViewModel;
    private NavController navController;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        navController = Navigation.findNavController(view);

        if (!homeViewModel.hasValidSession()) {
            navController.navigate(R.id.action_homeFragment_to_loginFragment);
            return;
        }

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(this::onMenuItemClick);

        RecyclerView featuredRecycler = view.findViewById(R.id.featured_recycler_view);
        featuredRecycler.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        // Recomendados para ti (featured) -> Horizontal and Compact
        TourAdapter featuredAdapter = new TourAdapter(true, true);
        featuredRecycler.setAdapter(featuredAdapter);

        RecyclerView activitiesRecycler = view.findViewById(R.id.activities_recycler_view);
        activitiesRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        // Explora actividades -> Vertical and Compact (Requirement 3)
        TourAdapter activitiesAdapter = new TourAdapter(false, true);
        activitiesRecycler.setAdapter(activitiesAdapter);

        homeViewModel.getFeaturedTours().observe(getViewLifecycleOwner(), featuredAdapter::updateData);
        homeViewModel.getAllTours().observe(getViewLifecycleOwner(), activitiesAdapter::updateData);

        View scrollView = view.findViewById(R.id.scroll_view);
        ProgressBar loadingSpinner = view.findViewById(R.id.loading_spinner);
        homeViewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            loadingSpinner.setVisibility(loading ? View.VISIBLE : View.GONE);
            scrollView.setVisibility(loading ? View.GONE : View.VISIBLE);
        });

        homeViewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error.resolve(requireContext()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (homeViewModel != null) {
            homeViewModel.reloadRecommended();
        }
    }

    private boolean onMenuItemClick(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            logout();
            return true;
        } else if (id == R.id.action_theme_toggle) {
            toggleTheme();
            return true;
        }
        return false;
    }

    private void toggleTheme() {
        int currentMode = androidx.appcompat.app.AppCompatDelegate.getDefaultNightMode();
        if (currentMode == androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                    androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                    androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
        }
    }

    private void logout() {
        homeViewModel.logout();
        navController.navigate(R.id.action_homeFragment_to_loginFragment);
    }

    @Override
    public void onDestroyView() {
        navController = null;
        homeViewModel = null;
        super.onDestroyView();
    }
}
