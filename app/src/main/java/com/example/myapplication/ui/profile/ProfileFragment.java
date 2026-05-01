package com.example.myapplication.ui.profile;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.widget.ImageView;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.data.model.BookingSummaryItem;
import com.example.myapplication.data.model.UserProfileData;
import com.example.myapplication.data.session.SessionManager;
import com.example.myapplication.ui.profile.viewmodel.ProfileViewModel;
import com.example.myapplication.util.BiometricHelper;
import com.example.myapplication.util.FormatUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import dagger.hilt.android.AndroidEntryPoint;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

@AndroidEntryPoint
public class ProfileFragment extends Fragment {

    @Inject
    SessionManager sessionManager;

    private ProfileViewModel viewModel;
    private NavController navController;

    private ActivityResultLauncher<String> permissionLauncher;
    private ActivityResultLauncher<String> galleryLauncher;

    private ShapeableImageView profilePhoto;
    private TextView emailText;
    private TextInputLayout layoutFirstName;
    private TextInputLayout layoutLastName;
    private TextInputEditText editFirstName;
    private TextInputEditText editLastName;
    private TextInputEditText editPhone;
    private MaterialButton btnSave;
    private ProgressBar loadingSpinner;
    private View scrollView;
    private ChipGroup chipGroupCategories;

    // Summary views
    private TextView statCompleted;
    private TextView statPending;
    private TextView badgeHoy;
    private TextView linkVerTodas;

    private View recentItem1;
    private View recentItem2;
    private View recentDivider;
    private MaterialCardView recent1IconBg;
    private MaterialCardView recent2IconBg;
    private ImageView recent1Icon;
    private ImageView recent2Icon;
    private TextView recent1Name;
    private TextView recent1Dest;
    private TextView recent1Meta;
    private TextView recent1Time;
    private ImageView recent1Avatar;
    private TextView recent2Name;
    private TextView recent2Dest;
    private TextView recent2Meta;
    private TextView recent2Time;
    private ImageView recent2Avatar;

