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
        featured.add(new TourActivity(
                "Navegación por el Delta", "Tigre, Buenos Aires", "Aventura", "6 horas", "$85.00", 2,
                "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=800",
                "Disfrutá de un día inolvidable navegando por los canales del Delta. Conocé la flora y fauna local mientras te relajás con el sonido del agua.",
                4.9f, 124, "Equipos de seguridad, Almuerzo criollo y Traslados.", "Estación Fluvial de Tigre, Muelle 4",
                "Juan Pérez", "Español e Inglés", "Cancelación gratuita 24hs antes", null
        ));
        featured.add(new TourActivity(
                "Tour Gastronómico", "Buenos Aires", "Gastronomía", "3 horas", "$45.00", 5,
                "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=500",
                "Disfrutá de los mejores sabores porteños en un recorrido por bodegones históricos.",
                4.8f, 85, "Degustación de 3 platos, bebida y postre.", "Plaza de Mayo",
                "Carlos Gómez", "Español", "Cancelación gratuita 24hs antes", null
        ));
        return featured;
    }

    public List<TourActivity> getAllTours() {
        List<TourActivity> all = new ArrayList<>();
        all.add(new TourActivity(
                "Free Tour Recoleta", "Buenos Aires", "Free Tour", "2 horas", "Gratis", 10,
                "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=500",
                "Conocé la historia del barrio más elegante de Buenos Aires.",
                4.7f, 250, "Recorrido guiado.", "Cementerio de la Recoleta",
                "Ana Torres", "Español", "Cancelación libre", null
        ));
        all.add(new TourActivity(
                "Visita al Teatro Colón", "Buenos Aires", "Visita Guiada", "1 hora", "$25.00", 8,
                "https://images.unsplash.com/photo-1552832230-c0197dd311b5?w=500",
                "Recorré uno de los teatros de ópera más importantes del mundo.",
                5.0f, 500, "Entrada al teatro y guía oficial.", "Entrada principal Teatro Colón",
                "Personal del Teatro", "Multilingüe", "Sujeto a disponibilidad", null
        ));
        all.add(new TourActivity(
                "Show de Tango", "San Telmo", "Experiencia", "4 horas", "$120.00", 15,
                "https://images.unsplash.com/photo-1558618666-fcd25c85f82e?w=500",
                "Viví una noche de tango auténtico en el corazón de San Telmo.",
                4.6f, 310, "Cena show, bebida incluida.", "Esquina de San Telmo",
                "Pareja Rodríguez", "Español", "Cancelación gratuita 48hs antes", null
        ));
        all.add(new TourActivity(
                "Clase de Cocina Criolla", "Palermo", "Gastronomía", "3 horas", "$60.00", 4,
                "https://images.unsplash.com/photo-1556910103-1c02745aae4d?w=500",
                "Aprendé a preparar platos típicos argentinos con un chef profesional.",
                4.5f, 78, "Ingredientes, delantal y recetario.", "Mercado de Palermo",
                "Chef Martínez", "Español e Inglés", "Cancelación gratuita 24hs antes", null
        ));
        return all;
    }
}
