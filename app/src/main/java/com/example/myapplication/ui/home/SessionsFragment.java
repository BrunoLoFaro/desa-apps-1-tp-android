package com.example.myapplication.ui.home;

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
import com.example.myapplication.data.model.TourActivity;
import com.example.myapplication.ui.home.viewmodel.CreateBookingViewModel;
import com.example.myapplication.ui.home.viewmodel.DetailViewModel;
import com.google.android.material.appbar.MaterialToolbar;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SessionsFragment extends Fragment {

    private TourActivity tourActivity;
    private DetailViewModel detailViewModel;
    private CreateBookingViewModel createBookingViewModel;
    private SessionAdapter sessionAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            tourActivity = (TourActivity) getArguments().getSerializable("activity_data");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sessions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(view).navigateUp());

        RecyclerView recyclerView = view.findViewById(R.id.sessions_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        ProgressBar loading = view.findViewById(R.id.loading_spinner);

        sessionAdapter = new SessionAdapter(session -> {
            // Reserva directa con 1 participante
            createBookingViewModel.create(session.id, 1);
        });
        recyclerView.setAdapter(sessionAdapter);

        detailViewModel = new ViewModelProvider(this).get(DetailViewModel.class);
        createBookingViewModel = new ViewModelProvider(this).get(CreateBookingViewModel.class);

        createBookingViewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (loading != null) {
                loading.setVisibility(Boolean.TRUE.equals(isLoading) ? View.VISIBLE : View.GONE);
            }
        });

        createBookingViewModel.getBooking().observe(getViewLifecycleOwner(), booking -> {
            if (booking != null) {
                Toast.makeText(requireContext(), "Reserva creada con éxito", Toast.LENGTH_SHORT).show();
                createBookingViewModel.clearBooking();
                // Navegar de vuelta al home o mis reservas
                Navigation.findNavController(requireView()).navigate(R.id.action_homeFragment_to_bookingsFragment);
            }
        });

        createBookingViewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error.resolve(requireContext()), Toast.LENGTH_SHORT).show();
            }
        });

        if (tourActivity != null && tourActivity.getId() != null) {
            detailViewModel.getSessions().observe(getViewLifecycleOwner(), sessions -> {
                sessionAdapter.updateData(sessions);
            });
            detailViewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
                if (createBookingViewModel.isLoading().getValue() != Boolean.TRUE) {
                    loading.setVisibility(Boolean.TRUE.equals(isLoading) ? View.VISIBLE : View.GONE);
                }
            });
            detailViewModel.load(tourActivity.getId());
        }
    }
}
