package com.example.filter.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.filter.R;
import com.example.filter.adapters.CustomSpinnerAdapter;

import java.util.Arrays;
import java.util.List;

public class RegisterActivity extends BaseActivity {
    private ImageView photo;
    private EditText titleEditText;
    private Spinner studioSpinner;
    private List<String> studioNames;
    private EditText tagEditText;
    private EditText pointEditText;
    private TextView alertTxt1;
    private TextView alertTxt2;
    private TextView alertTxt3;
    private RadioGroup saleRadioGroup;
    private RadioButton freeRadio;
    private RadioButton paidRadio;
    private boolean isFree = true;
    private boolean isPointFirstEdited = false;
    private ImageButton registerBtn;
    private ScrollView scrollView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        photo = findViewById(R.id.photo);
        titleEditText = findViewById(R.id.titleEditText);
        studioSpinner = findViewById(R.id.studioSpinner);
        tagEditText = findViewById(R.id.tagEditText);
        pointEditText = findViewById(R.id.pointEditText);
        alertTxt1 = findViewById(R.id.alertTxt1);
        alertTxt2 = findViewById(R.id.alertTxt2);
        alertTxt3 = findViewById(R.id.alertTxt3);
        saleRadioGroup = findViewById(R.id.saleRadioGroup);
        freeRadio = findViewById(R.id.freeRadio);
        paidRadio = findViewById(R.id.paidRadio);
        registerBtn = findViewById(R.id.registerBtn);
        scrollView = findViewById(R.id.scrollView);

        String imagePath = getIntent().getStringExtra("final_image");
        if (imagePath != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            if (bitmap != null) {
                photo.setImageBitmap(bitmap);
            }
        }

        titleEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 15) {
                    alertTxt1.setVisibility(View.VISIBLE);
                } else {
                    alertTxt1.setVisibility(View.INVISIBLE);
                }
            }
        });

        List<String> studioList = Arrays.asList("대표 스튜디오 이름1", "대표 스튜디오 이름2", "대표 스튜디오 이름3");

        CustomSpinnerAdapter adapter = new CustomSpinnerAdapter(this, studioList);
        studioSpinner.setAdapter(adapter);

        studioSpinner.post(() -> {
            studioSpinner.setDropDownVerticalOffset(studioSpinner.getHeight());
        });

        tagEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String[] tags = s.toString().trim().split("\\s+");
                if (tags.length > 5) {
                    alertTxt2.setVisibility(View.VISIBLE);
                } else {
                    alertTxt2.setVisibility(View.INVISIBLE);
                }
            }
        });

        saleRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.freeRadio) {
                isFree = true;
                pointEditText.setText("0");
                pointEditText.setTextColor(Color.parseColor("#888888"));
                pointEditText.setEnabled(false);
                alertTxt3.setText("무료 필터의 경우 가격을 측정할 수 없습니다.");
                alertTxt3.setVisibility(View.VISIBLE);
            } else if (checkedId == R.id.paidRadio) {
                isFree = false;
                pointEditText.setTextColor(Color.BLACK);
                pointEditText.setEnabled(true);
                alertTxt3.setVisibility(View.INVISIBLE);
            }
        });

        pointEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isFree) return;

                if (paidRadio.isChecked() && !isPointFirstEdited && !s.toString().equals("0")) {
                    isPointFirstEdited = true;
                    String newText = s.toString().replaceFirst("^0+", "");
                    if (newText.isEmpty()) newText = "0";
                    pointEditText.setText(newText);
                    pointEditText.setSelection(newText.length());
                    return;
                }
                try {
                    int point = Integer.parseInt(s.toString());
                    if (point < 10 || point > 300 || point % 10 != 0) {
                        alertTxt3.setText("판매 불가한 가격입니다.");
                        alertTxt3.setVisibility(View.VISIBLE);
                    } else {
                        alertTxt3.setVisibility(View.INVISIBLE);
                    }
                } catch (NumberFormatException e) {
                    alertTxt3.setText("판매 불가한 가격입니다.");
                    alertTxt3.setVisibility(View.VISIBLE);
                }
            }
        });

        registerBtn.setOnClickListener(v -> {
            String title = titleEditText.getText().toString().trim();
            String tagStr = tagEditText.getText().toString().trim();
            String[] tags = tagStr.isEmpty() ? new String[]{} : tagStr.split("\\s+");
            String pointStr = pointEditText.getText().toString().trim();

            if (title.isEmpty() || title.length() > 15) {
                alertTxt1.setText(title.isEmpty() ? "필터 이름을 입력해주세요." : "작성 가능한 이름은 최대 15자 입니다.");
                alertTxt1.setVisibility(View.VISIBLE);
                titleEditText.requestFocus();
                titleEditText.getParent().requestChildFocus(titleEditText, titleEditText);
                scrollUp(titleEditText);
                return;
            } else if (tags.length > 5) {
                tagEditText.requestFocus();
                tagEditText.getParent().requestChildFocus(tagEditText, tagEditText);
                scrollUp(tagEditText);
                return;
            } else if (!isFree) {
                try {
                    int point = Integer.parseInt(pointStr);
                    if (point < 10 || point > 300 || point % 10 != 0) {
                        pointEditText.requestFocus();
                        pointEditText.getParent().requestChildFocus(pointEditText, pointEditText);
                        return;
                    }
                } catch (NumberFormatException e) {
                    pointEditText.requestFocus();
                    pointEditText.getParent().requestChildFocus(pointEditText, pointEditText);
                    return;
                }
            }

            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
            startActivity(intent);
        });
    }

    private void scrollUp(View view) {
        scrollView.post(() -> {
            int offset = 100;
            int y = view.getTop() - offset;
            if (y < 0) y = 0;
            scrollView.smoothScrollTo(0, y);
        });
    }
}

