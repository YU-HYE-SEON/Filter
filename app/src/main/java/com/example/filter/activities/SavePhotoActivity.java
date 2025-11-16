package com.example.filter.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.filter.R;
import com.example.filter.etc.ClickUtils;
import com.example.filter.apis.dto.FilterDtoCreateRequest;
import com.example.filter.etc.FaceStickerData;
import com.example.filter.etc.ImageUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SavePhotoActivity extends BaseActivity {
    private ImageButton backBtn;
    private ImageView photo;
    private ConstraintLayout bottomArea;
    private AppCompatButton toArchiveBtn, toRegisterBtn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_save_photo);
        backBtn = findViewById(R.id.backBtn);
        photo = findViewById(R.id.photo);
        bottomArea = findViewById(R.id.bottomArea);
        toArchiveBtn = findViewById(R.id.toArchiveBtn);
        toRegisterBtn = findViewById(R.id.toRegisterBtn);

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

        /// 얼굴인식스티커 정보 받기 ///
        String stickerImageNoFacePath = getIntent().getStringExtra("sticker_image_no_face_path");
        ArrayList<FaceStickerData> faceStickers =
                (ArrayList<FaceStickerData>) getIntent().getSerializableExtra("face_stickers");
        /*if (faceStickers != null && !faceStickers.isEmpty()) {
            for (FaceStickerData d : faceStickers) {
                Log.d("StickerFlow", String.format(
                        "[SavePhotoActivity] 받은 FaceStickerData → relX=%.4f, relY=%.4f, relW=%.4f, relH=%.4f, rot=%.4f, groupId=%d",
                        d.relX, d.relY, d.relW, d.relH, d.rot, d.groupId
                ));
            }
        } else {
            Log.d("StickerFlow", "[SavePhotoActivity] faceStickers가 비어있음 혹은 null입니다.");
        }*/


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
            Intent intent = new Intent(SavePhotoActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        ClickUtils.clickDim(toArchiveBtn);
        toArchiveBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            finish();
        });

        ClickUtils.clickDim(toRegisterBtn);
        /// 중첩 클릭되면 안 됨 ///
        toRegisterBtn.setOnClickListener(v -> {
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
            //intent.putExtra("sticker_image_path", stickerImagePath);

            /// 얼굴인식스티커 정보 전달 ///
            intent.putExtra("stickerImageNoFacePath", stickerImageNoFacePath);
            intent.putExtra("face_stickers", new ArrayList<>(faceStickers));

            List<FilterDtoCreateRequest.Sticker> stickers = new ArrayList<>();
            for (FaceStickerData d : faceStickers) {
                FilterDtoCreateRequest.Sticker s = new FilterDtoCreateRequest.Sticker();
                s.placementType = "face";
                s.x = d.relX;
                s.y = d.relY;
                s.scale = (d.relW + d.relH) / 2f;
                //s.relW = d.relW;
                //s.relH = d.relH;
                s.rotation = d.rot;
                s.stickerId = d.groupId;
                stickers.add(s);

                /*Log.d("StickerFlow", String.format(
                        "[SavePhotoActivity] 전달 준비 → relX=%.4f, relY=%.4f, relW=%.4f, relH=%.4f, rot=%.4f, groupId=%d",
                        d.relX, d.relY, d.relW, d.relH, d.rot, d.groupId
                ));*/
            }

            FilterDtoCreateRequest.ColorAdjustments adj =
                    (FilterDtoCreateRequest.ColorAdjustments) getIntent().getSerializableExtra("color_adjustments");
            if (adj != null) intent.putExtra("color_adjustments", adj);

            startActivity(intent);
        });
    }
}
