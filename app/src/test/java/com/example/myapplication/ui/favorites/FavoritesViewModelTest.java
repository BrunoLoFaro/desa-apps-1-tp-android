package com.example.myapplication.ui.favorites;

/*
 * CASOS DE PRUEBA — Sección Favoritos (para reviewers)
 * =====================================================
 *
 * PRUEBAS MANUALES
 * ----------------
 * [ ] Abrir la sección Favoritos → se muestra el spinner de carga y luego los ítems.
 * [ ] Sin favoritos → se muestra el estado vacío ("No tienes favoritos").
 * [ ] Quitar un favorito desde la lista → el ítem desaparece de inmediato (optimista)
 *     y no vuelve si la operación es exitosa.
 * [ ] Simular error de red al quitar favorito → el ítem reaparece con el corazón relleno.
 * [ ] Navegar a otra sección y volver a Favoritos → la lista se refresca (solo 1 request).
 * [ ] Rotar pantalla → la lista no se recarga desde el servidor (ViewModel sobrevive).
 * [ ] Agregar un favorito desde Explorar y volver → aparece en la lista al reentrar.
 * [ ] Actividad sin cupos → chip "Sin cupos" visible (rojo), botón Reservar deshabilitado.
 * [ ] Actividad con precio cambiado → chip "Nuevo precio" visible.
 * [ ] Actividad con cupos liberados → chip "Se liberaron cupos" visible.
 * [ ] Precio y cupos cambiados al mismo tiempo → ambos chips visibles (nunca "Sin cupos").
 * [ ] Actividad sin cupos + precio cambiado → solo chip "Sin cupos", no "Nuevo precio".
 *
 * PRUEBAS AUTOMATIZADAS (ver métodos @Test abajo)
 * ------------------------------------------------
 * [x] constructor no dispara getFavorites
 * [x] loadFavorites activa el estado de carga
 * [x] loadFavorites con éxito actualiza la lista de favoritos
 * [x] loadFavorites con body null retorna lista vacía (no crash)
 * [x] loadFavorites con error setea el mensaje de error y apaga el loading
 * [x] toggleFavorite(remove) elimina el ítem de forma optimista
 * [x] toggleFavorite error restaura la lista completa (rollback)
 * [x] toggleFavorite error restaura el flag isFavorite del ítem
 * [x] toggleFavorite éxito dispara una recarga desde el repositorio
 * [x] backend hasPriceChange=true → priceChanged preservado en favoritos
 * [x] backend hasAvailabilityChange=true → slotsChanged preservado en favoritos
 * [x] priceChanged y slotsChanged simultáneos → ambos flags preservados
 * [x] sin cambios → ambos flags false
 */

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.myapplication.data.common.RepositoryCallback;
import com.example.myapplication.data.common.UiMessage;
import com.example.myapplication.data.model.TourActivity;
import com.example.myapplication.data.repository.TourRepository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

