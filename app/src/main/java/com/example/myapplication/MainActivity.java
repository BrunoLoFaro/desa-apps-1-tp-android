package com.example.myapplication;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.example.myapplication.data.session.SessionManager;
import com.example.myapplication.ui.main.MainViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    @Inject
    SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        new ViewModelProvider(this).get(MainViewModel.class);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();

        // AppBarConfiguration: define qué destinos son top-level (mostrados en el nav bar)
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.homeFragment,
                R.id.exploreFragment,
                R.id.favoritesFragment,
                R.id.bookingsFragment,
                R.id.profileFragment
        ).build();

        // Setup bottom nav
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            int current = navController.getCurrentDestination() != null
                    ? navController.getCurrentDestination().getId()
                    : 0;

            if (id == R.id.homeFragment) {
                if (current == R.id.homeFragment) return true;
                return navController.popBackStack(R.id.homeFragment, false);
            }

            if (id == R.id.exploreFragment) {
                if (current == R.id.exploreFragment) return true;
                navController.navigate(R.id.exploreFragment);
                return true;
            }

            if (id == R.id.nav_bookings) {
                if (current == R.id.bookingsFragment) return true;
                navController.navigate(R.id.bookingsFragment);
                return true;
            }

            if (id == R.id.favoritesFragment) {
                if (current == R.id.favoritesFragment) return true;
                navController.navigate(R.id.favoritesFragment);
                return true;
            }

            if (id == R.id.profileFragment) {
                if (current == R.id.profileFragment) return true;
                navController.navigate(R.id.profileFragment);
                return true;
            }

            return false;
        });

        bottomNav.setOnItemReselectedListener(item -> {
            // no-op
        });

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int destId = destination.getId();
            if (destId == R.id.homeFragment || destId == R.id.bookingsFragment || destId == R.id.favoritesFragment || destId == R.id.profileFragment || destId == R.id.exploreFragment) {
                bottomNav.setVisibility(View.VISIBLE);
            } else {
                bottomNav.setVisibility(View.GONE);
            }

            if (destId == R.id.homeFragment) {
                bottomNav.getMenu().findItem(R.id.homeFragment).setChecked(true);
            } else if (destId == R.id.exploreFragment) {
                bottomNav.getMenu().findItem(R.id.exploreFragment).setChecked(true);
            } else if (destId == R.id.bookingsFragment) {
                bottomNav.getMenu().findItem(R.id.nav_bookings).setChecked(true);
            } else if (destId == R.id.favoritesFragment) {
                bottomNav.getMenu().findItem(R.id.favoritesFragment).setChecked(true);
            } else if (destId == R.id.profileFragment) {
                bottomNav.getMenu().findItem(R.id.profileFragment).setChecked(true);
            }
        });

        // Cierre de sesión forzado al recibir 401 del servidor
        sessionManager.getForceLogoutEvent().observe(this, shouldLogout -> {
            if (shouldLogout) {
                sessionManager.clearSession();
                navController.navigate(R.id.loginFragment);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            sessionManager.clearSession();
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment);
            NavController navController = navHostFragment.getNavController();
            navController.navigate(R.id.loginFragment);
            return true;
        } else if (id == R.id.action_theme_toggle) {
            int currentMode = AppCompatDelegate.getDefaultNightMode();
            if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
