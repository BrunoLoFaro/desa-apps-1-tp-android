package com.example.myapplication.ui.profile;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
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
import com.bumptech.glide.Glide;
import com.example.myapplication.R;
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
import java.io.File;
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
    private ProgressBar loadingSpinner;
    private View scrollView;

    private List<String> pendingPreferences = null;

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

        profilePhoto.setOnClickListener(v -> checkPermissionAndOpenGallery());
        btnSave.setOnClickListener(v -> onSaveClicked());

        observeViewModel();
    }

    private void bindViews(@NonNull View view) {
        profilePhoto       = view.findViewById(R.id.profile_photo);
        emailText          = view.findViewById(R.id.profile_email);
        layoutFirstName    = view.findViewById(R.id.layout_first_name);
        layoutLastName     = view.findViewById(R.id.layout_last_name);
        editFirstName      = view.findViewById(R.id.edit_first_name);
        editLastName       = view.findViewById(R.id.edit_last_name);
        editPhone          = view.findViewById(R.id.edit_phone);
        chipGroupPreferences = view.findViewById(R.id.chip_group_preferences);
        btnSave            = view.findViewById(R.id.btn_save);
        loadingSpinner     = view.findViewById(R.id.loading_spinner);
        scrollView         = view.findViewById(R.id.scroll_view);
    }

    private void observeViewModel() {
        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            loadingSpinner.setVisibility(loading ? View.VISIBLE : View.GONE);
            scrollView.setVisibility(loading ? View.GONE : View.VISIBLE);
            btnSave.setEnabled(!loading);
        });

        viewModel.getProfile().observe(getViewLifecycleOwner(), this::populateProfileFields);

        viewModel.getCategories().observe(getViewLifecycleOwner(), this::buildCategoryChips);

        viewModel.getPreferences().observe(getViewLifecycleOwner(), prefs -> {
            pendingPreferences = prefs;
            applyPreferenceChips(prefs);
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

        Uri selectedUri = viewModel.getSelectedPhotoUri().getValue();
        if (selectedUri != null && "content".equals(selectedUri.getScheme())) return;

        File localFile = viewModel.getLocalProfileImage();
        if (localFile.exists() && localFile.length() > 0) {
            Glide.with(this)
                    .load(localFile)
                    .placeholder(android.R.drawable.ic_menu_camera)
                    .circleCrop()
                    .into(profilePhoto);
        } else {
            profilePhoto.setImageResource(android.R.drawable.ic_menu_camera);
        }
    }

    private void buildCategoryChips(List<String> categories) {
        if (categories == null || chipGroupPreferences == null) return;
        chipGroupPreferences.removeAllViews();
        for (String cat : categories) {
            Chip chip = new Chip(new ContextThemeWrapper(
                    requireContext(),
                    com.google.android.material.R.style.Widget_Material3_Chip_Filter));
            chip.setTag(cat);
            chip.setText(formatCategoryLabel(cat));
            chip.setCheckable(true);
            chipGroupPreferences.addView(chip);
        }
        if (pendingPreferences != null) {
            applyPreferenceChips(pendingPreferences);
        }
    }

    private void applyPreferenceChips(List<String> savedCategories) {
        if (savedCategories == null || chipGroupPreferences == null) return;
        for (int i = 0; i < chipGroupPreferences.getChildCount(); i++) {
            View child = chipGroupPreferences.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                Object tag = chip.getTag();
                chip.setChecked(tag != null && savedCategories.contains(tag.toString()));
            }
        }
    }

    private static String formatCategoryLabel(String category) {
        if (category == null || category.isEmpty()) return "";
        String[] words = category.replace("_", " ").toLowerCase().split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                sb.append(word.substring(1));
                sb.append(' ');
            }
        }
        return sb.toString().trim();
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

        List<String> selectedCategories = new ArrayList<>();
        for (int i = 0; i < chipGroupPreferences.getChildCount(); i++) {
            View child = chipGroupPreferences.getChildAt(i);
            if (child instanceof Chip && ((Chip) child).isChecked()) {
                Object tag = child.getTag();
                if (tag != null) selectedCategories.add(tag.toString());
            }
        }

        viewModel.saveAll(firstName, lastName, phone, selectedCategories);
    }

    private String getText(TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
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
        navController        = null;
        profilePhoto         = null;
        emailText            = null;
        layoutFirstName      = null;
        layoutLastName       = null;
        editFirstName        = null;
        editLastName         = null;
        editPhone            = null;
        chipGroupPreferences = null;
        btnSave              = null;
        loadingSpinner       = null;
        scrollView           = null;
        super.onDestroyView();
    }
}
