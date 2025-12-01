package com.example.filter.activities.apply;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.filter.R;
import com.example.filter.activities.BaseActivity;
import com.example.filter.api_datas.request_dto.FilterDtoCreateRequest;
import com.example.filter.api_datas.response_dto.FilterResponse;
import com.example.filter.apis.FilterApi;
import com.example.filter.apis.client.AppRetrofitClient;
import com.example.filter.etc.CGLRenderer;
import com.example.filter.etc.ClickUtils;
import com.example.filter.etc.FGLRenderer;
import com.example.filter.overlayviews.FaceBoxOverlayView;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@androidx.camera.core.ExperimentalGetImage
public class CameraActivity extends BaseActivity {
    private static final int CAMERA_PERMISSION_CODE = 10;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
    private ExecutorService cameraExecutor;
    private PreviewView camera;
    private FrameLayout cameraContainer, overlay;
    private ImageButton backBtn;
    private AppCompatButton transitionBtn, flashBtn, timerBtn, ratioBtn, photoBtn;
    private int transitionClickCount = 0;
    private int ratioClickCount = 0;
    private String filterId;
    private GLSurfaceView glSurfaceView;
    private CGLRenderer renderer;
    private FaceBoxOverlayView faceBox;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_camera);
        backBtn = findViewById(R.id.backBtn);
        transitionBtn = findViewById(R.id.transitionBtn);
        cameraContainer = findViewById(R.id.cameraContainer);
        camera = findViewById(R.id.camera);
        overlay = findViewById(R.id.overlay);
        flashBtn = findViewById(R.id.flashBtn);
        timerBtn = findViewById(R.id.timerBtn);
        ratioBtn = findViewById(R.id.ratioBtn);
        photoBtn = findViewById(R.id.photoBtn);

        cameraExecutor = Executors.newSingleThreadExecutor();

        glSurfaceView = new GLSurfaceView(this);
        glSurfaceView.setEGLContextClientVersion(2);
        renderer = new CGLRenderer(this, glSurfaceView);
        glSurfaceView.setRenderer(renderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        cameraContainer.addView(glSurfaceView);
        overlay.bringToFront();

        transitionClickCount = 0;
        setTransitionMode(0);

        ratioClickCount = 1;
        updateRatioMode(1);

        filterId = getIntent().getStringExtra("filterId");
        if (filterId != null) {
            loadFilterData(Long.parseLong(filterId));
        }

        backBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            finish();
        });

        transitionBtn.setOnClickListener(v -> {
            Log.d("카메라전환", "카메라 전환 버튼 누름");
            transitionClickCount++;
            int mode = transitionClickCount % 2;
            setTransitionMode(mode);
        });

        ratioBtn.setOnClickListener(v -> {
            ratioClickCount++;
            int mode = ratioClickCount % 3;
            updateRatioMode(mode);
        });

        /// 중첩 클릭되면 안 됨 ///
        photoBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            ClickUtils.disableTemporarily(v, 800);

            Intent intent = new Intent(CameraActivity.this, ApplyFilterActivity.class);
            startActivity(intent);
        });
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
        imageView.setImageBitmap(BitmapFactory.decodeFile(path));
        imageView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        overlay.addView(imageView);

        Glide.with(this).load(path).into(imageView);
        //overlay.setZ(20f);
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

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview.Builder builder = new Preview.Builder();

                Preview preview = builder.build();
                preview.setSurfaceProvider(camera.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();

                imageAnalysis.setAnalyzer(cameraExecutor, image -> {
                    Bitmap bitmap = convertImageToBitmap(image);
                    int rotation = image.getImageInfo().getRotationDegrees();
                    bitmap = rotateBitmap(bitmap, rotation);

                    renderer.setBitmap(bitmap);

                    glSurfaceView.requestRender();
                    image.close();
                });

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void setTransitionMode(int mode) {
        switch (mode) {
            case 1:
                cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                break;
            case 0:
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                break;
        }

        startCamera();
    }

    private void updateRatioMode(int mode) {
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) cameraContainer.getLayoutParams();

        switch (mode) {
            case 1:
                params.dimensionRatio = "1:1";
                params.verticalBias = 0.2f;
                break;

            case 2:
                params.dimensionRatio = "3:4";
                params.verticalBias = 0.25f;
                break;

            case 0:
                params.dimensionRatio = "9:16";
                params.verticalBias = 0.25f;
                break;
        }

        cameraContainer.setLayoutParams(params);
        cameraContainer.requestLayout();

        renderer.setCropRatioMode(mode);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}
