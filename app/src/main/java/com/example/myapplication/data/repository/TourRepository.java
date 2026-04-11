package com.example.myapplication.data.repository;

import com.example.myapplication.data.model.TourActivity;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Single source of truth for tour/activity data.
 * Currently returns static data; ready to be wired to an API endpoint.
 */
@Singleton
public class TourRepository {

    @Inject
    public TourRepository() {}

    public List<TourActivity> getFeaturedTours() {
        List<TourActivity> featured = new ArrayList<>();
        featured.add(new TourActivity("Tour Gastronómico", "Buenos Aires", "Gastronomía", "3 horas", "$45.00", 5, ""));
        featured.add(new TourActivity("Excursión a Tigre", "Delta del Tigre", "Excursión", "6 horas", "$80.00", 2, ""));
        return featured;
    }

    public List<TourActivity> getAllTours() {
        List<TourActivity> all = new ArrayList<>();
        all.add(new TourActivity("Free Tour Recoleta", "Buenos Aires", "Free Tour", "2 horas", "Gratis", 10, ""));
        all.add(new TourActivity("Visita al Teatro Colón", "Buenos Aires", "Visita Guiada", "1 hora", "$25.00", 8, ""));
        all.add(new TourActivity("Show de Tango", "San Telmo", "Experiencia", "4 horas", "$120.00", 15, ""));
        all.add(new TourActivity("Clase de Cocina Criolla", "Palermo", "Gastronomía", "3 horas", "$60.00", 4, ""));
        return all;
    }
}
