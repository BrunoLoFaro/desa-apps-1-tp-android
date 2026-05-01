package com.example.myapplication.ui.profile;

import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import com.example.myapplication.util.PhoneCountryCode;
import com.example.myapplication.util.SimpleTextWatcher;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;

public class CountryPickerBottomSheet extends BottomSheetDialogFragment {

    interface CountrySelectedListener {
        void onCountrySelected(PhoneCountryCode country);
    }

    private static final String ARG_SELECTED_CODE = "selected_code";

    public static CountryPickerBottomSheet newInstance(@Nullable String currentCode) {
        CountryPickerBottomSheet sheet = new CountryPickerBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_SELECTED_CODE, currentCode);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_country_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String selectedCode = getArguments() != null
                ? getArguments().getString(ARG_SELECTED_CODE) : null;

        Fragment parent = getParentFragment();
        if (!(parent instanceof CountrySelectedListener)) return;
        CountrySelectedListener listener = (CountrySelectedListener) parent;

        view.findViewById(R.id.btn_close).setOnClickListener(v -> dismiss());

        CountryPickerAdapter adapter = new CountryPickerAdapter(selectedCode, country -> {
            listener.onCountrySelected(country);
            dismiss();
        });

        RecyclerView recycler = view.findViewById(R.id.recycler_countries);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        TextInputEditText search = view.findViewById(R.id.edit_search);
        search.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                adapter.filter(s != null ? s.toString() : "");
            }
        });
    }
}
