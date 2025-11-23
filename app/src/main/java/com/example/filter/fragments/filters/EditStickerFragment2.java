/*
package com.example.filter.fragments.filters;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
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
import com.example.filter.etc.ClickUtils;
import com.example.filter.etc.Controller;
import com.example.filter.etc.FaceDetect;
import com.example.filter.etc.StickerMeta;
import com.example.filter.etc.StickerViewModel;
import com.example.filter.overlayviews.FaceBoxOverlayView;

public class EditStickerFragment2 extends Fragment {
    private AppCompatButton saveBtn;
    public static int sessionId = 0;
    public static int stickerId = 0;
    private int editingStickerId = -1;  //-1이면 새로운 스티커 배치, -1 아니면 기존에 배치한 스티커 수정
    int originalIndex = -1;
    private View stickerFrame;
    private ImageView deleteController, faceModel;
    private ConstraintLayout bottomArea1;
    private LinearLayout stickerEdit;
    private CheckBox faceCheckBox;
    private TextView txt;
    private ImageButton cancelBtn, checkBtn;
    private FrameLayout stickerOverlay, faceOverlay;
    //ImageButton undoSticker, redoSticker, originalSticker;
    private FaceBoxOverlayView faceBox;
    private float pivotX, pivotY, tempPivotX, tempPivotY;
    private float x, y, r, tempX, tempY, tempR;
    private int w, h, tempW, tempH;
    private Drawable stickerDrawable;
    private StickerMeta meta;
    public static boolean isFace = false;
    //private int childCount = 0;
    //private StickerMeta prevMeta, nextMeta;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f_edit_my_sticker, container, false);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        txt = v.findViewById(R.id.txt);
        cancelBtn = v.findViewById(R.id.cancelBtn);
        checkBtn = v.findViewById(R.id.checkBtn);

        stickerOverlay = requireActivity().findViewById(R.id.stickerOverlay);
        faceOverlay = requireActivity().findViewById(R.id.faceOverlay);
        faceModel = requireActivity().findViewById(R.id.faceModel);
        faceCheckBox = requireActivity().findViewById(R.id.faceCheckBox);
        stickerEdit = requireActivity().findViewById(R.id.stickerEdit);
        bottomArea1 = requireActivity().findViewById(R.id.bottomArea1);

        if (bottomArea1 != null) {
            stickerEdit.setVisibility(View.VISIBLE);
        }

        */
