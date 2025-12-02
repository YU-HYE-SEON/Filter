package com.example.filter.activities.apply;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.graphics.drawable.GradientDrawable;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.filter.R;
import com.example.filter.activities.BaseActivity;
import com.example.filter.api_datas.FaceStickerData;
import com.example.filter.api_datas.request_dto.FilterDtoCreateRequest;
import com.example.filter.api_datas.response_dto.FilterResponse;
import com.example.filter.apis.FilterApi;
import com.example.filter.apis.client.AppRetrofitClient;
import com.example.filter.etc.CGLRenderer;
import com.example.filter.etc.ClickUtils;
import com.example.filter.etc.Controller;
import com.example.filter.etc.ImageUtils;
import com.example.filter.etc.StickerMeta;
import com.example.filter.overlayviews.FaceBoxOverlayView;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@androidx.camera.core.ExperimentalGetImage
public class CameraActivity extends BaseActivity {
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
    private ExecutorService cameraExecutor;
    private PreviewView camera;
    private FrameLayout cameraContainer, overlay, stickerOverlay;
    private ImageButton backBtn;
    private AppCompatButton transitionBtn, flashBtn, timerBtn, ratioBtn, r1Btn, r2Btn, r3Btn, photoBtn;
    private String filterId;
    private GLSurfaceView glSurfaceView;
    private CGLRenderer renderer;
    private FaceBoxOverlayView faceBox;
    private ArrayList<FaceStickerData> faceStickers;
    private List<View> currentStickerViews = new ArrayList<>();
    private ConstraintLayout topArea;
    private boolean isToastVisible = false;
    private boolean isFaceStickerActive = false;
    private boolean isFaceCurrentlyDetected = false;
    private boolean isInitialToastDone = false;

    private ImageCapture imageCapture;
    private Bitmap capturedBitmap = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //getWindow().setFlags(
        //        android.view.WindowManager.LayoutParams.FLAG_SECURE,
        //        android.view.WindowManager.LayoutParams.FLAG_SECURE
        //);

        setContentView(R.layout.a_camera);
        topArea = findViewById(R.id.topArea);
        backBtn = findViewById(R.id.backBtn);
        transitionBtn = findViewById(R.id.transitionBtn);
        cameraContainer = findViewById(R.id.cameraContainer);
        camera = findViewById(R.id.camera);
        overlay = findViewById(R.id.overlay);
        stickerOverlay = findViewById(R.id.stickerOverlay);
        flashBtn = findViewById(R.id.flashBtn);
        timerBtn = findViewById(R.id.timerBtn);
        ratioBtn = findViewById(R.id.ratioBtn);
        r1Btn = findViewById(R.id.r1Btn);
        r2Btn = findViewById(R.id.r2Btn);
        r3Btn = findViewById(R.id.r3Btn);
        photoBtn = findViewById(R.id.photoBtn);

        cameraExecutor = Executors.newSingleThreadExecutor();

        glSurfaceView = new GLSurfaceView(this);
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        glSurfaceView.getHolder().setFormat(android.graphics.PixelFormat.TRANSLUCENT);
        renderer = new CGLRenderer(this, glSurfaceView);
        glSurfaceView.setRenderer(renderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        cameraContainer.addView(glSurfaceView);
        stickerOverlay = new FrameLayout(this);
        stickerOverlay.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        cameraContainer.addView(stickerOverlay);
        stickerOverlay.bringToFront();

        faceBox = new FaceBoxOverlayView(this);
        cameraContainer.addView(faceBox, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        setTransitionMode();

        filterId = getIntent().getStringExtra("filterId");
        if (filterId != null) {
            loadFilterData(Long.parseLong(filterId));
        }

        backBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            finish();
        });

        transitionBtn.setOnClickListener(v -> {
            setTransitionMode();
        });

        ratioBtn.setOnClickListener(v -> {
            r1Btn.setVisibility(View.VISIBLE);
            r2Btn.setVisibility(View.VISIBLE);
            r3Btn.setVisibility(View.VISIBLE);
            updateRatioMode();
        });

