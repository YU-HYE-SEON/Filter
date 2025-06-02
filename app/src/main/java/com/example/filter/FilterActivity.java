package com.example.filter;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MotionEvent;
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
    private ImageView brightnessIcon; //밝기 아이콘
    private ImageView exposureIcon; //노출 아이콘
    private ImageView contrastIcon; //대비 아이콘
    private ImageView sharpnessIcon; //선명하게 아이콘
    private ImageView saturationIcon; //채도 아이콘

    //하단부2 - 조절 선택
    private LinearLayout cropChoice;    //조절 선택 시 나오는 하단부
    private ImageButton cancelButton;    //조절 선택 하단부 취소 버튼
    private ImageButton checkButton;    //조절 선택 하단부 완료 버튼

    //하단부3 - 조절 외 선택
    private LinearLayout seekbar;   //조절 외 선택 시 나오는 하단부 (seekbar 나오는)
    private CustomSeekbar customSeekBar;    //custom seekbar 자체
    private ImageButton cancelButton2;    //seekbar 취소 버튼
    private ImageButton checkButton2;    //seekbar 완료 버튼
    private TextView filterText;    //조절 외 선택한 기능 이름

    //상단부
    private View topArea;  //상단바
    private ImageButton backButton; //뒤로가기 버튼 → 기존 사진앱 열어서 photoPreview 위치에 사진 이미지 불러오기

    //사진 이미지 부분
    //GLSurfaceView.Renderer (어떻게 그릴지에 대한 정보) + GLSurfaceView (그릴 화면 또는 공간 (사진 이미지)) → (사진과 openGL 연동)
    private GLSurfaceView glSurfaceView;    //사진 이미지 (openGL과 연동)
    private GLRenderer renderer;    //GLSurfaceView에 어떻게 그릴지에 대한 정보를 담고있는 객체
    private ActivityResultLauncher<Intent> galleryLauncher; //사진 앱 호출 및 이미지 가져오기

    private Animation slideUp;   //상단부&하단부 올라가는 애니메이션
    private Animation slideDown;  //상단부 내려가는 애니메이션

    private int brightnessValue = 0;    //밝기 필터값
    private int exposureValue = 0;  //노출 필터값
    private int contrastValue = 0;  //대비 필터값
    private int sharpnessValue = 0; //선명하게 필터값
    private int saturationValue = 0;    //채도 필터값
    private int selectFilterId = -1; // 현재 선택한 필터 ID, -1 → 아무것도 선택 안 함
    private int tempSeekbarValue = 0;   //seekbar 조절한 값 임시 저장 변수 (실시간 미리보기용 o, 최종 적용x)

    public enum Type {
        BRIGHTNESS, EXPOSURE, CONTRAST, SHARPNESS, SATURATION
    }

    public static Type type;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);

        bottomArea = findViewById(R.id.bottomArea);

        scrollView = findViewById(R.id.scrollView);
        crop = findViewById(R.id.crop);
        brightnessIcon = findViewById(R.id.brightness);
        exposureIcon = findViewById(R.id.exposure);
        contrastIcon = findViewById(R.id.contrast);
        sharpnessIcon = findViewById(R.id.sharpness);
        saturationIcon = findViewById(R.id.saturation);

        cropChoice = findViewById(R.id.cropChoice);
        cancelButton = findViewById(R.id.cancelButton);
        checkButton = findViewById(R.id.checkButton);

        seekbar = findViewById(R.id.seekbar);
        customSeekBar = findViewById(R.id.customSeekbar);
        cancelButton2 = findViewById(R.id.cancelButton2);
        checkButton2 = findViewById(R.id.checkButton2);
        filterText = findViewById(R.id.filterText);

        topArea = findViewById(R.id.topArea);
        backButton = findViewById(R.id.backButton);

        glSurfaceView = findViewById(R.id.photoPreview);
        glSurfaceView.setEGLContextClientVersion(2);    //openGL 버전 2.0 사용
        renderer = new GLRenderer(this, glSurfaceView); //현재 불러온 사진 이미지 전달
        glSurfaceView.setRenderer(renderer);    //glSurfaceView에 renderer 정보대로 이미지 그리기 (필터값대로 그려짐)
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);   //RENDERMODE_WHEN_DIRTY : 수동 렌더링 모드, requestRender() 호출요청할 때만 실행

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

                                //사진 이미지 화면에 그리기 메서드 호출
                                loadBitmapToRenderer(bitmap);

                                //사진 새롭게 불러올 때마다 필터값 초기화
                                renderer.resetAllFilter();
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
                } else if (id == R.id.brightness || id == R.id.exposure || id == R.id.contrast || id == R.id.sharpness || id == R.id.saturation) {
                    scrollView.setVisibility(View.GONE);
                    cropChoice.setVisibility(View.GONE);
                    bottomArea.startAnimation(slideDown);

                    selectFilterId = id;

                    if (v.getId() == R.id.brightness) {
                        type = Type.BRIGHTNESS;
                        filterText.setText("밝기");
                        customSeekBar.setProgress(brightnessValue);
                        tempSeekbarValue = brightnessValue;
                    } else if (v.getId() == R.id.exposure) {
                        type = Type.EXPOSURE;
                        filterText.setText("노출");
                        customSeekBar.setProgress(exposureValue);
                        tempSeekbarValue = exposureValue;
                    } else if (v.getId() == R.id.contrast) {
                        type = Type.CONTRAST;
                        filterText.setText("대비");
                        customSeekBar.setProgress(contrastValue);
                        tempSeekbarValue = contrastValue;
                    } else if (v.getId() == R.id.sharpness) {
                        type = Type.SHARPNESS;
                        filterText.setText("선명하게");
                        customSeekBar.setProgress(sharpnessValue);
                        tempSeekbarValue = sharpnessValue;
                    } else if (v.getId() == R.id.saturation) {
                        type = Type.SATURATION;
                        filterText.setText("채도");
                        customSeekBar.setProgress(saturationValue);
                        tempSeekbarValue = saturationValue;
                    }

                    customSeekBar.setMinZero(type);

                    seekbar.setVisibility(View.VISIBLE);
                    bottomArea.startAnimation(slideUp);

                    topArea.startAnimation(slideUp);
                    topArea.setVisibility(View.GONE);
                } else if (id == R.id.cancelButton) {
                    cropChoice.setVisibility(View.GONE);
                    bottomArea.startAnimation(slideDown);

                    scrollView.setVisibility(View.VISIBLE);
                    bottomArea.startAnimation(slideUp);

                    topArea.setVisibility(View.VISIBLE);
                    topArea.startAnimation(slideDown);
                } else if (id == R.id.cancelButton2) {
                    if (type != null) renderer.cancelValue(type);

                    if (type == Type.BRIGHTNESS) customSeekBar.setProgress(brightnessValue);
                    else if (type == Type.EXPOSURE) customSeekBar.setProgress(exposureValue);
                    else if (type == Type.CONTRAST) customSeekBar.setProgress(contrastValue);
                    else if (type == Type.SHARPNESS) customSeekBar.setProgress(sharpnessValue);
                    else if (type == Type.SATURATION) customSeekBar.setProgress(saturationValue);

                    seekbar.setVisibility(View.GONE);
                    bottomArea.startAnimation(slideDown);

                    scrollView.setVisibility(View.VISIBLE);
                    bottomArea.startAnimation(slideUp);

                    topArea.setVisibility(View.VISIBLE);
                    topArea.startAnimation(slideDown);

                    selectFilterId = -1;
                    type = null;
                } else if (id == R.id.checkButton2) {
                    int currentValue = customSeekBar.getProgress();

                    if (type != null) renderer.applyValue(type, currentValue);

                    if (type == Type.BRIGHTNESS) brightnessValue = currentValue;
                    else if (type == Type.EXPOSURE) exposureValue = currentValue;
                    else if (type == Type.CONTRAST) contrastValue = currentValue;
                    else if (type == Type.SHARPNESS) sharpnessValue = currentValue;
                    else if (type == Type.SATURATION) saturationValue = currentValue;

                    seekbar.setVisibility(View.GONE);
                    bottomArea.startAnimation(slideDown);

                    scrollView.setVisibility(View.VISIBLE);
                    bottomArea.startAnimation(slideUp);

                    topArea.setVisibility(View.VISIBLE);
                    topArea.startAnimation(slideDown);

                    selectFilterId = -1;
                    type = null;
                }
            }
        };

        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            galleryLauncher.launch(intent);
        });

        crop.setOnClickListener(listener);
        brightnessIcon.setOnClickListener(listener);
        exposureIcon.setOnClickListener(listener);
        contrastIcon.setOnClickListener(listener);
        sharpnessIcon.setOnClickListener(listener);
        saturationIcon.setOnClickListener(listener);
        cancelButton.setOnClickListener(listener);
        checkButton.setOnClickListener(listener);
        cancelButton2.setOnClickListener(listener);
        checkButton2.setOnClickListener(listener);

        //customSeekBar의 progress값대로 실시간 미리보기 적용
        customSeekBar.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                int progress = customSeekBar.getProgress();
                tempSeekbarValue = progress;

                if (type != null) renderer.setTempValue(type, tempSeekbarValue);

                glSurfaceView.requestRender();
            }
            return false;
        });
    }


    //사진 이미지 화면에 그리기
    private void loadBitmapToRenderer(Bitmap bitmap) {
        if (renderer != null) {
            renderer.setBitmap(bitmap); //renderer에 현재 띄운 사진 이미지 전달
            glSurfaceView.requestRender();  //그리기 요청 메서드
        }
    }
}