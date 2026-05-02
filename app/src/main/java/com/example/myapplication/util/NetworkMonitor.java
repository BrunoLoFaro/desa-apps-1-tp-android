package com.example.myapplication.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import dagger.hilt.android.qualifiers.ApplicationContext;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Singleton reactive wrapper over ConnectivityManager.
 * Inject this instead of calling ConnectivityUtils or registering callbacks per-fragment.
 * Observe {@link #isOnline()} from any ViewModel or Activity to react to network changes.
 */
@Singleton
public class NetworkMonitor {

    private final MutableLiveData<Boolean> _isOnline = new MutableLiveData<>();

    @Inject
    public NetworkMonitor(@ApplicationContext Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        // Set initial value synchronously so observers don't start blind
        _isOnline.setValue(ConnectivityUtils.isOnline(context));

        if (cm == null) return;

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        cm.registerNetworkCallback(request, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                _isOnline.postValue(true);
            }

            @Override
            public void onLost(Network network) {
                // Recheck: another network might still be available
                _isOnline.postValue(ConnectivityUtils.isOnline(context));
            }

            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities caps) {
                boolean hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                _isOnline.postValue(hasInternet);
            }
        });
    }

    public LiveData<Boolean> isOnline() {
        return _isOnline;
    }

    public boolean isCurrentlyOnline() {
        return Boolean.TRUE.equals(_isOnline.getValue());
    }
}
