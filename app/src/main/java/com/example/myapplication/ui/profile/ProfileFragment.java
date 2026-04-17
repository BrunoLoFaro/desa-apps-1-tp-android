package com.example.myapplication.ui.profile;

import android.net.Uri;
import android.os.Build;
import android.widget.ImageView;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.data.model.BookingSummaryItem;
import com.example.myapplication.data.model.UserProfileData;
import com.example.myapplication.ui.profile.viewmodel.ProfileViewModel;
import com.example.myapplication.util.FormatUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import dagger.hilt.android.AndroidEntryPoint;
import java.io.File;
import java.util.List;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

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
    private MaterialButton btnSave;
    private ProgressBar loadingSpinner;
    private View scrollView;

    // Summary views
    private TextView statCompleted;
    private TextView statPending;
    private View recentItem1;
    private View recentItem2;
    private View recentDivider;
    private ImageView recent1Icon;
    private ImageView recent2Icon;
    private TextView recent1Name;
    private TextView recent1Meta;
    private TextView recent2Name;
    private TextView recent2Meta;
    private TextView linkVerTodas;

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
        linkVerTodas.setOnClickListener(v ->
                navController.navigate(R.id.action_profileFragment_to_bookingsFragment));

        observeViewModel();
    }

    private void bindViews(@NonNull View view) {
        profilePhoto    = view.findViewById(R.id.profile_photo);
        emailText       = view.findViewById(R.id.profile_email);
        layoutFirstName = view.findViewById(R.id.layout_first_name);
        layoutLastName  = view.findViewById(R.id.layout_last_name);
        editFirstName   = view.findViewById(R.id.edit_first_name);
        editLastName    = view.findViewById(R.id.edit_last_name);
        editPhone       = view.findViewById(R.id.edit_phone);
        btnSave         = view.findViewById(R.id.btn_save);
        loadingSpinner  = view.findViewById(R.id.loading_spinner);
        scrollView      = view.findViewById(R.id.scroll_view);

        statCompleted = view.findViewById(R.id.stat_completed_count);
        statPending   = view.findViewById(R.id.stat_pending_count);
        recentItem1   = view.findViewById(R.id.recent_item_1);
        recentItem2   = view.findViewById(R.id.recent_item_2);
        recentDivider = view.findViewById(R.id.recent_divider);
        recent1Icon   = view.findViewById(R.id.recent_1_icon);
        recent2Icon   = view.findViewById(R.id.recent_2_icon);
        recent1Name   = view.findViewById(R.id.recent_1_name);
        recent1Meta   = view.findViewById(R.id.recent_1_meta);
        recent2Name   = view.findViewById(R.id.recent_2_name);
        recent2Meta   = view.findViewById(R.id.recent_2_meta);
        linkVerTodas  = view.findViewById(R.id.link_ver_todas);
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

        viewModel.getSelectedPhotoUri().observe(getViewLifecycleOwner(), uri -> {
            if (uri != null) {
                Glide.with(this)
                        .load(uri)
                        .placeholder(android.R.drawable.ic_menu_camera)
                        .circleCrop()
                        .into(profilePhoto);
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

    private void bindRecentActivities(List<BookingSummaryItem> items) {
        if (items == null || items.isEmpty()) {
            recentItem1.setVisibility(View.GONE);
            recentDivider.setVisibility(View.GONE);
            recentItem2.setVisibility(View.GONE);
            return;
        }
        bindRecentItem(items.get(0), recentItem1, recent1Icon, recent1Name, recent1Meta);
        if (items.size() > 1) {
            recentDivider.setVisibility(View.VISIBLE);
            bindRecentItem(items.get(1), recentItem2, recent2Icon, recent2Name, recent2Meta);
        } else {
            recentDivider.setVisibility(View.GONE);
            recentItem2.setVisibility(View.GONE);
        }
    }

    private void bindRecentItem(BookingSummaryItem item, View container,
                                 ImageView icon, TextView name, TextView meta) {
        container.setVisibility(View.VISIBLE);
        name.setText(item.getActivityName());
        String metaText = formatStatus(item.getStatus()) + " · " + FormatUtils.formatShortDate(item.getDate());
        meta.setText(metaText);
        int color = metaColor(item.getStatus());
        meta.setTextColor(color);
        icon.setColorFilter(color);
    }

    private String formatStatus(String status) {
        if ("COMPLETED".equals(status)) return getString(R.string.booking_status_completed);
        if ("CANCELLED".equals(status)) return getString(R.string.booking_status_cancelled);
        return status != null ? status : "";
    }

    private int metaColor(String status) {
        TypedValue tv = new TypedValue();
        int attr = "CANCELLED".equals(status)
                ? com.google.android.material.R.attr.colorError
                : com.google.android.material.R.attr.colorOnSurfaceVariant;
        requireContext().getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
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

        viewModel.saveAll(firstName, lastName, phone, viewModel.getCurrentPreferences());
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
        navController    = null;
        profilePhoto     = null;
        emailText        = null;
        layoutFirstName  = null;
        layoutLastName   = null;
        editFirstName    = null;
        editLastName     = null;
        editPhone        = null;
        btnSave          = null;
        loadingSpinner   = null;
        scrollView       = null;
        statCompleted = null;
        statPending   = null;
        recentItem1   = null;
        recentItem2   = null;
        recentDivider = null;
        recent1Icon   = null;
        recent2Icon   = null;
        recent1Name   = null;
        recent1Meta   = null;
        recent2Name   = null;
        recent2Meta   = null;
        linkVerTodas  = null;
        super.onDestroyView();
    }
}
