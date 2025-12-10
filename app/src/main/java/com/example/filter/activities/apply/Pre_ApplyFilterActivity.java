package com.example.filter.activities.apply;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;
import com.example.filter.R;
import com.example.filter.activities.BaseActivity;
import com.example.filter.activities.filterinfo.FilterInfoActivity;
import com.example.filter.api_datas.FaceStickerData;
import com.example.filter.api_datas.request_dto.FilterDtoCreateRequest;
import com.example.filter.api_datas.response_dto.FilterResponse;
import com.example.filter.apis.FilterApi;
import com.example.filter.apis.client.AppRetrofitClient;
import com.example.filter.dialogs.Pre_ApplyEixtDialog;
import com.example.filter.etc.ClickUtils;
import com.example.filter.etc.Controller;
import com.example.filter.etc.FGLRenderer;
import com.example.filter.etc.ImageUtils;
import com.example.filter.etc.StickerMeta;
import com.example.filter.overlayviews.FaceBoxOverlayView;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@androidx.camera.core.ExperimentalGetImage
public class Pre_ApplyFilterActivity extends BaseActivity {
    public interface FaceDetectionCallback {
        void onFacesDetected(List<Face> faces, Bitmap originalBitmap);
    }

    private String faceDetectMessage = null;
    private boolean readyDone = false;
    private boolean loadingAnimFinishedOnce = false;
    private int loadingAnimPlayCount = 0;
    private final int MIN_PLAY_COUNT = 1;
    private FrameLayout loadingContainer, loadingFinishContainer;
    private LottieAnimationView loadingAnim, loadingFinishAnim;
    private TextView successTxt;
    private ImageButton backBtn;
    private AppCompatButton saveBtn;
    private FrameLayout photoContainer, stickerOverlay;
    private ConstraintLayout bottomArea;
    private GLSurfaceView glSurfaceView;
    private FGLRenderer renderer;
    private ArrayList<FaceStickerData> faceStickers;
    private Bitmap finalBitmapWithStickers = null;
    private FrameLayout modalOff;
    private View buyFilterOn, buyFilterSuccessOn, dimBackground;
    private ImageButton buyBtn, useBtn, closeBtn;
    private boolean isModalVisible = false, isBuy = false;
    private TextView filterTitle, point, currentPoint1, currentPoint2, useBtnTxt, closeBtnTxt;
    private String title, price;
    private String filterId;
    public static FaceBoxOverlayView faceBox;
    private Bitmap originalImageBitmap;
    private ConstraintLayout topArea;
    private boolean isToastVisible = false;
    private boolean isFaceStickerActive = false;

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!loadingAnimFinishedOnce) {
            loadingAnim.setSpeed(2.5f);

            loadingAnim.addAnimatorUpdateListener(animation -> {
                float progress = (float) animation.getAnimatedValue();

                if (progress >= 0.33f && readyDone && !loadingAnimFinishedOnce) {
                    loadingAnimFinishedOnce = true;
                    finishLoading();
                }
            });

            loadingAnim.playAnimation();
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(
                android.view.WindowManager.LayoutParams.FLAG_SECURE,
                android.view.WindowManager.LayoutParams.FLAG_SECURE
        );

        setContentView(R.layout.a_pre_apply_photo);
        topArea = findViewById(R.id.topArea);
        backBtn = findViewById(R.id.backBtn);
        saveBtn = findViewById(R.id.saveBtn);
        photoContainer = findViewById(R.id.photoContainer);
        stickerOverlay = findViewById(R.id.stickerOverlay);
        bottomArea = findViewById(R.id.bottomArea);
        modalOff = findViewById(R.id.modalOff);

        successTxt = findViewById(R.id.successTxt);
        loadingContainer = findViewById(R.id.loadingContainer);
        loadingAnim = findViewById(R.id.loadingAnim);
        loadingFinishContainer = findViewById(R.id.loadingFinishContainer);
        loadingFinishAnim = findViewById(R.id.loadingFinishAnim);
        loadingFinishContainer.setVisibility(View.GONE);
        loadingFinishAnim.setVisibility(View.GONE);
        loadingContainer.setVisibility(View.VISIBLE);

        //시스템 바 인셋 설정
        final View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), nav.bottom);
            return insets;
        });

        saveBtn.setAlpha(0.4f);
        saveBtn.setEnabled(false);
        saveBtn.setClickable(false);

        glSurfaceView = new GLSurfaceView(this);
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        glSurfaceView.getHolder().setFormat(android.graphics.PixelFormat.TRANSLUCENT);
        glSurfaceView.setPreserveEGLContextOnPause(true);
        renderer = new FGLRenderer(this, glSurfaceView, true);
        glSurfaceView.setRenderer(renderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        photoContainer.addView(glSurfaceView, 0);

        renderer.setOnBitmapCaptureListener(baseBitmap -> {
            try {
                Bitmap finalBitmap = baseBitmap.copy(Bitmap.Config.ARGB_8888, true);
                Canvas canvas = new Canvas(finalBitmap);

                int vX = renderer.getViewportX();
                int vY = renderer.getViewportY();
                int vW = renderer.getViewportWidth();
                int vH = renderer.getViewportHeight();

                Bitmap overlayBitmap = Bitmap.createBitmap(
                        photoContainer.getWidth(),
                        photoContainer.getHeight(),
                        Bitmap.Config.ARGB_8888
                );
                Canvas overlayCanvas = new Canvas(overlayBitmap);
                stickerOverlay.draw(overlayCanvas);

                Rect src = new Rect(vX, vY, vX + vW, vY + vH);
                Rect dst = new Rect(0, 0, finalBitmap.getWidth(), finalBitmap.getHeight());
                canvas.drawBitmap(overlayBitmap, src, dst, null);
                overlayBitmap.recycle();

                finalBitmapWithStickers = finalBitmap;

                if (!readyDone) {
                    readyDone = true;
                    checkAndFinishLoading();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        filterId = getIntent().getStringExtra("filterId");

        String finalImagePath = getIntent().getStringExtra("final_image_path");
        boolean isGetPath = finalImagePath != null;

        Bitmap imageToDisplay = null;
        Uri imageUri = getIntent().getData();

        if (filterId != null) {
            loadFilterData(Long.parseLong(filterId));
        }

        if (isGetPath) {
            imageToDisplay = BitmapFactory.decodeFile(finalImagePath);

            if (imageToDisplay != null) {
                finalBitmapWithStickers = imageToDisplay;
            }

            if (!readyDone) {
                readyDone = true;
                checkAndFinishLoading();
            }
        }

        if (imageUri != null) {
            try {
                Bitmap bmp = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                if (bmp != null) {
                    this.originalImageBitmap = bmp;

                    if (imageToDisplay == null) {
                        imageToDisplay = bmp;
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (imageToDisplay != null) {
            renderer.setBitmap(imageToDisplay);
            glSurfaceView.requestRender();

            faceBox = new FaceBoxOverlayView(this);
            photoContainer.addView(faceBox, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            Bitmap faceDetectTarget = (this.originalImageBitmap != null) ? this.originalImageBitmap : imageToDisplay;
            detectFaces(faceDetectTarget, (faces, originalBitmap) -> {
                if (faces.isEmpty()) {
                    return;
                }
            });
        }

        backBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            showExitConfirmDialog();
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showExitConfirmDialog();
            }
        });

        saveBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            if (isModalVisible) return;
            if (!isBuy) {
                showModal(buyFilterOn);
            } else {
                // 액티비티 이동
                renderer.captureBitmap();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        moveToApplyActivity();
                    }
                }, 200);
            }
        });

        setupModal();
    }

    private void setupModal() {
        FrameLayout rootView = findViewById(R.id.modalOff);
        buyFilterOn = getLayoutInflater().inflate(R.layout.m_buy_filter, null);
        filterTitle = buyFilterOn.findViewById(R.id.filterTitle);
        point = buyFilterOn.findViewById(R.id.point);
        currentPoint1 = buyFilterOn.findViewById(R.id.currentPoint1);
        buyBtn = buyFilterOn.findViewById(R.id.buyBtn);

        buyFilterSuccessOn = getLayoutInflater().inflate(R.layout.m_buy_filter_success, null);
        currentPoint2 = buyFilterSuccessOn.findViewById(R.id.currentPoint2);
        useBtn = buyFilterSuccessOn.findViewById(R.id.useBtn);
        useBtnTxt = buyFilterSuccessOn.findViewById(R.id.useBtnTxt);
        closeBtn = buyFilterSuccessOn.findViewById(R.id.closeBtn);
        closeBtnTxt = buyFilterSuccessOn.findViewById(R.id.closeBtnTxt);

        dimBackground = new View(this);
        dimBackground.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        dimBackground.setBackgroundColor(Color.parseColor("#B3000000"));
        dimBackground.setVisibility(View.GONE);

        rootView.addView(dimBackground);
        rootView.addView(buyFilterOn);
        rootView.addView(buyFilterSuccessOn);

        buyFilterOn.setVisibility(View.GONE);
        buyFilterOn.setTranslationY(800);
        buyFilterSuccessOn.setVisibility(View.GONE);
        buyFilterSuccessOn.setTranslationY(800);

        dimBackground.setOnClickListener(v -> hideModal());

        if (buyBtn != null) {
            buyBtn.setOnClickListener(v -> {
                buyBtn.setEnabled(false);
                buyBtn.setClickable(false);

                // 1. 로컬 포인트 체크 (사전 검증)
                SharedPreferences sp = getSharedPreferences("points", MODE_PRIVATE);
                int current = sp.getInt("current_point", 0);
                int priceInt = 0;
                try {
                    priceInt = Integer.parseInt(price);
                } catch (Exception e) {
                }

                if (current < priceInt) {
                    Toast.makeText(this, "포인트가 부족합니다.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // ✅ [수정] 2. 서버 구매 API 호출
                requestPurchaseFilter(Long.parseLong(filterId), current, priceInt);
            });
        }

        useBtnTxt.setText("저장하기");
        closeBtnTxt.setText("닫기");

        if (useBtn != null) useBtn.setOnClickListener(v -> {
            hideModal();
            // 액티비티 이동
            renderer.captureBitmap();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    moveToApplyActivity();
                }
            }, 200);
        });

        if (closeBtn != null) closeBtn.setOnClickListener(v -> hideModal());
    }

    private void moveToApplyActivity() {
        Intent intent = new Intent(Pre_ApplyFilterActivity.this, ApplyFilterActivity.class);
        intent.putExtra("filterId", filterId);

        if (finalBitmapWithStickers != null) {
            String tempImagePath = ImageUtils.saveBitmapToCache(Pre_ApplyFilterActivity.this, finalBitmapWithStickers);
            if (tempImagePath != null) {
                intent.putExtra("final_image_path", tempImagePath);
            }
        }

        startActivity(intent);
        finish();
    }

    // ✅ [추가] 서버에 구매/사용 요청
    private void requestPurchaseFilter(long id, int currentPoint, int priceInt) {
        FilterApi api = AppRetrofitClient.getInstance(this).create(FilterApi.class);

        api.useFilter(id).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    // 1. 성공 시 로컬 포인트 차감 및 저장
                    int newCurrent = currentPoint - priceInt;
                    SharedPreferences sp = getSharedPreferences("points", MODE_PRIVATE);
                    sp.edit().putInt("current_point", newCurrent).apply();

                    isBuy = true;
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("filter_bought", true);
                    setResult(RESULT_OK, resultIntent);

                    // 3. 모달 전환 애니메이션 (구매 -> 성공)
                    buyFilterOn.setVisibility(View.GONE);
                    if (currentPoint2 != null) currentPoint2.setText(newCurrent + "P");
                    showModal(buyFilterSuccessOn);

                    Log.d("필터체험", "구매 성공");
                } else {
                    buyBtn.setEnabled(true);
                    buyBtn.setClickable(true);

                    // 실패 시 (이미 구매했거나, 포인트 부족 등 서버 에러)
                    Log.e("필터체험", "구매 실패: " + response.code());
                    Toast.makeText(Pre_ApplyFilterActivity.this, "구매에 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                buyBtn.setEnabled(true);
                buyBtn.setClickable(true);

                Log.e("필터체험", "통신 오류", t);
                Toast.makeText(Pre_ApplyFilterActivity.this, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showModal(View view) {
        isModalVisible = true;
        dimBackground.setVisibility(View.VISIBLE);
        view.setVisibility(View.VISIBLE);
        view.animate().translationY(0).setDuration(300).setInterpolator(new AccelerateDecelerateInterpolator()).start();
    }

    private void hideModal() {
        View tempTarget = null;
        if (buyFilterOn.getVisibility() == View.VISIBLE) tempTarget = buyFilterOn;
        else if (buyFilterSuccessOn.getVisibility() == View.VISIBLE)
            tempTarget = buyFilterSuccessOn;

        if (tempTarget != null) {
            final View target = tempTarget;
            target.animate().translationY(800).setDuration(250).setInterpolator(new AccelerateDecelerateInterpolator())
                    .withEndAction(() -> {
                        target.setVisibility(View.GONE);
                        dimBackground.setVisibility(View.GONE);
                        isModalVisible = false;
                    })
                    .start();
        } else {
            dimBackground.setVisibility(View.GONE);
            isModalVisible = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (dimBackground != null && dimBackground.getVisibility() == View.VISIBLE) {
            dimBackground.setVisibility(View.GONE);
            isModalVisible = false;
        }
    }

    private void applyAdjustments(FilterDtoCreateRequest.ColorAdjustments a) {
        renderer.updateValue("밝기", (float) a.brightness * 100f);
        renderer.updateValue("노출", (float) a.exposure * 100f);
        renderer.updateValue("대비", (float) a.contrast * 100f);
        renderer.updateValue("하이라이트", (float) a.highlight * 100f);
        renderer.updateValue("그림자", (float) a.shadow * 100f);
        renderer.updateValue("온도", (float) a.temperature * 100f);
        renderer.updateValue("색조", (float) a.hue * 100f);

        // 채도는 계산식이 있으므로 전체를 괄호로 묶거나 변수를 먼저 캐스팅
        renderer.updateValue("채도", ((float) a.saturation - 1.0f) * 100f);
        renderer.updateValue("선명하게", (float) a.sharpen * 100f);
        renderer.updateValue("흐리게", (float) a.blur * 100f);
        renderer.updateValue("비네트", (float) a.vignette * 100f);
        renderer.updateValue("노이즈", (float) a.noise * 100f);

        glSurfaceView.requestRender();
        glSurfaceView.postDelayed(() -> {
            renderer.captureBitmap();
        }, 150);
    }

    private void applyBrushStickerImage(FrameLayout overlay, String path) {
        ImageView imageView = new ImageView(this);

        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setAdjustViewBounds(true);

        imageView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        Glide.with(this).load(path).into(imageView);
        overlay.addView(imageView);
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

            Log.e("Register123", "얼굴스티커 | " + data);
            Log.e("Register123", "얼굴스티커 | " + resp);

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

                    boolean fromCamera = getIntent().getBooleanExtra("from_camera", false);
                    if (!fromCamera) {
                        applyAdjustments(mapColorAdjustments(data.colorAdjustments));

                        if (data.stickerImageNoFaceUrl != null) {
                            glSurfaceView.postDelayed(() -> {
                                final int vW = renderer.getViewportWidth();
                                final int vH = renderer.getViewportHeight();
                                final int vX = renderer.getViewportX();
                                final int vY = renderer.getViewportY();

                                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) stickerOverlay.getLayoutParams();
                                params.width = vW;
                                params.height = vH;
                                params.leftMargin = vX;
                                params.topMargin = vY;
                                stickerOverlay.setLayoutParams(params);

                                applyBrushStickerImage(stickerOverlay, data.stickerImageNoFaceUrl);
                            }, 150);

                        }

                        ArrayList<FaceStickerData> stickers = mapFaceStickers(data.stickers);
                        if (!stickers.isEmpty() && originalImageBitmap != null) {
                            Pre_ApplyFilterActivity.this.faceStickers = stickers;

                            isFaceStickerActive = true;

                            detectFaces(originalImageBitmap, null);
                        }
                    }

                    title = data.name;
                    price = String.valueOf(data.price);

                    if (filterTitle != null) filterTitle.setText(title);
                    if (point != null) point.setText(price + "P");

                    SharedPreferences sp = getSharedPreferences("points", MODE_PRIVATE);
                    if (currentPoint1 != null)
                        currentPoint1.setText(sp.getInt("current_point", 0) + "P");
                } else {
                    Log.e("필터체험", "필터 정보 조회 실패: " + response.code());
                    Toast.makeText(Pre_ApplyFilterActivity.this, "필터 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<FilterResponse> call, Throwable t) {
                Log.e("필터체험", "통신 오류", t);
                Toast.makeText(Pre_ApplyFilterActivity.this, "필터 정보를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /// 얼굴인식스티커 적용하기 ///
    private void detectFaces(Bitmap bitmap, Pre_ApplyFilterActivity.FaceDetectionCallback callback) {
        if (!isFaceStickerActive) return;

        if (bitmap == null) {
            if (callback != null) {
                callback.onFacesDetected(new ArrayList<>(), null);
            }
            return;
        }

        InputImage image = InputImage.fromBitmap(bitmap, 0);

        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build();

        FaceDetector detector = FaceDetection.getClient(options);

        detector.process(image)
                .addOnSuccessListener(faces -> {
                    List<Rect> rects = new ArrayList<>();
                    boolean facesFound = !faces.isEmpty();

                    for (int i = 0; i < faces.size(); i++) {
                        Face face = faces.get(i);
                        rects.add(face.getBoundingBox());
                    }

                    if (faceBox != null) {
                        if (!faces.isEmpty()) {
                            faceBox.setVisibility(View.VISIBLE);
                            faceBox.setFaceBoxes(rects, bitmap.getWidth(), bitmap.getHeight());
                        } else {
                            faceBox.clearBoxes();
                            faceBox.setVisibility(View.GONE);
                        }
                    }

                    if (faceStickers == null || faceStickers.isEmpty())
                        return;

                    runOnUiThread(() -> {
                        if (facesFound) {
                            faceDetectMessage = "얼굴 인식 성공";
                        } else {
                            faceDetectMessage = "얼굴을 감지하지 못했습니다";
                        }

                        Bitmap original = originalImageBitmap;
                        if (original == null) return;

                        final int vW = renderer.getViewportWidth();
                        final int vH = renderer.getViewportHeight();
                        final int vX = renderer.getViewportX();
                        final int vY = renderer.getViewportY();

                        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) stickerOverlay.getLayoutParams();
                        if (params.width != vW || params.height != vH || params.leftMargin != vX || params.topMargin != vY) {
                            params.width = vW;
                            params.height = vH;
                            params.leftMargin = vX;
                            params.topMargin = vY;
                            stickerOverlay.setLayoutParams(params);
                        }

                        for (Face face : faces) {
                            for (FaceStickerData d : faceStickers) {
                                StickerMeta meta = new StickerMeta(d.relX, d.relY, d.relW, d.relH, d.rot);
                                List<float[]> placements = StickerMeta.recalculate(faces, original, stickerOverlay, meta, this);
                                for (float[] p : placements) {
                                    StickerMeta.cloneSticker(stickerOverlay, d.stickerPath, this, p);
                                }
                            }
                        }
                    });

                    if (callback != null) {
                        callback.onFacesDetected(faces, bitmap);
                    }

                    detector.close();
                })
                .addOnFailureListener(e -> {
                    faceDetectMessage = "얼굴을 감지하지 못했습니다";

                    if (faceBox != null) {
                        faceBox.clearBoxes();
                        faceBox.setVisibility(View.GONE);
                    }
                    if (callback != null) {
                        callback.onFacesDetected(new ArrayList<>(), bitmap);
                    }
                    detector.close();
                });
    }

    public FGLRenderer getRenderer() {
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

    private void showExitConfirmDialog() {
        new Pre_ApplyEixtDialog(this, new Pre_ApplyEixtDialog.Pre_ApplyEixtDialogListener() {
            @Override
            public void onKeep() {
            }

            @Override
            public void onExit() {
                Intent intent = new Intent(Pre_ApplyFilterActivity.this, FilterInfoActivity.class);
                intent.putExtra("filterId", filterId);
                startActivity(intent);
                finish();
            }
        }).show();
    }

    private void checkAndFinishLoading() {
        if (!readyDone) return;

        if (loadingAnimPlayCount >= MIN_PLAY_COUNT) {
            if (!loadingAnimFinishedOnce) {
                loadingAnimFinishedOnce = true;

                loadingAnim.cancelAnimation();

                finishLoading();
            }
        }
    }

    private void finishLoading() {
        if (!readyDone || !loadingAnimFinishedOnce) return;

        loadingAnim.cancelAnimation();
        loadingContainer.setVisibility(View.GONE);

        loadingFinishContainer.setVisibility(View.VISIBLE);
        loadingFinishAnim.setVisibility(View.VISIBLE);
        loadingFinishAnim.setScaleX(0.5f);
        loadingFinishAnim.setScaleY(0.5f);
        loadingFinishAnim.animate().scaleX(1.2f).scaleY(1.2f).setDuration(250).start();
        loadingFinishAnim.playAnimation();

        if (faceDetectMessage != null) {
            showToast(faceDetectMessage);
        }

        loadingFinishContainer.animate()
                .alpha(0f)
                .setDuration(600)
                .withEndAction(() -> {
                    loadingFinishContainer.setVisibility(View.GONE);
                    loadingFinishAnim.setVisibility(View.GONE);
                })
                .start();

        successTxt.setText("필터 적용 완료!");

        saveBtn.setAlpha(1.0f);
        saveBtn.setEnabled(true);
        saveBtn.setClickable(true);
    }
}