    private MaterialButton btnEnableBiometric;
    private MaterialButton btnLogout;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (Boolean.TRUE.equals(isGranted)) {
                        openGallery();
                    } else {
                        Toast.makeText(requireContext(),
                                R.string.error_permission_denied, Toast.LENGTH_SHORT).show();
                    }
                });

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) saveAndDisplay(uri);
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);

        bindViews(view);

        loadSavedImage();

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> navController.navigateUp());

        view.findViewById(R.id.edit_photo_btn).setOnClickListener(v -> checkPermissionAndOpenGallery());
        btnSave.setOnClickListener(v -> onSaveClicked());
        linkVerTodas.setOnClickListener(v ->
                navController.navigate(R.id.action_profileFragment_to_bookingsFragment));

        btnEnableBiometric.setOnClickListener(v -> onBiometricCtaClicked());

        btnLogout.setOnClickListener(v -> {
            sessionManager.clearSession();
            navController.navigate(R.id.loginFragment);
        });

        updateBiometricCta();
        observeViewModel();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateBiometricCta();
    }

    private void updateBiometricCta() {
        if (btnEnableBiometric == null) return;

        boolean supported = canAuthenticate();
        btnEnableBiometric.setEnabled(supported);
        if (!supported) {
            btnEnableBiometric.setText(R.string.profile_enable_biometric);
            return;
        }

        boolean enabled = BiometricHelper.isBiometricEnabled(requireContext());
        btnEnableBiometric.setText(enabled
                ? R.string.profile_disable_biometric
                : R.string.profile_enable_biometric);
    }

    private void onBiometricCtaClicked() {
        if (!canAuthenticate()) {
            Toast.makeText(requireContext(), R.string.biometric_enroll_error, Toast.LENGTH_SHORT).show();
            return;
        }

        boolean enabled = BiometricHelper.isBiometricEnabled(requireContext());
        if (enabled) {
            BiometricHelper.setBiometricEnabled(requireContext(), false);
            BiometricHelper.setBiometricSkipped(requireContext(), true);
            updateBiometricCta();
            return;
        }

        BiometricHelper.setBiometricSkipped(requireContext(), false);
        BiometricHelper.setBiometricEnabled(requireContext(), false);
        Bundle args = new Bundle();
        args.putString("origin", "profile");
        navController.navigate(R.id.action_profileFragment_to_biometricEnrollFragment, args);
    }

    private boolean canAuthenticate() {
        int authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG
                | BiometricManager.Authenticators.DEVICE_CREDENTIAL;
        return BiometricManager.from(requireContext()).canAuthenticate(authenticators)
                == BiometricManager.BIOMETRIC_SUCCESS;
    }

    private void bindViews(@NonNull View view) {
        profilePhoto    = view.findViewById(R.id.profile_photo);
        emailText       = view.findViewById(R.id.profile_email);
        layoutFirstName = view.findViewById(R.id.layout_first_name);
        layoutLastName  = view.findViewById(R.id.layout_last_name);
        editFirstName   = view.findViewById(R.id.edit_first_name);
        editLastName    = view.findViewById(R.id.edit_last_name);
        editPhone       = view.findViewById(R.id.edit_phone);
        btnSave             = view.findViewById(R.id.btn_save);
        loadingSpinner      = view.findViewById(R.id.loading_spinner);
        scrollView          = view.findViewById(R.id.scroll_view);
        chipGroupCategories = view.findViewById(R.id.chip_group_categories);

        statCompleted  = view.findViewById(R.id.stat_completed_count);
        statPending    = view.findViewById(R.id.stat_pending_count);
        badgeHoy       = view.findViewById(R.id.badge_hoy);
        linkVerTodas   = view.findViewById(R.id.link_ver_todas);

        recentItem1    = view.findViewById(R.id.recent_item_1);
        recentItem2    = view.findViewById(R.id.recent_item_2);
        recentDivider  = view.findViewById(R.id.recent_divider);
        recent1IconBg  = view.findViewById(R.id.recent_1_icon_bg);
        recent2IconBg  = view.findViewById(R.id.recent_2_icon_bg);
        recent1Icon    = view.findViewById(R.id.recent_1_icon);
        recent2Icon    = view.findViewById(R.id.recent_2_icon);
        recent1Name    = view.findViewById(R.id.recent_1_name);
        recent1Dest    = view.findViewById(R.id.recent_1_dest);
        recent1Meta    = view.findViewById(R.id.recent_1_meta);
        recent1Time    = view.findViewById(R.id.recent_1_time);
        recent1Avatar  = view.findViewById(R.id.recent_1_avatar);
        recent2Name    = view.findViewById(R.id.recent_2_name);
        recent2Dest    = view.findViewById(R.id.recent_2_dest);
        recent2Meta    = view.findViewById(R.id.recent_2_meta);
        recent2Time    = view.findViewById(R.id.recent_2_time);
        recent2Avatar  = view.findViewById(R.id.recent_2_avatar);

        btnEnableBiometric = view.findViewById(R.id.btn_enable_biometric);
        btnLogout = view.findViewById(R.id.btn_logout);
    }

    private void observeViewModel() {
        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            loadingSpinner.setVisibility(loading ? View.VISIBLE : View.GONE);
            scrollView.setVisibility(loading ? View.GONE : View.VISIBLE);
            btnSave.setEnabled(!loading);
        });

        viewModel.getProfile().observe(getViewLifecycleOwner(), this::populateProfileFields);

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error.resolve(requireContext()), Toast.LENGTH_SHORT).show();
                viewModel.errorConsumed();
            }
        });

        viewModel.isSaveSuccess().observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(requireContext(), R.string.profile_saved_ok, Toast.LENGTH_SHORT).show();
                viewModel.saveSuccessConsumed();
            }
        });

        viewModel.getCategories().observe(getViewLifecycleOwner(), categories -> {
            if (categories == null || chipGroupCategories == null) return;
            rebuildCategoryChips(categories, viewModel.getCurrentPreferences());
        });

        viewModel.getPreferences().observe(getViewLifecycleOwner(), preferences -> {
            if (chipGroupCategories == null || chipGroupCategories.getChildCount() == 0) return;
            List<String> prefs = preferences != null ? preferences : Collections.emptyList();
            for (int i = 0; i < chipGroupCategories.getChildCount(); i++) {
                Chip chip = (Chip) chipGroupCategories.getChildAt(i);
                chip.setChecked(prefs.contains((String) chip.getTag()));
            }
        });

        viewModel.getHistorialCount().observe(getViewLifecycleOwner(), count -> {
            if (statCompleted != null) statCompleted.setText(String.valueOf(count != null ? count : 0));
        });

        viewModel.getPendingCount().observe(getViewLifecycleOwner(), count -> {
            if (statPending != null) statPending.setText(String.valueOf(count != null ? count : 0));
        });

        viewModel.getRecentActivities().observe(getViewLifecycleOwner(), this::bindRecentActivities);
    }

    private void populateProfileFields(UserProfileData profile) {
        if (profile == null) return;

        emailText.setText(profile.getEmail());

        if (editFirstName.getText() == null || editFirstName.getText().toString().isEmpty()) {
            editFirstName.setText(profile.getFirstName());
        }
        if (editLastName.getText() == null || editLastName.getText().toString().isEmpty()) {
            editLastName.setText(profile.getLastName());
        }
        if (editPhone.getText() == null || editPhone.getText().toString().isEmpty()) {
            editPhone.setText(profile.getPhone());
        }
    }

    private void bindRecentActivities(List<BookingSummaryItem> items) {
        if (items == null || items.isEmpty()) {
            if (badgeHoy != null) badgeHoy.setVisibility(View.GONE);
            recentItem1.setVisibility(View.GONE);
            recentDivider.setVisibility(View.GONE);
            recentItem2.setVisibility(View.GONE);
            return;
        }

        BookingSummaryItem first = items.get(0);
        boolean isFirstToday = LocalDate.now().toString().equals(first.getDate());
        if (badgeHoy != null) badgeHoy.setVisibility(isFirstToday ? View.VISIBLE : View.GONE);

        bindRecentItem(first, recentItem1,
                recent1IconBg, recent1Icon, recent1Name, recent1Dest, recent1Meta, recent1Time, recent1Avatar);

        if (items.size() > 1) {
            recentDivider.setVisibility(View.VISIBLE);
            bindRecentItem(items.get(1), recentItem2,
                    recent2IconBg, recent2Icon, recent2Name, recent2Dest, recent2Meta, recent2Time, recent2Avatar);
        } else {
            recentDivider.setVisibility(View.GONE);
            recentItem2.setVisibility(View.GONE);
        }
    }

    private void bindRecentItem(BookingSummaryItem item, View container,
                                 MaterialCardView iconBg, ImageView icon,
                                 TextView name, TextView dest, TextView meta,
                                 TextView time, ImageView avatar) {
        container.setVisibility(View.VISIBLE);
        name.setText(item.getActivityName());
        dest.setText(item.getDestination());

        boolean isConfirmed = "CONFIRMED".equalsIgnoreCase(item.getStatus());

        if (isConfirmed) {
            iconBg.setCardBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.md_theme_primaryContainer));
            icon.setImageResource(R.drawable.ic_calendar);
            icon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.md_theme_primary));
            String guide = item.getGuideName();
            if (guide != null && !guide.isEmpty()) {
                meta.setText(getString(R.string.history_guide_prefix, guide));
            } else {
                meta.setText("");
            }
            String t = item.getTime();
            if (t != null && !t.isEmpty()) {
                time.setText(t);
                time.setVisibility(View.VISIBLE);
            } else {
                time.setVisibility(View.GONE);
            }
        } else {
            iconBg.setCardBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.md_theme_primaryContainer));
            icon.setImageResource(R.drawable.ic_check);
            icon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.success));
            meta.setText(getString(R.string.booking_status_completed));
            time.setVisibility(View.GONE);
        }
    }

    private void rebuildCategoryChips(List<String> categories, List<String> selectedPrefs) {
        if (chipGroupCategories == null) return;
        chipGroupCategories.removeAllViews();
        for (String cat : categories) {
            Chip chip = new Chip(requireContext());
            chip.setText(categoryDisplayName(cat));
            chip.setTag(cat);
            chip.setCheckable(true);
            chip.setCheckedIconVisible(true);
            chip.setChecked(selectedPrefs != null && selectedPrefs.contains(cat));
            chipGroupCategories.addView(chip);
        }
    }

    private String categoryDisplayName(String cat) {
        if (cat == null) return "";
        switch (cat.toLowerCase()) {
            case "aventura":      return getString(R.string.category_aventura);
            case "gastronomia":   return getString(R.string.category_gastronomia);
            case "excursion":     return getString(R.string.category_excursion);
            case "visita_guiada": return getString(R.string.category_visita_guiada);
            case "free_tour":     return getString(R.string.category_free_tour);
            case "otra":          return getString(R.string.category_otra);
            default:
                String s = cat.replace('_', ' ').toLowerCase();
                return Character.toUpperCase(s.charAt(0)) + s.substring(1);
        }
    }

    private List<String> getSelectedCategories() {
        List<String> selected = new ArrayList<>();
        if (chipGroupCategories == null) return selected;
        for (int i = 0; i < chipGroupCategories.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupCategories.getChildAt(i);
            if (chip.isChecked()) selected.add((String) chip.getTag());
        }
        return selected;
    }

    private void onSaveClicked() {
        String firstName = getText(editFirstName);
        String lastName  = getText(editLastName);
        String phone     = getText(editPhone);

        boolean valid = true;
        layoutFirstName.setError(null);
        layoutLastName.setError(null);

        if (firstName.length() < 2 || firstName.length() > 80) {
            layoutFirstName.setError(getString(R.string.error_invalid_first_name));
            valid = false;
        }
        if (lastName.length() < 2 || lastName.length() > 80) {
            layoutLastName.setError(getString(R.string.error_invalid_last_name));
            valid = false;
        }
        if (!valid) return;

        viewModel.saveAll(firstName, lastName, phone, getSelectedCategories());
    }

    private String getText(TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }

    private void saveAndDisplay(Uri uri) {
        viewModel.saveProfileImageUri(uri);
        displayImage(uri);
    }

    private void loadSavedImage() {
        Uri saved = viewModel.getSavedProfileImageUri();
        if (saved != null) {
            displayImage(saved);
        }
    }

    private void displayImage(Uri uri) {
        Glide.with(this)
                .load(uri)
                .circleCrop()
                .into(profilePhoto);
    }

    private void checkPermissionAndOpenGallery() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(requireContext(), permission)
                == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            permissionLauncher.launch(permission);
        }
    }

    private void openGallery() {
        galleryLauncher.launch("image/*");
    }

    @Override
    public void onDestroyView() {
        navController    = null;
        profilePhoto     = null;
        emailText        = null;
        layoutFirstName  = null;
        layoutLastName   = null;
        editFirstName    = null;
        editLastName     = null;
        editPhone        = null;
        btnSave             = null;
        loadingSpinner      = null;
        scrollView          = null;
        chipGroupCategories = null;
        statCompleted    = null;
        statPending      = null;
        badgeHoy         = null;
        linkVerTodas     = null;
        recentItem1      = null;
        recentItem2      = null;
        recentDivider    = null;
        recent1IconBg    = null;
        recent2IconBg    = null;
        recent1Icon      = null;
        recent2Icon      = null;
        recent1Name      = null;
        recent1Dest      = null;
        recent1Meta      = null;
        recent1Time      = null;
        recent1Avatar    = null;
        recent2Name      = null;
        recent2Dest      = null;
        recent2Meta      = null;
        recent2Time      = null;
        recent2Avatar    = null;
        super.onDestroyView();
    }
}
