package com.example.filter.activities.apply;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.filter.R;
import com.example.filter.activities.BaseActivity;
import com.example.filter.activities.review.ReviewActivity;
import com.example.filter.api_datas.response_dto.FilterResponse;
import com.example.filter.api_datas.response_dto.ReviewResponse;
import com.example.filter.apis.FilterApi;
import com.example.filter.apis.ReviewApi;
import com.example.filter.apis.UserApi;
import com.example.filter.apis.client.AppRetrofitClient;
import com.example.filter.etc.ClickUtils;
import com.example.filter.etc.FGLRenderer;
import com.example.filter.api_datas.request_dto.FilterDtoCreateRequest;
import com.example.filter.api_datas.FaceStickerData;
import com.example.filter.etc.ImageUtils;
import com.example.filter.etc.StickerMeta;
import com.example.filter.overlayviews.FaceBoxOverlayView;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@androidx.camera.core.ExperimentalGetImage
public class ApplyFilterActivity extends BaseActivity {
    public interface FaceDetectionCallback {
        void onFacesDetected(List<Face> faces, Bitmap originalBitmap);
    }

    private ImageButton backBtn;
    private FrameLayout photoContainer, stickerOverlay;
    private ConstraintLayout bottomArea;
    private AppCompatButton toGalleryBtn, toRegisterReviewBtn;
    private GLSurfaceView glSurfaceView;
    private FGLRenderer renderer;
    private ArrayList<FaceStickerData> faceStickers;
    private Bitmap finalBitmapWithStickers = null;
    private FrameLayout reviewPopOff;
    private View reviewPopOn, dimBackground;
    private ConstraintLayout reviewPop;
    private ImageView iconSnsNone, iconSnsInsta, iconSnsTwitter;
    private TextView snsId;
    private AppCompatButton reviewBtn;
    private boolean isReviewPopVisible = false;
    private String filterId;
    public static FaceBoxOverlayView faceBox;
    private Bitmap originalImageBitmap;

