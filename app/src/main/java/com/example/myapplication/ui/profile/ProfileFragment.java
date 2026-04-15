package com.example.myapplication.ui.profile;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.data.model.BookingSummaryItem;
import com.example.myapplication.data.model.UserProfileData;
import com.example.myapplication.ui.profile.viewmodel.ProfileViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.ArrayList;
import java.util.List;

@AndroidEntryPoint
public class ProfileFragment extends Fragment {

    private ProfileViewModel viewModel;
    private NavController navController;

    private ActivityResultLauncher<String> pickImageLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    private ShapeableImageView profilePhoto;
    private TextView emailText;
    private TextInputLayout layoutFirstName;
    private TextInputLayout layoutLastName;
    private TextInputEditText editFirstName;
    private TextInputEditText editLastName;
    private TextInputEditText editPhone;
    private ChipGroup chipGroupPreferences;
    private MaterialButton btnSave;
    private RecyclerView activitySummaryRecycler;
    private TextView emptyActivitiesText;
    private ProgressBar loadingSpinner;
    private View scrollView;
    private ActivitySummaryAdapter summaryAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> { if (uri != null) viewModel.setSelectedPhotoUri(uri); }
        );

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> { if (Boolean.TRUE.equals(isGranted)) openGallery(); }
        );
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

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> navController.navigateUp());

        summaryAdapter = new ActivitySummaryAdapter();
        activitySummaryRecycler.setAdapter(summaryAdapter);

        profilePhoto.setOnClickListener(v -> checkPermissionAndOpenGallery());
        btnSave.setOnClickListener(v -> onSaveClicked());

        observeViewModel();
    }

    private void bindViews(@NonNull View view) {
        profilePhoto = view.findViewById(R.id.profile_photo);
        emailText = view.findViewById(R.id.profile_email);
        layoutFirstName = view.findViewById(R.id.layout_first_name);
        layoutLastName = view.findViewById(R.id.layout_last_name);
        editFirstName = view.findViewById(R.id.edit_first_name);
        editLastName = view.findViewById(R.id.edit_last_name);
        editPhone = view.findViewById(R.id.edit_phone);
        chipGroupPreferences = view.findViewById(R.id.chip_group_preferences);
        btnSave = view.findViewById(R.id.btn_save);
        activitySummaryRecycler = view.findViewById(R.id.activity_summary_recycler);
        emptyActivitiesText = view.findViewById(R.id.empty_activities_text);
        loadingSpinner = view.findViewById(R.id.loading_spinner);
        scrollView = view.findViewById(R.id.scroll_view);
    }

    private void observeViewModel() {
        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            loadingSpinner.setVisibility(loading ? View.VISIBLE : View.GONE);
            scrollView.setVisibility(loading ? View.GONE : View.VISIBLE);
            btnSave.setEnabled(!loading);
        });

        viewModel.getProfile().observe(getViewLifecycleOwner(), this::populateProfileFields);

        viewModel.getPreferences().observe(getViewLifecycleOwner(), this::applyPreferenceChips);

        viewModel.getActivitySummary().observe(getViewLifecycleOwner(), items -> {
            if (items == null || items.isEmpty()) {
                emptyActivitiesText.setVisibility(View.VISIBLE);
                activitySummaryRecycler.setVisibility(View.GONE);
            } else {
                emptyActivitiesText.setVisibility(View.GONE);
                activitySummaryRecycler.setVisibility(View.VISIBLE);
                summaryAdapter.updateData(items);
            }
        });

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

        viewModel.getSelectedPhotoUri().observe(getViewLifecycleOwner(), uri -> {
            if (uri != null) {
                Glide.with(this)
                        .load(uri)
                        .placeholder(android.R.drawable.ic_menu_camera)
                        .circleCrop()
                        .into(profilePhoto);
            }
        });
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

        String photoUrl = profile.getProfilePhotoUrl();
        Glide.with(this)
                .load(photoUrl != null && !photoUrl.isEmpty() ? photoUrl : null)
                .placeholder(android.R.drawable.ic_menu_camera)
                .circleCrop()
                .into(profilePhoto);
    }

    private void applyPreferenceChips(List<String> savedCategories) {
        if (savedCategories == null) return;
        setChipChecked(R.id.chip_aventura, savedCategories.contains("AVENTURA"));
        setChipChecked(R.id.chip_gastronomia, savedCategories.contains("GASTRONOMIA"));
        setChipChecked(R.id.chip_excursion, savedCategories.contains("EXCURSION"));
        setChipChecked(R.id.chip_visita_guiada, savedCategories.contains("VISITA_GUIADA"));
        setChipChecked(R.id.chip_free_tour, savedCategories.contains("FREE_TOUR"));
        setChipChecked(R.id.chip_otra, savedCategories.contains("OTRA"));
    }

    private void setChipChecked(int chipId, boolean checked) {
        View chip = chipGroupPreferences.findViewById(chipId);
        if (chip instanceof Chip) ((Chip) chip).setChecked(checked);
    }

    private void onSaveClicked() {
        String firstName = getText(editFirstName);
        String lastName = getText(editLastName);
        String phone = getText(editPhone);

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

        UserProfileData current = viewModel.getProfile().getValue();
        String photoUrl = current != null ? current.getProfilePhotoUrl() : null;

        List<String> selectedCategories = new ArrayList<>();
        if (isChipChecked(R.id.chip_aventura))      selectedCategories.add("AVENTURA");
        if (isChipChecked(R.id.chip_gastronomia))   selectedCategories.add("GASTRONOMIA");
        if (isChipChecked(R.id.chip_excursion))     selectedCategories.add("EXCURSION");
        if (isChipChecked(R.id.chip_visita_guiada)) selectedCategories.add("VISITA_GUIADA");
        if (isChipChecked(R.id.chip_free_tour))     selectedCategories.add("FREE_TOUR");
        if (isChipChecked(R.id.chip_otra))          selectedCategories.add("OTRA");

        viewModel.saveAll(firstName, lastName, phone, photoUrl, selectedCategories);
    }

    private String getText(TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }

    private boolean isChipChecked(int chipId) {
        View chip = chipGroupPreferences.findViewById(chipId);
        return chip instanceof Chip && ((Chip) chip).isChecked();
    }

    private void checkPermissionAndOpenGallery() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(requireContext(), permission)
                == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            requestPermissionLauncher.launch(permission);
        }
    }

    private void openGallery() {
        pickImageLauncher.launch("image/*");
    }

    @Override
    public void onDestroyView() {
        navController = null;
        profilePhoto = null;
        emailText = null;
        layoutFirstName = null;
        layoutLastName = null;
        editFirstName = null;
        editLastName = null;
        editPhone = null;
        chipGroupPreferences = null;
        btnSave = null;
        activitySummaryRecycler = null;
        emptyActivitiesText = null;
        loadingSpinner = null;
        scrollView = null;
        summaryAdapter = null;
        super.onDestroyView();
    }
}
