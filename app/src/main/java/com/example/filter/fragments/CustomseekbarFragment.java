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

import com.example.filter.etc.ClickUtils;
import com.example.filter.etc.CustomSeekbar;
import com.example.filter.activities.FilterActivity;
import com.example.filter.R;

public class CustomseekbarFragment extends Fragment {
    private TextView filterText;
    private ImageButton cancelBtn, checkBtn;
    private CustomSeekbar customSeekbar;
    private Fragment previousFragment;
    private ImageButton undoColor, redoColor;
    private String filterType = "";
    private int startValue = 0;
    private int filterValue = 0;
    private boolean hasAdjusted = false;

    public void setPreviousFragment(Fragment fragment) {
        this.previousFragment = fragment;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f_customseekbar, container, false);

        filterText = view.findViewById(R.id.filterText);
        customSeekbar = view.findViewById(R.id.customSeekbar);
        cancelBtn = view.findViewById(R.id.cancelBtn);
        checkBtn = view.findViewById(R.id.checkBtn);

        undoColor = requireActivity().findViewById(R.id.undoColor);
        redoColor = requireActivity().findViewById(R.id.redoColor);

        undoColor.setVisibility(View.INVISIBLE);
        redoColor.setVisibility(View.INVISIBLE);

        FilterActivity activity = (FilterActivity) getActivity();
        if (activity != null) {
            activity.refreshOriginalColorButton();
        }

        Bundle bundle = getArguments();
        if (bundle != null) {
            filterType = bundle.getString("filterType", "");

            if ("선명하게".equals(filterType) || "흐리게".equals(filterType) || "비네트".equals(filterType) || "노이즈".equals(filterType)) {
                customSeekbar.setMinZero(filterType);
            }

            filterText.setText(filterType);

            int currentValue = 0;
            if (getActivity() instanceof FilterActivity) {
                currentValue = ((FilterActivity) getActivity()).getCurrentValue(filterType);
            }

            startValue = currentValue;
            filterValue = currentValue;
            hasAdjusted = false;

            customSeekbar.setProgress(currentValue);
        } else {
            filterValue = customSeekbar.getProgress();
            startValue = filterValue;
            hasAdjusted = false;
        }

        customSeekbar.setOnProgressChangeListener(new CustomSeekbar.OnProgressChangeListener() {
            @Override
            public void onProgressChanged(CustomSeekbar customSeekbar, int progress) {
                filterValue = progress;

                if (progress != startValue) {
                    hasAdjusted = true;
                }

                if (getActivity() instanceof FilterActivity) {
                    ((FilterActivity) getActivity()).onTempValue(filterType, filterValue);
                }
            }
        });

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ClickUtils.isFastClick(500)) return;

                FilterActivity activity = (FilterActivity) getActivity();

                if (activity == null) return;
                activity.previewOriginalColors(false);

                int id = v.getId();
                if (id == R.id.cancelBtn) {
                    activity.onCancelValue(filterType);
                    activity.onUpdateValue(filterType, startValue);
                    showPreviousFagement();
                } else if (id == R.id.checkBtn) {
                    activity.onUpdateValue(filterType, filterValue);
                    activity.recordColorEdit(filterType, startValue, filterValue);
                    showPreviousFagement();
                }
            }
        };

        cancelBtn.setOnClickListener(listener);
        checkBtn.setOnClickListener(listener);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        FilterActivity a = (FilterActivity) getActivity();
        if (a != null) {
            a.refreshOriginalColorButton();
        }
    }

    private void showPreviousFagement() {
        if (previousFragment != null) {
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
                    .replace(R.id.bottomArea2, new ColorsFragment())
                    .commit();
        }

        requireActivity().getSupportFragmentManager().executePendingTransactions();
        if (getActivity() instanceof FilterActivity) {
            ((FilterActivity) getActivity()).requestUpdateBackGate();
        }
    }
}