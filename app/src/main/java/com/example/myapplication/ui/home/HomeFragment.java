package com.example.myapplication.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.TourAdapter;
import com.example.myapplication.data.model.TourActivity;
import com.example.myapplication.data.session.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private SessionManager sessionManager;
    private NavController navController;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        navController = Navigation.findNavController(view);

        // Verificación de sesión
        if (!sessionManager.hasValidSession()) {
            navController.navigate(R.id.action_homeFragment_to_loginFragment);
            return;
        }

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(this::onMenuItemClick);

        setupFeaturedList(view);
        setupAllActivitiesList(view);
    }

    private boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logout();
            return true;
        }
        return false;
    }

    private void logout() {
        sessionManager.clearSession();
        navController.navigate(R.id.action_homeFragment_to_loginFragment);
    }

    private void setupFeaturedList(View view) {
        RecyclerView featuredRecycler = view.findViewById(R.id.featured_recycler_view);
        List<TourActivity> featured = new ArrayList<>();
        featured.add(new TourActivity("Tour Gastronómico", "Buenos Aires", "Gastronomía", "3 horas", "$45.00", 5, ""));
        featured.add(new TourActivity("Excursión a Tigre", "Delta del Tigre", "Excursión", "6 horas", "$80.00", 2, ""));
        
        TourAdapter adapter = new TourAdapter(featured, true);
        featuredRecycler.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        featuredRecycler.setAdapter(adapter);
    }

    private void setupAllActivitiesList(View view) {
        RecyclerView activitiesRecycler = view.findViewById(R.id.activities_recycler_view);
        List<TourActivity> all = new ArrayList<>();
        all.add(new TourActivity("Free Tour Recoleta", "Buenos Aires", "Free Tour", "2 horas", "Gratis", 10, ""));
        all.add(new TourActivity("Visita al Teatro Colón", "Buenos Aires", "Visita Guiada", "1 hora", "$25.00", 8, ""));
        all.add(new TourActivity("Show de Tango", "San Telmo", "Experiencia", "4 horas", "$120.00", 15, ""));
        all.add(new TourActivity("Clase de Cocina Criolla", "Palermo", "Gastronomía", "3 horas", "$60.00", 4, ""));

        TourAdapter adapter = new TourAdapter(all, false);
        activitiesRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        activitiesRecycler.setAdapter(adapter);
    }
}
