package com.example.filter.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.filter.etc.FGLRenderer;
import com.example.filter.R;
import com.example.filter.fragments.ToolsFragment;

import java.io.InputStream;

public class FilterActivity extends AppCompatActivity {
    private ConstraintLayout topArea;
    //private FGLSurfaceView photoPreview;
    private GLSurfaceView photoPreview;
    private FGLRenderer renderer;
    private ImageButton backBtn;
    private ImageButton saveBtn;
    private TextView saveTxt;
    private ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri photoUri = result.getData().getData();

                    if (photoUri != null) {
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(photoUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            inputStream.close();

                            ExifInterface exif = new ExifInterface(getContentResolver().openInputStream(photoUri));
                            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                            float rotate = 0;

                            switch (orientation) {
                                case ExifInterface.ORIENTATION_ROTATE_90:
                                    rotate = 90;
                                    break;
                                case ExifInterface.ORIENTATION_ROTATE_180:
                                    rotate = 180;
                                    break;
                                case ExifInterface.ORIENTATION_ROTATE_270:
                                    rotate = 270;
                                    break;
                            }

                            if (rotate != 0) {
                                Matrix matrix = new Matrix();
                                matrix.postRotate(rotate);
                                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                            }

                            loadBitmapToRenderer(bitmap);

                            renderer.resetAllFilter();

                        } catch (Exception e) {
                            Toast.makeText(this, "이미지 로드 실패", Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                        }
                    }
                }
            });

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
        if (photoPreview != null) photoPreview.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        hideSystemUI();
        if (photoPreview != null) photoPreview.onPause();
    }

    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE
        );
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
        setContentView(R.layout.activity_filter);
        topArea = findViewById(R.id.topArea);
        backBtn = findViewById(R.id.backBtn);
        saveBtn = findViewById(R.id.saveBtn);
        saveTxt = findViewById(R.id.saveTxt);
        photoPreview = findViewById(R.id.photoPreview);
        //renderer = photoPreview.getRenderer();
        photoPreview.setEGLContextClientVersion(2);
        renderer = new FGLRenderer(this, photoPreview);
        photoPreview.setRenderer(renderer);
        photoPreview.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.bottomArea, new ToolsFragment())
                    .commit();
        }

        backBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            galleryLauncher.launch(intent);
        });

        //saveBtn.setEnabled(false);

/*        getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.bottomArea);
                if (currentFragment instanceof ToolsFragment) {
                    animTADown();
                } else if (currentFragment instanceof CropFragment || currentFragment instanceof ColorsFragment) {
                    animTAUp();
                }
            }
        });*/
    }

    private void loadBitmapToRenderer(Bitmap bitmap) {
        if (renderer != null) {
            renderer.setBitmap(bitmap);
            photoPreview.requestRender();
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

    /*public void animTAUp() {
        if (topArea.getVisibility() == View.VISIBLE) {
            if (renderer != null) {
                photoPreview.requestRender(); // 애니메이션 시작 직전 렌더링 요청
            }

            topArea.animate()
                    .translationY(-topArea.getHeight())
                    .setDuration(200)
                    .setInterpolator(new LinearInterpolator())
                    .setUpdateListener(animation -> {
                        Object animatedValue = animation.getAnimatedValue("translationY");
                        if (animatedValue instanceof Float) {
                            float currentTranslationY = (Float) animatedValue;
                            if (renderer != null) {
                                renderer.setAnimOffsetY(currentTranslationY);
                                photoPreview.requestRender();
                            }
                        }
                    })
                    .withEndAction(() -> {
                        topArea.setVisibility(View.GONE);
                        topArea.setTranslationY(0);
                        if (renderer != null) {
                            renderer.setAnimOffsetY(0);
                            photoPreview.requestRender();
                        }
                    })
                    .start();

        }
    }

    public void animTADown() {
        if (topArea.getVisibility() == View.GONE) {
            topArea.setVisibility(View.VISIBLE);
            topArea.setTranslationY(-topArea.getHeight());
            if (renderer != null) {
                photoPreview.requestRender(); // 애니메이션 시작 직전 렌더링 요청
            }
            topArea.animate()
                    .translationY(0)
                    .setDuration(200)
                    .setInterpolator(new LinearInterpolator())
                    .setUpdateListener(animation -> {
                        Object animatedValue = animation.getAnimatedValue("translationY");
                        if (animatedValue instanceof Float) {
                            float currentTranslationY = (Float) animatedValue;
                            if (renderer != null) {
                                renderer.setAnimOffsetY(currentTranslationY);
                                photoPreview.requestRender();
                            }
                        }
                    })
                    .withEndAction(() -> {
                        if (renderer != null) {
                            renderer.setAnimOffsetY(0);
                            photoPreview.requestRender();
                        }
                    })
                    .start();
        }
    }*/
}