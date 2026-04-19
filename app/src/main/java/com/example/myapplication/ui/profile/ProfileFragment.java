package com.example.myapplication.ui.profile;

import android.net.Uri;
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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.myapplication.R;
import com.example.myapplication.data.model.BookingSummaryItem;
import com.example.myapplication.data.model.UserProfileData;
import com.example.myapplication.ui.profile.viewmodel.ProfileViewModel;
import com.example.myapplication.util.FormatUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import dagger.hilt.android.AndroidEntryPoint;
import java.io.File;
import java.time.LocalDate;
import java.util.List;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

@AndroidEntryPoint
public class ProfileFragment extends Fragment {

    private ProfileViewModel viewModel;
    private NavController navController;

    private ActivityResultLauncher<String> pickImageLauncher;

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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> { if (uri != null) viewModel.setSelectedPhotoUri(uri); }
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

        view.findViewById(R.id.edit_photo_btn).setOnClickListener(v -> openGallery());
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
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .placeholder(android.R.drawable.ic_menu_camera)
                    .circleCrop()
                    .into(profilePhoto);
        } else {
            profilePhoto.setImageResource(android.R.drawable.ic_menu_camera);
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
