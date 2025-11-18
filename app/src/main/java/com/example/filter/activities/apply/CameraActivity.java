package com.example.filter.activities.apply;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.filter.R;
import com.example.filter.activities.BaseActivity;
import com.example.filter.apis.dto.FilterDtoCreateRequest;
import com.example.filter.etc.ClickUtils;
import com.example.filter.etc.FGLRenderer;
import com.example.filter.etc.FaceStickerData;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends BaseActivity {
    private static final int CAMERA_PERMISSION_CODE = 10;
    private PreviewView previewView;
    private ExecutorService cameraExecutor;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;


    private ImageButton backBtn;
    private ImageView camera;
    private FGLRenderer renderer;
    private AppCompatButton ratioBtn, photoBtn;
    private FrameLayout cameraContainer, overlay;
    //private ExecutorService cameraExecutor;
    private FilterDtoCreateRequest.ColorAdjustments adj;
    private String brushPath;
    private String stickerImageNoFacePath;
    private ArrayList<FaceStickerData> faceStickers;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_camera);
        backBtn = findViewById(R.id.backBtn);
        camera = findViewById(R.id.camera);
        ratioBtn = findViewById(R.id.ratioBtn);
        photoBtn = findViewById(R.id.photoBtn);
        cameraContainer = findViewById(R.id.cameraContainer);
        overlay = findViewById(R.id.overlay);

        cameraExecutor = Executors.newSingleThreadExecutor();

        previewView = new PreviewView(this);
        previewView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        cameraContainer.addView(previewView, 0);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }

        adj = (FilterDtoCreateRequest.ColorAdjustments) getIntent().getSerializableExtra("color_adjustments");
        brushPath = getIntent().getStringExtra("brush_image_path");
        stickerImageNoFacePath = getIntent().getStringExtra("stickerImageNoFacePath");
        faceStickers = (ArrayList<FaceStickerData>) getIntent().getSerializableExtra("face_stickers");

        //if (adj != null) applyAdjustments(adj);
        if (brushPath != null) applyBrushStickerImage(overlay, brushPath);
        if (stickerImageNoFacePath != null) {
            applyBrushStickerImage(overlay, stickerImageNoFacePath);
        }

        backBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            finish();
        });


        /// 중첩 클릭되면 안 됨 ///
        photoBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            ClickUtils.disableTemporarily(v, 800);

            Intent intent = new Intent(CameraActivity.this, ApplyFilterActivity.class);
            startActivity(intent);
        });
    }

    /*private void applyAdjustments(FilterDtoCreateRequest.ColorAdjustments a) {
        renderer.updateValue("밝기", a.brightness * 100f);
        renderer.updateValue("노출", a.exposure * 100f);
        renderer.updateValue("대비", a.contrast * 100f);
        renderer.updateValue("하이라이트", a.highlight * 100f);
        renderer.updateValue("그림자", a.shadow * 100f);
        renderer.updateValue("온도", a.temperature * 100f);
        renderer.updateValue("색조", a.hue * 100f);
        renderer.updateValue("채도", (a.saturation - 1.0f) * 100f);
        renderer.updateValue("선명하게", a.sharpen * 100f);
        renderer.updateValue("흐리게", a.blur * 100f);
        renderer.updateValue("비네트", a.vignette * 100f);
        renderer.updateValue("노이즈", a.noise * 100f);

        //camera.requestRender();
        //camera.postDelayed(() -> {
        //    renderer.captureBitmap();
        //}, 150);
    }*/

    private void applyBrushStickerImage(FrameLayout overlay, String path) {
        ImageView imageView = new ImageView(this);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setAdjustViewBounds(true);
        imageView.setImageBitmap(BitmapFactory.decodeFile(path));
        imageView.setLayoutParams(
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                )
        );
        overlay.addView(imageView);
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview);

            } catch (ExecutionException | InterruptedException e) {
                Log.e("CameraActivity", "CameraX error", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        Toast.makeText(this, "촬영 버튼 클릭됨 (아직 구현 안됨)", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "카메라 권한이 필요합니다", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}
