package com.example.filter.fragments;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.filter.R;
import com.example.filter.etc.ClickUtils;
import com.example.filter.etc.Controller;
import com.example.filter.etc.FaceDetect;
import com.example.filter.etc.StickerMeta;
import com.example.filter.etc.StickerViewModel;
import com.example.filter.overlayviews.FaceBoxOverlayView;

import java.io.File;

public class EditStickerFragment extends Fragment {
    private View stickerFrame;
    private ImageView deleteController, faceModel;
    private CheckBox checkBox;
    private ImageButton cancelBtn, checkBtn;
    private FrameLayout stickerOverlay, faceOverlay;
    //ImageButton undoSticker, redoSticker, originalSticker;
    private FaceBoxOverlayView faceBox;
    private final int WRAPPER_MIN_DP = 100;
    private float x, y, r;
    private int w, h;
    private StickerMeta meta;
    public static boolean isFace = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f_edit_my_sticker, container, false);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        checkBox = v.findViewById(R.id.checkBox);
        cancelBtn = v.findViewById(R.id.cancelBtn);
        checkBtn = v.findViewById(R.id.checkBtn);

        stickerOverlay = requireActivity().findViewById(R.id.stickerOverlay);
        faceOverlay = requireActivity().findViewById(R.id.faceOverlay);
        faceModel = requireActivity().findViewById(R.id.faceModel);

        /*undoSticker = requireActivity().findViewById(R.id.undoSticker);
        redoSticker = requireActivity().findViewById(R.id.redoSticker);
        originalSticker = requireActivity().findViewById(R.id.originalSticker);

        if (undoSticker != null) undoSticker.setVisibility(View.INVISIBLE);
        if (redoSticker != null) redoSticker.setVisibility(View.INVISIBLE);
        if (originalSticker != null) originalSticker.setVisibility(View.INVISIBLE);*/

        if (stickerFrame == null && stickerOverlay.getChildCount() > 0) {
            stickerFrame = stickerOverlay.getChildAt(stickerOverlay.getChildCount() - 1);
        }

        if (stickerFrame != null) {
            //stickerImage = stickerFrame.findViewById(R.id.stickerImage);
            deleteSticker();
        }

        Controller.enableStickerControl(null, null, stickerFrame, stickerOverlay, faceOverlay, getResources());
        Controller.updateControllersSizeAndAngle(stickerFrame, getResources());

        Bundle args = getArguments() != null ? getArguments() : new Bundle();
        x = args.getFloat("x", stickerFrame.getX());
        y = args.getFloat("y", stickerFrame.getY());
        w = args.getInt("w", stickerFrame.getLayoutParams().width);
        h = args.getInt("h", stickerFrame.getLayoutParams().height);
        r = args.getFloat("r", stickerFrame.getRotation());

        int minPx = Controller.dp(WRAPPER_MIN_DP, getResources());
        int initW = Math.max(minPx, w);
        int initH = Math.max(minPx, h);

        stickerFrame.setX(x);
        stickerFrame.setY(y);
        stickerFrame.getLayoutParams().width = initW;
        stickerFrame.getLayoutParams().height = initH;
        stickerFrame.requestLayout();
        stickerFrame.setRotation(r);

        Bundle args4 = getArguments();
        if (args4 != null && args4.getBoolean("IS_CLONED_STICKER", false)) {
            checkBox.setChecked(true);
            checkBox.setEnabled(false);
            checkBox.setAlpha(0.4f);
            faceOverlay.setVisibility(View.VISIBLE);
            onFaceMode();
            showStickerCentered();
            Controller.enableStickerControl(null, null, stickerFrame, stickerOverlay, faceOverlay, getResources());
            Controller.updateControllersSizeAndAngle(stickerFrame, getResources());
            deleteSticker();
        }

        cancelBtn.setOnClickListener(x -> {
            if (ClickUtils.isFastClick(x, 400)) return;

            Bundle args3 = getArguments();
            boolean isClone = args3.getBoolean("IS_CLONED_STICKER", false);
            if (checkBox.isChecked()) {
                if (stickerFrame != null && stickerFrame.getParent() == faceOverlay) {
                    faceOverlay.removeView(stickerFrame);
                    stickerOverlay.addView(stickerFrame);
                }
                //Controller.enableStickerControl(null, null, stickerFrame, stickerOverlay, faceOverlay, getResources());
                //Controller.updateControllersSizeAndAngle(stickerFrame, getResources());

                offFaceMode();
                showStickerCentered();
                faceOverlay.removeAllViews();
                faceOverlay.setVisibility(View.GONE);

                if (!isClone) {
                    checkBox.setChecked(false);
                }
            }

            Controller.setControllersVisible(stickerFrame, false);

            String prevFragment = args3.getString("prev_frag");
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

        checkBtn.setOnClickListener(x -> {
            if (ClickUtils.isFastClick(x, 400)) return;

            StickerViewModel viewModel = new ViewModelProvider(requireActivity()).get(StickerViewModel.class);
            viewModel.setTempView(stickerFrame);

            Fragment stickerFragment = new StickersFragment();
            Bundle args2 = new Bundle();

            boolean isClone = getArguments().getBoolean("IS_CLONED_STICKER", false);

            if (isClone) {
                faceModel.post(() -> {
                    Bitmap bmp = Bitmap.createBitmap(faceModel.getWidth(), faceModel.getHeight(), Bitmap.Config.ARGB_8888);
                    Canvas c = new Canvas(bmp);
                    faceModel.draw(c);
                    FaceDetect.detectFaces(bmp, faceBox, (faces, bitmap) -> {
                        if (faces.isEmpty()) return;
                        meta = StickerMeta.calculate(faces.get(0), bitmap, stickerFrame, faceOverlay);

                        if (isClone) {

                        }

                        faceOverlay.setVisibility(View.GONE);


                        args2.putBoolean("IS_FACE_MODE", true);
                        args2.putFloat("relX", meta.relX);
                        args2.putFloat("relY", meta.relY);
                        args2.putFloat("relW", meta.relW);
                        args2.putFloat("relH", meta.relH);
                        args2.putFloat("rot", meta.rot);
                        stickerFragment.setArguments(args2);
                    });
                });
            }

            Controller.removeStickerFrame(stickerFrame);

            stickerFragment.setArguments(args2);

            Controller.setControllersVisible(stickerFrame, false);

            requireActivity().runOnUiThread(() ->
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .setCustomAnimations(R.anim.slide_up, 0)
                            .replace(R.id.bottomArea2, stickerFragment)
                            .commit()
            );
        });

        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            faceBox = new FaceBoxOverlayView(requireContext());
            if (isChecked) {
                isFace = true;

                if (stickerFrame != null && stickerFrame.getParent() == stickerOverlay) {
                    stickerOverlay.removeView(stickerFrame);
                }
                faceOverlay.setVisibility(View.VISIBLE);

                LayoutInflater inflater = LayoutInflater.from(requireContext());
                View newSticker = inflater.inflate(R.layout.v_sticker_edit, faceOverlay, false);

                String path = getArguments() != null ? getArguments().getString("sticker_path", null) : null;
                ImageView newStickerImage = newSticker.findViewById(R.id.stickerImage);
                if (path != null) {
                    File f = new File(path);
                    if (f.exists()) newStickerImage.setImageURI(Uri.fromFile(f));
                    else {
                        int resId = getResources().getIdentifier(path, "drawable", requireContext().getPackageName());
                        if (resId != 0) newStickerImage.setImageResource(resId);
                    }
                } else {
                    ImageView oldImage = stickerFrame.findViewById(R.id.stickerImage);
                    newStickerImage.setImageDrawable(oldImage.getDrawable());
                }

                faceOverlay.addView(newSticker);
                stickerFrame = newSticker;
                deleteSticker();

                faceOverlay.addView(faceBox, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                showStickerCentered();

                faceModel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        int w = faceModel.getWidth();
                        int h = faceModel.getHeight();
                        if (w <= 0 || h <= 0) return;

                        faceModel.getViewTreeObserver().removeOnGlobalLayoutListener(this);

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
            float cx = (stickerOverlay.getWidth() - sizePx) / 2f;
            float cy = (stickerOverlay.getHeight() - sizePx) / 2f;
            stickerFrame.setX(cx);
            stickerFrame.setY(cy);
            stickerFrame.setRotation(0f);

            stickerFrame.postDelayed(() -> {
                stickerFrame.setVisibility(View.VISIBLE);
            }, 50);
        });
    }

    private void onFaceMode() {
        if (checkBox.isChecked()) {
            isFace = true;

            if (stickerFrame != null && stickerFrame.getParent() == stickerOverlay) {
                stickerOverlay.removeView(stickerFrame);
            }
            faceOverlay.setVisibility(View.VISIBLE);

            LayoutInflater inflater = LayoutInflater.from(requireContext());
            View newSticker = inflater.inflate(R.layout.v_sticker_edit, faceOverlay, false);

            String path = getArguments() != null ? getArguments().getString("sticker_path", null) : null;
            ImageView newStickerImage = newSticker.findViewById(R.id.stickerImage);
            if (path != null) {
                File f = new File(path);
                if (f.exists()) newStickerImage.setImageURI(Uri.fromFile(f));
                else {
                    int resId = getResources().getIdentifier(path, "drawable", requireContext().getPackageName());
                    if (resId != 0) newStickerImage.setImageResource(resId);
                }
            } else {
                ImageView oldImage = stickerFrame.findViewById(R.id.stickerImage);
                newStickerImage.setImageDrawable(oldImage.getDrawable());
            }

            faceOverlay.addView(newSticker);
            stickerFrame = newSticker;
            deleteSticker();

            faceOverlay.addView(faceBox, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            showStickerCentered();

            faceModel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    int w = faceModel.getWidth();
                    int h = faceModel.getHeight();
                    if (w <= 0 || h <= 0) return;

                    faceModel.getViewTreeObserver().removeOnGlobalLayoutListener(this);

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
    }

    private void offFaceMode() {
        if (!checkBox.isChecked()) {
            isFace = false;
            if (faceBox != null && faceOverlay != null) {
                if (stickerFrame != null && stickerFrame.getParent() == faceOverlay) {
                    faceOverlay.removeView(stickerFrame);
                }
                faceBox.clearBoxes();
                faceBox.setVisibility(View.GONE);
                faceOverlay.setVisibility(View.GONE);

                StickerViewModel viewModel = new ViewModelProvider(requireActivity()).get(StickerViewModel.class);
                View originalSticker = viewModel.getTempView();

                LayoutInflater inflater = LayoutInflater.from(requireContext());
                View newSticker = inflater.inflate(R.layout.v_sticker_edit, stickerOverlay, false);
                ImageView newStickerImage = newSticker.findViewById(R.id.stickerImage);

                if (originalSticker != null) {
                    ImageView oldImage = originalSticker.findViewById(R.id.stickerImage);
                    newStickerImage.setImageDrawable(oldImage.getDrawable());
                } else {
                    String path = getArguments() != null ? getArguments().getString("sticker_path", null) : null;
                    if (path != null) {
                        File f = new File(path);
                        if (f.exists()) newStickerImage.setImageURI(Uri.fromFile(f));
                        else {
                            int resId = getResources().getIdentifier(path, "drawable", requireContext().getPackageName());
                            if (resId != 0) newStickerImage.setImageResource(resId);
                        }
                    }
                }

                stickerOverlay.addView(newSticker);
                stickerFrame = newSticker;
                deleteSticker();
            }
            showStickerCentered();

            Controller.enableStickerControl(null, null, stickerFrame, stickerOverlay, faceOverlay, getResources());
            Controller.updateControllersSizeAndAngle(stickerFrame, getResources());
        }
    }

    private void deleteSticker() {
        if (stickerFrame == null) return;
        deleteController = stickerFrame.findViewById(R.id.deleteController);
        if (deleteController == null) return;

        deleteController.setOnClickListener(x -> {
            StickerViewModel vm = new ViewModelProvider(requireActivity()).get(StickerViewModel.class);
            Bundle args = getArguments();
            boolean isClone = args.getBoolean("IS_CLONED_STICKER", false);

            if (isClone) {
                Object tagId = stickerFrame.getTag(R.id.tag_sticker_clone_id);
                if (tagId instanceof Integer) {
                    vm.removeGroup((Integer) tagId, stickerOverlay);
                }

                faceOverlay.setVisibility(View.GONE);
            }

            Controller.removeStickerFrame(stickerFrame);

            String prevFragment = args.getString("prev_frag");
            if ("stickerF".equals(prevFragment)) {
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_up, 0)
                        .replace(R.id.bottomArea2, new StickersFragment())
                        .commit();
            } else if ("myStickerF".equals(prevFragment)) {
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_up, 0)
                        .replace(R.id.bottomArea2, new MyStickersFragment())
                        .commit();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (checkBox.isChecked() && faceBox != null && faceOverlay != null) {
            faceBox.clearBoxes();
            faceBox.setVisibility(View.GONE);
            faceOverlay.setVisibility(View.GONE);
        }
    }
}