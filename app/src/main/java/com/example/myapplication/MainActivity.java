package com.example.myapplication;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.example.myapplication.data.session.SessionManager;
import com.example.myapplication.ui.main.MainViewModel;
import com.example.myapplication.util.ThemePreferences;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;
import java.util.Objects;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    @Inject
    SessionManager sessionManager;

    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        final int initialLeft = toolbar.getPaddingLeft();
        final int initialTop = toolbar.getPaddingTop();
        final int initialRight = toolbar.getPaddingRight();
        final int initialBottom = toolbar.getPaddingBottom();
        final int initialHeight = toolbar.getLayoutParams() != null ? toolbar.getLayoutParams().height : 0;
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(initialLeft, initialTop + topInset, initialRight, initialBottom);
            ViewGroup.LayoutParams lp = v.getLayoutParams();
            if (lp != null && initialHeight > 0) {
                lp.height = initialHeight + topInset;
                v.setLayoutParams(lp);
            }
            return insets;
        });
        ViewCompat.requestApplyInsets(toolbar);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        new ViewModelProvider(this).get(MainViewModel.class);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        navController = Objects.requireNonNull(navHostFragment, "NavHostFragment not found").getNavController();

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

            // Hide global app bar for login and signup so fragments can render a full-image header
            View appBar = findViewById(R.id.app_bar_layout);
            if (appBar != null) {
                if (destId == R.id.loginFragment || destId == R.id.signupFragment) {
                    appBar.setVisibility(View.GONE);
                } else {
                    appBar.setVisibility(View.VISIBLE);
                }
            }

            boolean isTopLevel = destId == R.id.loginFragment || destId == R.id.homeFragment
                    || destId == R.id.exploreFragment || destId == R.id.favoritesFragment
                    || destId == R.id.bookingsFragment || destId == R.id.profileFragment;
            boolean hasPrevious = controller.getPreviousBackStackEntry() != null;
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(!isTopLevel && hasPrevious);
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
    public boolean onSupportNavigateUp() {
        if (navController.getCurrentDestination() != null
                && navController.getCurrentDestination().getId() == R.id.otpSignupCodeFragment) {
            return navController.popBackStack(R.id.loginFragment, false);
        }
        return navController.navigateUp();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_menu, menu);

        MenuItem themeItem = menu.findItem(R.id.action_theme_toggle);
        if (themeItem != null && themeItem.getActionView() != null) {
            View actionView = themeItem.getActionView();
            ImageButton button = actionView.findViewById(R.id.theme_toggle_button);
            if (button != null) {
                button.setOnClickListener(v -> toggleTheme());
            } else {
                actionView.setOnClickListener(v -> toggleTheme());
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_theme_toggle) {
            toggleTheme();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleTheme() {
        int currentMode = AppCompatDelegate.getDefaultNightMode();
        int nextMode = currentMode == AppCompatDelegate.MODE_NIGHT_YES
                ? AppCompatDelegate.MODE_NIGHT_NO
                : AppCompatDelegate.MODE_NIGHT_YES;
        ThemePreferences.setNightMode(this, nextMode);
    }
}
