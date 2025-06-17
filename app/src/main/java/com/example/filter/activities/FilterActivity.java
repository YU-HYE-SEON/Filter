package com.example.filter.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.filter.etc.ClickUtils;
import com.example.filter.etc.FGLRenderer;
import com.example.filter.R;
import com.example.filter.etc.ImageUtils;
import com.example.filter.fragments.ColorsFragment;
import com.example.filter.fragments.CustomseekbarFragment;
import com.example.filter.fragments.ToolsFragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FilterActivity extends BaseActivity {
    private ConstraintLayout topArea;
    private GLSurfaceView photoPreview;
    private FGLRenderer renderer;
    private ImageButton backBtn;
    private ImageButton saveBtn;
    private TextView saveTxt;
    private ActivityResultLauncher<Intent> galleryReSelectionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri photoUri = result.getData().getData();
                    if (photoUri != null) {
                        loadImageFromUri(photoUri);
                        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                        getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.bottomArea, new ToolsFragment())
                                .commit();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);
        topArea = findViewById(R.id.topArea);
        backBtn = findViewById(R.id.backBtn);
        saveBtn = findViewById(R.id.saveBtn);
        saveTxt = findViewById(R.id.saveTxt);
        photoPreview = findViewById(R.id.photoPreview);

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
            loadImageFromUri(photoUri);
        } else {
            finish();
            return;
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.bottomArea, new ToolsFragment())
                    .commit();
        }

        backBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(500)) return;

            onBackPressed();
        });

        saveBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(500)) return;

            renderer.setOnBitmapCaptureListener(fullBitmap -> {
                Bitmap cropped = renderer.cropCenterRegion(fullBitmap
                        , renderer.getViewportX()
                        , renderer.getViewportY()
                        , renderer.getViewportWidth()
                        , renderer.getViewportHeight());
                File tempFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "temp_captured_image.png");
                try (FileOutputStream out = new FileOutputStream(tempFile)) {
                    cropped.compress(Bitmap.CompressFormat.PNG, 100, out);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                Intent intent = new Intent(FilterActivity.this, SavePhotoActivity.class);
                intent.putExtra("saved_image", tempFile.getAbsolutePath());
                startActivity(intent);
            });
            renderer.captureBitmap();
        });
    }

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
            if (exifInputStream != null) exifInputStream.close();

            if (rotate != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotate);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            }

            renderer.setBitmap(bitmap);
            photoPreview.requestRender();
            if (renderer != null) {
                renderer.resetAllFilter();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
        } else {
            Fragment currentFragment = fragmentManager.findFragmentById(R.id.bottomArea);
            if (currentFragment instanceof ToolsFragment) {
                Intent intent = new Intent(FilterActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            } else {
                super.onBackPressed();
            }
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
}