/*undoSticker = requireActivity().findViewById(R.id.undoSticker);
        redoSticker = requireActivity().findViewById(R.id.redoSticker);
        originalSticker = requireActivity().findViewById(R.id.originalSticker);

        if (undoSticker != null) undoSticker.setVisibility(View.INVISIBLE);
        if (redoSticker != null) redoSticker.setVisibility(View.INVISIBLE);
        if (originalSticker != null) originalSticker.setVisibility(View.INVISIBLE);*//*


        Bundle args = getArguments();
        Bundle args2 = new Bundle();
        String prevFragment = args.getString("prev_frag");
        boolean isClone = args.getBoolean("IS_CLONED_STICKER", false);
        String stickerUrl = args.getString("stickerUrl");

        if (args != null && args.containsKey("selected_index")) {
            int index = args.getInt("selected_index");
            originalIndex = index;

            if (index >= 0 && index < stickerOverlay.getChildCount()) {
                stickerFrame = stickerOverlay.getChildAt(index);
            }
        } else if (stickerOverlay.getChildCount() > 0) {
            stickerFrame = stickerOverlay.getChildAt(stickerOverlay.getChildCount() - 1);
        }

        if (stickerOverlay != null && stickerFrame != null) {
            for (int i = 0; i < stickerOverlay.getChildCount(); i++) {
                View child = stickerOverlay.getChildAt(i);
                if (child != stickerFrame && !Boolean.TRUE.equals(child.getTag(R.id.tag_brush_layer))) {
                    Controller.setStickerActive(child, false);
                }
            }
        }

        ImageView stickerImage = stickerFrame.findViewById(R.id.stickerImage);
        Glide.with(this).load(stickerUrl).into(stickerImage);

        w = args.getInt("w", stickerFrame.getLayoutParams().width);
        h = args.getInt("h", stickerFrame.getLayoutParams().height);
        pivotX = args.getFloat("pivotX", stickerFrame.getPivotX());
        pivotY = args.getFloat("pivotY", stickerFrame.getPivotY());
        x = args.getFloat("x", stickerFrame.getX());
        y = args.getFloat("y", stickerFrame.getY());
        r = args.getFloat("r", stickerFrame.getRotation());

        tempW = w;
        tempH = h;
        tempPivotX = pivotX;
        tempPivotY = pivotY;
        tempX = x;
        tempY = y;
        tempR = r;

        int minPx = Controller.dp(230, getResources());
        int initW = Math.max(minPx, w);
        int initH = Math.max(minPx, h);

        stickerFrame.getLayoutParams().width = initW;
        stickerFrame.getLayoutParams().height = initH;
        if ("stickerF".equals(prevFragment)) {
            stickerFrame.getLayoutParams().width = w;
            stickerFrame.getLayoutParams().height = h;
        }
        stickerFrame.requestLayout();
        stickerFrame.setPivotX(stickerFrame.getWidth() / 2f);
        stickerFrame.setPivotY(stickerFrame.getHeight() / 2f);
        stickerFrame.setX(x);
        stickerFrame.setY(y);
        stickerFrame.setRotation(r);

        //stickerFrame.post(() -> {
        //    if ("myStickerF".equals(prevFragment))
        //        Log.d("스티커", String.format("에딧 첫진입 | 스티커프레임 pivotX = %.1f, pivotY = %.1f, x = %.1f, y = %.1f, w=%d, h=%d, r=%.1f",
        //                stickerFrame.getPivotX(), stickerFrame.getPivotY(), stickerFrame.getX(), stickerFrame.getY(), stickerFrame.getWidth(), stickerFrame.getHeight(), stickerFrame.getRotation()));
        //    else
        //        Log.d("스티커", String.format("에딧 재진입 | 스티커프레임 pivotX = %.1f, pivotY = %.1f, x = %.1f, y = %.1f, w=%d, h=%d, r=%.1f",
        //                stickerFrame.getPivotX(), stickerFrame.getPivotY(), stickerFrame.getX(), stickerFrame.getY(), stickerFrame.getWidth(), stickerFrame.getHeight(), stickerFrame.getRotation()));
        //});


        if (stickerFrame != null) {
            Controller.enableStickerControl(null, null, stickerFrame, stickerOverlay, faceOverlay, getResources());
            Controller.updateControllersSizeAndAngle(stickerFrame, getResources());
            deleteSticker();
        }

        if (stickerFrame != null) {
            Object tag = stickerFrame.getTag(R.id.tag_sticker_id);
            if (tag instanceof Integer) {
                editingStickerId = (int) tag;
                //Log.d("스티커", "스티커 재편집 | ID = " + editingStickerId);
            }
        }

        if (stickerFrame != null && !isClone) {
            stickerFrame.bringToFront();
        }

        if (isClone) {
            faceCheckBox.setChecked(true);
            faceCheckBox.setEnabled(false);
            faceCheckBox.setAlpha(0.4f);
            faceBox = new FaceBoxOverlayView(requireContext());
            onFaceMode();
        }

        cancelBtn.setOnClickListener(view -> {
            if (ClickUtils.isFastClick(view, 400)) return;

            if (faceCheckBox.isChecked() && !isClone) {
                offFaceMode();
            }

            if (isClone && editingStickerId != -1) {
                if (faceBox != null && faceOverlay != null) {
                    if (stickerFrame != null && stickerFrame.getParent() == faceOverlay) {
                        faceOverlay.removeView(stickerFrame);
                    }
                    faceBox.clearBoxes();
                    faceBox.setVisibility(View.GONE);
                    faceOverlay.setVisibility(View.GONE);
                }
            }

            if (!faceCheckBox.isChecked()) {
                stickerFrame.getLayoutParams().width = tempW;
                stickerFrame.getLayoutParams().height = tempH;
                stickerFrame.requestLayout();
                stickerFrame.setPivotX(tempPivotX);
                stickerFrame.setPivotY(tempPivotY);
                stickerFrame.setX(tempX);
                stickerFrame.setY(tempY);
                stickerFrame.setRotation(tempR);

                //stickerFrame.post(() -> {
                //    Log.d("스티커", String.format("에딧 캔슬 | 스티커프레임 pivotX = %.1f, pivotY = %.1f, x = %.1f, y = %.1f, w=%d, h=%d, r=%.1f",
                //            stickerFrame.getPivotX(), stickerFrame.getPivotY(), stickerFrame.getX(), stickerFrame.getY(), stickerFrame.getWidth(), stickerFrame.getHeight(), stickerFrame.getRotation()));
                //});
            }

            if (stickerOverlay != null && stickerFrame != null && originalIndex >= 0 && !isClone) {
                stickerOverlay.removeView(stickerFrame);
                stickerOverlay.addView(stickerFrame, originalIndex);
            }

            Controller.setControllersVisible(stickerFrame, false);

            if ("stickerF".equals(prevFragment)) {
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_up, 0)
                        .replace(R.id.bottomArea2, new StickersFragment())
                        .commit();

            } else if ("myStickerF".equals(prevFragment)) {
                Controller.removeStickerFrame(stickerFrame);

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_up, 0)
                        .replace(R.id.bottomArea2, new MyStickersFragment())
                        .commit();
            }
        });

        //if (stickerOverlay != null) childCount = stickerOverlay.getChildCount();

        checkBtn.setOnClickListener(view -> {
            if (ClickUtils.isFastClick(view, 400)) return;

            Fragment stickerFragment = new StickersFragment();
            FilterActivity activity = (FilterActivity) requireActivity();

            /// (클론스티커) 새로 만드는 경우
            if (faceCheckBox.isChecked()) {
                StickerViewModel viewModel = new ViewModelProvider(requireActivity()).get(StickerViewModel.class);
                viewModel.setTempView(editingStickerId != -1 ? editingStickerId : stickerId, stickerFrame);

                args2.putBoolean("IS_FACE_MODE", true);
                args2.putFloat("relX", meta.relX);
                args2.putFloat("relY", meta.relY);
                args2.putFloat("relW", meta.relW);
                args2.putFloat("relH", meta.relH);
                args2.putFloat("rot", meta.rot);
                stickerFragment.setArguments(args2);

                Controller.removeStickerFrame(stickerFrame);
                faceOverlay.setVisibility(View.GONE);

                //prevMeta = meta;
            }

            /// (클론스티커) 수정하는 경우
            if (isClone && editingStickerId != -1) {
                StickerViewModel viewModel = new ViewModelProvider(requireActivity()).get(StickerViewModel.class);
                viewModel.setTempView(editingStickerId != -1 ? editingStickerId : stickerId, stickerFrame);

                //prevMeta = meta;

                Bitmap bmp = Bitmap.createBitmap(faceModel.getWidth(), faceModel.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(bmp);
                faceModel.draw(c);
                FaceDetect.detectFaces(bmp, faceBox, (faces, bitmap) -> {
                    if (!faces.isEmpty()) {
                        meta = StickerMeta.calculate(faces.get(0), bitmap, stickerFrame, faceOverlay);
                    }
                });

                args2.putInt("editingStickerId", editingStickerId);
                args2.putBoolean("IS_FACE_MODE", true);
                args2.putBoolean("IS_CLONE_MODIFY", isClone);
                args2.putFloat("relX", meta.relX);
                args2.putFloat("relY", meta.relY);
                args2.putFloat("relW", meta.relW);
                args2.putFloat("relH", meta.relH);
                args2.putFloat("rot", meta.rot);
                stickerFragment.setArguments(args2);

                Controller.removeStickerFrame(stickerFrame);
                faceOverlay.setVisibility(View.GONE);

                //nextMeta = meta;
            }

            args2.putBoolean("TRIGGERED_BY_CHECK", true);

            */
