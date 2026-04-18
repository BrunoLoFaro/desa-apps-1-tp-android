package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

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

                if (id == R.id.nav_home) {
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

                // Explore/Profile are placeholders for now.
                Toast.makeText(this, "Próximamente", Toast.LENGTH_SHORT).show();
                return false;
            });
            bottomNav.setOnItemReselectedListener(item -> {
                // no-op
            });

            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int destId = destination.getId();
                if (destId == R.id.homeFragment || destId == R.id.bookingsFragment || destId == R.id.exploreFragment) {
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
                }
            });
        }
    }
}
