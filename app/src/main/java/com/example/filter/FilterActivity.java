package com.example.filter;

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
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.filter.fragments.FilterToolsFragment;

import java.io.InputStream;

public class FilterActivity extends AppCompatActivity {
    private GLSurfaceView glSurfaceView;
    private GLRenderer renderer;
    private ImageButton backBtn;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private Bitmap currentBitmap;

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
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
        setContentView(R.layout.filter_activity);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.bottomArea, new FilterToolsFragment())
                    .commit();
        }

        backBtn = findViewById(R.id.backBtn);
        glSurfaceView = findViewById(R.id.photoPreview);
        glSurfaceView.setEGLContextClientVersion(2);
        renderer = new GLRenderer(this, glSurfaceView);
        glSurfaceView.setRenderer(renderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        galleryLauncher = registerForActivityResult(
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

                                currentBitmap = bitmap;
                                loadBitmapToRenderer(bitmap);

                                renderer.resetAllFilter();

                            } catch (Exception e) {
                                Toast.makeText(this, "이미지 로드 실패", Toast.LENGTH_SHORT).show();
                                e.printStackTrace();
                            }
                        }
                    }
                }
        );

        backBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            galleryLauncher.launch(intent);
        });
    }

    private void loadBitmapToRenderer(Bitmap bitmap) {
        if (renderer != null) {
            renderer.setBitmap(bitmap);
            glSurfaceView.requestRender();
        }
    }

    public int getCurrentValue(String filterType) {
        if (filterType != null) {
            return renderer.getCurrentValue(filterType);
        }
        return 0;
    }

    public void onTempValue(String filterType, int value) {
        if (filterType != null) {
            renderer.setTempValue(filterType, value);
        }
    }

    public void onUpdateValue(String filterType, int value) {
        if (filterType != null) {
            renderer.updateValue(filterType, value);
        }
    }

    public void onCancelValue(String filterType) {
        if (filterType != null) {
            renderer.cancelValue(filterType);
        }
    }
}