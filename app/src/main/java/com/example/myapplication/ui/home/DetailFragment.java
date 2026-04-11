package com.example.myapplication.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.data.model.TourActivity;
import com.google.android.material.appbar.MaterialToolbar;

public class DetailFragment extends Fragment {

    private View rootView;

    private TourActivity tourActivity;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            tourActivity = (TourActivity) getArguments().getSerializable("activity_data");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootView = view;

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(view).navigateUp());

        if (tourActivity != null) {
            toolbar.setTitle(tourActivity.getName());
            
            // Buscamos la vista incluida
            View content = view.findViewById(R.id.detail_content);
            if (content != null) {
                populateDetails(content);
            }
        }
    }

    private void populateDetails(View root) {
        ImageView image = root.findViewById(R.id.activity_image);
        TextView category = root.findViewById(R.id.activity_category);
        TextView name = root.findViewById(R.id.activity_name);
        TextView destination = root.findViewById(R.id.activity_destination);
        TextView duration = root.findViewById(R.id.activity_duration);
        TextView price = root.findViewById(R.id.activity_price);
        TextView slots = root.findViewById(R.id.activity_slots);
        
        View detailedContainer = root.findViewById(R.id.detailed_info_container);
        TextView description = root.findViewById(R.id.activity_description);
        TextView rating = root.findViewById(R.id.activity_rating);
        TextView language = root.findViewById(R.id.activity_language);
        TextView guide = root.findViewById(R.id.activity_guide);
        TextView meetingPoint = root.findViewById(R.id.activity_meeting_point);
        TextView includes = root.findViewById(R.id.activity_includes);
        TextView cancellation = root.findViewById(R.id.activity_cancellation);

        // Forzar visibilidad del contenedor de detalles
        if (detailedContainer != null) {
            detailedContainer.setVisibility(View.VISIBLE);
        }

        name.setText(tourActivity.getName());
        destination.setText(tourActivity.getDestination());
        category.setText(tourActivity.getCategory().toUpperCase());
        duration.setText(tourActivity.getDuration());
        price.setText(tourActivity.getPrice());
        slots.setText(getString(R.string.slots_available, tourActivity.getAvailableSlots()));
        
        if (description != null) description.setText(tourActivity.getDescription());
        if (rating != null) rating.setText(String.valueOf(tourActivity.getRating()));
        if (language != null) language.setText("Idioma: " + tourActivity.getLanguage());
        if (guide != null) guide.setText("Guía: " + tourActivity.getGuideName());
        if (meetingPoint != null) meetingPoint.setText("Encuentro: " + tourActivity.getMeetingPoint());
        if (includes != null) includes.setText(tourActivity.getWhatIncluded());
        if (cancellation != null) cancellation.setText(tourActivity.getCancellationPolicy());

        if (image != null) {
            // Ajustar altura de imagen para detalle (opcional, como tenías en tu Activity)
            ViewGroup.LayoutParams lp = image.getLayoutParams();
            lp.height = (int) (240 * getResources().getDisplayMetrics().density);
            image.setLayoutParams(lp);

            Glide.with(this)
                    .load(tourActivity.getImageUrl())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .centerCrop()
                    .into(image);
        }
    }

    @Override
    public void onDestroyView() {
        rootView = null;
        super.onDestroyView();
    }
}
