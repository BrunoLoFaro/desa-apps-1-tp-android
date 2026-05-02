package com.example.myapplication.ui.profile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import com.example.myapplication.util.PhoneCountryCode;
import com.example.myapplication.util.PhoneCountryCodes;
import java.util.ArrayList;
import java.util.List;

class CountryPickerAdapter extends RecyclerView.Adapter<CountryPickerAdapter.ViewHolder> {

    interface OnCountryClickListener {
        void onClick(PhoneCountryCode country);
    }

    private List<PhoneCountryCode> displayed = new ArrayList<>(PhoneCountryCodes.ALL);
    private String selectedCode;
    private final OnCountryClickListener listener;

    CountryPickerAdapter(String selectedCode, OnCountryClickListener listener) {
        this.selectedCode = selectedCode;
        this.listener = listener;
    }

    void filter(String query) {
        if (query == null || query.trim().isEmpty()) {
            displayed = new ArrayList<>(PhoneCountryCodes.ALL);
        } else {
            String q = query.trim().toLowerCase();
            List<PhoneCountryCode> filtered = new ArrayList<>();
            for (PhoneCountryCode cc : PhoneCountryCodes.ALL) {
                if (cc.getCountryName().toLowerCase().contains(q)
                        || cc.getCode().contains(q)) {
                    filtered.add(cc);
                }
            }
            displayed = filtered;
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_country_picker, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        PhoneCountryCode cc = displayed.get(position);
        h.flag.setText(cc.getFlagEmoji());
        h.name.setText(cc.getCountryName());
        h.code.setText(cc.getCode());
        h.check.setVisibility(cc.getCode().equals(selectedCode) ? View.VISIBLE : View.INVISIBLE);
        h.itemView.setOnClickListener(v -> listener.onClick(cc));
    }

    @Override
    public int getItemCount() { return displayed.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView flag, name, code;
        final ImageView check;

        ViewHolder(View v) {
            super(v);
            flag  = v.findViewById(R.id.tv_country_flag);
            name  = v.findViewById(R.id.tv_country_name);
            code  = v.findViewById(R.id.tv_country_code);
            check = v.findViewById(R.id.iv_check);
        }
    }
}
