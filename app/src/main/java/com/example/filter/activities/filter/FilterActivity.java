package com.example.filter.activities.filter;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
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
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.AppCompatButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;
import com.example.filter.R;
import com.example.filter.activities.BaseActivity;
import com.example.filter.activities.MainActivity;
import com.example.filter.apis.client.AppRetrofitClient;
import com.example.filter.apis.UploadApi;
import com.example.filter.dialogs.FilterEixtDialog;
import com.example.filter.api_datas.FaceStickerData;
import com.example.filter.api_datas.FilterCreationData;
import com.example.filter.etc.StickerViewModel;
import com.example.filter.fragments.filters.BrushFragment;
import com.example.filter.overlayviews.BrushOverlayView;
import com.example.filter.etc.ClickUtils;
import com.example.filter.overlayviews.CropBoxOverlayView;
import com.example.filter.etc.FGLRenderer;
import com.example.filter.api_datas.request_dto.FilterDtoCreateRequest;
import com.example.filter.etc.StickerStore;
import com.example.filter.fragments.filters.ColorsFragment;
import com.example.filter.fragments.filters.StickersFragment;
import com.example.filter.fragments.filters.ToolsFragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FilterActivity extends BaseActivity {
    private boolean isAnimOnce = false;
    private LottieAnimationView loadingFinishAnim;

    /// UI ///
    private FrameLayout stickerOverlay, brushOverlay;
    private GLSurfaceView photoPreview;
    private ImageButton closeBtn;
    private AppCompatButton saveBtn;
    private ImageButton undoColor, redoColor, originalColor;
    private ImageButton undoBrush, redoBrush;

    /// renderer, photoImage ///
    private FGLRenderer renderer;
    private Bitmap originalBitmap, transformedBitmap, cropBeforeBitmap, baseImageForCrop;

    /// 시스템 ///
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    /// 크롭 ///
    public enum CropMode {
        NONE, FREE, OTO, TTF, NTS
    }

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
    private float rotationDegree = 0;       //임시 회전각
    private boolean flipHorizontal = false, flipVertical = false;       //임시 반전
    private int accumRotationDeg = 0;       //최종 적용 회전각
    private boolean accumFlipH = false, accumFlipV = false;             //최종 적용 반전
    private boolean lastRotationLeft = false, lastRotationRight = false;
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

    private List<FaceStickerData> faceStickerList = new ArrayList<>();

    /// 필터 등록 제한 ///
    private boolean allowRegister = false;

    /// life cycle ///
    @SuppressLint({"ClickableViewAccessibility", "WrongConstant"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_filter);
        closeBtn = findViewById(R.id.closeBtn);
        saveBtn = findViewById(R.id.saveBtn);
        photoPreview = findViewById(R.id.photoPreview);
        stickerOverlay = findViewById(R.id.stickerOverlay);
        brushOverlay = findViewById(R.id.brushOverlay);

        undoColor = findViewById(R.id.undoColor);
        redoColor = findViewById(R.id.redoColor);
        originalColor = findViewById(R.id.originalColor);
        undoBrush = findViewById(R.id.undoBrush);
        redoBrush = findViewById(R.id.redoBrush);

        loadingFinishAnim = findViewById(R.id.loadingFinishAnim);
        loadingFinishAnim.setVisibility(View.GONE);

        Window window = getWindow();
        window.setNavigationBarColor(Color.BLACK);

        StickerStore.get().init(getApplicationContext());

        if (photoPreview != null) {
            photoPreview.setEGLContextClientVersion(2);
            renderer = new FGLRenderer(this, photoPreview, true);
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
                if (currentCropMode == CropMode.NONE)
                    return true;

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
        setupSaveButton();
        setupImagePicker();

        cropOverlay = null;

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            FrameLayout full = findViewById(R.id.fullScreenContainer);
            ConstraintLayout filter = findViewById(R.id.filterActivity);
            ConstraintLayout main = findViewById(R.id.main);

            Fragment f = getSupportFragmentManager().findFragmentById(R.id.fullScreenContainer);
            if (f == null) {
                full.setVisibility(View.GONE);
                filter.setVisibility(View.VISIBLE);
                main.setBackgroundColor(Color.BLACK);
                window.setNavigationBarColor(Color.BLACK);
            }
        });

        closeBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            showExitConfirmDialog();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        StickerViewModel viewModel = new ViewModelProvider(this).get(StickerViewModel.class);
        viewModel.getFaceStickerData().observe(this, data -> {
            if (data == null)
                return;

            boolean exists = false;
            for (FaceStickerData d : faceStickerList) {
                if (d.groupId == data.groupId) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                faceStickerList.add(data);
            }

            if (data.stickerPath != null && !data.stickerPath.isEmpty()) {
                ImageView iv = new ImageView(this);
                Glide.with(this).load(data.stickerPath).into(iv);
                iv.setRotation(data.rot);
            }
        });

        viewModel.getFaceStickerDataToDelete().observe(this, groupId -> {
            if (groupId == null)
                return;

            if (faceStickerList != null) {
                faceStickerList.removeIf(stickerData -> stickerData.groupId == groupId);
            }
        });
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

    /// 저장 버튼 설정 (수정됨: S3 업로드 후 이동) ///
    private void setupSaveButton() {
        saveBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400))
                return;
            ClickUtils.disableTemporarily(v, 3000); // 업로드 시간 고려

            renderer.setOnBitmapCaptureListener(new FGLRenderer.OnBitmapCaptureListener() {
                @Override
                public void onBitmapCaptured(Bitmap bitmap) {
                    int vX = renderer.getViewportX();
                    int vY = renderer.getViewportY();
                    int vW = renderer.getViewportWidth();
                    int vH = renderer.getViewportHeight();

                    List<View> clonesToHide = new ArrayList<>();
                    for (int i = 0; i < stickerOverlay.getChildCount(); i++) {
                        View child = stickerOverlay.getChildAt(i);
                        Object tag = child.getTag(R.id.tag_sticker_clone);
                        if (tag != null) {
                            clonesToHide.add(child);
                        }
                    }

                    try {
                        String tempFilterId = UUID.randomUUID().toString();

                        // 1. 브러쉬 오버레이 파일 저장
                        Bitmap brushBitmap = Bitmap.createBitmap(vW, vH, Bitmap.Config.ARGB_8888);
                        Canvas brushCanvas = new Canvas(brushBitmap);
                        brushCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                        brushCanvas.save();
                        brushCanvas.translate(-vX, -vY);
                        if (brushOverlay != null)
                            brushOverlay.draw(brushCanvas);
                        brushCanvas.restore();

                        File tempBrushFile = new File(getCacheDir(), "brush_" + tempFilterId + ".png");
                        try (FileOutputStream out = new FileOutputStream(tempBrushFile)) {
                            brushBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                        }

                        // 2. 최종 편집본 (Preview) 파일 저장
                        Bitmap finalBitmap = Bitmap.createBitmap(vW, vH, Bitmap.Config.ARGB_8888);
                        Canvas finalCanvas = new Canvas(finalBitmap);
                        finalCanvas.drawBitmap(bitmap, 0, 0, null);
                        finalCanvas.save();
                        finalCanvas.translate(-vX, -vY);
                        if (stickerOverlay != null)
                            stickerOverlay.draw(finalCanvas);
                        if (brushOverlay != null)
                            brushOverlay.draw(finalCanvas);
                        finalCanvas.restore();

                        File previewFile = new File(getCacheDir(), "preview_" + tempFilterId + ".png");
                        try (FileOutputStream out = new FileOutputStream(previewFile)) {
                            finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                        }

                        // ---------------------------------------------------------
                        // ✅ 3. [수정됨] 원본 이미지 (Original) 파일 저장
                        // (회전/크롭은 적용하되, 필터는 없는 깨끗한 이미지 생성)
                        // ---------------------------------------------------------
                        File originalFile = new File(getCacheDir(), "original_" + tempFilterId + ".png");
                        try (FileOutputStream out = new FileOutputStream(originalFile)) {
                            // ★ 헬퍼 메서드로 '지오메트리만 적용된' 비트맵 생성
                            Bitmap geomBitmap = createGeometryOnlyBitmap();

                            if (geomBitmap != null) {
                                geomBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                            } else {
                                // 만약 생성 실패시 fallback으로 현재 캡처본 사용
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                            }
                        }

                        // 4. 얼굴 인식 스티커 제외 이미지 (No Face) 파일 저장
                        Bitmap stickerBitmap_noFace = Bitmap.createBitmap(vW, vH, Bitmap.Config.ARGB_8888);
                        Canvas stickerCanvas_noFace = new Canvas(stickerBitmap_noFace);
                        stickerCanvas_noFace.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                        stickerCanvas_noFace.save();
                        stickerCanvas_noFace.translate(-vX, -vY);
                        if (stickerOverlay != null) {
                            for (View child : clonesToHide)
                                child.setAlpha(0f);
                            stickerOverlay.draw(stickerCanvas_noFace);
                            for (View child : clonesToHide)
                                child.setAlpha(1f);
                        }
                        stickerCanvas_noFace.restore();

                        File tempStickerFile_noFace = new File(getCacheDir(),
                                "sticker_image_no_face_" + tempFilterId + ".png");
                        try (FileOutputStream out = new FileOutputStream(tempStickerFile_noFace)) {
                            stickerBitmap_noFace.compress(Bitmap.CompressFormat.PNG, 100, out);
                        }

                        // ✅ 2. 서버에 이미지 업로드 시작 -> 성공 시 다음 화면 이동
                        uploadImagesAndProceed(originalFile, previewFile, bitmap, tempStickerFile_noFace);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            renderer.captureBitmap();
        });
    }

    // ✅ [추가] 이미지 업로드 및 화면 전환 메서드
    // ---------------------------------------------------------------
    // ✅ [수정됨] 1단계: 필터 이미지(Original, Preview) 업로드
    // ---------------------------------------------------------------
    private void uploadImagesAndProceed(File originalFile, File previewFile, Bitmap bitmap, File stickerNoFaceFile) {
        // 1. 필터 이미지 Body 생성
        RequestBody reqOriginal = RequestBody.create(MediaType.parse("image/png"), originalFile);
        MultipartBody.Part partOriginal = MultipartBody.Part.createFormData("original", originalFile.getName(),
                reqOriginal);

        RequestBody reqPreview = RequestBody.create(MediaType.parse("image/png"), previewFile);
        MultipartBody.Part partPreview = MultipartBody.Part.createFormData("preview", previewFile.getName(),
                reqPreview);

        UploadApi api = AppRetrofitClient.getInstance(this).create(UploadApi.class);

        // 2. 필터 이미지 업로드 요청
        api.uploadFilterImages(partOriginal, partPreview).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, String> urls = response.body();
                    String s3OriginalUrl = urls.get("originalImageUrl");
                    String s3PreviewUrl = urls.get("previewImageUrl");

                    // ★ 성공 시 2단계: 스티커 이미지 업로드 호출 (연쇄 호출)
                    uploadStickerImageAndMove(s3OriginalUrl, s3PreviewUrl, bitmap, stickerNoFaceFile);

                } else {
                    Log.e("FilterUpload", "필터 이미지 업로드 실패: " + response.code());
                    Toast.makeText(FilterActivity.this, "필터 업로드 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                Log.e("FilterUpload", "통신 오류", t);
                Toast.makeText(FilterActivity.this, "업로드 중 오류 발생", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ---------------------------------------------------------------
    // ✅ [추가됨] 2단계: 스티커 이미지 업로드 후 화면 이동
    // ---------------------------------------------------------------
    private void uploadStickerImageAndMove(String s3OriginalUrl, String s3PreviewUrl, Bitmap bitmap,
                                           File stickerNoFaceFile) {
        // 파일 존재 여부 확인
        if (stickerNoFaceFile == null || !stickerNoFaceFile.exists() || stickerNoFaceFile.length() == 0) {
            moveToNextActivity(s3OriginalUrl, s3PreviewUrl, null, bitmap);
            return;
        }

        // RequestBody 생성
        RequestBody reqSticker = RequestBody.create(MediaType.parse("image/png"), stickerNoFaceFile);
        MultipartBody.Part partSticker = MultipartBody.Part.createFormData("file", stickerNoFaceFile.getName(),
                reqSticker);

        UploadApi api = AppRetrofitClient.getInstance(this).create(UploadApi.class);

        // ⚠️ [수정] Call<String> -> Call<ResponseBody>
        api.uploadStickerImage(partSticker).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        // ✅ [핵심] 날것의 데이터를 문자열로 변환 (이게 S3 URL임)
                        String s3StickerUrl = response.body().string();

                        Log.d("FilterUpload", "스티커 이미지 업로드 완료: " + s3StickerUrl);

                        // 화면 이동
                        moveToNextActivity(s3OriginalUrl, s3PreviewUrl, s3StickerUrl, bitmap);

                    } catch (Exception e) {
                        e.printStackTrace();
                        moveToNextActivity(s3OriginalUrl, s3PreviewUrl, null, bitmap); // 실패 시 null 넘기고 진행
                    }
                } else {
                    Log.e("FilterUpload", "스티커 이미지 업로드 실패: " + response.code());
                    // 실패해도 일단 다음 화면으로 넘어가게 처리 (선택 사항)
                    moveToNextActivity(s3OriginalUrl, s3PreviewUrl, null, bitmap);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("FilterUpload", "스티커 통신 오류", t);
                // 통신 오류 시에도 일단 진행 (선택 사항)
                moveToNextActivity(s3OriginalUrl, s3PreviewUrl, null, bitmap);
            }
        });
    }

    // ---------------------------------------------------------------
    // ✅ [수정됨] 화면 이동 (URL들을 받아서 DTO에 담음)
    // ---------------------------------------------------------------
    private void moveToNextActivity(String s3OriginalUrl, String s3PreviewUrl, String s3StickerUrl, Bitmap bitmap) {
        FilterCreationData data = new FilterCreationData();

        // 1. 서버에서 받은 S3 URL들을 DTO에 주입
        data.originalImageUrl = s3OriginalUrl;
        data.editedImageUrl = s3PreviewUrl;
        data.stickerImageNoFaceUrl = s3StickerUrl;

        // 2. 나머지 데이터 설정 (기존 코드 유지)
        data.aspectX = bitmap.getWidth();
        data.aspectY = bitmap.getHeight();

        data.colorAdjustments.brightness = renderer.getCurrentValue("밝기") / 100.0;
        data.colorAdjustments.exposure = renderer.getCurrentValue("노출") / 100.0;
        // ... (색상 값 매핑 생략 - 기존 유지) ...
        data.colorAdjustments.contrast = renderer.getCurrentValue("대비") / 100.0;
        data.colorAdjustments.highlight = renderer.getCurrentValue("하이라이트") / 100.0;
        data.colorAdjustments.shadow = renderer.getCurrentValue("그림자") / 100.0;
        data.colorAdjustments.temperature = renderer.getCurrentValue("온도") / 100.0;
        data.colorAdjustments.hue = renderer.getCurrentValue("색조") / 100.0;
        data.colorAdjustments.saturation = (renderer.getCurrentValue("채도") / 100.0) + 1.0;
        data.colorAdjustments.sharpen = renderer.getCurrentValue("선명하게") / 100.0;
        data.colorAdjustments.blur = renderer.getCurrentValue("흐리게") / 100.0;
        data.colorAdjustments.vignette = renderer.getCurrentValue("비네트") / 100.0;
        data.colorAdjustments.noise = renderer.getCurrentValue("노이즈") / 100.0;

        // 스티커 정보 (DB ID 사용)
        if (faceStickerList != null) {
            for (FaceStickerData d : faceStickerList) {
                FilterDtoCreateRequest.FaceSticker s = new FilterDtoCreateRequest.FaceSticker();
                // ✅ FaceStickerData의 서버 ID 사용
                s.stickerId = d.stickerDbId;

                s.relX = d.relX;
                s.relY = d.relY;
                s.relW = d.relW;
                s.relH = d.relH;
                s.rot = d.rot;
                data.stickers.add(s);
            }
        }

        Intent intent = new Intent(FilterActivity.this, SavePhotoActivity.class);
        intent.putExtra("allowRegister", allowRegister);
        intent.putExtra("filter_data", data);

        // 화면 표시용으로는 로딩이 빠른 'Preview S3 URL'을 사용 (Glide가 캐싱함)
        intent.putExtra("display_image_path", s3PreviewUrl);

        startActivity(intent);
        finish();
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
                });
    }

    /// 회전 크롭 줌 모두 적용된 이미지가 오리지널이미지가 되도록 하는 메서드 ///
    private Bitmap createGeometryOnlyBitmap() {
        Bitmap source = transformedBitmap != null ? transformedBitmap : originalBitmap;
        if (source == null) return null;

        if (appliedCropRectN != null) {
            Rect cropPx = new Rect(
                    (int) (appliedCropRectN.left * source.getWidth()),
                    (int) (appliedCropRectN.top * source.getHeight()),
                    (int) (appliedCropRectN.right * source.getWidth()),
                    (int) (appliedCropRectN.bottom * source.getHeight())
            );
            return Bitmap.createBitmap(source, cropPx.left, cropPx.top,
                    cropPx.width(), cropPx.height());
        }

        return source.copy(source.getConfig(), true);
    }


    /// 사진 이미지 가져오기 ///
    private void loadImageFromUri(Uri photoUri) {
        if (renderer == null || photoPreview == null) return;

        try {
            InputStream inputStream = getContentResolver().openInputStream(photoUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null)
                inputStream.close();

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
            if (exifInputStream != null)
                exifInputStream.close();

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

        FrameLayout photoContainer = findViewById(R.id.photoContainer);
        if (cropOverlay != null)
            photoContainer.removeView(cropOverlay);

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
        photoContainer.addView(cropOverlay, new FrameLayout.LayoutParams(
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
        if (vpW <= 0 || vpH <= 0)
            return;

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
        if (lastCropRectN == null || renderer == null)
            return null;
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

    private Rect calcCenteredCropRect(int vpX, int vpY, int vpW, int vpH, boolean fullSize, boolean fixed, int ratioX,
                                      int ratioY) {
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
        if (horizontal)
            flipHorizontal = !flipHorizontal;
        else
            flipVertical = !flipVertical;
        applyTransform();
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
                    m, true);

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
            if (pendingFlipH)
                accumFlipH = !accumFlipH;
            if (pendingFlipV)
                accumFlipV = !accumFlipV;
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
        if (originalBitmap == null)
            return;
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
        if (filterType == null)
            return;
        if (before == after)
            return;

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
        if (!canUndoColor())
            return;
        ColorEdit e = colorHistory.get(colorCursor);
        if (renderer != null)
            renderer.updateValue(e.filterType, e.before);
        colorCursor--;
        photoPreview.requestRender();
        refreshOriginalColorButton();
    }

    public void redoColor() {
        if (!canRedoColor())
            return;
        ColorEdit e = colorHistory.get(colorCursor + 1);
        if (renderer != null)
            renderer.updateValue(e.filterType, e.after);
        colorCursor++;
        photoPreview.requestRender();
        refreshOriginalColorButton();
    }

    public void previewOriginalColors(boolean on) {
        if (renderer == null)
            return;

        if (on) {
            if (isPreviewingOriginalColors)
                return;
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
                    if (v != null)
                        renderer.updateValue(k, v);
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
            if (cur != base)
                return false;
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

    /// geometry ///
    private float[] mapPointN(float x, float y, int rotDeg, boolean flipH, boolean flipV) {
        if (flipH)
            x = 1f - x;
        if (flipV)
            y = 1f - y;

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
        if (src == null)
            return null;
        float[] lt = mapPointN(src.left, src.top, rotDeg, flipH, flipV);
        float[] rb = mapPointN(src.right, src.bottom, rotDeg, flipH, flipV);
        RectF out = new RectF(
                Math.min(lt[0], rb[0]),
                Math.min(lt[1], rb[1]),
                Math.max(lt[0], rb[0]),
                Math.max(lt[1], rb[1]));

        out.left = Math.max(0f, Math.min(1f, out.left));
        out.top = Math.max(0f, Math.min(1f, out.top));
        out.right = Math.max(0f, Math.min(1f, out.right));
        out.bottom = Math.max(0f, Math.min(1f, out.bottom));
        return out;
    }

    private float[] mapPanN(float txN, float tyN, int rotDeg, boolean flipH, boolean flipV) {
        if (flipH)
            txN = -txN;
        if (flipV)
            tyN = -tyN;

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
        if (renderer == null || brush == null)
            return;
        int x = renderer.getViewportX();
        int y = renderer.getViewportY();
        int w = renderer.getViewportWidth();
        int h = renderer.getViewportHeight();
        brush.setClipRect(new Rect(x, y, x + w, y + h));
    }

    private void updateBrushClipFromRenderer() {
        if (brushOverlay == null || renderer == null)
            return;
        for (int i = 0; i < brushOverlay.getChildCount(); i++) {
            View v = brushOverlay.getChildAt(i);
            if (v instanceof BrushOverlayView) {
                applyBrushClipRect((BrushOverlayView) v);
                break;
            }
        }
    }

    /// UI, 시스템 ///
    public void updateSaveButtonState() {
        if (stickerOverlay == null || saveBtn == null)
            return;

        boolean geometryEdited = isRotationEdited() || isCropEdited();
        boolean colorAdjusted = getCurrentValue("밝기") != 0 ||
                getCurrentValue("노출") != 0 ||
                getCurrentValue("대비") != 0 ||
                getCurrentValue("하이라이트") != 0 ||
                getCurrentValue("그림자") != 0 ||
                getCurrentValue("온도") != 0 ||
                getCurrentValue("색조") != 0 ||
                getCurrentValue("채도") != 0 ||
                getCurrentValue("선명하게") != 0 ||
                getCurrentValue("흐리게") != 0 ||
                getCurrentValue("비네트") != 0 ||
                getCurrentValue("노이즈") != 0;
        boolean hasDrawable = false;

        int count = stickerOverlay.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = stickerOverlay.getChildAt(i);
            Boolean isBrush = (Boolean) child.getTag(R.id.tag_brush_layer);

            if (!Boolean.TRUE.equals(isBrush)) {
                hasDrawable = true;
                break;
            }

            if (Boolean.TRUE.equals(isBrush)) {
                if (child instanceof ImageView) {
                    ImageView imageView = (ImageView) child;
                    Drawable drawable = imageView.getDrawable();

                    if (drawable instanceof BitmapDrawable) {
                        Bitmap bmp = ((BitmapDrawable) drawable).getBitmap();
                        if (bmp != null && !bmp.isRecycled()) {
                            if (BrushFragment.hasAnyVisiblePixel(bmp)) {
                                hasDrawable = true;
                                break;
                            }
                        }
                    }
                }
            }
        }

        allowRegister = (colorAdjusted || hasDrawable);
        boolean isEdited = geometryEdited || colorAdjusted || hasDrawable;

        if (isEdited && !isAnimOnce) {
            loadingFinishAnim.setVisibility(View.VISIBLE);
            loadingFinishAnim.animate().scaleX(2f).scaleY(2f).setDuration(250).start();
            loadingFinishAnim.playAnimation();
            isAnimOnce = true;
        }

        if (!isEdited) isAnimOnce = false;

        saveBtn.setEnabled(isEdited);
        saveBtn.setAlpha(isEdited ? 1f : 0.0f);
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
        }).show();
    }

    /// getter ///
    public FGLRenderer getRenderer() {
        return renderer;
    }

    public GLSurfaceView getPhotoPreview() {
        return photoPreview;
    }
}