public class FavoritesViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private TourRepository tourRepository;

    private FavoritesViewModel viewModel;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        viewModel = new FavoritesViewModel(tourRepository);
    }

    // ── carga básica ───────────────────────────────────────────────────────────

    @Test
    public void constructor_doesNotTriggerGetFavorites() {
        verify(tourRepository, never()).getFavorites(any());
    }

    @Test
    public void loadFavorites_setsLoadingTrue() {
        viewModel.loadFavorites();
        assertTrue(Boolean.TRUE.equals(viewModel.loading.getValue()));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void loadFavorites_onSuccess_updatesFavoritesAndClearsLoading() {
        TourActivity activity = makeFavorite(1L, "Tour A", true);
        ArgumentCaptor<RepositoryCallback<List<TourActivity>>> captor =
                ArgumentCaptor.forClass(RepositoryCallback.class);

        viewModel.loadFavorites();
        verify(tourRepository).getFavorites(captor.capture());
        captor.getValue().onSuccess(Arrays.asList(activity));

        List<TourActivity> result = viewModel.favorites.getValue();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Tour A", result.get(0).getName());
        assertFalse(Boolean.TRUE.equals(viewModel.loading.getValue()));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void loadFavorites_onSuccessWithNullBody_setsEmptyList() {
        ArgumentCaptor<RepositoryCallback<List<TourActivity>>> captor =
                ArgumentCaptor.forClass(RepositoryCallback.class);

        viewModel.loadFavorites();
        verify(tourRepository).getFavorites(captor.capture());
        captor.getValue().onSuccess(null);

        assertNotNull(viewModel.favorites.getValue());
        assertTrue(viewModel.favorites.getValue().isEmpty());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void loadFavorites_onError_setsErrorAndClearsLoading() {
        ArgumentCaptor<RepositoryCallback<List<TourActivity>>> captor =
                ArgumentCaptor.forClass(RepositoryCallback.class);
        UiMessage errorMsg = UiMessage.from("Error de red");

        viewModel.loadFavorites();
        verify(tourRepository).getFavorites(captor.capture());
        captor.getValue().onError(errorMsg);

        assertNotNull(viewModel.error.getValue());
        assertFalse(Boolean.TRUE.equals(viewModel.loading.getValue()));
    }

    // ── toggle favorito ────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    @Test
    public void toggleFavorite_remove_optimisticallyRemovesItemFromList() {
        seedFavorites(makeFavorite(10L, "Tour X", true));

        viewModel.toggleFavorite(10L, false);

        List<TourActivity> current = viewModel.favorites.getValue();
        assertNotNull(current);
        assertTrue(current.isEmpty());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void toggleFavorite_onError_rollsBackList() {
        seedFavorites(makeFavorite(10L, "Tour X", true));

        ArgumentCaptor<RepositoryCallback<Void>> captor =
                ArgumentCaptor.forClass(RepositoryCallback.class);

        viewModel.toggleFavorite(10L, false);
        verify(tourRepository).toggleFavorite(eq(10L), eq(false), captor.capture());
        captor.getValue().onError(UiMessage.from("Error"));

        List<TourActivity> rolledBack = viewModel.favorites.getValue();
        assertNotNull(rolledBack);
        assertEquals(1, rolledBack.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void toggleFavorite_onError_restoresFavoriteFlagOnItem() {
        TourActivity activity = makeFavorite(10L, "Tour X", true);
        activity.setFavorite(false);
        seedFavorites(activity);

        ArgumentCaptor<RepositoryCallback<Void>> captor =
                ArgumentCaptor.forClass(RepositoryCallback.class);

        viewModel.toggleFavorite(10L, false);
        verify(tourRepository).toggleFavorite(eq(10L), eq(false), captor.capture());
        captor.getValue().onError(UiMessage.from("Error"));

        assertTrue(activity.isFavorite());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void toggleFavorite_onSuccess_reloadsFavoritesFromRepository() {
        seedFavorites(makeFavorite(10L, "Tour X", true));

        ArgumentCaptor<RepositoryCallback<Void>> captor =
                ArgumentCaptor.forClass(RepositoryCallback.class);

        viewModel.toggleFavorite(10L, false);
        verify(tourRepository).toggleFavorite(eq(10L), eq(false), captor.capture());
        captor.getValue().onSuccess(null);

        verify(tourRepository, times(2)).getFavorites(any());
    }

    // ── flags de cambio (vienen del backend, pasados por el repositorio) ───────

    @SuppressWarnings("unchecked")
    @Test
    public void loadFavorites_priceChangedFlag_preservedInResult() {
        TourActivity activity = makeFavorite(1L, "Tour A", true);
        activity.setPriceChanged(true);
        ArgumentCaptor<RepositoryCallback<List<TourActivity>>> captor =
                ArgumentCaptor.forClass(RepositoryCallback.class);

        viewModel.loadFavorites();
        verify(tourRepository).getFavorites(captor.capture());
        captor.getValue().onSuccess(Arrays.asList(activity));

        assertTrue(viewModel.favorites.getValue().get(0).isPriceChanged());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void loadFavorites_slotsChangedFlag_preservedInResult() {
        TourActivity activity = makeFavorite(1L, "Tour A", true);
        activity.setSlotsChanged(true);
        ArgumentCaptor<RepositoryCallback<List<TourActivity>>> captor =
                ArgumentCaptor.forClass(RepositoryCallback.class);

        viewModel.loadFavorites();
        verify(tourRepository).getFavorites(captor.capture());
        captor.getValue().onSuccess(Arrays.asList(activity));

        assertTrue(viewModel.favorites.getValue().get(0).isSlotsChanged());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void loadFavorites_bothFlagsChanged_bothPreserved() {
        TourActivity activity = makeFavorite(1L, "Tour A", true);
        activity.setPriceChanged(true);
        activity.setSlotsChanged(true);
        activity.setFavoriteUpdate(true);
        ArgumentCaptor<RepositoryCallback<List<TourActivity>>> captor =
                ArgumentCaptor.forClass(RepositoryCallback.class);

        viewModel.loadFavorites();
        verify(tourRepository).getFavorites(captor.capture());
        captor.getValue().onSuccess(Arrays.asList(activity));

        TourActivity result = viewModel.favorites.getValue().get(0);
        assertTrue(result.isPriceChanged());
        assertTrue(result.isSlotsChanged());
        assertTrue(result.hasFavoriteUpdate());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void loadFavorites_noChanges_flagsRemainFalse() {
        TourActivity activity = makeFavorite(1L, "Tour A", true);
        ArgumentCaptor<RepositoryCallback<List<TourActivity>>> captor =
                ArgumentCaptor.forClass(RepositoryCallback.class);

        viewModel.loadFavorites();
        verify(tourRepository).getFavorites(captor.capture());
        captor.getValue().onSuccess(Arrays.asList(activity));

        TourActivity result = viewModel.favorites.getValue().get(0);
        assertFalse(result.isPriceChanged());
        assertFalse(result.isSlotsChanged());
        assertFalse(result.hasFavoriteUpdate());
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private TourActivity makeFavorite(long id, String name, boolean isFavorite) {
        TourActivity activity = new TourActivity(name, "Destino", "Aventura",
                "2h", "1000", 10, null);
        activity.setId(id);
        activity.setFavorite(isFavorite);
        return activity;
    }

    @SuppressWarnings("unchecked")
    private void seedFavorites(TourActivity... activities) {
        ArgumentCaptor<RepositoryCallback<List<TourActivity>>> captor =
                ArgumentCaptor.forClass(RepositoryCallback.class);
        viewModel.loadFavorites();
        verify(tourRepository, atLeastOnce()).getFavorites(captor.capture());
        captor.getValue().onSuccess(Arrays.asList(activities));
    }
}