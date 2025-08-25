package com.example.filter.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.filter.R;
import com.example.filter.activities.FilterActivity;
import com.example.filter.etc.ClickUtils;

public class RotationFragment extends Fragment {
    private ImageButton leftRotationIcon;
    private ImageButton rightRotationIcon;
    private ImageButton horizontalFlip;
    private ImageButton verticalFlip;
    private ImageButton cancelBtn;
    private ImageButton checkBtn;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f_rotation, container, false);

        leftRotationIcon = view.findViewById(R.id.leftRotationIcon);
        rightRotationIcon = view.findViewById(R.id.rightRotationIcon);
        horizontalFlip = view.findViewById(R.id.horizontalFlip);
        verticalFlip = view.findViewById(R.id.verticalFlip);
        cancelBtn = view.findViewById(R.id.cancelBtn);
        checkBtn = view.findViewById(R.id.checkBtn);

        final long[] lastTap = {0};

        attachPressEffect(leftRotationIcon, () -> {
            long now = SystemClock.uptimeMillis();
            if (now - lastTap[0] < 120) return; lastTap[0] = now;

            FilterActivity a = (FilterActivity) getActivity();
            if (a != null) a.rotatePhoto(-90);
        });

        attachPressEffect(rightRotationIcon, () -> {
            long now = SystemClock.uptimeMillis();
            if (now - lastTap[0] < 120) return; lastTap[0] = now;

            FilterActivity a = (FilterActivity) getActivity();
            if (a != null) a.rotatePhoto(90);
        });

        attachPressEffect(horizontalFlip, () -> {
            long now = SystemClock.uptimeMillis();
            if (now - lastTap[0] < 120) return; lastTap[0] = now;

            FilterActivity a = (FilterActivity) getActivity();
            if (a != null) a.flipPhoto(true);
        });

        attachPressEffect(verticalFlip, () -> {
            long now = SystemClock.uptimeMillis();
            if (now - lastTap[0] < 120) return; lastTap[0] = now;

            FilterActivity a = (FilterActivity) getActivity();
            if (a != null) a.flipPhoto(false);
        });

        leftRotationIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(500)) return;

                FilterActivity activity = (FilterActivity) getActivity();
                if (activity != null) {
                    activity.rotatePhoto(-90);
                }
            }
        });

        rightRotationIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(500)) return;

                FilterActivity activity = (FilterActivity) getActivity();
                if (activity != null) {
                    activity.rotatePhoto(90);
                }
            }
        });

        horizontalFlip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(500)) return;

                FilterActivity activity = (FilterActivity) getActivity();
                if (activity != null) {
                    activity.flipPhoto(true);
                }
            }
        });

        verticalFlip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(500)) return;

                FilterActivity activity = (FilterActivity) getActivity();
                if (activity != null) {
                    activity.flipPhoto(false);
                }
            }
        });

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(500)) return;

                FilterActivity activity = (FilterActivity) getActivity();
                if (activity != null) {
                    activity.restoreOriginalPhoto();
                    activity.getSupportFragmentManager()
                            .beginTransaction()
                            .setCustomAnimations(R.anim.slide_up, 0)
                            .replace(R.id.bottomArea2, new ToolsFragment())
                            .commit();
                }
            }
        });

        checkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(500)) return;

                FilterActivity activity = (FilterActivity) getActivity();
                if (activity != null) {
                    activity.commitTransformations(true);
                    activity.getSupportFragmentManager()
                            .beginTransaction()
                            .setCustomAnimations(R.anim.slide_up, 0)
                            .replace(R.id.bottomArea2, new ToolsFragment())
                            .commit();
                }
            }
        });

        return view;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void attachPressEffect(ImageButton btn,Runnable onUpInside) {
        btn.setClickable(true);
        btn.setFocusable(true);
        btn.setOnTouchListener((v, ev) -> {
            switch (ev.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: {
                    btn.setImageResource(R.drawable.rotation_icon_yes);
                    v.setPressed(true);
                    return true;
                }

                /*case MotionEvent.ACTION_MOVE: {
                    boolean inside = isPointInsideView(v, ev);
                    btn.setImageResource(inside ? R.drawable.rotation_icon_yes : R.drawable.rotation_icon_no);
                    v.setPressed(inside);
                    return true;
                }*/

                case MotionEvent.ACTION_UP: {
                    boolean inside = isPointInsideView(v, ev);
                    btn.setImageResource(R.drawable.rotation_icon_no);
                    v.setPressed(false);
                    if (inside && onUpInside != null) {
                        onUpInside.run();
                    }
                    return true;
                }

                case MotionEvent.ACTION_CANCEL: {
                    btn.setImageResource(R.drawable.rotation_icon_no);
                    v.setPressed(false);
                    return true;
                }
            }
            return false;
        });
    }

    private boolean isPointInsideView(View v, MotionEvent ev) {
        float x = ev.getX();
        float y = ev.getY();
        return (x >= 0 && y >= 00 && x <= v.getWidth() && y <= v.getHeight());
    }
}
