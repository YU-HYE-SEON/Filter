package com.example.filter.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.filter.etc.CustomSeekbar;
import com.example.filter.activities.FilterActivity;
import com.example.filter.R;

public class CustomseekbarFragment extends Fragment {
    private TextView filterText;
    private ImageButton cancelBtn;
    private ImageButton checkBtn;
    private CustomSeekbar customSeekbar;
    private int filterValue = 0;
    private Fragment previousFragment;

    public void setPreviousFragment(Fragment fragment) {
        this.previousFragment = fragment;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customseekbar, container, false);

        filterText = view.findViewById(R.id.filterText);
        Bundle bundle = getArguments();

        customSeekbar = view.findViewById(R.id.customSeekbar);
        cancelBtn = view.findViewById(R.id.cancelBtn);
        checkBtn = view.findViewById(R.id.checkBtn);

        if (bundle != null) {
            String filterType = bundle.getString("filterType", "");

            if(filterType == "선명하게"){
                customSeekbar.setMinZero(filterType);
            }

            filterText.setText(filterType);

            int currentValue = 0;
            if (getActivity() instanceof FilterActivity) {
                currentValue = ((FilterActivity) getActivity()).getCurrentValue(filterType);
            }
            customSeekbar.setProgress(currentValue);
            filterValue = currentValue;
        } else {
            filterValue = customSeekbar.getProgress();
        }

        customSeekbar.setOnProgressChangeListener(new CustomSeekbar.OnProgressChangeListener() {
            @Override
            public void onProgressChanged(CustomSeekbar customSeekbar, int progress) {
                filterValue = progress;
                if (getActivity() instanceof FilterActivity) {
                    String filterType = getArguments().getString("filterType", "");
                    ((FilterActivity) getActivity()).onTempValue(filterType, filterValue);
                }
            }
        });


        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int id = v.getId();
                if (id == R.id.cancelBtn) {
                    showPreviousFagement();

                    if (getActivity() instanceof FilterActivity) {
                        String filterType = getArguments().getString("filterType", "");
                        ((FilterActivity) getActivity()).onCancelValue(filterType);
                    }
                } else if (id == R.id.checkBtn) {
                    showPreviousFagement();

                    if (getActivity() instanceof FilterActivity) {
                        String filterType = getArguments().getString("filterType", "");
                        ((FilterActivity) getActivity()).onUpdateValue(filterType, filterValue);
                    }
                }
            }
        };

        cancelBtn.setOnClickListener(listener);
        checkBtn.setOnClickListener(listener);

        return view;
    }

    private void showPreviousFagement() {
        if (previousFragment != null) {
            //((FilterActivity) requireActivity()).animTADown();

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.slide_up, 0)
                    .remove(CustomseekbarFragment.this)
                    .show(previousFragment)
                    .commit();
        } else {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.slide_up, 0)
                    .replace(R.id.bottomArea, new ColorsFragment())
                    .commit();
        }
    }
}
