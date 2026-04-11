package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import com.bumptech.glide.Glide;
import com.example.myapplication.data.model.TourActivity;

public class DetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inflamos el layout base
        View rootView = getLayoutInflater().inflate(R.layout.item_activity, null);
        
        // Lo envolvemos en un ScrollView para que se vea toda la info
        NestedScrollView scrollView = new NestedScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.addView(rootView);
        
        setContentView(scrollView);

        TourActivity activity = (TourActivity) getIntent().getSerializableExtra("activity_data");

        if (activity != null) {
            setupViews(rootView, activity);
        }
    }

    private void setupViews(View root, TourActivity activity) {
        ImageView image = root.findViewById(R.id.activity_image);
        TextView name = root.findViewById(R.id.activity_name);
        TextView category = root.findViewById(R.id.activity_category);
        TextView destination = root.findViewById(R.id.activity_destination);
        TextView duration = root.findViewById(R.id.activity_duration);
        TextView price = root.findViewById(R.id.activity_price);
        TextView slots = root.findViewById(R.id.activity_slots);
        
        // Contenedor de detalles
        View detailedContainer = root.findViewById(R.id.detailed_info_container);
        if (detailedContainer != null) {
            detailedContainer.setVisibility(View.VISIBLE);
        }

        // Ajustar imagen para detalle
        if (image != null) {
            ViewGroup.LayoutParams lp = image.getLayoutParams();
            lp.height = (int) (240 * getResources().getDisplayMetrics().density);
            image.setLayoutParams(lp);
        }

        // Llenar datos básicos
        name.setText(activity.getName());
        category.setText(activity.getCategory().toUpperCase());
        destination.setText(activity.getDestination());
        duration.setText(activity.getDuration());
        price.setText(activity.getPrice());
        slots.setText(getString(R.string.slots_available, activity.getAvailableSlots()));
        
        // Llenar datos extra
        TextView description = root.findViewById(R.id.activity_description);
        TextView language = root.findViewById(R.id.activity_language);
        TextView guide = root.findViewById(R.id.activity_guide);
        TextView meetingPoint = root.findViewById(R.id.activity_meeting_point);
        TextView includes = root.findViewById(R.id.activity_includes);
        TextView cancellation = root.findViewById(R.id.activity_cancellation);
        TextView rating = root.findViewById(R.id.activity_rating);

        if (description != null) description.setText(activity.getDescription());
        if (rating != null) rating.setText(String.valueOf(activity.getRating()));
        if (language != null) language.setText("Idioma: " + activity.getLanguage());
        if (guide != null) guide.setText("Guía: " + activity.getGuideName());
        if (meetingPoint != null) meetingPoint.setText("Encuentro: " + activity.getMeetingPoint());
        if (includes != null) includes.setText(activity.getWhatIncluded());
        if (cancellation != null) cancellation.setText(activity.getCancellationPolicy());

        Glide.with(this)
                .load(activity.getImageUrl())
                .centerCrop()
                .into(image);
    }
}
