package com.example.myapplication;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.data.model.TourActivity;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        setupFeaturedList();
        setupAllActivitiesList();
    }

    private void setupFeaturedList() {
        RecyclerView featuredRecycler = findViewById(R.id.featured_recycler_view);
        List<TourActivity> featured = new ArrayList<>();
        featured.add(new TourActivity("Tour Gastronómico", "Buenos Aires", "Gastronomía", "3 horas", "$45.00", 5, ""));
        featured.add(new TourActivity("Excursión a Tigre", "Delta del Tigre", "Excursión", "6 horas", "$80.00", 2, ""));
        
        TourAdapter adapter = new TourAdapter(featured, true);
        featuredRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        featuredRecycler.setAdapter(adapter);
    }

    private void setupAllActivitiesList() {
        RecyclerView activitiesRecycler = findViewById(R.id.activities_recycler_view);
        List<TourActivity> all = new ArrayList<>();
        all.add(new TourActivity("Free Tour Recoleta", "Buenos Aires", "Free Tour", "2 horas", "Gratis", 10, ""));
        all.add(new TourActivity("Visita al Teatro Colón", "Buenos Aires", "Visita Guiada", "1 hora", "$25.00", 8, ""));
        all.add(new TourActivity("Show de Tango", "San Telmo", "Experiencia", "4 horas", "$120.00", 15, ""));
        all.add(new TourActivity("Clase de Cocina Criolla", "Palermo", "Gastronomía", "3 horas", "$60.00", 4, ""));

        TourAdapter adapter = new TourAdapter(all, false);
        activitiesRecycler.setLayoutManager(new LinearLayoutManager(this));
        activitiesRecycler.setAdapter(adapter);
    }
}
