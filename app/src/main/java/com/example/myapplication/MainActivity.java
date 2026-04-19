package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import com.example.myapplication.data.session.SessionManager;
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

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                int current = navController.getCurrentDestination() != null
                        ? navController.getCurrentDestination().getId()
                        : 0;

                if (id == R.id.homeFragment) {
                    if (current == R.id.homeFragment) return true;
                    return navController.popBackStack(R.id.homeFragment, false);
                }

                if (id == R.id.nav_explore) {
                    if (current == R.id.exploreFragment) return true;
                    navController.navigate(R.id.exploreFragment);
                    return true;
                }

                if (id == R.id.nav_bookings) {
                    if (current == R.id.bookingsFragment) return true;
                    navController.navigate(R.id.bookingsFragment);
                    return true;
                }

                if (id == R.id.profileFragment) {
                    if (current == R.id.profileFragment) return true;
                    navController.navigate(R.id.profileFragment);
                    return true;
                }

                // Explore is a placeholder for now.
                Toast.makeText(this, "Próximamente", Toast.LENGTH_SHORT).show();
                return false;
            });
            bottomNav.setOnItemReselectedListener(item -> {
                // no-op
            });

            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int destId = destination.getId();
                if (destId == R.id.homeFragment || destId == R.id.bookingsFragment || destId == R.id.exploreFragment || destId == R.id.profileFragment) {
                    bottomNav.setVisibility(View.VISIBLE);
                } else {
                    bottomNav.setVisibility(View.GONE);
                }

                if (destId == R.id.homeFragment) {
                    bottomNav.getMenu().findItem(R.id.nav_home).setChecked(true);
                } else if (destId == R.id.exploreFragment) {
                    bottomNav.getMenu().findItem(R.id.nav_explore).setChecked(true);
                } else if (destId == R.id.bookingsFragment) {
                    bottomNav.getMenu().findItem(R.id.nav_bookings).setChecked(true);
                } else if (destId == R.id.profileFragment) {
                    bottomNav.getMenu().findItem(R.id.profileFragment).setChecked(true);
                }
            });

            // Cierre de sesión forzado al recibir 401 del servidor
            sessionManager.getForceLogoutEvent().observe(this, shouldLogout -> {
                if (Boolean.TRUE.equals(shouldLogout)) {
                    navController.navigate(R.id.loginFragment, null,
                            new NavOptions.Builder()
                                    .setPopUpTo(R.id.nav_graph, true)
                                    .build());
                    sessionManager.consumeForceLogout();
                }
            });
        }
    }
}
