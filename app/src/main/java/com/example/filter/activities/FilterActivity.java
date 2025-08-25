package com.example.filter.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.filter.dialogs.FilterEixtDialog;
import com.example.filter.etc.ClickUtils;
import com.example.filter.etc.CropBoxOverlayView;
import com.example.filter.etc.FGLRenderer;
import com.example.filter.R;
import com.example.filter.etc.StickerStore;
import com.example.filter.fragments.ColorsFragment;
import com.example.filter.fragments.StickersFragment;
import com.example.filter.fragments.ToolsFragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class FilterActivity extends BaseActivity {
    private FrameLayout stickerOverlay;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private OnBackPressedCallback backGateCallback;
    private ImageButton undoColor, redoColor, originalColor;
    private ConstraintLayout topArea;
    private GLSurfaceView photoPreview;
    private FGLRenderer renderer;
    private ImageButton backBtn, saveBtn;
    private TextView saveTxt;
    private Bitmap originalBitmap, transformedBitmap, cropBeforeBitmap, baseImageForCrop;
    private float rotationDegree = 0;
    private boolean flipHorizontal = false, flipVertical = false;
    private RectF lastCropRectN = null;
    private CropBoxOverlayView cropOverlay;

    public enum CropMode {NONE, FREE, OTO, TTF, NTS}

    private CropMode currentCropMode = CropMode.NONE;
    private CropMode lastCropMode = CropMode.NONE;
    private CropMode appliedCropMode = CropMode.NONE;
    private float scaleFactor = 1.0f;
    private ScaleGestureDetector scaleDetector;
    private float lastTouchX = 0f, lastTouchY = 0f;
    private boolean isTwoFingerGesture = false;
    private float translateX = 0f, translateY = 0f;
    private boolean suppressNextSingleFingerMove = false;
    private float lastScale = 1f;
    private float lastTxN = 0f, lastTyN = 0f;
    private boolean needResetCropRectOnce = false;
    private Bitmap cropSessionEntryBitmap = null;
    private boolean isCropSessionLatched = false;
    private boolean colorEdited = false;
    private boolean beforePressed = false;

    private static class ColorEdit {
        final String filterType;
        final int before, after;

        ColorEdit(String t, int b, int a) {
            this.filterType = t;
            this.before = b;
            this.after = a;
        }
    }

    private final ArrayList<ColorEdit> colorHistory = new ArrayList<>();
    private int colorCursor = -1;

    public void recordColorEdit(String filterType, int before, int after) {
        if (filterType == null) return;
        if (before == after) return;

        while (colorHistory.size() > colorCursor + 1) {
            colorHistory.remove(colorHistory.size() - 1);
        }
        colorHistory.add(new ColorEdit(filterType, before, after));
        colorCursor = colorHistory.size() - 1;

        setColorEdited(true);
    }

    public boolean canUndoColor() {
        return colorCursor >= 0;
    }

    public boolean canRedoColor() {
        return colorCursor < colorHistory.size() - 1;
    }

    public void undoColor() {
        if (!canUndoColor()) return;
        ColorEdit e = colorHistory.get(colorCursor);
        if (renderer != null) renderer.updateValue(e.filterType, e.before);
        colorCursor--;
        photoPreview.requestRender();
        refreshOriginalColorButton();
    }

    public void redoColor() {
        if (!canRedoColor()) return;
        ColorEdit e = colorHistory.get(colorCursor + 1);
        if (renderer != null) renderer.updateValue(e.filterType, e.after);
        colorCursor++;
        photoPreview.requestRender();
        refreshOriginalColorButton();
    }

    private final String[] FILTER_KEYS = new String[]{
            "밝기", "노출", "대비", "하이라이트", "그림자",
            "온도", "색조", "채도", "선명하게", "흐리게", "비네트", "노이즈"
    };

    private final HashMap<String, Integer> savedFilterValues = new HashMap<>();
    private boolean isPreviewingOriginalColors = false;

    public void previewOriginalColors(boolean on) {
        if (renderer == null) return;

        if (on) {
            if (isPreviewingOriginalColors) return;
            savedFilterValues.clear();
            for (String k : FILTER_KEYS) {
                savedFilterValues.put(k, getCurrentValue(k));
            }
            renderer.resetAllFilter();
            photoPreview.requestRender();
            isPreviewingOriginalColors = true;
        } else {
            if (!savedFilterValues.isEmpty()) {
                for (String k : FILTER_KEYS) {
                    Integer v = savedFilterValues.get(k);
                    if (v != null) renderer.updateValue(k, v);
                }
                savedFilterValues.clear();
                photoPreview.requestRender();
            }
            isPreviewingOriginalColors = false;
        }
    }

    public void setUndoRedoEnabled(boolean enabled) {
        if (undoColor != null) {
            undoColor.setEnabled(enabled);
            undoColor.setAlpha(enabled ? 1f : 0.4f);
        }
        if (redoColor != null) {
            redoColor.setEnabled(enabled);
            redoColor.setAlpha(enabled ? 1f : 0.4f);
        }
    }

    private final HashMap<String, Integer> baselineFilterValues = new HashMap<>();

    private boolean isAtBaseline() {
        for (String k : FILTER_KEYS) {
            int cur = getCurrentValue(k);
            int base = baselineFilterValues.containsKey(k) ? baselineFilterValues.get(k) : 0;
            if (cur != base) return false;
        }
        return true;
    }

    public void refreshOriginalColorButton() {
        boolean same = isAtBaseline();
        if (originalColor != null) {
            originalColor.setEnabled(!same);
            originalColor.setAlpha(!same ? 1f : 0.4f);
        }
    }

    private int accumRotationDeg = 0;
    private boolean accumFlipH = false;
    private boolean accumFlipV = false;
    private boolean rotationEdited = false;

    public boolean isRotationEdited() {
        return rotationEdited;
    }

    private static int normDeg(int d) {
        int n = d % 360;
        return (n < 0) ? n + 360 : n;
    }

    private boolean isGeometrySameAsOriginal() {
        int r = normDeg(accumRotationDeg);
        if (r == 0 && !accumFlipH && !accumFlipV) return true;
        if (r == 180 && accumFlipH && accumFlipV) return true;
        return false;
    }

    private void refreshRotationEditedFlag() {
        rotationEdited = !isGeometrySameAsOriginal();
    }

    private boolean cropEdited = false;

    public boolean isCropEdited() {
        return cropEdited;
    }

    public void setCropEdited(boolean edited) {
        this.cropEdited = edited;
    }

    public void resetCropState() {
        lastCropMode = CropMode.NONE;
        appliedCropMode = CropMode.NONE;
        lastCropRectN = null;
        needResetCropRectOnce = true;

        isCropSessionLatched = false;
        cropSessionEntryBitmap = null;

        if (renderer != null && renderer.getCurrentBitmap() != null) {
            Bitmap cur = renderer.getCurrentBitmap();
            baseImageForCrop = cur.copy(cur.getConfig(), true);
        }
    }

    public float getScaleFactor() {
        return scaleFactor;
    }

    public float getTranslateX() {
        return translateX;
    }

    public float getTranslateY() {
        return translateY;
    }

    public boolean isViewportIdentity() {
        return Math.abs(scaleFactor - 1f) < 1e-3f
                && Math.abs(translateX) < 1e-2f
                && Math.abs(translateY) < 1e-2f;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_filter);
        topArea = findViewById(R.id.topArea);
        backBtn = findViewById(R.id.backBtn);
        saveBtn = findViewById(R.id.saveBtn);
        saveTxt = findViewById(R.id.saveTxt);
        photoPreview = findViewById(R.id.photoPreview);
        stickerOverlay = findViewById(R.id.stickerOverlay);

        undoColor = findViewById(R.id.undoColor);
        redoColor = findViewById(R.id.redoColor);
        originalColor = findViewById(R.id.originalColor);

        StickerStore.get().init(getApplicationContext());

        originalColor.setOnTouchListener((v, ev) -> {
            switch (ev.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    v.setPressed(true);
                    if (undoColor != null) {
                        undoColor.setEnabled(false);
                        undoColor.setAlpha(0.4f);
                    }
                    if (redoColor != null) {
                        redoColor.setEnabled(false);
                        redoColor.setAlpha(0.4f);
                    }
                    previewOriginalColors(true);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.setPressed(false);
                    previewOriginalColors(false);
                    if (undoColor != null) {
                        boolean canUndo = canUndoColor();
                        undoColor.setEnabled(canUndo);
                        undoColor.setAlpha(canUndo ? 1f : 0.4f);
                    }
                    if (redoColor != null) {
                        boolean canRedo = canRedoColor();
                        redoColor.setEnabled(canRedo);
                        redoColor.setAlpha(canRedo ? 1f : 0.4f);
                    }
                    return true;
            }
            return true;
        });

        cropOverlay = null;

        if (photoPreview != null) {
            photoPreview.setEGLContextClientVersion(2);
            renderer = new FGLRenderer(this, photoPreview);
            photoPreview.setRenderer(renderer);
            photoPreview.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        } else {
            finish();
            return;
        }

        Uri photoUri = getIntent().getData();
        if (photoUri != null) {
            loadImageFromUri(photoUri);
        } else {
            finish();
            return;
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.bottomArea2, new ToolsFragment())
                    .commit();
        }

        scaleDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if (currentCropMode == CropMode.NONE) return true;

                scaleFactor *= detector.getScaleFactor();
                scaleFactor = Math.max(1.0f, Math.min(scaleFactor, 4.0f));
                renderer.setScaleFactor(scaleFactor);

                float extra = Math.abs(scaleFactor - 1f);
                float maxTx = renderer.getViewportWidth() * extra / 2f;
                float maxTy = renderer.getViewportHeight() * extra / 2f;

                if (scaleFactor <= 1.0f) {
                    translateX = 0f;
                    translateY = 0f;
                } else {
                    translateX = Math.max(-maxTx, Math.min(maxTx, translateX));
                    translateY = Math.max(-maxTy, Math.min(maxTy, translateY));
                }

                renderer.setTranslate(translateX, translateY);
                photoPreview.requestRender();
                return true;
            }
        });

        saveBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(500)) return;

            renderer.setOnBitmapCaptureListener(fullBitmap -> {
                runOnUiThread(() -> {
                    try {
                        Bitmap composed = fullBitmap.copy(Bitmap.Config.ARGB_8888, true);
                        if (stickerOverlay != null) {
                            Canvas canvas = new Canvas(composed);
                            stickerOverlay.draw(canvas);
                        }

                        Bitmap cropped = renderer.cropCenterRegion(
                                composed,
                                renderer.getViewportX(),
                                renderer.getViewportY(),
                                renderer.getViewportWidth(),
                                renderer.getViewportHeight()
                        );

                        File tempFile = new File(getCacheDir(), "temp_captured_image.png");
                        try (FileOutputStream out = new FileOutputStream(tempFile)) {
                            cropped.compress(Bitmap.CompressFormat.PNG, 100, out);
                        }

                        Intent intent = new Intent(FilterActivity.this, SavePhotoActivity.class);
                        intent.putExtra("saved_image", tempFile.getAbsolutePath());
                        startActivity(intent);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            });
            renderer.captureBitmap();
        });

        backGateCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Fragment cur = getSupportFragmentManager().findFragmentById(R.id.bottomArea2);
                if (isBackEnabledFor(cur)) {
                    handleBackNavChain();
                } else {

                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, backGateCallback);

        getSupportFragmentManager().registerFragmentLifecycleCallbacks(
                new androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks() {
                    @Override
                    public void onFragmentResumed(@NonNull androidx.fragment.app.FragmentManager fm,
                                                  @NonNull Fragment f) {
                        if (f.getId() == R.id.bottomArea2) {
                            updateBackAndSaveUiEnabled(f);
                        }
                    }
                }, true
        );


        backBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(500)) return;
            Fragment cur = getSupportFragmentManager().findFragmentById(R.id.bottomArea2);
            if (isBackEnabledFor(cur)) {
                handleBackNavChain();
            }
        });

        Fragment cur = getSupportFragmentManager().findFragmentById(R.id.bottomArea2);
        updateBackAndSaveUiEnabled(cur);

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showExitConfirmDialog();
            }
        });

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            Intent i = new Intent(FilterActivity.this, FilterActivity.class);
                            i.setData(uri);
                            startActivity(i);
                            finish();
                        }
                    }
                }
        );

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            FrameLayout full = findViewById(R.id.fullScreenFragmentContainer);
            ConstraintLayout filter = findViewById(R.id.filterActivity);
            ConstraintLayout main = findViewById(R.id.main);

            Fragment f = getSupportFragmentManager().findFragmentById(R.id.fullScreenFragmentContainer);
            if (f == null) {
                full.setVisibility(View.GONE);
                filter.setVisibility(View.VISIBLE);
                main.setBackgroundColor(Color.BLACK);
            }
        });
    }

    private boolean isBackEnabledFor(Fragment f) {
        return (f instanceof ToolsFragment)
                || (f instanceof ColorsFragment)
                || (f instanceof StickersFragment);
    }

    private void updateBackAndSaveUiEnabled(Fragment f) {
        boolean enabled = isBackEnabledFor(f);
        if (backBtn != null) {
            backBtn.setEnabled(enabled);
            backBtn.setClickable(enabled);
            backBtn.setAlpha(enabled ? 1f : 0.3f);
        }

        if (saveBtn != null) {
            saveBtn.setEnabled(enabled);
            saveBtn.setClickable(enabled);
            saveBtn.setAlpha(enabled ? 1f : 0.3f);
            saveTxt.setAlpha(enabled ? 1f : 0.3f);
        }
    }

    public void requestUpdateBackGate() {
        Fragment cur = getSupportFragmentManager().findFragmentById(R.id.bottomArea2);
        updateBackAndSaveUiEnabled(cur);
    }

    private void loadImageFromUri(Uri photoUri) {
        if (renderer == null || photoPreview == null) {
            return;
        }

        try {
            InputStream inputStream = getContentResolver().openInputStream(photoUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) inputStream.close();

            if (bitmap == null) {
                return;
            }

            InputStream exifInputStream = getContentResolver().openInputStream(photoUri);
            ExifInterface exif = new ExifInterface(exifInputStream);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            float r = 0;
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    r = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    r = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    r = 270;
                    break;
            }
            if (exifInputStream != null) exifInputStream.close();

            if (r != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(r);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            }

            renderer.setBitmap(bitmap);

            baseImageForCrop = bitmap.copy(bitmap.getConfig(), true);

            originalBitmap = bitmap.copy(bitmap.getConfig(), true);
            transformedBitmap = originalBitmap.copy(originalBitmap.getConfig(), true);
            photoPreview.requestRender();

            accumRotationDeg = 0;
            accumFlipH = false;
            accumFlipV = false;
            refreshRotationEditedFlag();

            cropEdited = false;
            resetCropState();

            if (renderer != null) {
                renderer.resetAllFilter();
                baselineFilterValues.clear();
                for (String k : FILTER_KEYS) {
                    baselineFilterValues.put(k, getCurrentValue(k));
                }
                refreshOriginalColorButton();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleBack() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
            return;
        }

        Fragment current = fm.findFragmentById(R.id.bottomArea2);
        if (current instanceof ToolsFragment) {
            Intent intent = new Intent(FilterActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        } else {
            finish();
        }
    }

    private void handleBackNavChain() {
        FragmentManager fm = getSupportFragmentManager();

        Fragment full = fm.findFragmentById(R.id.fullScreenFragmentContainer);
        if (full != null) {
            showExitConfirmDialog();
            return;
        }

        Fragment cur = fm.findFragmentById(R.id.bottomArea2);

        if (cur instanceof StickersFragment) {
            FrameLayout overlay = findViewById(R.id.stickerOverlay);
            if (overlay != null) overlay.removeAllViews();

            fm.beginTransaction()
                    .setCustomAnimations(R.anim.slide_up, 0)
                    .replace(R.id.bottomArea2, new ColorsFragment())
                    .commit();

            ConstraintLayout bottomArea1 = findViewById(R.id.bottomArea1);
            bottomArea1.setVisibility(View.VISIBLE);

            return;
        }

        if (cur instanceof ColorsFragment) {
            fm.beginTransaction()
                    .setCustomAnimations(R.anim.slide_up, 0)
                    .replace(R.id.bottomArea2, new ToolsFragment())
                    .commit();

            ConstraintLayout bottomArea1 = findViewById(R.id.bottomArea1);
            bottomArea1.setVisibility(View.INVISIBLE);

            return;
        }

        if (cur instanceof ToolsFragment) {
            openImagePicker();
            return;
        }

        handleBack();
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }


    private void showExitConfirmDialog() {
        new FilterEixtDialog(this, new FilterEixtDialog.FilterEixtDialogListener() {
            @Override
            public void onKeep() {
            }

            @Override
            public void onExit() {
                finish();
            }
        }
        ).withMessage("편집한 내용을 저장하지 않고\n종료하시겠습니까?")
                .withButton1Text("예")
                .withButton2Text("아니오")
                .show();
    }


    public int getCurrentValue(String filterType) {
        if (renderer != null && filterType != null) {
            return renderer.getCurrentValue(filterType);
        }
        return 0;
    }

    public void onTempValue(String filterType, int value) {
        if (renderer != null && filterType != null) {
            renderer.setTempValue(filterType, value);
            refreshOriginalColorButton();
        }
    }

    public void onUpdateValue(String filterType, int value) {
        if (filterType != null) {
            renderer.updateValue(filterType, value);
            refreshOriginalColorButton();
        }
    }

    public void onCancelValue(String filterType) {
        if (renderer != null && filterType != null) {
            renderer.cancelValue(filterType);
            refreshOriginalColorButton();
        }
    }

    public void rotatePhoto(int degree) {
        rotationDegree = (rotationDegree + degree) % 360;
        applyTransform();
    }

    public void flipPhoto(boolean horizontal) {
        if (horizontal) flipHorizontal = !flipHorizontal;
        else flipVertical = !flipVertical;
        applyTransform();
    }

    public void restoreOriginalPhoto() {
        rotationDegree = 0;
        flipHorizontal = false;
        flipVertical = false;
        if (originalBitmap != null) {
            renderer.setBitmap(originalBitmap);
            photoPreview.requestRender();
        }
    }

    public void commitTransformations() {
        commitTransformations(false);
    }

    public void commitTransformations(boolean imageGeometryChanged) {
        int pendingRot = normDeg((int) rotationDegree);
        boolean pendingFlipH = flipHorizontal;
        boolean pendingFlipV = flipVertical;

        Bitmap current = (renderer != null) ? renderer.getCurrentBitmap() : null;
        if (current != null) {
            originalBitmap = current.copy(current.getConfig(), true);
            transformedBitmap = originalBitmap.copy(originalBitmap.getConfig(), true);
            renderer.setBitmap(originalBitmap);
        }

        if (imageGeometryChanged && baseImageForCrop != null) {
            Matrix m = new Matrix();
            m.postRotate(rotationDegree);
            float sx = flipHorizontal ? -1f : 1f;
            float sy = flipVertical ? -1f : 1f;
            m.postScale(sx, sy);
            baseImageForCrop = Bitmap.createBitmap(
                    baseImageForCrop, 0, 0,
                    baseImageForCrop.getWidth(), baseImageForCrop.getHeight(),
                    m, true
            );

            if (lastCropRectN != null) {
                int rot = ((int) rotationDegree % 360 + 360) % 360;
                lastCropRectN = mapRectN(lastCropRectN, rot, flipHorizontal, flipVertical);
            } else {
                needResetCropRectOnce = true;
            }

            float[] p = mapPanN(lastTxN, lastTyN, (int) rotationDegree, flipHorizontal, flipVertical);
            lastTxN = p[0];
            lastTyN = p[1];
        }

        if (imageGeometryChanged) {
            accumRotationDeg = normDeg(accumRotationDeg + pendingRot);
            if (pendingFlipH) accumFlipH = !accumFlipH;
            if (pendingFlipV) accumFlipV = !accumFlipV;
            refreshRotationEditedFlag();
        }

        rotationDegree = 0f;
        flipHorizontal = false;
        flipVertical = false;
        resetViewportTransform();
    }

    public void lockInCurrentCropMode() {
        if (lastCropMode != CropMode.NONE) {
            appliedCropMode = lastCropMode;
        }
    }

    public void revertLastSelectionToApplied() {
        lastCropMode = appliedCropMode;
    }

    public CropMode getAppliedCropMode() {
        return appliedCropMode;
    }

    private Rect calcCenteredCropRect(int vpX, int vpY, int vpW, int vpH,
                                      boolean fullSize, boolean fixed, int ratioX, int ratioY) {
        final float factor = fullSize ? 1.0f : 0.9f;

        if (!fixed || ratioX <= 0 || ratioY <= 0) {
            int w = Math.round(vpW * factor);
            int h = Math.round(vpH * factor);
            int l = vpX + (vpW - w) / 2;
            int t = vpY + (vpH - h) / 2;
            return new Rect(l, t, l + w, t + h);
        }

        float target = (float) ratioX / (float) ratioY;
        int cw, ch;

        float vRatio = (float) vpW / (float) vpH;
        if (vRatio > target) {
            ch = Math.round(vpH * factor);
            cw = Math.round(ch * target);
        } else {
            cw = Math.round(vpW * factor);
            ch = Math.round(cw / target);
        }

        int left = vpX + (vpW - cw) / 2;
        int top = vpY + (vpH - ch) / 2;
        return new Rect(left, top, left + cw, top + ch);
    }

    private float[] mapPointN(float x, float y, int rotDeg, boolean flipH, boolean flipV) {
        if (flipH) x = 1f - x;
        if (flipV) y = 1f - y;

        int r = ((rotDeg % 360) + 360) % 360;
        switch (r) {
            case 90:
                return new float[]{1f - y, x};
            case 180:
                return new float[]{1f - x, 1f - y};
            case 270:
                return new float[]{y, 1f - x};
            default:
                return new float[]{x, y};
        }
    }

    private RectF mapRectN(RectF src, int rotDeg, boolean flipH, boolean flipV) {
        if (src == null) return null;
        float[] lt = mapPointN(src.left, src.top, rotDeg, flipH, flipV);
        float[] rb = mapPointN(src.right, src.bottom, rotDeg, flipH, flipV);
        RectF out = new RectF(
                Math.min(lt[0], rb[0]),
                Math.min(lt[1], rb[1]),
                Math.max(lt[0], rb[0]),
                Math.max(lt[1], rb[1])
        );

        out.left = Math.max(0f, Math.min(1f, out.left));
        out.top = Math.max(0f, Math.min(1f, out.top));
        out.right = Math.max(0f, Math.min(1f, out.right));
        out.bottom = Math.max(0f, Math.min(1f, out.bottom));
        return out;
    }

    private float[] mapPanN(float txN, float tyN, int rotDeg, boolean flipH, boolean flipV) {
        if (flipH) txN = -txN;
        if (flipV) tyN = -tyN;

        int r = ((rotDeg % 360) + 360) % 360;
        switch (r) {
            case 90:
                return new float[]{tyN, -txN};
            case 180:
                return new float[]{-txN, -tyN};
            case 270:
                return new float[]{-tyN, txN};
            default:
                return new float[]{txN, tyN};
        }
    }

    private void applyTransform() {
        if (originalBitmap == null) return;
        Matrix matrix = new Matrix();
        matrix.postRotate(rotationDegree);
        float sx = flipHorizontal ? -1f : 1f;
        float sy = flipVertical ? -1f : 1f;
        matrix.postScale(sx, sy);
        transformedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0,
                originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);

        if (transformedBitmap != null) {
            renderer.setBitmap(transformedBitmap);
        }
    }

    public CropBoxOverlayView getCropOverlay() {
        return cropOverlay;
    }

    public Bitmap getCropBeforeBitmap() {
        return cropBeforeBitmap;
    }

    public FGLRenderer getRenderer() {
        return renderer;
    }

    public GLSurfaceView getPhotoPreview() {
        return photoPreview;
    }

    public void restoreCropBeforeState() {
        Bitmap restore = (isCropSessionLatched && cropSessionEntryBitmap != null)
                ? cropSessionEntryBitmap
                : cropBeforeBitmap;

        if (restore != null) {
            renderer.setBitmap(restore);

            scaleFactor = 1.0f;
            translateX = 0f;
            translateY = 0f;
            rotationDegree = 0f;
            flipHorizontal = false;
            flipVertical = false;

            renderer.setScaleFactor(scaleFactor);
            renderer.setTranslate(translateX, translateY);
            photoPreview.requestRender();
        }
    }

    public void showCropOverlay(boolean fullSize, boolean fixed, int ratioX, int ratioY, boolean restorePrevRect) {
        if (!isCropSessionLatched && renderer != null && renderer.getCurrentBitmap() != null) {
            Bitmap cur = renderer.getCurrentBitmap();
            cropSessionEntryBitmap = cur.copy(cur.getConfig(), true);
            isCropSessionLatched = true;
        }

        if (renderer != null && renderer.getCurrentBitmap() != null) {
            cropBeforeBitmap = renderer.getCurrentBitmap()
                    .copy(renderer.getCurrentBitmap().getConfig(), true);
        }

        FrameLayout container = findViewById(R.id.photoPreviewContainer);
        if (cropOverlay != null) container.removeView(cropOverlay);

        if (restorePrevRect && baseImageForCrop != null) {
            renderer.setBitmap(baseImageForCrop);

            int w = photoPreview.getWidth();
            int h = photoPreview.getHeight();
            renderer.onSurfaceChanged(null, w, h);

            restoreViewTransformFromSnapshot();
        } else {
            resetViewportTransform();
        }

        cropOverlay = new CropBoxOverlayView(this);
        container.addView(cropOverlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        int vpX = renderer.getViewportX();
        int vpY = renderer.getViewportY();
        int vpW = renderer.getViewportWidth();
        int vpH = renderer.getViewportHeight();

        cropOverlay.setFixedAspectRatio(fixed);
        cropOverlay.setAspectRatio(ratioX, ratioY);
        cropOverlay.setViewportInfo(vpX, vpY, vpW, vpH);

        Rect prev = (restorePrevRect) ? getLastCropRectInViewportPx() : null;
        if (prev != null) {
            cropOverlay.setCropRect(prev);
            needResetCropRectOnce = false;
            return;
        }

        if (needResetCropRectOnce) {
            Rect center = calcCenteredCropRect(vpX, vpY, vpW, vpH, fullSize, fixed, ratioX, ratioY);
            cropOverlay.setCropRect(center);
            needResetCropRectOnce = false;
            return;
        }

        cropOverlay.initializeCropBox(vpX, vpY, vpW, vpH, fullSize);
    }

    public void hideCropOverlay() {
        if (cropOverlay != null) {
            ((ViewGroup) cropOverlay.getParent()).removeView(cropOverlay);
            cropOverlay = null;
        }

        isCropSessionLatched = false;
        cropSessionEntryBitmap = null;
    }

    public void setCurrentCropMode(CropMode mode) {
        this.currentCropMode = mode;

        if (mode != CropMode.NONE) {
            this.lastCropMode = mode;
        }
    }

    public CropMode getLastCropMode() {
        return lastCropMode;
    }

    private Rect getLastCropRectInViewportPx() {
        if (lastCropRectN == null || renderer == null) return null;
        int vpX = renderer.getViewportX();
        int vpY = renderer.getViewportY();
        int vpW = renderer.getViewportWidth();
        int vpH = renderer.getViewportHeight();

        int l = vpX + Math.round(lastCropRectN.left * vpW);
        int t = vpY + Math.round(lastCropRectN.top * vpH);
        int r = vpX + Math.round(lastCropRectN.right * vpW);
        int b = vpY + Math.round(lastCropRectN.bottom * vpH);
        return new Rect(l, t, r, b);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                isTwoFingerGesture = false;
                suppressNextSingleFingerMove = false;
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                if (event.getPointerCount() == 2) {
                    isTwoFingerGesture = true;
                    suppressNextSingleFingerMove = false;
                }
                break;

            case MotionEvent.ACTION_POINTER_UP:
                if (event.getPointerCount() == 2) {
                    suppressNextSingleFingerMove = true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (suppressNextSingleFingerMove) {
                    suppressNextSingleFingerMove = false;
                    break;
                }

                if (currentCropMode != CropMode.NONE && !isTwoFingerGesture && event.getPointerCount() == 1) {
                    float x = event.getX();
                    float y = event.getY();

                    float dx = (x - lastTouchX);
                    float dy = (y - lastTouchY);

                    if (scaleFactor > 1.0f) {
                        float newTranslateX = translateX + dx;
                        float newTranslateY = translateY - dy;

                        float maxTranslateX = renderer.getViewportWidth() * (scaleFactor - 1f) / 2f;
                        float maxTranslateY = renderer.getViewportHeight() * (scaleFactor - 1f) / 2f;

                        newTranslateX = Math.max(-maxTranslateX, Math.min(maxTranslateX, newTranslateX));
                        newTranslateY = Math.max(-maxTranslateY, Math.min(maxTranslateY, newTranslateY));

                        translateX = newTranslateX;
                        translateY = newTranslateY;

                        lastTouchX = x;
                        lastTouchY = y;

                        renderer.setTranslate(translateX, translateY);
                        photoPreview.requestRender();
                    }
                }
                break;

            case MotionEvent.ACTION_UP:

            case MotionEvent.ACTION_CANCEL:
                isTwoFingerGesture = false;
                break;
        }
        if (event.getPointerCount() >= 2) {
            scaleDetector.onTouchEvent(event);
        }

        return true;
    }

    public void resetViewportTransform() {
        scaleFactor = 1.0f;
        translateX = 0f;
        translateY = 0f;

        if (renderer != null) {
            renderer.setScaleFactor(1.0f);
            renderer.setTranslate(0f, 0f);
            photoPreview.requestRender();
        }
    }

    public void setLastCropRectNormalized(RectF rectN) {
        this.lastCropRectN = rectN;
    }

    public void snapshotViewTransformForRestore() {
        int vpW = renderer.getViewportWidth();
        int vpH = renderer.getViewportHeight();
        if (vpW <= 0 || vpH <= 0) return;

        lastScale = scaleFactor;
        lastTxN = translateX / (float) vpW;
        lastTyN = translateY / (float) vpH;
    }

    private void restoreViewTransformFromSnapshot() {
        int vpW = renderer.getViewportWidth();
        int vpH = renderer.getViewportHeight();
        scaleFactor = lastScale;
        translateX = lastTxN * vpW;
        translateY = lastTyN * vpH;
        renderer.setScaleFactor(scaleFactor);
        renderer.setTranslate(translateX, translateY);
        photoPreview.requestRender();
    }

    public boolean isColorEdited() {
        return colorEdited;
    }

    public void setColorEdited(boolean edited) {
        this.colorEdited = edited;
        refreshOriginalColorButton();
        refreshOriginalColorButton();
    }

    public boolean isBeforePressed() {
        return beforePressed;
    }

    public void setBeforePressed(boolean pressed) {
        this.beforePressed = pressed;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null && intent.getBooleanExtra("EXIT_BY_CHILD", false)) {
            finish();
        }
    }

}