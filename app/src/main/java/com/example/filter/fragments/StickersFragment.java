package com.example.filter.fragments;

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
import androidx.appcompat.widget.AppCompatButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.filter.R;
import com.example.filter.activities.FilterActivity;
import com.example.filter.activities.LoadActivity;
import com.example.filter.etc.ClickUtils;
import com.example.filter.etc.Controller;
import com.example.filter.etc.FaceDetect;
import com.example.filter.etc.FaceStickerData;
import com.example.filter.etc.StickerMeta;
import com.example.filter.etc.StickerViewModel;
import com.example.filter.overlayviews.FaceBoxOverlayView;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class StickersFragment extends Fragment {
    private ConstraintLayout topArea;
    private boolean isToastVisible = false;
    private FaceBoxOverlayView faceBox;
    private LinearLayout myStickerBtn, loadStickerBtn, brushBtn, AIStickerBtn;
    private ImageView myStickerIcon, brushIcon;
    private TextView myStickerTxt, brushTxt;
    private FrameLayout photoContainer, stickerOverlay, fullScreenContainer;
    private ConstraintLayout filterActivity, bottomArea1;
    private ImageButton undoColor, redoColor, originalColor;
    //private ImageButton undoSticker, redoSticker, originalSticker;
    private LinearLayout brushToSticker;
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

        topArea = activity.findViewById(R.id.topArea);
        photoContainer = activity.findViewById(R.id.photoContainer);
        stickerOverlay = activity.findViewById(R.id.stickerOverlay);
        bottomArea1 = activity.findViewById(R.id.bottomArea1);
        undoColor = activity.findViewById(R.id.undoColor);
        redoColor = activity.findViewById(R.id.redoColor);
        originalColor = activity.findViewById(R.id.originalColor);

        /*undoSticker = activity.findViewById(R.id.undoSticker);
        redoSticker = activity.findViewById(R.id.redoSticker);
        originalSticker = activity.findViewById(R.id.originalSticker);*/
        brushToSticker = activity.findViewById(R.id.brushToSticker);

       /* FilterActivity a = (FilterActivity) getActivity();
        if (a != null) {
            undoSticker.setOnClickListener(v -> {
                a.previewOriginalStickers(false);
                a.undoSticker();
            });

            redoSticker.setOnClickListener(v -> {
                a.previewOriginalStickers(false);
                a.redoSticker();
            });

            a.refreshStickerButtons();
        }*/

        if (bottomArea1 != null) {
            undoColor.setVisibility(View.INVISIBLE);
            redoColor.setVisibility(View.INVISIBLE);
            originalColor.setVisibility(View.INVISIBLE);
            /*undoSticker.setVisibility(View.VISIBLE);
            redoSticker.setVisibility(View.VISIBLE);
            originalSticker.setVisibility(View.VISIBLE);*/
            bottomArea1.setVisibility(View.VISIBLE);
            brushToSticker.setVisibility(View.GONE);
        }

        Bundle args = getArguments();
        boolean fromCheck = args != null && args.getBoolean("TRIGGERED_BY_CHECK", false);
        faceBox = new FaceBoxOverlayView(requireContext());

        photoContainer.addView(faceBox, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        activity.getPhotoPreview().queueEvent(() -> {
            Bitmap bmp = activity.getRenderer().getCurrentBitmap();
            activity.runOnUiThread(() -> FaceDetect.detectFaces(bmp, faceBox, (faces, bitmap) -> {
                if (faces.isEmpty()) {
                    if (fromCheck) {
                        showToast("얼굴을 감지하지 못했습니다");
                    }
                    return;
                }

                if (!faces.isEmpty() && args != null && (args.getBoolean("IS_FACE_MODE"))) {
                    StickerViewModel viewModel = new ViewModelProvider(requireActivity()).get(StickerViewModel.class);
                    int groupId = args.getInt("editingStickerId", EditStickerFragment.stickerId);
                    View stickerFrame = viewModel.getTempView(groupId);
                    StickerMeta meta = new StickerMeta(
                            args.getFloat("relX"),
                            args.getFloat("relY"),
                            args.getFloat("relW"),
                            args.getFloat("relH"),
                            args.getFloat("rot")
                    );

                    List<float[]> placement = StickerMeta.recalculate(faces, bitmap, stickerOverlay, meta, requireContext());
                    requireActivity().runOnUiThread(() -> {
                        viewModel.removeCloneGroup(groupId, stickerOverlay);
                        viewModel.setFaceStickerDataToDelete(groupId);

                        for (float[] p : placement) {
                            View cloneSticker = StickerMeta.cloneSticker(stickerOverlay, stickerFrame, requireContext(), p);
                            if (cloneSticker != null) {
                                cloneSticker.setTag(R.id.tag_sticker_id, groupId);
                                cloneSticker.setTag(R.id.tag_sticker_clone, true);
                                cloneSticker.setTag(R.id.tag_brush_layer, false);
                                viewModel.addCloneGroup(groupId, cloneSticker);
                                moveEditSticker(cloneSticker);
                                ((FilterActivity) getActivity()).updateSaveButtonState();
                            }
                        }

                        setIcon();
                        showToast("얼굴 인식 성공");

                        ImageView stickerImage = stickerFrame.findViewById(R.id.stickerImage);
                        Bitmap stickerBitmap = null;
                        if (stickerImage != null && stickerImage.getDrawable() != null) {
                            stickerImage.setDrawingCacheEnabled(true);
                            stickerBitmap = Bitmap.createBitmap(stickerImage.getDrawingCache());
                            stickerImage.setDrawingCacheEnabled(false);
                        }
                        String stickerPath = null;
                        if (stickerBitmap != null) {
                            try {
                                File file = new File(requireContext().getCacheDir(),
                                        "face_sticker_" + System.currentTimeMillis() + ".png");
                                FileOutputStream out = new FileOutputStream(file);
                                stickerBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                                out.close();
                                stickerPath = file.getAbsolutePath();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        FaceStickerData data = new FaceStickerData(meta.relX, meta.relY, meta.relW, meta.relH, meta.rot, groupId, stickerBitmap, stickerPath);
                        viewModel.setFaceStickerData(data);

                        //List<View> group = viewModel.getCloneGroup(groupId);
                        //StringBuilder sb = new StringBuilder();
                        //for (View v : group) {
                        //    sb.append(v.hashCode()).append(" ");
                        //}
                        //Log.d("스티커", String.format("최종 | [세션ID = %d] | [클론스티커ID = %d] | 개수 = %d, 구성 = %s", sessionId, groupId, group.size(), sb.toString()));
                        //viewModel.setTempView(null);
                    });

                    /*StickerMeta prevMeta = new StickerMeta(
                            args.getFloat("prev_relX"), args.getFloat("prev_relY"),
                            args.getFloat("prev_relW"), args.getFloat("prev_relH"),
                            args.getFloat("prev_rot")
                    );

                    StickerMeta nextMeta = new StickerMeta(
                            args.getFloat("next_relX"), args.getFloat("next_relY"),
                            args.getFloat("next_relW"), args.getFloat("next_relH"),
                            args.getFloat("next_rot")
                    );

                    boolean isCloneModify = args.getBoolean("IS_CLONE_MODIFY", false);

                    activity.recordCloneGroup(isCloneModify, groupId, stickerFrame, prevMeta, nextMeta);*/
                }
            }));
        });

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

    /*public void reapplyListenersToClones(List<View> clones) {
        if (clones == null) return;
        for (View clone : clones) {
            moveEditSticker(clone);
        }
    }*/

    public void moveEditSticker(View stickerFrame) {
        if (stickerFrame == null || stickerFrame.getParent() == null) return;


        stickerFrame.setOnClickListener(v -> {
            Controller.setControllersVisible(stickerFrame, true);

            stickerFrame.setPivotX(stickerFrame.getWidth() / 2f);
            stickerFrame.setPivotY(stickerFrame.getHeight() / 2f);

            //stickerFrame.post(() -> {
            //    Log.d("스티커", String.format("스 | 스티커프레임 pivotX = %.1f, pivotY = %.1f, x = %.1f, y = %.1f, w=%d, h=%d, r=%.1f",
            //            stickerFrame.getPivotX(), stickerFrame.getPivotY(), stickerFrame.getX(), stickerFrame.getY(), stickerFrame.getWidth(), stickerFrame.getHeight(), stickerFrame.getRotation()));
            //});

            EditStickerFragment editStickerFragment = new EditStickerFragment();
            Bundle args = new Bundle();

            args.putString("prev_frag", "stickerF");
            args.putInt("selected_index", ((ViewGroup) stickerFrame.getParent()).indexOfChild(stickerFrame));

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

            editStickerFragment.setArguments(args);

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.slide_up, 0)
                    .replace(R.id.bottomArea2, editStickerFragment)
                    .commit();
        });
    }

    private void setIcon() {
        int count = stickerOverlay.getChildCount();

        for (int i = 0; i < count; i++) {
            View child = stickerOverlay.getChildAt(i);
            Boolean isBrush = (Boolean) child.getTag(R.id.tag_brush_layer);

            if (!Boolean.TRUE.equals(isBrush)) {
                myStickerIcon.setImageResource(R.drawable.icon_mysticker_yes);
                myStickerTxt.setTextColor(Color.parseColor("#C2FA7A"));
            }

            if (Boolean.TRUE.equals(isBrush)) {
                if (child instanceof ImageView) {
                    ImageView imageView = (ImageView) child;
                    Drawable drawable = imageView.getDrawable();

                    if (drawable instanceof BitmapDrawable) {
                        Bitmap bmp = ((BitmapDrawable) drawable).getBitmap();
                        if (bmp != null && !bmp.isRecycled()) {
                            if (BrushFragment.hasAnyVisiblePixel(bmp)) {
                                brushIcon.setImageResource(R.drawable.icon_brush_yes);
                                brushTxt.setTextColor(Color.parseColor("#C2FA7A"));
                            }
                        }
                    }
                }
            }
        }
    }

    public void showToast(String message) {
        isToastVisible = true;

        View old = topArea.findViewWithTag("inline_banner");
        if (old != null) topArea.removeView(old);

        TextView tv = new TextView(requireContext());
        tv.setTag("inline_banner");
        tv.setText(message);
        tv.setTextColor(0XFFFFFFFF);
        tv.setTextSize(16);
        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        tv.setPadding(Controller.dp(14, getResources()), Controller.dp(10, getResources()), Controller.dp(14, getResources()), Controller.dp(10, getResources()));
        tv.setElevation(Controller.dp(4, getResources()));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xCC222222);
        bg.setCornerRadius(Controller.dp(16, getResources()));
        tv.setBackground(bg);

        ConstraintLayout.LayoutParams lp =
                new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT,
                        ConstraintLayout.LayoutParams.WRAP_CONTENT);
        lp.startToStart = topArea.getId();
        lp.endToEnd = topArea.getId();
        lp.topToTop = topArea.getId();
        lp.bottomToBottom = topArea.getId();
        tv.setLayoutParams(lp);

        tv.setAlpha(0f);
        topArea.addView(tv);
        tv.animate().alpha(1f).setDuration(150).start();

        tv.postDelayed(() -> tv.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> {
                    if (tv.getParent() == topArea) topArea.removeView(tv);
                    isToastVisible = false;
                })
                .start(), 2000);
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

            child.post(() -> {
                child.setPivotX(child.getWidth() / 2f);
                child.setPivotY(child.getHeight() / 2f);
            });

            Controller.setStickerActive(child, true);
            Controller.setControllersVisible(child, false);
            moveEditSticker(child);
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
            }
        }

        if (faceBox != null) {
            faceBox.clearBoxes();
            faceBox.setVisibility(View.GONE);
        }
    }
}