/*args2.putFloat("prev_relX", prevMeta.relX);
            args2.putFloat("prev_relY", prevMeta.relY);
            args2.putFloat("prev_relW", prevMeta.relW);
            args2.putFloat("prev_relH", prevMeta.relH);
            args2.putFloat("prev_rot", prevMeta.rot);

            args2.putFloat("next_relX", nextMeta.relX);
            args2.putFloat("next_relY", nextMeta.relY);
            args2.putFloat("next_relW", nextMeta.relW);
            args2.putFloat("next_relH", nextMeta.relH);
            args2.putFloat("next_rot", nextMeta.rot);*//*



            /// (일반스티커) 새로 만들거나 수정하는 경우
            if (!faceCheckBox.isChecked()) {
                w = stickerFrame.getLayoutParams().width;
                h = stickerFrame.getLayoutParams().height;
                pivotX = stickerFrame.getPivotX();
                pivotY = stickerFrame.getPivotY();
                x = stickerFrame.getX();
                y = stickerFrame.getY();
                r = stickerFrame.getRotation();

                //stickerFrame.post(() -> {
                //    Log.d("스티커", String.format("에딧 체크 | 스티커프레임 pivotX = %.1f, pivotY = %.1f, x = %.1f, y = %.1f, w=%d, h=%d, r=%.1f",
                //            stickerFrame.getPivotX(), stickerFrame.getPivotY(), stickerFrame.getX(), stickerFrame.getY(), stickerFrame.getWidth(), stickerFrame.getHeight(), stickerFrame.getRotation()));
                //});

                */