        /// 중첩 클릭되면 안 됨 ///
        photoBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            ClickUtils.disableTemporarily(v, 800);

            takePhoto();
        });
    }

    private void moveToNextActivity(Bitmap bitmap) {
        //if (bitmap == null) return;

        String path = ImageUtils.saveBitmapToCache(CameraActivity.this, bitmap);

        //if (path == null) return;

        boolean isBuy = getIntent().getBooleanExtra("isBuy", false);
        boolean isMine = getIntent().getBooleanExtra("isMine", false);
        Intent intent;

        if (!isBuy && !isMine) {
            intent = new Intent(CameraActivity.this, Pre_ApplyFilterActivity.class);
        } else {
            intent = new Intent(CameraActivity.this, ApplyFilterActivity.class);
        }

        intent.putExtra("from_camera", true);
        intent.putExtra("final_image_path", path);
        intent.putExtra("filterId", filterId);
        startActivity(intent);
        finish();
    }

    private void applyAdjustments(FilterDtoCreateRequest.ColorAdjustments a) {
        renderer.updateValue("밝기", (float) a.brightness * 100f);
        renderer.updateValue("노출", (float) a.exposure * 100f);
        renderer.updateValue("대비", (float) a.contrast * 100f);
        renderer.updateValue("하이라이트", (float) a.highlight * 100f);
        renderer.updateValue("그림자", (float) a.shadow * 100f);
        renderer.updateValue("온도", (float) a.temperature * 100f);
        renderer.updateValue("색조", (float) a.hue * 100f);
        renderer.updateValue("채도", ((float) a.saturation - 1.0f) * 100f);
        renderer.updateValue("선명하게", (float) a.sharpen * 100f);
        renderer.updateValue("흐리게", (float) a.blur * 100f);
        renderer.updateValue("비네트", (float) a.vignette * 100f);
        renderer.updateValue("노이즈", (float) a.noise * 100f);

        glSurfaceView.requestRender();
    }

    private void applyBrushStickerImage(FrameLayout overlay, String path) {
        ImageView imageView = new ImageView(this);

        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setAdjustViewBounds(true);

        imageView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        Glide.with(this).load(path).into(imageView);
        overlay.addView(imageView);
        overlay.setZ(20f);
        overlay.invalidate();
    }

    private FilterDtoCreateRequest.ColorAdjustments mapColorAdjustments
            (Map<String, Double> map) {
        if (map == null) return new FilterDtoCreateRequest.ColorAdjustments();

        FilterDtoCreateRequest.ColorAdjustments adj = new FilterDtoCreateRequest.ColorAdjustments();
        adj.brightness = map.getOrDefault("brightness", 0.0);
        adj.exposure = map.getOrDefault("exposure", 0.0);
        adj.contrast = map.getOrDefault("contrast", 0.0);
        adj.highlight = map.getOrDefault("highlight", 0.0);
        adj.shadow = map.getOrDefault("shadow", 0.0);
        adj.temperature = map.getOrDefault("temperature", 0.0);
        adj.hue = map.getOrDefault("hue", 0.0);
        adj.saturation = map.getOrDefault("saturation", 0.0);
        adj.sharpen = map.getOrDefault("sharpen", 0.0);
        adj.blur = map.getOrDefault("blur", 0.0);
        adj.vignette = map.getOrDefault("vignette", 0.0);
        adj.noise = map.getOrDefault("noise", 0.0);
        return adj;
    }

    private ArrayList<FaceStickerData> mapFaceStickers
            (List<FilterResponse.FaceStickerResponse> responses) {
        ArrayList<FaceStickerData> dataList = new ArrayList<>();
        if (responses == null) return dataList;

        for (FilterResponse.FaceStickerResponse resp : responses) {
            FaceStickerData data = new FaceStickerData();
            data.stickerDbId = resp.stickerId;
            data.relX = (float) resp.relX;
            data.relY = (float) resp.relY;
            data.relW = (float) resp.relW;
            data.relH = (float) resp.relH;
            data.rot = (float) resp.rot;
            data.stickerPath = resp.stickerImageUrl;

            Log.e("Register123", "카메라 | 얼굴스티커 | " + data);
            Log.e("Register123", "카메라 | 얼굴스티커 | " + resp);

            dataList.add(data);
        }
        return dataList;
    }

    private void loadFilterData(long id) {
        FilterApi api = AppRetrofitClient.getInstance(this).create(FilterApi.class);
        api.getFilter(id).enqueue(new Callback<FilterResponse>() {
            @Override
            public void onResponse(Call<FilterResponse> call, Response<FilterResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    FilterResponse data = response.body();

                    applyAdjustments(mapColorAdjustments(data.colorAdjustments));

                    if (data.stickerImageNoFaceUrl != null) {
                        applyBrushStickerImage(overlay, data.stickerImageNoFaceUrl);
                    }

                    ArrayList<FaceStickerData> stickers = mapFaceStickers(data.stickers);
                    if (!stickers.isEmpty()) {
                        CameraActivity.this.faceStickers = stickers;

                        isFaceStickerActive = true;
                    }

                } else {
                    Log.e("카메라", "필터 정보 조회 실패: " + response.code());
                    Toast.makeText(CameraActivity.this, "필터 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<FilterResponse> call, Throwable t) {
                Log.e("카메라", "통신 오류", t);
                Toast.makeText(CameraActivity.this, "필터 정보를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void detectFaces(Bitmap bitmap) {
        if (!isFaceStickerActive) return;

        InputImage inputImage = InputImage.fromBitmap(bitmap, 0);

        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .build();

        FaceDetector detector = FaceDetection.getClient(options);

        detector.process(inputImage)
                .addOnSuccessListener(faces -> {
                    List<Rect> rects = new ArrayList<>();
                    boolean facesFound = !faces.isEmpty();
                    for (Face f : faces) rects.add(f.getBoundingBox());

                    final int vW = renderer.getViewportWidth();
                    final int vH = renderer.getViewportHeight();
                    final int vX = renderer.getViewportX();
                    final int vY = renderer.getViewportY();

                    runOnUiThread(() -> {
                        if (!isInitialToastDone) {
                            if (facesFound) {
                                isFaceCurrentlyDetected = true;
                                showToast("얼굴 인식 성공");
                            } else {
                                isFaceCurrentlyDetected = false;
                                showToast("얼굴을 감지하지 못했습니다");
                            }
                            isInitialToastDone = true;
                        } else {
                            if (facesFound && !isFaceCurrentlyDetected) {
                                isFaceCurrentlyDetected = true;
                                showToast("얼굴 인식 성공");
                            } else if (!facesFound && isFaceCurrentlyDetected) {
                                isFaceCurrentlyDetected = false;
                                showToast("얼굴을 감지하지 못했습니다");
                            }
                        }

                        if (!rects.isEmpty()) {
                            faceBox.setVisibility(View.VISIBLE);
                            faceBox.setFaceBoxes(rects, bitmap.getWidth(), bitmap.getHeight(), vW, vH, vX, vY);
                        } else {
                            faceBox.clearBoxes();
                            faceBox.setVisibility(View.GONE);
                        }

                        if (!faces.isEmpty() && faceStickers != null && !faceStickers.isEmpty()) {
                            Bitmap original = bitmap;
                            if (original == null) return;


                            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) stickerOverlay.getLayoutParams();
                            if (params.width != vW || params.height != vH || params.leftMargin != vX || params.topMargin != vY) {
                                params.width = vW;
                                params.height = vH;
                                params.leftMargin = vX;
                                params.topMargin = vY;
                                stickerOverlay.setLayoutParams(params);
                            }


                            List<float[]> allPlacements = new ArrayList<>();

                            for (FaceStickerData d : faceStickers) {
                                StickerMeta meta = new StickerMeta(d.relX, d.relY, d.relW, d.relH, d.rot);
                                List<float[]> placements = StickerMeta.recalculate(faces, original, stickerOverlay, meta, CameraActivity.this);
                                allPlacements.addAll(placements);
                            }
                            // 2. 현재 뷰와 필요한 뷰의 개수 비교 및 관리
                            int requiredViews = allPlacements.size();
                            int currentViews = currentStickerViews.size();

                            // A. 불필요한 뷰 제거
                            if (currentViews > requiredViews) {
                                for (int i = currentViews - 1; i >= requiredViews; i--) {
                                    View viewToRemove = currentStickerViews.remove(i);
                                    stickerOverlay.removeView(viewToRemove);
                                }
                            }
                            // B. 부족한 뷰 추가
                            else if (currentViews < requiredViews) {
                                for (int i = currentViews; i < requiredViews; i++) {
                                    FaceStickerData d = faceStickers.get(i % faceStickers.size());

                                    View newSticker = StickerMeta.cloneStickerForCamera(stickerOverlay, d.stickerPath, CameraActivity.this);
                                    currentStickerViews.add(newSticker);
                                }
                            }

                            // 3. 모든 뷰의 위치 및 크기 업데이트
                            for (int i = 0; i < requiredViews; i++) {
                                View stickerView = currentStickerViews.get(i);
                                float[] p = allPlacements.get(i); // p = [X, Y, W, H, R]

                                // 크기 업데이트
                                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) stickerView.getLayoutParams();
                                lp.width = Math.round(p[2]);
                                lp.height = Math.round(p[3]);
                                stickerView.setLayoutParams(lp);

                                // 위치 및 회전 업데이트
                                stickerView.setX(p[0]);
                                stickerView.setY(p[1]);
                                stickerView.setRotation(p[4]);
                            }
                        } else {
                            // 얼굴이 감지되지 않거나 스티커가 없으면 모든 뷰 제거 (이때만 지움)
                            for (View view : currentStickerViews) {
                                stickerOverlay.removeView(view);
                            }
                            currentStickerViews.clear();
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    runOnUiThread(() -> {
                        if (!isInitialToastDone) {
                            isFaceCurrentlyDetected = false;
                            showToast("얼굴을 감지하지 못했습니다");
                            isInitialToastDone = true;
                        } else if (isFaceCurrentlyDetected) {
                            isFaceCurrentlyDetected = false;
                            showToast("얼굴을 감지하지 못했습니다");
                        }

                        faceBox.clearBoxes();
                        faceBox.setVisibility(View.GONE);

                        // 얼굴 감지 실패 시 모든 뷰 제거
                        for (View view : currentStickerViews) {
                            stickerOverlay.removeView(view);
                        }
                        currentStickerViews.clear();
                    });
                });
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview.Builder builder = new Preview.Builder();

                Preview preview = builder.build();
                preview.setSurfaceProvider(camera.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).build();

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();

                imageAnalysis.setAnalyzer(cameraExecutor, image -> {
                    Bitmap bitmap = convertImageToBitmap(image);
                    int rotation = image.getImageInfo().getRotationDegrees();
                    bitmap = rotateBitmap(bitmap, rotation);

                    if (cameraSelector.equals(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                        Matrix matrix = new Matrix();
                        matrix.preScale(-1f, 1f);
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                    }

                    detectFaces(bitmap);

                    renderer.setBitmap(bitmap);
                    glSurfaceView.requestRender();
                    image.close();
                });

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalysis);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        if (renderer == null || glSurfaceView == null) {
            return;
        }

        renderer.captureFinalBitmap(new CGLRenderer.BitmapCaptureListener() {
            @Override
            public void onBitmapCaptured(Bitmap baseBitmap) {
                runOnUiThread(() -> {
                    if (baseBitmap == null) {
                        return;
                    }

                    Bitmap finalBitmap = baseBitmap.copy(Bitmap.Config.ARGB_8888, true);
                    Canvas canvas = new Canvas(finalBitmap);

                    int vX = renderer.getViewportX();
                    int vY = renderer.getViewportY();
                    int vW = renderer.getViewportWidth();
                    int vH = renderer.getViewportHeight();

                    Bitmap screenSizedOverlay = Bitmap.createBitmap(cameraContainer.getWidth(), cameraContainer.getHeight(), Bitmap.Config.ARGB_8888);
                    Canvas screenCanvas = new Canvas(screenSizedOverlay);

                    overlay.draw(screenCanvas);
                    stickerOverlay.draw(screenCanvas);

                    Bitmap mergedOverlayBitmap = Bitmap.createBitmap(vW, vH, Bitmap.Config.ARGB_8888);
                    Canvas mergedCanvas = new Canvas(mergedOverlayBitmap);

                    Rect srcRect = new Rect(vX, vY, vX + vW, vY + vH);
                    Rect dstRect = new Rect(0, 0, vW, vH);

                    mergedCanvas.drawBitmap(screenSizedOverlay, srcRect, dstRect, null);

                    screenSizedOverlay.recycle();

                    canvas.drawBitmap(mergedOverlayBitmap, 0, 0, null);
                    mergedOverlayBitmap.recycle();

                    capturedBitmap = finalBitmap;
                    moveToNextActivity(capturedBitmap);
                });
            }
        });
    }

    private void setTransitionMode() {
        if (cameraSelector.equals(CameraSelector.DEFAULT_BACK_CAMERA)) {
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
        } else {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
        }

        startCamera();
    }

    private void updateRatioMode() {
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) cameraContainer.getLayoutParams();

        r1Btn.setOnClickListener(v -> {
            params.dimensionRatio = "1:1";
            params.verticalBias = 0.2f;

            ratioBtn.setText(r1Btn.getText());

            r1Btn.setVisibility(View.GONE);
            r2Btn.setVisibility(View.GONE);
            r3Btn.setVisibility(View.GONE);

            cameraContainer.setLayoutParams(params);
            cameraContainer.requestLayout();
            renderer.setCropRatioMode(1);
        });

        r2Btn.setOnClickListener(v -> {
            params.dimensionRatio = "3:4";
            params.verticalBias = 0.25f;

            ratioBtn.setText(r2Btn.getText());

            r1Btn.setVisibility(View.GONE);
            r2Btn.setVisibility(View.GONE);
            r3Btn.setVisibility(View.GONE);

            cameraContainer.setLayoutParams(params);
            cameraContainer.requestLayout();
            renderer.setCropRatioMode(2);
        });

        r3Btn.setOnClickListener(v -> {
            params.dimensionRatio = "9:16";
            params.verticalBias = 0.25f;

            ratioBtn.setText(r3Btn.getText());

            r1Btn.setVisibility(View.GONE);
            r2Btn.setVisibility(View.GONE);
            r3Btn.setVisibility(View.GONE);

            cameraContainer.setLayoutParams(params);
            cameraContainer.requestLayout();
            renderer.setCropRatioMode(3);
        });
    }

    public static Bitmap convertImageToBitmap(ImageProxy image) {
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];

        // U와 V를 NV21 형식으로 합치기
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(
                nv21,
                ImageFormat.NV21,
                image.getWidth(),
                image.getHeight(),
                null
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(
                new Rect(0, 0, image.getWidth(), image.getHeight()),
                100,
                out
        );

        byte[] jpegBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.length);
    }

    private static Bitmap rotateBitmap(Bitmap bitmap, int rotationDegrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(rotationDegrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public CGLRenderer getRenderer() {
        return renderer;
    }

    public void showToast(String message) {
        isToastVisible = true;

        View old = topArea.findViewWithTag("inline_banner");
        if (old != null) topArea.removeView(old);

        TextView tv = new TextView(this);
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
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}