    // 서버로부터 받아온 sns 아이디 저장
    private String instagramId = "";
    private String xId = "";
    private SocialType type = SocialType.NONE;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_apply_photo);
        backBtn = findViewById(R.id.backBtn);
        photoContainer = findViewById(R.id.photoContainer);
        stickerOverlay = findViewById(R.id.stickerOverlay);
        bottomArea = findViewById(R.id.bottomArea);
        toGalleryBtn = findViewById(R.id.toGalleryBtn);
        toRegisterReviewBtn = findViewById(R.id.toRegisterReviewBtn);
        reviewPopOff = findViewById(R.id.reviewPopOff);

        //시스템 바 인셋 설정
        final View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), nav.bottom);
            return insets;
        });

        loadSocial();
        setupReviewPop();

        glSurfaceView = new GLSurfaceView(this);
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        glSurfaceView.getHolder().setFormat(android.graphics.PixelFormat.TRANSLUCENT);
        glSurfaceView.setPreserveEGLContextOnPause(true);
        renderer = new FGLRenderer(this, glSurfaceView, false);
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

                //사진 저장 메서드 호출
                ImageUtils.saveBitmapToGallery(ApplyFilterActivity.this, finalBitmapWithStickers);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        filterId = getIntent().getStringExtra("filterId");

        if (filterId != null) {
            loadFilterData(Long.parseLong(filterId));
        }
        Uri imageUri = getIntent().getData();
        if (imageUri != null) {
            try {
                Bitmap bmp = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                if (bmp != null) {
                    this.originalImageBitmap = bmp;

                    renderer.setBitmap(bmp);
                    glSurfaceView.requestRender();

                    faceBox = new FaceBoxOverlayView(this);
                    photoContainer.addView(faceBox, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    detectFaces(this.originalImageBitmap, (faces, originalBitmap) -> {
                        if (faces.isEmpty()) return;
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        backBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            finish();
        });
    }

    private void setupReviewPop() {
        FrameLayout rootView = findViewById(R.id.reviewPopOff);
        reviewPopOn = getLayoutInflater().inflate(R.layout.m_review_pop, null);
        reviewPop = reviewPopOn.findViewById(R.id.reviewPop);
        iconSnsNone = reviewPopOn.findViewById(R.id.iconSnsNone);
        iconSnsInsta = reviewPopOn.findViewById(R.id.iconSnsInsta);
        iconSnsTwitter = reviewPopOn.findViewById(R.id.iconSnsTwitter);
        snsId = reviewPopOn.findViewById(R.id.snsId);
        reviewBtn = reviewPopOn.findViewById(R.id.reviewBtn);

        dimBackground = new View(this);
        dimBackground.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        dimBackground.setBackgroundColor(Color.parseColor("#B3000000"));
        dimBackground.setVisibility(View.GONE);

        rootView.addView(dimBackground);
        rootView.addView(reviewPopOn);
        reviewPopOn.setVisibility(View.GONE);
        reviewPopOn.setTranslationY(800);

        dimBackground.setOnClickListener(v -> hideReviewPop());

        toRegisterReviewBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            if (isReviewPopVisible) return;
            showReviewPop();
        });


        if (instagramId.isEmpty() || instagramId == null) {
            iconSnsInsta.setEnabled(false);
            iconSnsInsta.setVisibility(View.GONE);
        } else {
            iconSnsInsta.setOnClickListener(v -> {
                type = SocialType.INSTAGRAM;
                snsId.setText(instagramId);
                Log.d("sns선택", "선택됨");
            });
        }

        if (xId.isEmpty() || xId == null) {
            iconSnsTwitter.setEnabled(false);
            iconSnsTwitter.setVisibility(View.GONE);
        } else {
            iconSnsTwitter.setOnClickListener(v -> {
                type = SocialType.X;
                snsId.setText(xId);
                Log.d("sns선택", "선택됨");
            });
        }

        iconSnsNone.setOnClickListener(v -> {
            type = SocialType.NONE;
            snsId.setText("선택 안 함");
            Log.d("sns선택", "선택됨");
        });

        /// 중첩 클릭되면 안 됨 ///
        reviewBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            ClickUtils.disableTemporarily(v, 800);

            if (finalBitmapWithStickers == null) return;

            String savedPath = ImageUtils.saveBitmapToCache(ApplyFilterActivity.this, finalBitmapWithStickers);

            requestCreateReview(savedPath, Long.parseLong(filterId), type);
        });
    }

    /**
     * 서버로부터 소셜 아이디 정보 받아오기
     */
    private void loadSocial() {
        UserApi userApi = AppRetrofitClient.getInstance(this).create(UserApi.class);

        // 소셜 아이디 불러오기
        userApi.getSocialIds().enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                Map<String, String> ids = response.body();

                instagramId = ids.get("instagramId");
                xId = ids.get("xId");

                // todo: 값이 있는지 여부에 따라서 버튼 activation 결정
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                Log.e("Review", "통신 오류", t);
                Toast.makeText(ApplyFilterActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void requestCreateReview(String imagePath, long filterId, SocialType socialType) {
        File file = new File(imagePath);
        if (!file.exists()) {
            Toast.makeText(this, "이미지 파일이 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Request 데이터 구성
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
        MultipartBody.Part multipartFile = MultipartBody.Part.createFormData("file", file.getName(), requestFile);
        RequestBody filterIdBody = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(filterId));
        RequestBody socialTypeBody = RequestBody.create(MediaType.parse("text/plain"), socialType.toString());

        // Retrofit API 인터페이스 생성
        ReviewApi api = AppRetrofitClient.getInstance(this).create(ReviewApi.class);

        // 리뷰생성 API 호출
        api.createReview(multipartFile, filterIdBody, socialTypeBody).enqueue(new Callback<ReviewResponse>() {
            @Override
            public void onResponse(Call<ReviewResponse> call, Response<ReviewResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d("Review", "등록 성공");
                    Toast.makeText(ApplyFilterActivity.this, "리뷰가 등록되었습니다.", Toast.LENGTH_SHORT).show();

                    ReviewResponse reviewResponse = response.body();
                    moveToReview(reviewResponse); // Activity 전환
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Error";
                        Log.e("Review", "등록 실패: " + response.code() + ", " + errorBody);
                        Toast.makeText(ApplyFilterActivity.this, "등록 실패", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<ReviewResponse> call, Throwable t) {
                Log.e("리뷰등록", "통신 오류", t);
                Toast.makeText(ApplyFilterActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void moveToReview(ReviewResponse response) {
        setResult(RESULT_OK);

        Intent intent = new Intent(ApplyFilterActivity.this, ReviewActivity.class);
        intent.putExtra("filterId", filterId);
        intent.putExtra("review_response", response);
        startActivity(intent);
        hideReviewPop();
        finish();
    }

    private void showReviewPop() {
        isReviewPopVisible = true;
        dimBackground.setVisibility(View.VISIBLE);
        reviewPopOn.setVisibility(View.VISIBLE);
        reviewPopOn.animate()
                .translationY(0)
                .setDuration(300)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setListener(null)
                .start();
    }

    private void hideReviewPop() {
        reviewPopOn.animate()
                .translationY(800)
                .setDuration(250)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        reviewPopOn.setVisibility(View.GONE);
                        dimBackground.setVisibility(View.GONE);
                        isReviewPopVisible = false;
                    }
                })
                .start();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (dimBackground != null && dimBackground.getVisibility() == View.VISIBLE) {
            dimBackground.setVisibility(View.GONE);
            isReviewPopVisible = false;
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
        imageView.setImageBitmap(BitmapFactory.decodeFile(path));
        imageView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        overlay.addView(imageView);

        Glide.with(this).load(path).into(imageView);
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

                    applyAdjustments(mapColorAdjustments(data.colorAdjustments));

                    if (data.stickerImageNoFaceUrl != null) {
                        applyBrushStickerImage(stickerOverlay, data.stickerImageNoFaceUrl);
                    }

                    ArrayList<FaceStickerData> stickers = mapFaceStickers(data.stickers);
                    if (!stickers.isEmpty() && originalImageBitmap != null) {
                        ApplyFilterActivity.this.faceStickers = stickers;
                        detectFaces(originalImageBitmap, null);
                    }

                } else {
                    Log.e("ApplyFilter", "필터 정보 조회 실패: " + response.code());
                    Toast.makeText(ApplyFilterActivity.this, "필터 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<FilterResponse> call, Throwable t) {
                Log.e("ApplyFilter", "통신 오류", t);
                Toast.makeText(ApplyFilterActivity.this, "필터 정보를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /// 얼굴인식스티커 적용하기 ///
    private void detectFaces(Bitmap bitmap, ApplyFilterActivity.FaceDetectionCallback callback) {
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
                        Bitmap original = originalImageBitmap;
                        if (original == null) return;

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
}
