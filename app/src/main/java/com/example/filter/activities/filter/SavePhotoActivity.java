package com.example.filter.activities.filter;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.filter.R;
import com.example.filter.activities.BaseActivity;
import com.example.filter.activities.MainActivity;
import com.example.filter.etc.ClickUtils;
import com.example.filter.api_datas.FilterCreationData; // ✅ Import 필수
import com.example.filter.etc.ImageUtils;

import java.io.File;

public class SavePhotoActivity extends BaseActivity {
    private boolean animationDone = false;
    private boolean saveDone = false;
    private FrameLayout loadingContainer, loadingFinishContainer;
    private LottieAnimationView loadingAnim, loadingFinishAnim;
    private ImageButton backBtn;
    private TextView saveSuccessTxt;
    private ImageView photo;
    private ConstraintLayout bottomArea;
    private AppCompatButton toGalleryBtn, toRegisterBtn;

    // ✅ 데이터를 담을 객체 선언
    private FilterCreationData filterData;
    private String displayImagePath; // 화면 표시용 이미지 경로
    private static final int PERMISSION_REQUEST_CODE = 100;
    private Bitmap imageToSave;
    private boolean isSavedToGallery = false;       //사진 중복 저장 안 되게

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus){
            loadingAnim.playAnimation();
            loadingAnim.setRepeatCount(0);
            loadingAnim.addAnimatorListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (!animationDone) {
                        animationDone = true;
                        finishLoading();
                    }
                }
            });
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_save_photo);

        backBtn = findViewById(R.id.backBtn);
        saveSuccessTxt = findViewById(R.id.saveSuccessTxt);
        photo = findViewById(R.id.photo);
        bottomArea = findViewById(R.id.bottomArea);
        toGalleryBtn = findViewById(R.id.toGalleryBtn);
        toRegisterBtn = findViewById(R.id.toRegisterBtn);

        loadingContainer = findViewById(R.id.loadingContainer);
        loadingAnim = findViewById(R.id.loadingAnim);
        loadingFinishContainer = findViewById(R.id.loadingFinishContainer);
        loadingFinishAnim = findViewById(R.id.loadingFinishAnim);
        loadingFinishContainer.setVisibility(View.GONE);
        loadingFinishAnim.setVisibility(View.GONE);
        loadingContainer.setVisibility(View.VISIBLE);

        // 시스템 바 인셋 처리 (기존 코드 유지)
        ViewCompat.setOnApplyWindowInsetsListener(bottomArea, (v, insets) -> {
            Insets nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) v.getLayoutParams();
            lp.bottomMargin = 0;
            v.setLayoutParams(lp);
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), nav.bottom);
            return insets;
        });

        // ---------------------------------------------------------------
        // ✅ [핵심 수정] 1. FilterActivity에서 보낸 데이터 받기
        // ---------------------------------------------------------------
        // 개별 데이터(String, int...)를 받는 대신 객체 하나를 통째로 받습니다.
        filterData = getIntent().getParcelableExtra("filter_data");

        // 화면에 보여줄 최종 이미지 경로는 별도로 받음 (로딩 속도 최적화)
        // (만약 FilterCreationData 안에 editedImageUrl에 로컬 경로가 있다면 그걸 써도 됩니다)
        displayImagePath = getIntent().getStringExtra("display_image_path");
        if (displayImagePath == null && filterData != null) {
            // 혹시 display_image_path가 안 넘어왔다면 editedImageUrl 사용
            displayImagePath = filterData.editedImageUrl;
        }

        // ---------------------------------------------------------------
        // ✅ UI 설정
        // ---------------------------------------------------------------
        toGalleryBtn.getBackground().setColorFilter(Color.parseColor("#00499A"), PorterDuff.Mode.SRC_ATOP);
        toGalleryBtn.setTextColor(Color.parseColor("#989898"));
        toGalleryBtn.setEnabled(false);
        toGalleryBtn.setClickable(false);

        toRegisterBtn.getBackground().setColorFilter(Color.parseColor("#759749"), PorterDuff.Mode.SRC_ATOP);
        toRegisterBtn.setTextColor(Color.parseColor("#00499A"));
        toRegisterBtn.setEnabled(false);
        toRegisterBtn.setClickable(false);

        // 이미지 표시
        if (displayImagePath != null) {
            // 로컬 파일인지 확인 (S3 URL일 경우 Glide를 써야 하지만,
            // 여기서는 FilterActivity가 로컬 경로를 넘겨준다고 가정)
            File file = new File(displayImagePath);
            if (file.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(displayImagePath);
                if (bitmap != null) {
                    photo.setImageBitmap(bitmap);

                    imageToSave = bitmap;

                    checkAndSaveImage();
                }
            } else if (displayImagePath.startsWith("http")) {
                // 만약 S3 URL이라면 Glide 사용 (build.gradle에 Glide가 있어야 함)
                //com.bumptech.glide.Glide.with(this).load(displayImagePath).into(photo);

                /// 갤러리에 Feel'em앨범으로 사진 저장
                com.bumptech.glide.Glide.with(this).asBitmap().load(displayImagePath).into(new CustomTarget<Bitmap>() {

                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        photo.setImageBitmap(resource);
                        imageToSave = resource;

                        checkAndSaveImage();
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });
            }
        }

        // ---------------------------------------------------------------
        // ✅ 버튼 리스너 설정
        // ---------------------------------------------------------------
        backBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            Intent intent = new Intent(SavePhotoActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        ClickUtils.clickDim(toRegisterBtn);
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


        // ★ [핵심 수정] 2. RegisterActivity로 데이터 넘기기
        toRegisterBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            ClickUtils.disableTemporarily(v, 800);

            if (filterData == null) return; // 데이터가 없으면 진행 불가

            Intent intent = new Intent(SavePhotoActivity.this, RegisterActivity.class);

            // 받은 객체를 그대로 다시 넘깁니다. (Toss)
            intent.putExtra("filter_data", filterData);

            // 화면 표시용 경로도 편의상 같이 넘김
            intent.putExtra("display_image_path", displayImagePath);

            startActivity(intent);
        });
    }

    private void checkAndSaveImage() {
        if (imageToSave == null) return;
        if (isSavedToGallery) return;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        PERMISSION_REQUEST_CODE);
                return;
            }
        } else if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
                return;
            }
        }

        /// 사진 저장 메서드 호출
        ImageUtils.saveBitmapToGallery(SavePhotoActivity.this, imageToSave);

        isSavedToGallery = true;

        saveDone = true;
        finishLoading();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkAndSaveImage();
            } else {
                Toast.makeText(this, "갤러리 저장 권한이 거부되어 이미지를 저장할 수 없습니다.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void finishLoading() {
        if (!animationDone || !saveDone) return;

        loadingFinishContainer.setVisibility(View.VISIBLE);
        loadingFinishAnim.setVisibility(View.VISIBLE);
        loadingAnim.pauseAnimation();
        loadingContainer.setVisibility(View.GONE);
        loadingFinishAnim.setScaleX(0.5f);
        loadingFinishAnim.setScaleY(0.5f);
        loadingFinishAnim.animate().scaleX(1.2f).scaleY(1.2f).setDuration(250).start();
        loadingFinishAnim.playAnimation();
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
            boolean allow = getIntent().getBooleanExtra("allowRegister", true);
            if (allow) {
                toRegisterBtn.getBackground().setColorFilter(Color.parseColor("#C2FA7A"), PorterDuff.Mode.SRC_ATOP);
                toRegisterBtn.setTextColor(Color.parseColor("#007AFF"));
                toRegisterBtn.setEnabled(true);
                toRegisterBtn.setClickable(true);
            }
        });

        Toast.makeText(this, "저장 완료!", Toast.LENGTH_SHORT).show();
    }
}