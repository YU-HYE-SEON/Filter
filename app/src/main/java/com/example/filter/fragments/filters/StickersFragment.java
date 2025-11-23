package com.example.filter.fragments.filters;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.filter.R;
import com.example.filter.activities.filter.FilterActivity;
import com.example.filter.activities.filter.LoadActivity;
import com.example.filter.etc.ClickUtils;
import com.example.filter.etc.Controller;
import com.example.filter.etc.FaceDetect;
import com.example.filter.api_datas.FaceStickerData;
import com.example.filter.etc.StickerMeta;
import com.example.filter.etc.StickerViewModel;
import com.example.filter.overlayviews.FaceBoxOverlayView;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class StickersFragment extends Fragment {
    private LinearLayout myStickerBtn, loadStickerBtn, brushBtn, AIStickerBtn;
    private ImageView myStickerIcon, brushIcon;
    private TextView myStickerTxt, brushTxt;
    private FrameLayout stickerOverlay, fullScreenContainer;
    private ConstraintLayout filterActivity, bottomArea1;
    private ImageButton undoColor, redoColor, originalColor;
    private LinearLayout brushToSticker, stickerEdit;

    private ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri photoUri = result.getData().getData();

                    if (photoUri != null) {
                        Intent intent = new Intent(requireContext(), LoadActivity.class);
                        intent.setData(photoUri);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(intent);
                    } else {
                        Toast.makeText(requireContext(), "사진 선택 실패: URI 없음", Toast.LENGTH_SHORT).show();
                    }
                }
            });


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f_stickers, container, false);

        myStickerBtn = view.findViewById(R.id.myStickerBtn);
        myStickerIcon = view.findViewById(R.id.myStickerIcon);
        myStickerTxt = view.findViewById(R.id.myStickerTxt);

        loadStickerBtn = view.findViewById(R.id.loadStickerBtn);

        brushBtn = view.findViewById(R.id.brushBtn);
        brushIcon = view.findViewById(R.id.brushIcon);
        brushTxt = view.findViewById(R.id.brushTxt);

        AIStickerBtn = view.findViewById(R.id.AIStickerBtn);

        FilterActivity activity = (FilterActivity) requireActivity();

        stickerOverlay = activity.findViewById(R.id.stickerOverlay);
        bottomArea1 = activity.findViewById(R.id.bottomArea1);
        undoColor = activity.findViewById(R.id.undoColor);
        redoColor = activity.findViewById(R.id.redoColor);
        originalColor = activity.findViewById(R.id.originalColor);
        brushToSticker = activity.findViewById(R.id.brushToSticker);
        stickerEdit = activity.findViewById(R.id.stickerEdit);

        if (bottomArea1 != null) {
            undoColor.setVisibility(View.INVISIBLE);
            redoColor.setVisibility(View.INVISIBLE);
            originalColor.setVisibility(View.INVISIBLE);
            bottomArea1.setVisibility(View.VISIBLE);
            brushToSticker.setVisibility(View.GONE);
            stickerEdit.setVisibility(View.GONE);
        }

        myStickerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(view, 400)) return;

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_up, 0)
                        .replace(R.id.bottomArea2, new MyStickersFragment())
                        .commit();
            }
        });

        loadStickerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(view, 400)) return;

                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                galleryLauncher.launch(intent);
            }
        });

        brushBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(view, 400)) return;

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_up, 0)
                        .replace(R.id.bottomArea2, new BrushFragment())
                        .commit();
            }
        });

        AIStickerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(view, 400)) return;

                fullScreenContainer = requireActivity().findViewById(R.id.fullScreenContainer);
                filterActivity = requireActivity().findViewById(R.id.filterActivity);
                ConstraintLayout main = requireActivity().findViewById(R.id.main);

                fullScreenContainer.setVisibility(View.VISIBLE);
                filterActivity.setVisibility(View.GONE);
                main.setBackgroundColor(Color.parseColor("#007AFF"));

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fullScreenContainer, new AIStickerViewFragment())
                        .addToBackStack("ai_sticker_view")
                        .commit();
            }
        });

        return view;
    }

    /*public void moveEditSticker(View stickerFrame) {
        if (stickerFrame == null || stickerFrame.getParent() == null) return;

        stickerFrame.setOnClickListener(v -> {
            Controller.setControllersVisible(stickerFrame, true);

            stickerFrame.setPivotX(stickerFrame.getWidth() / 2f);
            stickerFrame.setPivotY(stickerFrame.getHeight() / 2f);

            Bundle args = new Bundle();
            args.putInt("selected_index", ((ViewGroup) stickerFrame.getParent()).indexOfChild(stickerFrame));

            boolean fromMySticker = Boolean.TRUE.equals(stickerFrame.getTag(R.id.tag_from_mysticker));
            if (fromMySticker) {
                MyStickersFragment myStickersFragment = new MyStickersFragment();

                args.putString("stickerUrl", (String) stickerFrame.getTag(R.id.tag_sticker_url));
                args.putBoolean("EDIT_EXISTING", true);
                args.putFloat("prevElevation", ViewCompat.getZ(stickerFrame));
                args.putInt("sticker_index", ((ViewGroup) stickerFrame.getParent()).indexOfChild(stickerFrame));

                myStickersFragment.setArguments(args);

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_up, 0)
                        .replace(R.id.bottomArea2, myStickersFragment)
                        .commit();
            } else {
                EditStickerFragment editStickerFragment = new EditStickerFragment();

                Object tag = stickerFrame.getTag(R.id.tag_sticker_clone);
                if (tag != null) {
                    args.putBoolean("IS_CLONED_STICKER", true);
                } else {
                    args.putInt("w", stickerFrame.getLayoutParams().width);
                    args.putInt("h", stickerFrame.getLayoutParams().height);
                    args.putFloat("pivotX", stickerFrame.getPivotX());
                    args.putFloat("pivotY", stickerFrame.getPivotY());
                    args.putFloat("x", stickerFrame.getX());
                    args.putFloat("y", stickerFrame.getY());
                    args.putFloat("r", stickerFrame.getRotation());
                }

                // ✅ [추가] 편집 화면으로 넘어갈 때도 DB ID를 유지해서 넘겨줍니다
                Object dbTag = stickerFrame.getTag(R.id.tag_sticker_db_id);
                if (dbTag instanceof Long) {
                    args.putLong("sticker_db_id", (Long) dbTag);
                }

                editStickerFragment.setArguments(args);

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_up, 0)
                        .replace(R.id.bottomArea2, editStickerFragment)
                        .commit();
            }
        });
    }*/

    private void setIcon() {
        int count = stickerOverlay.getChildCount();
        boolean hasSticker = false;
        boolean hasBrush = false;

        for (int i = 0; i < count; i++) {
            View child = stickerOverlay.getChildAt(i);
            Boolean isBrush = (Boolean) child.getTag(R.id.tag_brush_layer);
            String url = (String) child.getTag(R.id.tag_sticker_url);

            if (url != null && child.getVisibility() == View.VISIBLE) {
                hasSticker = true;
                ImageView img = child.findViewById(R.id.stickerImage);
                Glide.with(this).load(url).into(img);
            }

            if (Boolean.TRUE.equals(isBrush)) {
                if (child instanceof ImageView) {
                    ImageView imageView = (ImageView) child;
                    Drawable drawable = imageView.getDrawable();

                    if (drawable instanceof BitmapDrawable) {
                        Bitmap bmp = ((BitmapDrawable) drawable).getBitmap();
                        if (bmp != null && !bmp.isRecycled()) {
                            if (BrushFragment.hasAnyVisiblePixel(bmp)) {
                                hasBrush = true;
                            }
                        }
                    }
                }
            }
        }

        if (hasSticker) {
            myStickerIcon.setImageResource(R.drawable.icon_mysticker_yes);
            myStickerTxt.setTextColor(Color.parseColor("#C2FA7A"));
        } else {
            myStickerIcon.setImageResource(R.drawable.icon_mysticker_no);
            myStickerTxt.setTextColor(Color.parseColor("#90989F"));
        }

        if (hasBrush) {
            brushIcon.setImageResource(R.drawable.icon_brush_yes);
            brushTxt.setTextColor(Color.parseColor("#C2FA7A"));
        } else {
            brushIcon.setImageResource(R.drawable.icon_brush_no);
            brushTxt.setTextColor(Color.parseColor("#90989F"));
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        if (stickerOverlay == null) return;

        for (int i = 0; i < stickerOverlay.getChildCount(); i++) {
            View child = stickerOverlay.getChildAt(i);

            if (Boolean.TRUE.equals(child.getTag(R.id.tag_brush_layer))) {
                child.setOnClickListener(null);
                child.setClickable(false);
                child.setLongClickable(false);
                child.setEnabled(false);
                continue;
            }

            String url = (String) child.getTag(R.id.tag_sticker_url);
            if (url != null) {
                ImageView img = child.findViewById(R.id.stickerImage);
                if (img != null) {
                    Glide.with(requireContext())
                            .load(url)
                            .into(img);
                }
            }

            child.post(() -> {
                child.setPivotX(child.getWidth() / 2f);
                child.setPivotY(child.getHeight() / 2f);
            });

            //Controller.setStickerActive(child, true);
            Controller.setControllersVisible(child, false);
            //moveEditSticker(child);
        }

        setIcon();
        ((FilterActivity) getActivity()).updateSaveButtonState();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        FrameLayout stickerOverlay = getActivity() != null ? getActivity().findViewById(R.id.stickerOverlay) : null;
        if (stickerOverlay != null) {
            for (int i = 0; i < stickerOverlay.getChildCount(); i++) {
                View child = stickerOverlay.getChildAt(i);
                child.setOnClickListener(null);
                child.setOnTouchListener(null);
            }
        }
    }
}