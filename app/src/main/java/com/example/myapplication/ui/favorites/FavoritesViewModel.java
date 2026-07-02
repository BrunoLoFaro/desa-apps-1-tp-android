package com.example.myapplication.ui.favorites;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.myapplication.data.common.RepositoryCallback;
import com.example.myapplication.data.common.UiMessage;
import com.example.myapplication.data.model.TourActivity;
import com.example.myapplication.data.repository.TourRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import android.util.Log;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class FavoritesViewModel extends ViewModel {

    private static final Set<Long> priceChangedSeen = new HashSet<>();
    private static final Set<Long> priceChangedDone = new HashSet<>();
    private static final Map<Long, String> priceChangedDismissed = new HashMap<>(); // id → price at dismissal
    private static final Set<Long> slotsChangedSeen = new HashSet<>();
    private static final Set<Long> slotsChangedDone = new HashSet<>();
    private static final Map<Long, Integer> slotsChangedDismissed = new HashMap<>(); // id → slots at dismissal

    private final TourRepository tourRepository;
    private final MutableLiveData<List<TourActivity>> _favorites = new MutableLiveData<>();
    public LiveData<List<TourActivity>> favorites = _favorites;
    private final MutableLiveData<UiMessage> _error = new MutableLiveData<>();
    public LiveData<UiMessage> error = _error;
    private final MutableLiveData<Boolean> _loading = new MutableLiveData<>(false);
    public LiveData<Boolean> loading = _loading;

    @Inject
    public FavoritesViewModel(TourRepository tourRepository) {
        this.tourRepository = tourRepository;
    }

    public void loadFavorites() {
        _loading.setValue(true);
        Log.d("FavoritesViewModel", "Loading favorites...");
        tourRepository.getFavorites(new RepositoryCallback<List<TourActivity>>() {
            @Override
            public void onSuccess(List<TourActivity> data) {
                Log.d("FavoritesViewModel", "Favorites loaded: " + (data != null ? data.size() : 0));
                List<TourActivity> list = data != null ? data : new ArrayList<>();
                for (TourActivity item : list) {
                    processChipVisibility(item);
                }
                _favorites.setValue(list);
                _loading.setValue(false);
            }

            @Override
            public void onError(UiMessage error) {
                Log.e("FavoritesViewModel", "Error loading favorites");
                _error.setValue(error);
                _loading.setValue(false);
            }
        });
    }

    private void processChipVisibility(TourActivity item) {
        if (item.getId() == null) return;
        long id = item.getId();
        if (item.isPriceChanged()) {
            String dismissedAtPrice = priceChangedDismissed.get(id);
            if (dismissedAtPrice != null) {
                if (!dismissedAtPrice.equals(item.getPrice())) {
                    // New price change after previous dismissal — treat as new
                    priceChangedDismissed.remove(id);
                    priceChangedSeen.add(id);
                } else {
                    item.setPriceChanged(false);
                }
            } else if (priceChangedDone.contains(id)) {
                item.setPriceChanged(false);
                priceChangedDismissed.put(id, item.getPrice());
                priceChangedDone.remove(id);
                priceChangedSeen.remove(id);
            } else if (priceChangedSeen.contains(id)) {
                priceChangedDone.add(id);
            } else {
                priceChangedSeen.add(id);
            }
        } else {
            priceChangedSeen.remove(id);
            priceChangedDone.remove(id);
            priceChangedDismissed.remove(id);
        }

        Log.d("ChipDebug", "slots → id=" + id + " slotsChanged=" + item.isSlotsChanged()
                + " availableSlots=" + item.getAvailableSlots()
                + " dismissedAtSlots=" + slotsChangedDismissed.get(id)
                + " inSlotsSeen=" + slotsChangedSeen.contains(id)
                + " inSlotsDone=" + slotsChangedDone.contains(id));

        if (item.isSlotsChanged()) {
            Integer dismissedAtSlots = slotsChangedDismissed.get(id);
            if (dismissedAtSlots != null) {
                if (dismissedAtSlots != item.getAvailableSlots()) {
                    // Slots changed again after previous dismissal — treat as new
                    slotsChangedDismissed.remove(id);
                    slotsChangedSeen.add(id);
                } else {
                    item.setSlotsChanged(false);
                }
            } else if (slotsChangedDone.contains(id)) {
                item.setSlotsChanged(false);
                slotsChangedDismissed.put(id, item.getAvailableSlots());
                slotsChangedDone.remove(id);
                slotsChangedSeen.remove(id);
            } else if (slotsChangedSeen.contains(id)) {
                slotsChangedDone.add(id);
            } else {
                slotsChangedSeen.add(id);
            }
        } else {
            slotsChangedSeen.remove(id);
            slotsChangedDone.remove(id);
            slotsChangedDismissed.remove(id);
        }
    }

    public void toggleFavorite(long activityId, boolean isFavorite) {
        List<TourActivity> current = _favorites.getValue() != null
                ? new ArrayList<>(_favorites.getValue())
                : new ArrayList<>();

        TourActivity affectedItem = null;
        for (TourActivity item : current) {
            if (item.getId() != null && item.getId() == activityId) {
                affectedItem = item;
                break;
            }
        }
        final TourActivity itemToRestore = affectedItem;
        final boolean previousFavoriteState = !isFavorite;
        final List<TourActivity> rollback = new ArrayList<>(current);

        if (!isFavorite) {
            current.removeIf(item -> item.getId() != null && item.getId() == activityId);
            Log.d("FavoritesViewModel", "Removing favorite: " + activityId);
        }
        _favorites.setValue(current);

        tourRepository.toggleFavorite(activityId, isFavorite, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                Log.d("FavoritesViewModel", "Toggle favorite successful, reloading");
                loadFavorites();
            }

            @Override
            public void onError(UiMessage error) {
                Log.e("FavoritesViewModel", "Error toggling favorite");
                if (itemToRestore != null) {
                    itemToRestore.setFavorite(previousFavoriteState);
                }
                _favorites.setValue(rollback);
                _error.setValue(error);
            }
        });
    }
}