/*if (stickerOverlay != null) {
                    /// 일반스티커 생성 시 undo, redo 히스토리에 기록
                    if (editingStickerId == -1) {
                        int newStickerBaseline = Math.max(0, childCount - 1);
                        activity.recordSticker(newStickerBaseline);
                    } else {    /// 일반스티커 수정 시 undo, redo 히스토리에 기록
                        activity.recordStickerEdit(stickerFrame, tempX, tempY, tempW, tempH, tempR, originalIndex, x, y, w, h, r, stickerOverlay.indexOfChild(stickerFrame));
                    }
                }*//*

            }

            Controller.setControllersVisible(stickerFrame, false);

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.slide_up, 0)
                    .replace(R.id.bottomArea2, stickerFragment)
                    .commit();

            sessionId++;

            /// 스티커 새로 만들었을 때만 스티커아이디 증가
            if (editingStickerId == -1) {
                stickerId++;
                stickerFrame.setTag(R.id.tag_sticker_id, stickerId);

                //if (!faceCheckBox.isChecked()) {
                //    Log.d("스티커", String.format("스티커 생성 | [세션ID = %d] | [스티커ID = %d]", sessionId, stickerId));
                //}

            } */
/*else {
                if (!faceCheckBox.isChecked()) {
                    Log.d("스티커", String.format("스티커 수정 | [세션ID = %d] | [스티커ID = %d]", sessionId, editingStickerId));
                }
            }*//*

        });

        faceCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            faceBox = new FaceBoxOverlayView(requireContext());
            if (isChecked) {
                onFaceMode();
            } else {
                offFaceMode();
            }
        });
    }

    private void showStickerCentered() {
        stickerFrame.setVisibility(View.INVISIBLE);

        int sizePx = Controller.dp(230, getResources());
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(sizePx, sizePx);
        stickerFrame.setLayoutParams(lp);

        stickerOverlay.post(() -> {
            stickerFrame.setPivotX(sizePx / 2f);
            stickerFrame.setPivotY(sizePx / 2f);

            float cx = (stickerOverlay.getWidth() - sizePx) / 2f;
            float cy = (stickerOverlay.getHeight() - sizePx) / 2f;

            stickerFrame.setX(cx);
            stickerFrame.setY(cy);
            stickerFrame.setRotation(0f);

            stickerFrame.postDelayed(() -> {
                stickerFrame.setVisibility(View.VISIBLE);
            }, 50);

            //stickerFrame.post(() -> {
            //    Log.d("스티커", String.format("에딧 페이스 | 스티커프레임 pivotX = %.1f, pivotY = %.1f, x = %.1f, y = %.1f, w=%d, h=%d, r=%.1f",
            //            stickerFrame.getPivotX(), stickerFrame.getPivotY(), stickerFrame.getX(), stickerFrame.getY(), stickerFrame.getWidth(), stickerFrame.getHeight(), stickerFrame.getRotation()));
            //});
        });
    }

    private void onFaceMode() {
        isFace = true;

        setCheckboxSize(28.5f, 1f);
        txt.setText("얼굴 인식 모드");

        Bundle args = getArguments();
        boolean isClone = args.getBoolean("IS_CLONED_STICKER", false);
        String stickerUrl = args.getString("stickerUrl");
        ImageView stickerImage = stickerFrame.findViewById(R.id.stickerImage);
        Glide.with(this).load(stickerUrl).into(stickerImage);

        if (stickerFrame != null && stickerFrame.getParent() == stickerOverlay && !isClone) {
            stickerOverlay.removeView(stickerFrame);
        }

        faceOverlay.setVisibility(View.VISIBLE);

        faceCheckBox.setChecked(true);

        faceModel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int w = faceModel.getWidth();
                int h = faceModel.getHeight();
                if (w <= 0 || h <= 0) return;

                faceModel.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                ImageView stickerImage = stickerFrame.findViewById(R.id.stickerImage);
                Glide.with(requireActivity()).load(stickerUrl).into(stickerImage);

                deleteSticker();

                faceOverlay.addView(faceBox, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                showStickerCentered();

                Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(bmp);
                faceModel.draw(c);
                FaceDetect.detectFaces(bmp, faceBox, (faces, bitmap) -> {
                    if (faces.isEmpty()) return;

                    meta = StickerMeta.calculate(faces.get(0), bitmap, stickerFrame, faceOverlay);

                    Controller.enableStickerControl(faces.get(0), bitmap, stickerFrame, stickerOverlay, faceOverlay, getResources());
                });
            }
        });
    }

    private void offFaceMode() {
        isFace = false;

        setCheckboxSize(25f, 3f);
        txt.setText("스티커 편집");

        if (faceBox != null && faceOverlay != null) {
            if (stickerFrame != null && stickerFrame.getParent() == faceOverlay) {
                faceOverlay.removeView(stickerFrame);
            }
            faceBox.clearBoxes();
            faceBox.setVisibility(View.GONE);
            faceOverlay.setVisibility(View.GONE);

            faceCheckBox.setChecked(false);

            Bundle args = getArguments();
            String stickerUrl = args.getString("stickerUrl");
            ImageView stickerImage = stickerFrame.findViewById(R.id.stickerImage);
            Glide.with(requireActivity()).load(stickerUrl).into(stickerImage);

            deleteSticker();
        }
        showStickerCentered();

        Controller.enableStickerControl(null, null, stickerFrame, stickerOverlay, faceOverlay, getResources());
        Controller.updateControllersSizeAndAngle(stickerFrame, getResources());
    }

    private void deleteSticker() {
        if (stickerFrame == null) return;
        deleteController = stickerFrame.findViewById(R.id.deleteController);
        if (deleteController == null) return;

        deleteController.setOnClickListener(view -> {
            StickerViewModel viewModel = new ViewModelProvider(requireActivity()).get(StickerViewModel.class);
            Bundle args = getArguments();
            boolean isClone = args.getBoolean("IS_CLONED_STICKER", false);

            String prevFragment = args.getString("prev_frag");

            FilterActivity activity = (FilterActivity) requireActivity();

            /// 일반스티커 삭제 undo, redo 히스토리에 기록
            */
