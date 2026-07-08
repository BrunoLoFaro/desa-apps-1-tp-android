package com.example.myapplication.ui.home;

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
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.myapplication.R;
import com.example.myapplication.ui.home.viewmodel.NewsViewModel;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class NewsFragment extends Fragment {

    private NewsViewModel newsViewModel;
    private NewsAdapter newsAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private LinearLayout emptyView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_news, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        newsViewModel = new ViewModelProvider(this).get(NewsViewModel.class);
        NavController navController = Navigation.findNavController(view);

        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        RecyclerView recyclerView = view.findViewById(R.id.news_recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyView = view.findViewById(R.id.empty_view);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        newsAdapter = new NewsAdapter();
        recyclerView.setAdapter(newsAdapter);

        newsAdapter.setOnNewsClickListener(news -> {
            Bundle bundle = new Bundle();
            bundle.putLong("news_id", news.id);
            if (news.relatedActivityId != null) {
                bundle.putLong("related_activity_id", news.relatedActivityId);
            }
            navController.navigate(R.id.newsDetailFragment, bundle);
        });

        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadNews();
        });

        newsViewModel.getNewsList().observe(getViewLifecycleOwner(), newsItems -> {
            newsAdapter.updateData(newsItems);
            swipeRefreshLayout.setRefreshing(false);
            
            if (newsItems == null || newsItems.isEmpty()) {
                emptyView.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                emptyView.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        });

        newsViewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error.resolve(requireContext()), Toast.LENGTH_SHORT).show();
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        newsViewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            if (!loading) {
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        loadNews();
    }

    private void loadNews() {
        newsViewModel.loadNews(0, 20);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (newsViewModel != null) {
            loadNews();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        newsViewModel = null;
        newsAdapter = null;
    }
}
