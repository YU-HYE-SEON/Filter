package com.example.filter;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;


public class FilterActivity extends AppCompatActivity {
    private LinearLayout bottomArea;    //하단부

    //하단부1 - 필터 선택
    private HorizontalScrollView scrollView;  //조절, 밝기 등 아이콘 있는 스크롤 뷰
    private ImageView crop; //조절 아이콘
    private ImageView brightness; //밝기 아이콘
    private ImageView exposure; //노출 아이콘
    private ImageView contrast; //대비 아이콘
    private ImageView sharpness; //선명하게 아이콘
    private ImageView saturation; //채도 아이콘

    //하단부2 - 조절 선택
    private LinearLayout cropChoice;    //조절 선택 시 나오는 하단부
    private ImageButton closeButton;    //조절 선택 하단부 닫기 버튼
    private ImageButton checkButton;    //조절 선택 하단부 완료 버튼

    //하단부3 - 조절 외 선택
    private LinearLayout seekbar;   //조절 외 선택 시 나오는 하단부 (seekbar 나오는)
    private CustomSeekbar customSeekBar;    //custom seekbar 자체
    private ImageButton closeButton2;    //seekbar 닫기 버튼
    private ImageButton checkButton2;    //seekbar 완료 버튼
    private TextView filterText;    //조절 외 선택한 기능 이름

    //상단부
    private View topArea;  //상단바
    private ImageButton backButton; //뒤로가기 버튼 → 기존 사진앱 열어서 photoPreview 위치에 사진 이미지 불러오기

    private ImageView photoPreview; //사진을 띄울 위치
    private ActivityResultLauncher<Intent> galleryLauncher; //사진 앱 호출 및 이미지 가져오기

    private Animation slideUp;   //상단부&하단부 올라가는 애니메이션
    private Animation slideDown;  //상단부 내려가는 애니메이션

    private int brightnessValue = 0;    //밝기 필터값
    private int exposureValue = 0;  //노출 필터값
    private int contrastValue = 0;  //대비 필터값
    private int sharpnessValue = 0; //선명하게 필터값
    private int saturationValue = 0;    //채도 필터값
    private int selectFilterId = -1; // 현재 선택한 필터 ID, -1 → 아무것도 선택 안 함

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomArea=findViewById(R.id.bottomArea);

        scrollView = findViewById(R.id.scrollView);
        crop = findViewById(R.id.crop);
        brightness = findViewById(R.id.brightness);
        exposure = findViewById(R.id.exposure);
        contrast = findViewById(R.id.contrast);
        sharpness = findViewById(R.id.sharpness);
        saturation = findViewById(R.id.saturation);

        cropChoice = findViewById(R.id.cropChoice);
        closeButton = findViewById(R.id.closeButton);
        checkButton = findViewById(R.id.checkButton);

        seekbar = findViewById(R.id.seekbar);
        customSeekBar = findViewById(R.id.customSeekbar);
        closeButton2 = findViewById(R.id.closeButton2);
        checkButton2 = findViewById(R.id.checkButton2);
        filterText = findViewById(R.id.filterText);

        topArea = findViewById(R.id.topArea);
        backButton = findViewById(R.id.backButton);

        photoPreview = findViewById(R.id.photoPreview);

        slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down);

        customSeekBar.setProgress(0);   //custom seekbar 기본값 0으로 설정

        //사진 앱에서 이미지 불러오기
        galleryLauncher = registerForActivityResult(
                //사진 앱 호출 및 이미지 가져오기
                new ActivityResultContracts.StartActivityForResult(),
                //선택한 사진의 URI 가져오기
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri photoUri = result.getData().getData();

                        if (photoUri != null) {
                            try {
                                //가져온 URI의 이미지를 비트맵으로 변환
                                InputStream inputStream = getContentResolver().openInputStream(photoUri);
                                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                                inputStream.close();

                                //이미지가 회전되어 보이는 경우가 있어서 추가함
                                //사진 회전 관련 정보 읽기 위함
                                ExifInterface exif = new ExifInterface(getContentResolver().openInputStream(photoUri));
                                //회전 방향 얻기
                                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                                float rotate = 0;
                                //회전 정보가 90, 180, 270인 경우에만 회전 각도 설정
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
                                //0이 아닐 때만 회전 (0이면 사진이 회전된 채로 보이지 않고 잘 보인다는 것을 의미)
                                if (rotate != 0) {
                                    //이미지를 회전시키기 위한 행렬 생성 및 회전
                                    Matrix matrix = new Matrix();
                                    matrix.postRotate(rotate);
                                    //회전 정보가 적용된 사진 이미지를 비트맵에 다시 저장
                                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                                }

                                //photoPreview 위치에 사진 띄우기
                                photoPreview.setImageBitmap(bitmap);

                                //사진 새롭게 불러올 때마다 필터값 초기화
                                brightnessValue = 0;
                                exposureValue = 0;
                                contrastValue = 0;
                                sharpnessValue = 0;
                                saturationValue = 0;
                                selectFilterId = -1;
                                customSeekBar.setProgress(0);
                            } catch (Exception e) {
                                //이미지 불러오기 실패한 경우 메시지 띄우기
                                Toast.makeText(this, "이미지 로드 실패", Toast.LENGTH_SHORT).show();
                                e.printStackTrace();
                            }
                        }
                    }
                }
        );

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int id = v.getId();

                if (id == R.id.crop) {
                    scrollView.setVisibility(View.GONE);
                    seekbar.setVisibility(View.GONE);
                    bottomArea.startAnimation(slideDown);

                    cropChoice.setVisibility(View.VISIBLE);
                    bottomArea.startAnimation(slideUp);

                    topArea.startAnimation(slideUp);
                    topArea.setVisibility(View.GONE);
                }

                if (id == R.id.brightness || id == R.id.exposure || id == R.id.contrast || id == R.id.sharpness || id == R.id.saturation) {
                    scrollView.setVisibility(View.GONE);
                    cropChoice.setVisibility(View.GONE);
                    bottomArea.startAnimation(slideDown);

                    selectFilterId = id;

                    if (v.getId() == R.id.brightness) {
                        filterText.setText("밝기");
                        customSeekBar.setProgress(brightnessValue);
                    } else if (v.getId() == R.id.exposure) {
                        filterText.setText("노출");
                        customSeekBar.setProgress(exposureValue);
                    } else if (v.getId() == R.id.contrast) {
                        filterText.setText("대비");
                        customSeekBar.setProgress(contrastValue);
                    } else if (v.getId() == R.id.sharpness) {
                        filterText.setText("선명하게");
                        customSeekBar.setProgress(sharpnessValue);
                    } else if (v.getId() == R.id.saturation) {
                        filterText.setText("채도");
                        customSeekBar.setProgress(saturationValue);
                    }

                    seekbar.setVisibility(View.VISIBLE);
                    bottomArea.startAnimation(slideUp);

                    topArea.startAnimation(slideUp);
                    topArea.setVisibility(View.GONE);
                }

                if (id == R.id.closeButton) {
                    cropChoice.setVisibility(View.GONE);
                    bottomArea.startAnimation(slideDown);

                    scrollView.setVisibility(View.VISIBLE);
                    bottomArea.startAnimation(slideUp);

                    topArea.setVisibility(View.VISIBLE);
                    topArea.startAnimation(slideDown);
                }

                if (id == R.id.closeButton2) {
                    seekbar.setVisibility(View.GONE);
                    bottomArea.startAnimation(slideDown);

                    scrollView.setVisibility(View.VISIBLE);
                    bottomArea.startAnimation(slideUp);

                    topArea.setVisibility(View.VISIBLE);
                    topArea.startAnimation(slideDown);
                }

                if (id == R.id.checkButton2) {
                    int currentValue = customSeekBar.getProgress();

                    if (selectFilterId == R.id.brightness) brightnessValue = currentValue;
                    else if (selectFilterId == R.id.exposure) exposureValue = currentValue;
                    else if (selectFilterId == R.id.contrast) contrastValue = currentValue;
                    else if (selectFilterId == R.id.sharpness) sharpnessValue = currentValue;
                    else if (selectFilterId == R.id.saturation) saturationValue = currentValue;

                    seekbar.setVisibility(View.GONE);
                    bottomArea.startAnimation(slideDown);

                    scrollView.setVisibility(View.VISIBLE);
                    bottomArea.startAnimation(slideUp);

                    topArea.setVisibility(View.VISIBLE);
                    topArea.startAnimation(slideDown);
                }
            }
        };

        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            galleryLauncher.launch(intent);
        });

        crop.setOnClickListener(listener);
        brightness.setOnClickListener(listener);
        exposure.setOnClickListener(listener);
        contrast.setOnClickListener(listener);
        sharpness.setOnClickListener(listener);
        saturation.setOnClickListener(listener);
        closeButton.setOnClickListener(listener);
        checkButton.setOnClickListener(listener);
        closeButton2.setOnClickListener(listener);
        checkButton2.setOnClickListener(listener);
    }
}