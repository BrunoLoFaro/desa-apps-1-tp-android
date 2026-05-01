package com.example.myapplication.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import com.example.myapplication.ui.home.viewmodel.HomeViewModel;
import com.example.myapplication.ui.home.viewmodel.NewsViewModel;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.stream.Collectors;

@AndroidEntryPoint
public class HomeFragment extends androidx.fragment.app.Fragment {

    private HomeViewModel homeViewModel;
    private NewsViewModel newsViewModel;
    private NewsAdapter newsAdapter;
    private PromotionsAdapter promotionsAdapter;
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
        newsViewModel = new ViewModelProvider(this).get(NewsViewModel.class);
        navController = Navigation.findNavController(view);

        if (!homeViewModel.hasValidSession()) {
            navController.navigate(R.id.action_homeFragment_to_loginFragment);
            return;
        }

        View searchBarCard = view.findViewById(R.id.search_bar_card);
        EditText searchEditText = view.findViewById(R.id.search_edit_text);
        View.OnClickListener openExplore = v -> {
            if (navController.getCurrentDestination() != null
                    && navController.getCurrentDestination().getId() == R.id.exploreFragment) {
                return;
            }
            navController.navigate(R.id.exploreFragment);
        };
        if (searchBarCard != null) searchBarCard.setOnClickListener(openExplore);
        if (searchEditText != null) {
            searchEditText.setOnClickListener(openExplore);
        }

        RecyclerView featuredRecycler = view.findViewById(R.id.featured_recycler_view);
        featuredRecycler.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        TourAdapter featuredAdapter = new TourAdapter(true, true);
        featuredRecycler.setAdapter(featuredAdapter);

        featuredAdapter.setOnFavoriteToggleListener((activity, targetFavorite) -> {
            if (activity.getId() == null) return;
            homeViewModel.toggleFavorite(activity.getId(), targetFavorite, null);
        });

        homeViewModel.getFeaturedTours().observe(getViewLifecycleOwner(), featuredAdapter::updateData);

        RecyclerView newsRecycler = view.findViewById(R.id.news_recycler_view);
        newsRecycler.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        newsAdapter = new NewsAdapter();
        newsRecycler.setAdapter(newsAdapter);

        RecyclerView promotionsRecycler = view.findViewById(R.id.promotions_recycler_view);
        promotionsRecycler.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        promotionsAdapter = new PromotionsAdapter();
        promotionsRecycler.setAdapter(promotionsAdapter);

        newsViewModel.getNewsList().observe(getViewLifecycleOwner(), newsList -> {
            if (newsList != null) {
                newsAdapter.updateData(newsList.stream()
                        .filter(item -> "NEWS".equals(item.type))
                        .toList());
                promotionsAdapter.updateData(newsList.stream()
                        .filter(item -> "OFFER".equals(item.type))
                        .toList());
            }
        });
        newsViewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error.resolve(requireContext()), Toast.LENGTH_SHORT).show();
            }
        });

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
        if (homeViewModel != null && homeViewModel.hasValidSession()) {
            homeViewModel.refreshTours();
        }
        if (newsViewModel != null) {
            newsViewModel.loadNews(0, 10);
        }
    }

    @Override
    public void onDestroyView() {
        navController = null;
        homeViewModel = null;
        newsViewModel = null;
        newsAdapter = null;
        promotionsAdapter = null;
        super.onDestroyView();
    }
}
