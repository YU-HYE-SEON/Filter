package com.example.filter.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.filter.etc.ClickUtils;
import com.example.filter.etc.CropBoxOverlayView;
import com.example.filter.etc.FGLRenderer;
import com.example.filter.R;
import com.example.filter.fragments.ToolsFragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FilterActivity extends BaseActivity {
    private ConstraintLayout topArea;
    private GLSurfaceView photoPreview;
    private FGLRenderer renderer;
    private ImageButton backBtn, saveBtn;
    private TextView saveTxt;
    private ActivityResultLauncher<Intent> galleryReSelectionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri photoUri = result.getData().getData();
                    if (photoUri != null) {
                        loadImageFromUri(photoUri);
                        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                        getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.bottomArea, new ToolsFragment())
                                .commit();
                    }
                }
            });
    private Bitmap originalBitmap, transformedBitmap, cropBeforeBitmap,baseImageForCrop;
    private float rotationDegree = 0;
    private boolean flipHorizontal = false, flipVertical = false;
    private RectF lastCropRectN = null;  // 마지막 크롭 영역(뷰포트 기준 정규화 [0..1])
    private CropBoxOverlayView cropOverlay;
    public enum CropMode {NONE, FREE, OTO, TTF, NTS}
    private CropMode currentCropMode = CropMode.NONE;
    private float scaleFactor = 1.0f;
    private ScaleGestureDetector scaleDetector;
    private float lastTouchX = 0f, lastTouchY = 0f;
    private boolean isTwoFingerGesture = false;
    private float translateX = 0f, translateY = 0f;
    private boolean suppressNextSingleFingerMove = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);
        topArea = findViewById(R.id.topArea);
        backBtn = findViewById(R.id.backBtn);
        saveBtn = findViewById(R.id.saveBtn);
        saveTxt = findViewById(R.id.saveTxt);
        photoPreview = findViewById(R.id.photoPreview);

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
                    .replace(R.id.bottomArea, new ToolsFragment())
                    .commit();
        }

        scaleDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if (currentCropMode == CropMode.NONE) return true;

                scaleFactor *= detector.getScaleFactor();
                scaleFactor = Math.max(1.0f, Math.min(scaleFactor, 4.0f));
                renderer.setScaleFactor(scaleFactor);

                /*if (scaleFactor == 1.0f) {
                    translateX = 0;
                    translateY = 0;
                } else {
                    float maxTx = renderer.getViewportWidth() * (scaleFactor - 1f) / 2f;
                    float maxTy = renderer.getViewportHeight() * (scaleFactor - 1f) / 2f;
                    translateX = Math.max(-maxTx, Math.min(maxTx, translateX));
                    translateY = Math.max(-maxTy, Math.min(maxTy, translateY));
                }

                renderer.setTranslate(translateX, translateY);
                photoPreview.requestRender();
                return true;
            }*/
                // ② 배율별 이동 한계 계산 (확대/축소 모두에서 동작)
                float extra = Math.abs(scaleFactor - 1f); // 1에서 얼마나 벗어났는지
                float maxTx = renderer.getViewportWidth()  * extra / 2f;
                float maxTy = renderer.getViewportHeight() * extra / 2f;

                // 배율이 1 이하이면 이동을 0으로 고정(전체 보기)
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

        backBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(500)) return;

            onBackPressed();
        });

        saveBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(500)) return;

            renderer.setOnBitmapCaptureListener(fullBitmap -> {
                Bitmap cropped = renderer.cropCenterRegion(fullBitmap
                        , renderer.getViewportX()
                        , renderer.getViewportY()
                        , renderer.getViewportWidth()
                        , renderer.getViewportHeight());
                File tempFile = new File(getCacheDir(), "temp_captured_image.png");
                try (FileOutputStream out = new FileOutputStream(tempFile)) {
                    cropped.compress(Bitmap.CompressFormat.PNG, 100, out);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                Intent intent = new Intent(FilterActivity.this, SavePhotoActivity.class);
                intent.putExtra("saved_image", tempFile.getAbsolutePath());
                startActivity(intent);
            });
            renderer.captureBitmap();
        });
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
            if (renderer != null) {
                renderer.resetAllFilter();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        if (cropOverlay != null) {
            restoreCropBeforeState();
            hideCropOverlay();
            setCurrentCropMode(CropMode.NONE);
            return;
        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
        } else {
            Fragment currentFragment = fragmentManager.findFragmentById(R.id.bottomArea);
            if (currentFragment instanceof ToolsFragment) {
                Intent intent = new Intent(FilterActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            } else {
                super.onBackPressed();
            }
        }
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
        }
    }

    public void onUpdateValue(String filterType, int value) {
        if (filterType != null) {
            renderer.updateValue(filterType, value);
        }
    }

    public void onCancelValue(String filterType) {
        if (renderer != null && filterType != null) {
            renderer.cancelValue(filterType);
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
// 현재 렌더러의 비트맵을 기준으로 원본 갱신 (크롭 등 renderer에서 직접 바뀐 경우 포함)
        Bitmap current = (renderer != null) ? renderer.getCurrentBitmap() : null;
        if (current != null) {
            originalBitmap = current.copy(current.getConfig(), true);
            transformedBitmap = originalBitmap.copy(originalBitmap.getConfig(), true);
        }

        rotationDegree = 0;
        flipHorizontal = false;
        flipVertical = false;

        resetViewportTransform();
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
        if (cropBeforeBitmap != null) {
            renderer.setBitmap(cropBeforeBitmap);

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

    public void showCropOverlay(boolean fullSize, boolean fixed, int ratioX, int ratioY) {
        //cropBeforeBitmap = transformedBitmap.copy(transformedBitmap.getConfig(), true);
        if (renderer != null && renderer.getCurrentBitmap() != null) {
            cropBeforeBitmap = renderer.getCurrentBitmap().copy(renderer.getCurrentBitmap().getConfig(), true);
        }

        FrameLayout container = findViewById(R.id.photoPreviewContainer);

        if (cropOverlay != null) {
            container.removeView(cropOverlay);
        }

        if (lastCropRectN != null && baseImageForCrop != null) {
            renderer.setBitmap(baseImageForCrop);

            int w = photoPreview.getWidth();
            int h = photoPreview.getHeight();
            renderer.onSurfaceChanged(null, w, h);

            int vpW = renderer.getViewportWidth();
            int vpH = renderer.getViewportHeight();

            float rectW = lastCropRectN.width()  * vpW;
            float rectH = lastCropRectN.height() * vpH;
            float cx    = lastCropRectN.centerX() * vpW; // 뷰포트 내부 중심 x(px)
            float cy    = lastCropRectN.centerY() * vpH; // 뷰포트 내부 중심 y(px)

            float sW = vpW / rectW;
            float sH = vpH / rectH;
            float s  = Math.max(sW, sH);

            float txPx = s * (vpW / 2f - cx);
            float tyPx = s * (vpH / 2f - cy);

            scaleFactor = s;
            translateX  = txPx;
            translateY  = tyPx;

            renderer.setScaleFactor(scaleFactor);
            renderer.setTranslate(translateX, translateY);
            photoPreview.requestRender();
        } else {
            resetViewportTransform();
        }

        cropOverlay = new CropBoxOverlayView(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        container.addView(cropOverlay, params);

        int viewportX = renderer.getViewportX();
        int viewportY = renderer.getViewportY();
        int viewportWidth = renderer.getViewportWidth();
        int viewportHeight = renderer.getViewportHeight();

        cropOverlay.setFixedAspectRatio(fixed);
        cropOverlay.setAspectRatio(ratioX, ratioY);
        //cropOverlay.setScaleGestureDetector(scaleDetector);
        cropOverlay.initializeCropBox(viewportX, viewportY, viewportWidth, viewportHeight, fullSize);
    }

    public void hideCropOverlay() {
        if (cropOverlay != null) {
            ((ViewGroup) cropOverlay.getParent()).removeView(cropOverlay);
            cropOverlay = null;
        }
    }

    public void setCurrentCropMode(CropMode mode) {
        this.currentCropMode = mode;
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
                    // 두 손가락에서 하나 뗐을 때 → 다음 한 손가락 MOVE는 무시
                    suppressNextSingleFingerMove = true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (suppressNextSingleFingerMove) {
                    suppressNextSingleFingerMove = false; // 한 번만 무시
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
}