/*if ("stickerF".equals(prevFragment)) {
                if (stickerFrame != null) {
                    activity.recordStickerDelete(stickerFrame);
                }

                if (isClone && editingStickerId != -1) {
                    List<View> deletedClones = viewModel.getCloneGroup(editingStickerId);
                    if (deletedClones != null && !deletedClones.isEmpty()) {
                        activity.recordCloneGroupDelete(editingStickerId, stickerFrame, deletedClones, meta);
                    }
                }
            }*//*


            if (isClone) {
                if (stickerFrame != null && stickerFrame.getParent() == faceOverlay) {
                    faceOverlay.removeView(stickerFrame);
                }
                viewModel.removeCloneGroup(editingStickerId, stickerOverlay);
                viewModel.setFaceStickerDataToDelete(editingStickerId);
            } else {
                Controller.removeStickerFrame(stickerFrame);
            }

            if (faceBox != null && faceOverlay != null) {
                faceBox.clearBoxes();
                faceBox.setVisibility(View.GONE);
                faceOverlay.setVisibility(View.GONE);
            }

            if ("stickerF".equals(prevFragment)) {
                activity.getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_up, 0)
                        .replace(R.id.bottomArea2, new StickersFragment())
                        .commit();

            } else if ("myStickerF".equals(prevFragment)) {
                activity.getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_up, 0)
                        .replace(R.id.bottomArea2, new MyStickersFragment())
                        .commit();
            }
        });
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
        if (faceCheckBox.isChecked() && faceBox != null && faceOverlay != null) {
            faceBox.clearBoxes();
            faceBox.setVisibility(View.GONE);
            faceOverlay.setVisibility(View.GONE);
        }
        if (bottomArea1 != null) {
            stickerEdit.setVisibility(View.GONE);
        }
    }
}*/
