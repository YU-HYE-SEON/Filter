package com.example.filter.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.example.filter.R;
import com.example.filter.dialogs.FilterEixtDialog;
import com.example.filter.etc.FaceModeViewModel;
import com.example.filter.etc.FaceStickerData;
import com.example.filter.fragments.FaceFragment;
import com.example.filter.overlayviews.BrushOverlayView;
import com.example.filter.etc.ClickUtils;
import com.example.filter.overlayviews.CropBoxOverlayView;
import com.example.filter.etc.FGLRenderer;
import com.example.filter.apis.dto.FilterDtoCreateRequest;
import com.example.filter.etc.StickerStore;
import com.example.filter.fragments.ColorsFragment;
import com.example.filter.fragments.StickersFragment;
import com.example.filter.fragments.ToolsFragment;
import com.google.mlkit.vision.face.Face;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FilterActivity extends BaseActivity {
    /// UI ///
    private FrameLayout stickerOverlay, brushOverlay;
    private GLSurfaceView photoPreview;
    private ImageButton backBtn, saveBtn;
    private ImageButton undoColor, redoColor, originalColor;
    //private ImageButton undoSticker, redoSticker, originalSticker;
    private TextView saveTxt;

    /// renderer, photoImage ///
    private FGLRenderer renderer;
    private Bitmap originalBitmap, transformedBitmap, cropBeforeBitmap, baseImageForCrop;

    /// 시스템 ///
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private OnBackPressedCallback backGateCallback;

    /// 크롭 ///
    public enum CropMode {NONE, FREE, OTO, TTF, NTS;}

    private CropMode currentCropMode = CropMode.NONE;
    private CropMode lastCropMode = CropMode.NONE;
    private CropMode appliedCropMode = CropMode.NONE;
    private CropBoxOverlayView cropOverlay;
    private RectF lastCropRectN = null;
    private boolean cropEdited = false;
    private boolean needResetCropRectOnce = false;
    private Bitmap cropSessionEntryBitmap = null;
    private boolean isCropSessionLatched = false;
    public RectF appliedCropRectN = null;

    /// transform, gesture ///
    private float scaleFactor = 1.0f;
    private float translateX = 0f, translateY = 0f;
    private float lastScale = 1f, lastTxN = 0f, lastTyN = 0f;
    private ScaleGestureDetector scaleDetector;
    private float lastTouchX = 0f, lastTouchY = 0f;
    private boolean isTwoFingerGesture = false;
    private boolean suppressNextSingleFingerMove = false;

    /// 회전, 반전 ///
    private float rotationDegree = 0;
    private boolean flipHorizontal = false, flipVertical = false;
    private int accumRotationDeg = 0;
    private boolean lastRotationLeft = false, lastRotationRight = false;
    private boolean accumFlipH = false, accumFlipV = false;
    private boolean rotationEdited = false;

    /// 컬러 ///
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
    private boolean colorEdited = false;
    private final String[] FILTER_KEYS = new String[]{
            "밝기", "노출", "대비", "하이라이트", "그림자",
            "온도", "색조", "채도", "선명하게", "흐리게", "비네트", "노이즈"
    };
    private final HashMap<String, Integer> baselineFilterValues = new HashMap<>();
    private final HashMap<String, Integer> savedFilterValues = new HashMap<>();
    private boolean isPreviewingOriginalColors = false;

    /// 브러쉬, 스티커 ///
    private static class StickerOp {
        final ArrayList<View> views = new ArrayList<>();
        final ArrayList<Integer> positions = new ArrayList<>();
    }

    private static class StickerEdit {
        View view;
        float bx, by, br;
        int bw, bh;
        float ax, ay, ar;
        int aw, ah;
    }

    public static class ErasePatch {
        public Rect rect;
        public Bitmap before;
    }

    public static class EraseOp {
        public ImageView view;
        public ArrayList<ErasePatch> patches = new ArrayList<>();
        public Path pathOnBitmap;
        public float strokeWidthPx;
    }

    private static class HistoryOp {
        StickerOp sticker;
        int brushBefore = -1;
        int brushAfter = -1;
        StickerEdit edit;
        View deletedView;
        int deletedPos = -1;
        View zChangedView = null;
        int zBeforePos = -1;
        int zAfterPos = -1;
        Float zBeforeElevation = null;
        Float zAfterElevation = null;
        ArrayList<EraseOp> erases;

        boolean hasSticker() {
            return sticker != null && !sticker.views.isEmpty();
        }

        boolean hasBrush() {
            return brushBefore >= 0 && brushAfter >= 0 && brushBefore != brushAfter;
        }

        boolean hasStickerEdit() {
            return edit != null && edit.view != null;
        }

        boolean hasDeletion() {
            return deletedView != null && deletedPos >= 0;
        }

        boolean hasZChange() {
            return zChangedView != null && zBeforePos >= 0 && zAfterPos >= 0;
        }

        boolean hasErase() {
            return erases != null && !erases.isEmpty();
        }
    }

    private final ArrayList<HistoryOp> history = new ArrayList<>();
    private int historyCursor = -1;


    private List<FaceStickerData> faceStickerList = new ArrayList<>();

    /// life cycle ///
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_filter);
        backBtn = findViewById(R.id.backBtn);
        saveBtn = findViewById(R.id.saveBtn);
        saveTxt = findViewById(R.id.saveTxt);
        photoPreview = findViewById(R.id.photoPreview);
        stickerOverlay = findViewById(R.id.stickerOverlay);
        brushOverlay = findViewById(R.id.brushOverlay);

        undoColor = findViewById(R.id.undoColor);
        redoColor = findViewById(R.id.redoColor);
        originalColor = findViewById(R.id.originalColor);

        //undoSticker = findViewById(R.id.undoSticker);
        //redoSticker = findViewById(R.id.redoSticker);
        //originalSticker = findViewById(R.id.originalSticker);


        Window window = getWindow();
        window.setNavigationBarColor(Color.BLACK);

        StickerStore.get().init(getApplicationContext());

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
            final int takeFlags = getIntent().getFlags()
                    & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            try {
                getContentResolver().takePersistableUriPermission(photoUri, takeFlags);
            } catch (SecurityException ignored) {
            }

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

        setupColorButtons();
        //setupStickerButtons();
        setupSaveButton();
        setupBackNavigation();
        setupImagePicker();

        cropOverlay = null;

        Fragment cur = getSupportFragmentManager().findFragmentById(R.id.bottomArea2);
        updateBackAndSaveUiEnabled(cur);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showExitConfirmDialog();
            }
        });

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            FrameLayout full = findViewById(R.id.fullScreenFragmentContainer);
            ConstraintLayout filter = findViewById(R.id.filterActivity);
            ConstraintLayout main = findViewById(R.id.main);

            Fragment f = getSupportFragmentManager().findFragmentById(R.id.fullScreenFragmentContainer);
            if (f == null) {
                full.setVisibility(View.GONE);
                filter.setVisibility(View.VISIBLE);
                main.setBackgroundColor(Color.BLACK);
                window.setNavigationBarColor(Color.BLACK);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        /*FaceModeViewModel vm = new ViewModelProvider(this).get(FaceModeViewModel.class);
        vm.getFaceStickerData().observe(this, data -> {
            if (data == null) return;

            if (faceStickerList == null) faceStickerList = new ArrayList<>();
            faceStickerList.add(data);

            Log.d("StickerFlow", String.format(
                    "[FilterActivity] 받은 FaceStickerData → relX=%.4f, relY=%.4f, relW=%.4f, relH=%.4f, rot=%.4f, batchId=%s",
                    data.relX, data.relY, data.relW, data.relH, data.stickerR, data.batchId
            ));


            if (data.stickerBitmap != null) {
                ImageView iv = new ImageView(this);
                iv.setImageBitmap(data.stickerBitmap);
                iv.setRotation(data.stickerR);
            }

            Log.d("FilterActivity", "FaceSticker received: " +
                    "relX=" + data.relX + ", relY=" + data.relY + ", relW=" + data.relW + ", relH=" + data.relH);
        });

        vm.getFaceStickerDataToDelete().observe(this, batchId -> {
            if (batchId == null) return;

            if (faceStickerList != null) {
                boolean removed = faceStickerList.removeIf(stickerData -> batchId.equals(stickerData.batchId));

                if (removed) {
                    Log.d("StickerFlow", String.format(
                            "[FilterActivity] 이전 FaceStickerData 삭제 완료 (batchId=%s)", batchId
                    ));
                }
            }
        });*/
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null && intent.getBooleanExtra("EXIT_BY_CHILD", false)) {
            finish();
        }
    }

    /// UI Setting ///
    @SuppressLint("ClickableViewAccessibility")
    private void setupColorButtons() {
        originalColor.setOnTouchListener((v, ev) -> {
            switch (ev.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    v.setPressed(true);
                    originalColor.setAlpha(0.4f);
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
                    originalColor.setAlpha(1f);
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
    }

    /*@SuppressLint("ClickableViewAccessibility")
    private void setupStickerButtons() {
        originalSticker.setOnTouchListener((v, ev) -> {
            if (!v.isEnabled()) return true;
            switch (ev.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    v.setPressed(true);
                    originalSticker.setAlpha(0.4f);
                    if (undoSticker != null) {
                        undoSticker.setEnabled(false);
                        undoSticker.setAlpha(0.4f);
                    }
                    if (redoSticker != null) {
                        redoSticker.setEnabled(false);
                        redoSticker.setAlpha(0.4f);
                    }
                    previewOriginalStickers(true);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.setPressed(false);
                    //originalSticker.setAlpha(1f);
                    previewOriginalStickers(false);
                    refreshStickerButtons();
                    return true;
            }
            return true;
        });
    }*/

    /// 중첩 클릭되면 안 됨 ///
    private void setupSaveButton() {
        saveBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            ClickUtils.disableTemporarily(v, 800);

            renderer.setOnBitmapCaptureListener(new FGLRenderer.OnBitmapCaptureListener() {
                @Override
                public void onBitmapCaptured(Bitmap bitmap) {
                    int vX = renderer.getViewportX();
                    int vY = renderer.getViewportY();
                    int vW = renderer.getViewportWidth();
                    int vH = renderer.getViewportHeight();

                    try {
                        /*List<FilterDtoCreateRequest.Sticker> stickers = new ArrayList<>();
                        for (FaceStickerData d : faceStickerList) {
                            FilterDtoCreateRequest.Sticker s = new FilterDtoCreateRequest.Sticker();
                            s.placementType = "face";
                            s.x = d.relX;
                            s.y = d.relY;
                            s.scale = (d.relW + d.relH) / 2f;
                            //s.relW = d.relW;
                            //s.relH = d.relH;
                            s.rotation = d.stickerR;
                            s.stickerId = d.batchId.hashCode();
                            stickers.add(s);

                            Log.d("StickerFlow", String.format(
                                    "[FilterActivity:Save] 전달 준비 → relX=%.4f, relY=%.4f, relW=%.4f, relH=%.4f, rot=%.4f, batchId=%s",
                                    d.relX, d.relY, d.relW, d.relH, d.stickerR, d.batchId
                            ));
                        }*/

                        //조정값
                        FilterDtoCreateRequest.ColorAdjustments adj = new FilterDtoCreateRequest.ColorAdjustments();
                        adj.brightness = renderer.getCurrentValue("밝기") / 100f;
                        adj.exposure = renderer.getCurrentValue("노출") / 100f;
                        adj.contrast = renderer.getCurrentValue("대비") / 100f;
                        adj.highlight = renderer.getCurrentValue("하이라이트") / 100f;
                        adj.shadow = renderer.getCurrentValue("그림자") / 100f;
                        adj.temperature = renderer.getCurrentValue("온도") / 100f;
                        adj.hue = renderer.getCurrentValue("색조") / 100f;
                        //adj.saturation = renderer.getCurrentValue("채도") / 100f;
                        adj.saturation = (renderer.getCurrentValue("채도") / 100f) + 1.0f;
                        adj.sharpen = renderer.getCurrentValue("선명하게") / 100f;
                        adj.blur = renderer.getCurrentValue("흐리게") / 100f;
                        adj.vignette = renderer.getCurrentValue("비네트") / 100f;
                        adj.noise = renderer.getCurrentValue("노이즈") / 100f;

                        //브러쉬 오버레이 굽기
                        Bitmap brushBitmap = Bitmap.createBitmap(vW, vH, Bitmap.Config.ARGB_8888);
                        Canvas brushCanvas = new Canvas(brushBitmap);
                        brushCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                        brushCanvas.save();
                        brushCanvas.translate(-vX, -vY);
                        if (brushOverlay != null) brushOverlay.draw(brushCanvas);
                        brushCanvas.restore();

                        File tempBrushFile = new File(getCacheDir(), "brush_image.png");
                        try (FileOutputStream out = new FileOutputStream(tempBrushFile)) {
                            brushBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                        }

                        //스티커 오버레이 굽기
                        Bitmap stickerBitmap = Bitmap.createBitmap(vW, vH, Bitmap.Config.ARGB_8888);
                        Canvas stickerCanvas = new Canvas(stickerBitmap);
                        stickerCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                        stickerCanvas.save();
                        stickerCanvas.translate(-vX, -vY);
                        if (stickerOverlay != null) {
                            for (int i = 0; i < stickerOverlay.getChildCount(); i++) {
                                View child = stickerOverlay.getChildAt(i);
                                Object isFaceGenerated = child.getTag(R.id.tag_face_generated);

                                if (!Boolean.TRUE.equals(isFaceGenerated)) {
                                    child.draw(stickerCanvas);
                                }
                            }
                        }
                        stickerCanvas.restore();

                        File tempStickerFile = new File(getCacheDir(), "sticker_image.png");
                        try (FileOutputStream out = new FileOutputStream(tempStickerFile)) {
                            stickerBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                        }

                        //브러쉬와 스티커 포함된 최종 편집된 이미지
                        Bitmap finalBitmap = Bitmap.createBitmap(vW, vH, Bitmap.Config.ARGB_8888);
                        Canvas finalCanvas = new Canvas(finalBitmap);

                        finalCanvas.drawBitmap(bitmap, 0, 0, null);
                        finalCanvas.save();
                        finalCanvas.translate(-vX, -vY);
                        if (stickerOverlay != null) stickerOverlay.draw(finalCanvas);
                        if (brushOverlay != null) brushOverlay.draw(finalCanvas);
                        finalCanvas.restore();

                        File tempFile = new File(getCacheDir(), "temp_captured_image.png");
                        try (FileOutputStream out = new FileOutputStream(tempFile)) {
                            finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                        }

                        //데이터 전달
                        Intent intent = new Intent(FilterActivity.this, SavePhotoActivity.class);
                        intent.putExtra("saved_image", tempFile.getAbsolutePath());
                        intent.putExtra("original_image_path", getIntent().getData().toString());

                        if (appliedCropRectN != null) {
                            intent.putExtra("cropRectN_l", appliedCropRectN.left);
                            intent.putExtra("cropRectN_t", appliedCropRectN.top);
                            intent.putExtra("cropRectN_r", appliedCropRectN.right);
                            intent.putExtra("cropRectN_b", appliedCropRectN.bottom);
                        }
                        intent.putExtra("accumRotationDeg", accumRotationDeg);
                        intent.putExtra("accumFlipH", accumFlipH);
                        intent.putExtra("accumFlipV", accumFlipV);

                        intent.putExtra("color_adjustments", adj);
                        intent.putExtra("brush_image_path", tempBrushFile.getAbsolutePath());
                        intent.putExtra("sticker_image_path", tempStickerFile.getAbsolutePath());

                        //intent.putExtra("face_stickers", new ArrayList<>(faceStickerList));

                        startActivity(intent);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            renderer.captureBitmap();
        });
    }

    private void setupBackNavigation() {
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
                    public void onFragmentResumed(FragmentManager fm, Fragment f) {
                        if (f.getId() == R.id.bottomArea2) {
                            updateBackAndSaveUiEnabled(f);
                        }
                    }
                }, true
        );


        backBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            Fragment cur = getSupportFragmentManager().findFragmentById(R.id.bottomArea2);
            if (isBackEnabledFor(cur)) {
                handleBackNavChain();
            }
        });
    }

    private void setupImagePicker() {
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
    }

    /// 사진 이미지 가져오기 ///
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

        updateBrushClipFromRenderer();
    }

    /// transform ///
    public void resetViewportTransform() {
        scaleFactor = 1.0f;
        translateX = 0f;
        translateY = 0f;

        if (renderer != null) {
            renderer.setScaleFactor(1.0f);
            renderer.setTranslate(0f, 0f);
            photoPreview.requestRender();
        }

        updateBrushClipFromRenderer();
    }

    public boolean isViewportIdentity() {
        return Math.abs(scaleFactor - 1f) < 1e-3f
                && Math.abs(translateX) < 1e-2f
                && Math.abs(translateY) < 1e-2f;
    }

    /// gesture ///
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

    /// 크롭 ///
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

    public void setLastCropRectNormalized(RectF rectN) {
        this.lastCropRectN = rectN;
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

    public boolean isCropEdited() {
        return cropEdited;
    }

    public void setCropEdited(boolean edited) {
        this.cropEdited = edited;
    }

    public void lockInCurrentCropMode() {
        if (lastCropMode != CropMode.NONE) {
            appliedCropMode = lastCropMode;
        }
    }

    public void revertLastSelectionToApplied() {
        lastCropMode = appliedCropMode;
    }

    public CropBoxOverlayView getCropOverlay() {
        return cropOverlay;
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

    /// 회전, 반전 ///
    public void rotatePhoto(int degree) {
        rotationDegree = (rotationDegree + degree) % 360;
        applyTransform();
    }

    public void flipPhoto(boolean horizontal) {
        if (horizontal) flipHorizontal = !flipHorizontal;
        else flipVertical = !flipVertical;
        applyTransform();
    }

    public void setRotationEdited(boolean edited) {
        this.rotationEdited = edited;
    }


    public int getAccumRotationDeg() {
        return accumRotationDeg;
    }

    public void setLastRotationDirection(boolean left, boolean right) {
        this.lastRotationLeft = left;
        this.lastRotationRight = right;
    }

    public boolean isLastRotationLeft() {
        return lastRotationLeft;
    }

    public boolean isLastRotationRight() {
        return lastRotationRight;
    }

    public boolean isAccumFlipH() {
        return accumFlipH;
    }

    public boolean isAccumFlipV() {
        return accumFlipV;
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

            if (normDeg(accumRotationDeg) == 0 && !isAccumFlipH() && !isAccumFlipV()) {
                lastRotationLeft = false;
                lastRotationRight = false;
            }
        }

        rotationDegree = 0f;
        flipHorizontal = false;
        flipVertical = false;
        resetViewportTransform();

        updateBrushClipFromRenderer();
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

    public void restoreOriginalPhoto() {
        rotationDegree = 0;
        flipHorizontal = false;
        flipVertical = false;
        lastRotationLeft = false;
        lastRotationRight = false;

        if (originalBitmap != null) {
            renderer.setBitmap(originalBitmap);
            photoPreview.requestRender();
        }
    }

    /// 컬러 ///
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

    public void refreshOriginalColorButton() {
        boolean same = isAtBaseline();
        if (originalColor != null) {
            originalColor.setEnabled(!same);
            originalColor.setAlpha(!same ? 1f : 0.4f);
        }
    }

    private boolean isAtBaseline() {
        for (String k : FILTER_KEYS) {
            int cur = getCurrentValue(k);
            int base = baselineFilterValues.containsKey(k) ? baselineFilterValues.get(k) : 0;
            if (cur != base) return false;
        }
        return true;
    }

    public void setColorEdited(boolean edited) {
        this.colorEdited = edited;
        refreshOriginalColorButton();
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

    /*public void resetColorState() {
        isPreviewingOriginalColors = false;
        savedFilterValues.clear();

        if (renderer != null) {
            renderer.resetAllFilter();
            photoPreview.requestRender();
        }

        colorHistory.clear();
        colorCursor = -1;
        colorEdited = false;

        baselineFilterValues.clear();
        for (String k : FILTER_KEYS) {
            baselineFilterValues.put(k, getCurrentValue(k));
        }

        if (undoColor != null) {
            undoColor.setEnabled(false);
            undoColor.setAlpha(0.4f);
        }
        if (redoColor != null) {
            redoColor.setEnabled(false);
            redoColor.setAlpha(0.4f);
        }
        refreshOriginalColorButton();
    }*/

    /// geometry ///
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

    /// 브러쉬, 스티커 ///
    public void applyBrushClipRect(BrushOverlayView brush) {
        if (renderer == null || brush == null) return;
        int x = renderer.getViewportX();
        int y = renderer.getViewportY();
        int w = renderer.getViewportWidth();
        int h = renderer.getViewportHeight();
        brush.setClipRect(new Rect(x, y, x + w, y + h));
    }

    private void updateBrushClipFromRenderer() {
        if (brushOverlay == null || renderer == null) return;
        for (int i = 0; i < brushOverlay.getChildCount(); i++) {
            View v = brushOverlay.getChildAt(i);
            if (v instanceof BrushOverlayView) {
                applyBrushClipRect((BrushOverlayView) v);
                break;
            }
        }
    }

    private BrushOverlayView findBrushView() {
        if (brushOverlay == null) return null;
        for (int i = 0; i < brushOverlay.getChildCount(); i++) {
            View v = brushOverlay.getChildAt(i);
            if (v instanceof BrushOverlayView) return (BrushOverlayView) v;
        }
        return null;
    }

    /*private boolean hasAnyStickerPlaced() {
        FrameLayout so = findViewById(R.id.stickerOverlay);
        if (so == null) return false;
        for (int i = 0; i < so.getChildCount(); i++) {
            View child = so.getChildAt(i);
            if (Boolean.TRUE.equals(child.getTag(R.id.tag_brush_layer))) continue;
            return true;
        }
        return false;
    }*/

    /*private boolean bitmapHasAnyVisiblePixel(Bitmap bmp) {
        final int w = bmp.getWidth(), h = bmp.getHeight();
        int[] row = new int[w];
        for (int y = 0; y < h; y++) {
            bmp.getPixels(row, 0, w, 0, y, w, 1);
            for (int x = 0; x < w; x++) {
                if (((row[x] >>> 24) & 0xFF) != 0) return true;
            }
        }
        return false;
    }*/

    /*private boolean hasVisibleBrushContent() {
        BrushOverlayView bv = findBrushView();
        if (bv != null) {
            try {
                if (bv.hasEffectiveContent()) return true;
            } catch (Throwable ignore) {
                if (bv.getVisibleStrokeCount() > 0) return true;
            }
        }

        FrameLayout so = findViewById(R.id.stickerOverlay);
        if (so != null) {
            for (int i = 0; i < so.getChildCount(); i++) {
                View child = so.getChildAt(i);
                if (!(child instanceof ImageView)) continue;
                if (!Boolean.TRUE.equals(child.getTag(R.id.tag_brush_layer))) continue;

                Drawable d = ((ImageView) child).getDrawable();
                if (!(d instanceof BitmapDrawable)) continue;

                Bitmap bmp = ((BitmapDrawable) d).getBitmap();
                if (bmp != null && !bmp.isRecycled() && bitmapHasAnyVisiblePixel(bmp)) {
                    return true;
                }
            }
        }
        return false;
    }*/

    /*public void refreshStickerButtons() {
        if (undoSticker == null || redoSticker == null || originalSticker == null) return;

        boolean canUndo = canUndoSticker();
        boolean canRedo = canRedoSticker();

        boolean hasAnyPlacedStrict = hasAnyStickerPlaced() || hasVisibleBrushContent();

        undoSticker.setEnabled(canUndo);
        undoSticker.setAlpha(canUndo ? 1f : 0.4f);

        redoSticker.setEnabled(canRedo);
        redoSticker.setAlpha(canRedo ? 1f : 0.4f);

        originalSticker.setEnabled(hasAnyPlacedStrict);
        originalSticker.setAlpha(hasAnyPlacedStrict ? 1f : 0.4f);
    }*/

    /*private void pushHistory(HistoryOp op) {
        while (history.size() > historyCursor + 1) history.remove(history.size() - 1);
        history.add(op);
        historyCursor = history.size() - 1;
        refreshStickerButtons();
    }*/

    /*public void recordStickerPlacement(int baselineIndex) {
        FrameLayout overlay = findViewById(R.id.stickerOverlay);
        if (overlay == null) {
            refreshStickerButtons();
            return;
        }

        int childCount = overlay.getChildCount();
        if (baselineIndex < 0 || baselineIndex >= childCount) {
            refreshStickerButtons();
            return;
        }

        StickerOp sop = new StickerOp();
        for (int i = baselineIndex; i < childCount; i++) {
            View v = overlay.getChildAt(i);
            sop.views.add(v);
            sop.positions.add(i);
        }
        if (sop.views.isEmpty()) {
            refreshStickerButtons();
            return;
        }

        HistoryOp hop = new HistoryOp();
        hop.sticker = sop;

        pushHistory(hop);
    }*/

    /*public void recordStickerEdit(View v,
                                  float bx, float by, int bw, int bh, float br,
                                  float ax, float ay, int aw, int ah, float ar) {
        boolean samePos = Math.abs(bx - ax) < 0.5f && Math.abs(by - ay) < 0.5f;
        boolean sameRot = Math.abs(br - ar) < 0.5f;
        boolean sameSize = (bw == aw) && (bh == ah);
        if (samePos && sameRot && sameSize) {
            refreshStickerButtons();
            return;
        }

        StickerEdit se = new StickerEdit();
        se.view = v;
        se.bx = bx;
        se.by = by;
        se.bw = bw;
        se.bh = bh;
        se.br = br;
        se.ax = ax;
        se.ay = ay;
        se.aw = aw;
        se.ah = ah;
        se.ar = ar;

        HistoryOp hop = new HistoryOp();
        hop.edit = se;

        pushHistory(hop);
    }*/

    /*public void recordStickerDelete(View v) {
        FrameLayout overlay = findViewById(R.id.stickerOverlay);
        if (overlay == null) {
            refreshStickerButtons();
            return;
        }

        int pos = overlay.indexOfChild(v);
        if (pos < 0) {
            refreshStickerButtons();
            return;
        }

        HistoryOp hop = new HistoryOp();
        hop.deletedView = v;
        hop.deletedPos = pos;
        pushHistory(hop);
    }*/

    /*public boolean canUndoSticker() {
        return historyCursor >= 0;
    }*/

    /*public boolean canRedoSticker() {
        return historyCursor < history.size() - 1;
    }*/

    /*public void undoSticker() {
        if (!canUndoSticker()) return;

        HistoryOp op = history.get(historyCursor);
        FrameLayout overlay = findViewById(R.id.stickerOverlay);
        BrushOverlayView bv = findBrushView();

        if (op.hasSticker() && overlay != null) {
            for (int i = op.sticker.views.size() - 1; i >= 0; i--) {
                View v = op.sticker.views.get(i);
                if (v.getParent() == overlay) overlay.removeView(v);
            }
        }
        if (op.hasBrush() && bv != null) {
            bv.setVisibleStrokeCount(op.brushBefore);
        }
        if (op.hasStickerEdit()) {
            applyStickerGeom(op.edit.view,
                    op.edit.bx, op.edit.by, op.edit.bw, op.edit.bh, op.edit.br);
        }
        if (op.hasDeletion() && overlay != null) {
            if (op.deletedView.getParent() != overlay) {
                int pos = Math.min(op.deletedPos, overlay.getChildCount());
                overlay.addView(op.deletedView, pos);
            }
        }
        if (op.hasZChange() && overlay != null && op.zChangedView != null) {
            if (op.zChangedView.getParent() == overlay) {
                overlay.removeView(op.zChangedView);
            }
            int insert = Math.min(op.zBeforePos, overlay.getChildCount());
            overlay.addView(op.zChangedView, insert);
            if (op.zBeforeElevation != null) {
                ViewCompat.setZ(op.zChangedView, op.zBeforeElevation);
            }
            overlay.invalidate();
        }
        if (op.hasErase()) {
            for (EraseOp eo : op.erases) {
                if (!(eo.view instanceof ImageView)) continue;
                ImageView iv = eo.view;
                if (iv.getDrawable() == null || !(iv.getDrawable() instanceof BitmapDrawable))
                    continue;
                Bitmap bmp = ((BitmapDrawable) iv.getDrawable()).getBitmap();
                if (bmp == null || bmp.isRecycled()) continue;

                Canvas c = new Canvas(bmp);
                Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
                for (int i = eo.patches.size() - 1; i >= 0; i--) {
                    ErasePatch patch = eo.patches.get(i);
                    if (patch.before == null || patch.before.isRecycled()) continue;
                    c.save();
                    c.clipRect(patch.rect);
                    c.drawBitmap(patch.before, patch.rect.left, patch.rect.top, p);
                    c.restore();
                }
                iv.invalidate();
            }
        }
        historyCursor--;
        refreshStickerButtons();
    }*/

    /*public void redoSticker() {
        if (!canRedoSticker()) return;

        HistoryOp op = history.get(historyCursor + 1);
        FrameLayout overlay = findViewById(R.id.stickerOverlay);
        BrushOverlayView bv = findBrushView();

        if (op.hasSticker() && overlay != null) {
            for (int i = 0; i < op.sticker.views.size(); i++) {
                View v = op.sticker.views.get(i);
                if (v.getParent() == overlay) continue;
                int pos = Math.min(op.sticker.positions.get(i), overlay.getChildCount());
                overlay.addView(v, pos);
            }
        }
        if (op.hasBrush() && bv != null) {
            bv.setVisibleStrokeCount(op.brushAfter);
        }
        if (op.hasStickerEdit()) {
            applyStickerGeom(op.edit.view,
                    op.edit.ax, op.edit.ay, op.edit.aw, op.edit.ah, op.edit.ar);
        }
        if (op.hasDeletion() && overlay != null) {
            if (op.deletedView.getParent() == overlay) {
                overlay.removeView(op.deletedView);
            }
        }
        if (op.hasZChange() && overlay != null && op.zChangedView != null) {
            if (op.zChangedView.getParent() == overlay) {
                overlay.removeView(op.zChangedView);
            }
            int insert = Math.min(op.zAfterPos, overlay.getChildCount());
            overlay.addView(op.zChangedView, insert);
            if (op.zAfterElevation != null) {
                androidx.core.view.ViewCompat.setZ(op.zChangedView, op.zAfterElevation);
            }
            overlay.invalidate();
        }
        if (op.hasErase()) {
            for (EraseOp eo : op.erases) {
                if (!(eo.view instanceof ImageView)) continue;
                ImageView iv = eo.view;
                if (iv.getDrawable() == null || !(iv.getDrawable() instanceof BitmapDrawable))
                    continue;
                Bitmap bmp = ((BitmapDrawable) iv.getDrawable()).getBitmap();
                if (bmp == null || bmp.isRecycled()) continue;

                Canvas c = new Canvas(bmp);
                Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeJoin(Paint.Join.ROUND);
                p.setStrokeCap(Paint.Cap.ROUND);
                p.setStrokeWidth(eo.strokeWidthPx);
                p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                if (eo.pathOnBitmap != null) {
                    c.drawPath(eo.pathOnBitmap, p);
                }
                iv.invalidate();
            }
        }
        historyCursor++;
        refreshStickerButtons();
    }*/

    /*private void applyStickerGeom(View v, float x, float y, int w, int h, float rot) {
        v.setX(x);
        v.setY(y);
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        if (lp != null) {
            lp.width = w;
            lp.height = h;
            v.setLayoutParams(lp);
        }
        v.setPivotX(w / 2f);
        v.setPivotY(h / 2f);
        v.setRotation(rot);
        v.requestLayout();
    }*/

    /*public void previewOriginalStickers(boolean on) {
        FrameLayout overlay = findViewById(R.id.stickerOverlay);
        if (overlay != null) overlay.setVisibility(on ? View.INVISIBLE : View.VISIBLE);
        if (brushOverlay != null) brushOverlay.setVisibility(on ? View.INVISIBLE : View.VISIBLE);
    }*/

    public void resetStickerState(boolean removeOverlayChildren) {
        FrameLayout overlay = findViewById(R.id.stickerOverlay);
        if (removeOverlayChildren && overlay != null) {
            overlay.removeAllViews();
        }

        BrushOverlayView bv = findBrushView();
        if (removeOverlayChildren && bv != null) {
            bv.clear();
        }

        history.clear();
        historyCursor = -1;

        //previewOriginalStickers(false);

        //refreshStickerButtons();
    }

    /*public void recordStickerZOrderChange(View v, int beforeIndex, float beforeZ, int afterIndex, float afterZ) {
        if (beforeIndex == afterIndex && Math.abs(beforeZ - afterZ) < 0.5f) {
            refreshStickerButtons();
            return;
        }
        HistoryOp hop = new HistoryOp();
        hop.zChangedView = v;
        hop.zBeforePos = beforeIndex;
        hop.zAfterPos = afterIndex;
        hop.zBeforeElevation = beforeZ;
        hop.zAfterElevation = afterZ;
        pushHistory(hop);
    }*/

    /*public void recordBrushErase(List<EraseOp> ops) {
        if (ops == null || ops.isEmpty()) {
            refreshStickerButtons();
            return;
        }
        HistoryOp hop = new HistoryOp();
        hop.erases = new ArrayList<>(ops);
        pushHistory(hop);
    }*/

    /// UI, 시스템 ///
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
            FrameLayout stickerOverlay = findViewById(R.id.stickerOverlay);
            FrameLayout brushOverlay = findViewById(R.id.brushOverlay);
            if (stickerOverlay != null) stickerOverlay.removeAllViews();
            if (brushOverlay != null) brushOverlay.removeAllViews();

            resetStickerState(false);

            fm.beginTransaction()
                    .setCustomAnimations(R.anim.slide_up, 0)
                    .replace(R.id.bottomArea2, new ColorsFragment())
                    .commit();

            ConstraintLayout bottomArea1 = findViewById(R.id.bottomArea1);
            bottomArea1.setVisibility(View.VISIBLE);

            return;
        }

        if (cur instanceof ColorsFragment) {
            //resetColorState();

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
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    public void showExitConfirmDialog() {
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

    private int dpToPx(int dp) {
        float d = getResources().getDisplayMetrics().density;
        return Math.round(dp * d);
    }

    /// getter ///
    public FGLRenderer getRenderer() {
        return renderer;
    }

    public GLSurfaceView getPhotoPreview() {
        return photoPreview;
    }
}