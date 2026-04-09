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
        
        featured.add(new TourActivity(
            "Navegación por el Delta", "Tigre, Buenos Aires", "Aventura", "6 horas", "$85.00", 2,
            "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=800",
            "Disfruta de un día inolvidable navegando por los canales del Delta. Conoce la flora y fauna local mientras te relajas con el sonido del agua. Ideal para desconectar de la ciudad.",
            4.9f, 124, "Equipos de seguridad, Almuerzo criollo y Traslados.", "Estación Fluvial de Tigre, Muelle 4",
            "Juan Pérez", "Español e Inglés", "Cancelación gratuita 24hs antes", null
        ));

        featured.add(new TourActivity(
            "Tour Gastronómico", "Buenos Aires", "Gastronomía", "3 horas", "$45.00", 5, 
            "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=500",
            "Disfruta de los mejores sabores porteños en un recorrido por bodegones históricos.",
            4.8f, 85, "Degustación de 3 platos, bebida y postre.", "Plaza de Mayo", 
            "Carlos Gómez", "Español", "Cancelación gratuita 24hs antes", null
        ));
        
        TourAdapter adapter = new TourAdapter(featured, true);
        featuredRecycler.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        featuredRecycler.setAdapter(adapter);
    }

    private void setupAllActivitiesList(View view) {
        RecyclerView activitiesRecycler = view.findViewById(R.id.activities_recycler_view);
        List<TourActivity> all = new ArrayList<>();
        all.add(new TourActivity(
            "Free Tour Recoleta", "Buenos Aires", "Free Tour", "2 horas", "Gratis", 10,
            "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=500",
            "Conoce la historia del barrio más elegante de Buenos Aires.",
            4.7f, 250, "Recorrido guiado.", "Cementerio de la Recoleta",
            "Ana Torres", "Español", "Cancelación libre", null
        ));
        all.add(new TourActivity(
            "Visita al Teatro Colón", "Buenos Aires", "Visita Guiada", "1 hora", "$25.00", 8,
            "https://images.unsplash.com/photo-1552832230-c0197dd311b5?w=500",
            "Recorre uno de los teatros de ópera más importantes del mundo.",
            5.0f, 500, "Entrada al teatro y guía oficial.", "Entrada principal Teatro Colón",
            "Personal del Teatro", "Multilingüe", "Sujeto a disponibilidad", null
        ));

        TourAdapter adapter = new TourAdapter(all, true);
        activitiesRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        activitiesRecycler.setAdapter(adapter);
    }
}
