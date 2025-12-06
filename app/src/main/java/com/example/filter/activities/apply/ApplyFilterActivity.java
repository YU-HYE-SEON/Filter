package com.example.filter.activities.apply;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
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
import com.example.filter.etc.Controller;
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

    private String faceDetectMessage = null;
    private boolean saveDone = false;
    private boolean loadingAnimFinishedOnce = false;
    private int loadingAnimPlayCount = 0;
    private final int MIN_PLAY_COUNT = 1;
    private FrameLayout loadingContainer, loadingFinishContainer;
    private LottieAnimationView loadingAnim, loadingFinishAnim;
    private ImageButton backBtn;
    private TextView saveSuccessTxt;
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
    private ConstraintLayout topArea;
    private boolean isToastVisible = false;
    private boolean isFaceStickerActive = false;

    private boolean isBrushStickerReady = false;    //얼굴x스티커+브러쉬그림 잘 그려졌는지 확인
    private boolean isFaceStickerReady = false;     //얼굴o스티커 잘 부착됐는지 확인
    private boolean isStickerApplied = false;       //위에 두 스티커 모두 잘 나오는지 최종 확인 → 모두 잘 나온 후에 사진 저장

    private boolean isSavedToGallery = false;       //사진 중복 저장 안 되게

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!loadingAnimFinishedOnce) {
            loadingAnim.playAnimation();
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_apply_photo);
        topArea = findViewById(R.id.topArea);
        backBtn = findViewById(R.id.backBtn);
        saveSuccessTxt = findViewById(R.id.saveSuccessTxt);
        photoContainer = findViewById(R.id.photoContainer);
        stickerOverlay = findViewById(R.id.stickerOverlay);
        bottomArea = findViewById(R.id.bottomArea);
        toGalleryBtn = findViewById(R.id.toGalleryBtn);
        toRegisterReviewBtn = findViewById(R.id.toRegisterReviewBtn);
        reviewPopOff = findViewById(R.id.reviewPopOff);

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

        final AnimatorListenerAdapter loadingListener = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationRepeat(Animator animation) {
                super.onAnimationRepeat(animation);
                loadingAnimPlayCount++;
                Log.d("로딩애니횟수", "Repeat Count: " + loadingAnimPlayCount);

                if (loadingAnimPlayCount >= MIN_PLAY_COUNT && saveDone) {
                    if (!loadingAnimFinishedOnce) {
                        loadingAnimFinishedOnce = true;
                        loadingAnim.removeAnimatorListener(this);
                        finishLoading();
                    }
                }
            }
        };
        loadingAnim.addAnimatorListener(loadingListener);

        toGalleryBtn.getBackground().setColorFilter(Color.parseColor("#00499A"), PorterDuff.Mode.SRC_ATOP);
        toGalleryBtn.setTextColor(Color.parseColor("#989898"));
        toGalleryBtn.setEnabled(false);
        toGalleryBtn.setClickable(false);

        toRegisterReviewBtn.getBackground().setColorFilter(Color.parseColor("#759749"), PorterDuff.Mode.SRC_ATOP);
        toRegisterReviewBtn.setTextColor(Color.parseColor("#00499A"));
        toRegisterReviewBtn.setEnabled(false);
        toRegisterReviewBtn.setClickable(false);

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
                        vW, vH,
                        Bitmap.Config.ARGB_8888
                );
                Canvas overlayCanvas = new Canvas(overlayBitmap);
                stickerOverlay.draw(overlayCanvas);

                //Rect src = new Rect(vX, vY, vX + vW, vY + vH);
                //Rect dst = new Rect(0, 0, finalBitmap.getWidth(), finalBitmap.getHeight());
                canvas.drawBitmap(overlayBitmap, 0, 0, null);
                overlayBitmap.recycle();

                finalBitmapWithStickers = finalBitmap;

                //사진 저장 메서드 호출
                //ImageUtils.saveBitmapToGallery(ApplyFilterActivity.this, finalBitmapWithStickers);
                if (isStickerApplied && !isSavedToGallery) {
                    ImageUtils.saveBitmapToGallery(ApplyFilterActivity.this, finalBitmapWithStickers);
                    isSavedToGallery = true;

                    /*runOnUiThread(() -> {
                        toGalleryBtn.getBackground().setColorFilter(Color.parseColor("#007AFF"), PorterDuff.Mode.SRC_ATOP);
                        toGalleryBtn.setTextColor(Color.WHITE);
                        toGalleryBtn.setEnabled(true);
                        toGalleryBtn.setClickable(true);

                        toRegisterReviewBtn.getBackground().setColorFilter(Color.parseColor("#C2FA7A"), PorterDuff.Mode.SRC_ATOP);
                        toRegisterReviewBtn.setTextColor(Color.parseColor("#007AFF"));
                        toRegisterReviewBtn.setEnabled(true);
                        toRegisterReviewBtn.setClickable(true);
                    });

                    Toast.makeText(this, "저장 완료!", Toast.LENGTH_SHORT).show();*/

                    if (!saveDone) {
                        saveDone = true;
                        checkAndFinishLoading();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        filterId = getIntent().getStringExtra("filterId");

        boolean fromCamera = getIntent().getBooleanExtra("from_camera", false);
        String finalImagePath = getIntent().getStringExtra("final_image_path");
        boolean isGetPath = finalImagePath != null;

        Bitmap imageToDisplay = null;
        Uri imageUri = getIntent().getData();

        if (!fromCamera && !isGetPath && filterId != null) {
            loadFilterData(Long.parseLong(filterId));
        }

        if (isGetPath) {
            imageToDisplay = BitmapFactory.decodeFile(finalImagePath);

            if (imageToDisplay != null) {
                finalBitmapWithStickers = imageToDisplay;

                //ImageUtils.saveBitmapToGallery(ApplyFilterActivity.this, finalBitmapWithStickers);
                if (!isSavedToGallery) {
                    ImageUtils.saveBitmapToGallery(ApplyFilterActivity.this, finalBitmapWithStickers);
                    isSavedToGallery = true;

                    /*runOnUiThread(() -> {
                        toGalleryBtn.getBackground().setColorFilter(Color.parseColor("#007AFF"), PorterDuff.Mode.SRC_ATOP);
                        toGalleryBtn.setTextColor(Color.WHITE);
                        toGalleryBtn.setEnabled(true);
                        toGalleryBtn.setClickable(true);

                        toRegisterReviewBtn.getBackground().setColorFilter(Color.parseColor("#C2FA7A"), PorterDuff.Mode.SRC_ATOP);
                        toRegisterReviewBtn.setTextColor(Color.parseColor("#007AFF"));
                        toRegisterReviewBtn.setEnabled(true);
                        toRegisterReviewBtn.setClickable(true);
                    });

                    Toast.makeText(this, "저장 완료!", Toast.LENGTH_SHORT).show();*/

                    if (!saveDone) {
                        saveDone = true;
                        checkAndFinishLoading();
                    }
                }
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

                    /*renderer.setBitmap(bmp);
                    glSurfaceView.requestRender();

                    faceBox = new FaceBoxOverlayView(this);
                    photoContainer.addView(faceBox, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    detectFaces(this.originalImageBitmap, (faces, originalBitmap) -> {
                        if (faces.isEmpty()) return;
                    });*/
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
            finish();
        });


        ClickUtils.clickDim(toRegisterReviewBtn);
        ClickUtils.clickDim(toGalleryBtn);

        toGalleryBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;

            Uri collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(collection);
            intent.setType("image/*");
            intent.putExtra("android.intent.extra.LOCAL_ONLY", true);

            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "갤러리를 열 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
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

        iconSnsNone.setOnClickListener(v -> {
            type = SocialType.NONE;
            snsId.setText("선택 안 함");
        });

        iconSnsInsta.setOnClickListener(v -> {
            type = SocialType.INSTAGRAM;
            snsId.setText(instagramId);
        });

        iconSnsTwitter.setOnClickListener(v -> {
            type = SocialType.X;
            snsId.setText(xId);
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

    private void updateSNSIcons() {
        if (instagramId == null || instagramId.isEmpty()) {
            iconSnsInsta.setVisibility(View.GONE);
        } else {
            iconSnsInsta.setVisibility(View.VISIBLE);
        }

        if (xId == null || xId.isEmpty()) {
            iconSnsTwitter.setVisibility(View.GONE);
        } else {
            iconSnsTwitter.setVisibility(View.VISIBLE);
        }
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

                updateSNSIcons();
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

        imageView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        Glide.with(this).load(path).into(new CustomTarget<Drawable>() {
            @Override
            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                imageView.setImageDrawable(resource);
                overlay.addView(imageView);

                isBrushStickerReady = true;
                checkAndFinalizeStickers();
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
                isBrushStickerReady = true;
                checkAndFinalizeStickers();
            }
        });
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

    private void checkAndFinalizeStickers() {
        if (isBrushStickerReady && isFaceStickerReady) {
            if (!isStickerApplied) {
                isStickerApplied = true;

                glSurfaceView.postDelayed(() -> {
                    renderer.captureBitmap();
                }, 200);
            }
        }
    }

    private void loadFilterData(long id) {
        String finalImagePath = getIntent().getStringExtra("final_image_path");
        if (finalImagePath != null) return;

        FilterApi api = AppRetrofitClient.getInstance(this).create(FilterApi.class);
        api.getFilter(id).enqueue(new Callback<FilterResponse>() {
            @Override
            public void onResponse(Call<FilterResponse> call, Response<FilterResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    FilterResponse data = response.body();

                    applyAdjustments(mapColorAdjustments(data.colorAdjustments));

                    if (data.stickerImageNoFaceUrl != null) {
                        //applyBrushStickerImage(stickerOverlay, data.stickerImageNoFaceUrl);

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
                    } else {
                        isBrushStickerReady = true;
                    }

                    ArrayList<FaceStickerData> stickers = mapFaceStickers(data.stickers);
                    if (!stickers.isEmpty() && originalImageBitmap != null) {
                        ApplyFilterActivity.this.faceStickers = stickers;

                        isFaceStickerActive = true;

                        detectFaces(originalImageBitmap, null);
                    } else {
                        isFaceStickerReady = true;
                        isFaceStickerActive = false;
                    }

                    checkAndFinalizeStickers();

                } else {
                    Log.e("ApplyFilter", "필터 정보 조회 실패: " + response.code());
                    Toast.makeText(ApplyFilterActivity.this, "필터 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();

                    isBrushStickerReady = true;
                    isFaceStickerReady = true;
                    checkAndFinalizeStickers();
                }
            }

            @Override
            public void onFailure(Call<FilterResponse> call, Throwable t) {
                Log.e("ApplyFilter", "통신 오류", t);
                Toast.makeText(ApplyFilterActivity.this, "필터 정보를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show();

                isBrushStickerReady = true;
                isFaceStickerReady = true;
                checkAndFinalizeStickers();
            }
        });
    }

    /// 얼굴인식스티커 적용하기 ///
    private void detectFaces(Bitmap bitmap, ApplyFilterActivity.FaceDetectionCallback callback) {
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

                        stickerOverlay.postDelayed(() -> {
                            isFaceStickerReady = true;
                            checkAndFinalizeStickers();
                        }, 100);
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

                    isFaceStickerReady = true;
                    checkAndFinalizeStickers();

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

    private void checkAndFinishLoading() {
        if (!saveDone) return;

        if (loadingAnimPlayCount >= MIN_PLAY_COUNT) {
            if (!loadingAnimFinishedOnce) {
                loadingAnimFinishedOnce = true;

                loadingAnim.cancelAnimation();

                finishLoading();
            }
        }
    }

    private void finishLoading() {
        if (!saveDone || !loadingAnimFinishedOnce) return;

        loadingFinishContainer.setVisibility(View.VISIBLE);
        loadingFinishAnim.setVisibility(View.VISIBLE);
        loadingAnim.pauseAnimation();
        loadingContainer.setVisibility(View.GONE);
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

        saveSuccessTxt.setText("저장 완료!");

        runOnUiThread(() -> {
            toGalleryBtn.getBackground().setColorFilter(Color.parseColor("#007AFF"), PorterDuff.Mode.SRC_ATOP);
            toGalleryBtn.setTextColor(Color.WHITE);
            toGalleryBtn.setEnabled(true);
            toGalleryBtn.setClickable(true);

            toRegisterReviewBtn.getBackground().setColorFilter(Color.parseColor("#C2FA7A"), PorterDuff.Mode.SRC_ATOP);
            toRegisterReviewBtn.setTextColor(Color.parseColor("#007AFF"));
            toRegisterReviewBtn.setEnabled(true);
            toRegisterReviewBtn.setClickable(true);
        });

        Toast.makeText(this, "저장 완료!", Toast.LENGTH_SHORT).show();
    }
}
