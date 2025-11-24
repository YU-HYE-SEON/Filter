package com.example.filter.fragments.filters;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.filter.R;
import com.example.filter.activities.filter.FilterActivity;
import com.example.filter.api_datas.FaceStickerData;
import com.example.filter.etc.ClickUtils;
import com.example.filter.etc.Controller;
import com.example.filter.etc.FaceDetect;
import com.example.filter.etc.StickerMeta;
import com.example.filter.etc.StickerViewModel;
import com.example.filter.overlayviews.FaceBoxOverlayView;

import java.util.List;

public class FaceStickerFragment extends Fragment {
    private Fragment previousFragment;
    private String stickerUrl;
    private long sticker_db_id;
    public static int stickerId;
    private AppCompatButton saveBtn;
    private LayoutInflater inflater;
    private View stickerFrame;
    private ImageView stickerImage, deleteController, faceModel;
    private ConstraintLayout bottomArea1;
    private LinearLayout stickerEdit;
    private CheckBox faceCheckBox;
    private ImageButton cancelBtn, checkBtn;
    private FrameLayout faceOverlay;
    //ImageButton undoSticker, redoSticker, originalSticker;
    private FaceBoxOverlayView faceBox;
    private StickerMeta meta;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f_face_sticker, container, false);
        this.inflater = inflater;

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        cancelBtn = v.findViewById(R.id.cancelBtn);
        checkBtn = v.findViewById(R.id.checkBtn);

        faceOverlay = requireActivity().findViewById(R.id.faceOverlay);
        faceModel = requireActivity().findViewById(R.id.faceModel);
        faceCheckBox = requireActivity().findViewById(R.id.faceCheckBox);
        stickerEdit = requireActivity().findViewById(R.id.stickerEdit);
        bottomArea1 = requireActivity().findViewById(R.id.bottomArea1);

        if (bottomArea1 != null) {
            stickerEdit.setVisibility(View.VISIBLE);
            stickerEdit.setAlpha(0.4f);
            setCheckboxSize(28.5f, 1f);
            faceCheckBox.setChecked(true);
            faceCheckBox.setEnabled(false);
            faceOverlay.setVisibility(View.VISIBLE);
        }

        /*undoSticker = requireActivity().findViewById(R.id.undoSticker);
        redoSticker = requireActivity().findViewById(R.id.redoSticker);
        originalSticker = requireActivity().findViewById(R.id.originalSticker);

        if (undoSticker != null) undoSticker.setVisibility(View.INVISIBLE);
        if (redoSticker != null) redoSticker.setVisibility(View.INVISIBLE);
        if (originalSticker != null) originalSticker.setVisibility(View.INVISIBLE);*/

        /// faceOverlay 위에 EditStickerFragment와 같은 stickerFrame 두기
        /// EditStickerFragment의 stickerFrame을 아예 faceOverlay 위로 올리기 가능?
        Bundle args = getArguments();
        stickerUrl = args.getString("stickerUrl");
        sticker_db_id = args.getLong("sticker_db_id", -1L);
        showStickerCentered(stickerUrl);
        faceMode();

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPreviousFagement(v);
            }
        };
        cancelBtn.setOnClickListener(listener);
        checkBtn.setOnClickListener(listener);
    }

    private void showStickerCentered(String stickerUrl) {
        stickerFrame = inflater.inflate(R.layout.v_sticker_edit, faceOverlay, false);
        stickerImage = stickerFrame.findViewById(R.id.stickerImage);
        deleteController = stickerFrame.findViewById(R.id.deleteController);

        Glide.with(this)
                .load(stickerUrl)
                .into(stickerImage);

        int sizePx = Controller.dp(230, getResources());
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(sizePx, sizePx);
        stickerFrame.setLayoutParams(lp);

        faceOverlay.post(() -> {
            stickerFrame.setPivotX(sizePx / 2f);
            stickerFrame.setPivotY(sizePx / 2f);

            float cx = (faceOverlay.getWidth() - sizePx) / 2f;
            float cy = (faceOverlay.getHeight() - sizePx) / 2f;

            stickerFrame.setX(cx);
            stickerFrame.setY(cy);
            stickerFrame.setRotation(0);

            faceOverlay.addView(stickerFrame);
            stickerFrame.setTag(R.id.tag_sticker_db_id, sticker_db_id);

            Controller.enableStickerControl(null, null, stickerFrame, faceOverlay, getResources());
            Controller.updateControllersSizeAndAngle(stickerFrame, getResources());
        });

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPreviousFagement(v);
            }
        };
        deleteController.setOnClickListener(listener);
    }

    private void faceMode() {
        if (faceBox == null) {
            faceBox = new FaceBoxOverlayView(requireContext());
        }

        faceModel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int w = faceModel.getWidth();
                int h = faceModel.getHeight();
                if (w <= 0 || h <= 0) return;

                faceModel.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                faceOverlay.addView(faceBox, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

                Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(bmp);
                faceModel.draw(c);
                FaceDetect.detectFaces(bmp, faceBox, (faces, bitmap) -> {
                    if (faces.isEmpty()) return;

                    meta = StickerMeta.calculate(faces.get(0), bitmap, stickerFrame, faceOverlay);

                    Controller.enableStickerControl(faces.get(0), bitmap, stickerFrame, faceOverlay, getResources());
                    Controller.updateControllersSizeAndAngle(stickerFrame, getResources());
                });
            }
        });
    }

    private void showPreviousFagement(View v) {
        if (previousFragment != null) {
            if (faceBox != null && faceOverlay != null) {
                faceBox.clearBoxes();
                faceBox.setVisibility(View.GONE);
                faceOverlay.setVisibility(View.GONE);
            }
            Controller.removeStickerFrame(stickerFrame);

            if (v.getId() == R.id.checkBtn) {
                /// ⭐ Bundle로 넘겨주던 serverId 방식을 바꿨습니다 ⭐ ///
                Object dbTag = stickerFrame.getTag(R.id.tag_sticker_db_id);
                Log.e("SERVER_ID_TEST", "View clicked, tag=" + dbTag);
                long serverId = -1L;
                if (dbTag instanceof Long) {
                    serverId = (Long) dbTag;
                }

                ((EditStickerFragment) previousFragment).setFaceMeta(meta, stickerUrl, serverId);
                ((EditStickerFragment) previousFragment).applyPendingMeta();

                //Log.d("얼굴스티커", String.format("페이스스티커프래그먼트 | relX = %.1f, relY = %.1f, relW = %.1f, relH = %.1f, rot = %.1f", meta.relX, meta.relY, meta.relW, meta.relH, meta.rot));

                stickerId++;
            }

            ((EditStickerFragment) previousFragment).resetSelectAdapter();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.slide_up, 0)
                    .remove(FaceStickerFragment.this)
                    .show(previousFragment)
                    .commit();
        }
        Controller.removeStickerFrame(stickerFrame);
    }

    public void setPreviousFragment(Fragment fragment) {
        this.previousFragment = fragment;
    }

    private void setCheckboxSize(float dp1, float dp2) {
        int px = (int) dp(dp1);

        ViewGroup.LayoutParams lp = faceCheckBox.getLayoutParams();
        lp.width = px;
        lp.height = px;
        faceCheckBox.setLayoutParams(lp);

        faceCheckBox.requestLayout();

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) faceCheckBox.getLayoutParams();
        params.topMargin = (int) dp(dp2);
        faceCheckBox.setLayoutParams(params);
    }

    private float dp(float dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onResume() {
        super.onResume();
        saveBtn = requireActivity().findViewById(R.id.saveBtn);
        if (saveBtn != null) {
            saveBtn.setEnabled(false);
            saveBtn.setAlpha(0.4f);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (faceBox != null && faceOverlay != null) {
            faceBox.clearBoxes();
            faceBox.setVisibility(View.GONE);
            faceOverlay.setVisibility(View.GONE);
        }

        if (bottomArea1 != null) {
            setCheckboxSize(25f, 3f);
            faceCheckBox.setChecked(false);
        }
    }
}