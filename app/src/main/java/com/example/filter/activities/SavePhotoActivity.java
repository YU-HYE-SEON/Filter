package com.example.filter.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.filter.R;
import com.example.filter.etc.ClickUtils;
import com.example.filter.apis.FilterDtoCreateRequest;
import com.example.filter.etc.ImageUtils;

import java.io.File;

public class SavePhotoActivity extends BaseActivity {
    private ImageButton backBtn;
    private ImageView photo;
    private ConstraintLayout bottomArea;
    private ImageView backToHomeBtn, registerBtn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_save_photo);
        backBtn = findViewById(R.id.backBtn);
        photo = findViewById(R.id.photo);
        bottomArea = findViewById(R.id.bottomArea);
        backToHomeBtn = findViewById(R.id.backToHomeBtn);
        registerBtn = findViewById(R.id.registerBtn);

        ViewCompat.setOnApplyWindowInsetsListener(bottomArea, (v, insets) -> {
            Insets nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) v.getLayoutParams();
            lp.bottomMargin = 0;
            v.setLayoutParams(lp);
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), nav.bottom);
            return insets;
        });

        String originalPath = getIntent().getStringExtra("original_image_path");
        String savedImagePath = getIntent().getStringExtra("saved_image");

        float cropN_l = getIntent().getFloatExtra("cropRectN_l", -1f);
        float cropN_t = getIntent().getFloatExtra("cropRectN_t", -1f);
        float cropN_r = getIntent().getFloatExtra("cropRectN_r", -1f);
        float cropN_b = getIntent().getFloatExtra("cropRectN_b", -1f);

        int accumRotationDeg = getIntent().getIntExtra("accumRotationDeg", 0);
        boolean accumFlipH = getIntent().getBooleanExtra("accumFlipH", false);
        boolean accumFlipV = getIntent().getBooleanExtra("accumFlipV", false);

        String brushImagePath = getIntent().getStringExtra("brush_image_path");
        String stickerImagePath = getIntent().getStringExtra("sticker_image_path");

        if (savedImagePath != null) {
            File file = new File(savedImagePath);
            if (file.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(savedImagePath);
                if (bitmap != null) {
                    photo.setImageBitmap(bitmap);
                    //사진 저장 메서드 호출
                    ImageUtils.saveBitmapToGallery(SavePhotoActivity.this, bitmap);
                }
            }
        }

        backBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            finish();
        });

        backToHomeBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;

            Intent intent = new Intent(SavePhotoActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);

            finish();
        });

        /// 중첩 클릭되면 안 됨 ///
        registerBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            ClickUtils.disableTemporarily(v, 800);

            Intent intent = new Intent(SavePhotoActivity.this, RegisterActivity.class);
            intent.putExtra("final_image", savedImagePath);
            intent.putExtra("original_image_path", originalPath);

            intent.putExtra("cropRectN_l", cropN_l);
            intent.putExtra("cropRectN_t", cropN_t);
            intent.putExtra("cropRectN_r", cropN_r);
            intent.putExtra("cropRectN_b", cropN_b);

            intent.putExtra("accumRotationDeg", accumRotationDeg);
            intent.putExtra("accumFlipH", accumFlipH);
            intent.putExtra("accumFlipV", accumFlipV);

            intent.putExtra("brush_image_path", brushImagePath);
            intent.putExtra("sticker_image_path", stickerImagePath);

            FilterDtoCreateRequest.ColorAdjustments adj =
                    (FilterDtoCreateRequest.ColorAdjustments) getIntent().getSerializableExtra("color_adjustments");
            if (adj != null) intent.putExtra("color_adjustments", adj);

            startActivity(intent);
        });
    }
}
