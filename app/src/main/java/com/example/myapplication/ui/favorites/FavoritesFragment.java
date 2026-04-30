package com.example.myapplication.ui.favorites;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class FavoritesFragment extends Fragment {

    private View root;
    private RecyclerView recyclerViewFavorites;
    private LinearLayout emptyView;
    private ProgressBar progressBar;
    private FavoritesViewModel viewModel;
    private FavoritesAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_favorites, container, false);
        recyclerViewFavorites = root.findViewById(R.id.recycler_view_favorites);
        emptyView = root.findViewById(R.id.empty_view);
        progressBar = root.findViewById(R.id.progress_bar);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(FavoritesViewModel.class);

        setupRecyclerView();
        observeViewModel();
    }

    private void setupRecyclerView() {
        adapter = new FavoritesAdapter(activity -> viewModel.toggleFavorite(activity.getId(), false));
        recyclerViewFavorites.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewFavorites.setAdapter(adapter);
    }

    private void observeViewModel() {
        viewModel.favorites.observe(getViewLifecycleOwner(), favorites -> {
            if (favorites != null && !favorites.isEmpty()) {
                adapter.updateData(favorites);
                recyclerViewFavorites.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
            } else {
                recyclerViewFavorites.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
            }
        });

        viewModel.loading.observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        viewModel.error.observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error.resolve(getContext()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        root = null;
        recyclerViewFavorites = null;
        emptyView = null;
        progressBar = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.loadFavorites();
        }
    }
}
