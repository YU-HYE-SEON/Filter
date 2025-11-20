package com.example.filter.activities.filter;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;

import com.bumptech.glide.Glide;
import com.example.filter.R;
import com.example.filter.activities.BaseActivity;
import com.example.filter.activities.MainActivity;
import com.example.filter.activities.filterinfo.*;
import com.example.filter.apis.client.AppRetrofitClient;
import com.example.filter.api_datas.dto.FilterDtoCreateRequest;
import com.example.filter.api_datas.dto.FilterResponse;
import com.example.filter.apis.FilterApi;
import com.example.filter.etc.ClickUtils;
import com.example.filter.api_datas.FilterCreationData;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends BaseActivity {

    // 데이터 객체
    private FilterCreationData filterData;
    private String displayImagePath;

    // UI 요소
    private View topArea, photoView, contentContainer;
    private ImageView photo;
    private NestedScrollView scrollView;
    private LinearLayout priceBox;
    private EditText titleEditText, tagEditText, priceEditText;
    private TextView alertTxt1, alertTxt2, alertTxt3, tagTxt, saleTxt;
    private ImageButton free, pay, backBtn;
    private AppCompatButton registerBtn;

    // 상태 변수
    private boolean isFree = true;
    private boolean isSelectPrice = false;
    private boolean isPriceFirstEdited = false;

    // 키보드 및 스크롤 제어
    private enum Anchor {
        NONE, TAG_TXT, SALE_TXT, REGISTER_BTN
    }

    private Anchor pendingAnchor = Anchor.NONE;
    private boolean keyboardVisible = false;
    private boolean wasKeyboardVisible = false;
    private final Rect lastVisibleFrame = new Rect();
    private int keyboardHeight = 0;
    private float downX, downY;
    private boolean maybeTap = false;
    private int touchSlop;
    private boolean forceScroll = false;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_register);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // 1. 뷰 초기화
        topArea = findViewById(R.id.topArea);
        photoView = findViewById(R.id.photoView);
        photo = findViewById(R.id.photo);
        scrollView = findViewById(R.id.scrollView);
        contentContainer = findViewById(R.id.contentContainer);
        titleEditText = findViewById(R.id.titleEditText);
        tagEditText = findViewById(R.id.tagEditText);
        priceEditText = findViewById(R.id.priceEditText);
        priceBox = findViewById(R.id.priceBox);
        alertTxt1 = findViewById(R.id.alertTxt1);
        alertTxt2 = findViewById(R.id.alertTxt2);
        alertTxt3 = findViewById(R.id.alertTxt3);
        tagTxt = findViewById(R.id.tagTxt);
        saleTxt = findViewById(R.id.saleTxt);
        free = findViewById(R.id.free);
        pay = findViewById(R.id.pay);
        registerBtn = findViewById(R.id.registerBtn);
        backBtn = findViewById(R.id.backBtn);

        // 2. 데이터 수신
        filterData = getIntent().getParcelableExtra("filter_data");
        displayImagePath = getIntent().getStringExtra("display_image_path");

        // 3. 이미지 표시
        if (displayImagePath != null) {
            File file = new File(displayImagePath);
            if (file.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(displayImagePath);
                photo.setImageBitmap(bitmap);
            } else {
                // S3 URL인 경우 Glide 사용
                Glide.with(this).load(displayImagePath).into(photo);
            }
        }

        // 4. UI 초기 설정 및 리스너 등록
        setupScrollAndInsets();
        setupInputListeners();
        setupPriceButtons();

        ClickUtils.clickDim(registerBtn);

        // 5. 등록 버튼 리스너
        registerBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400))
                return;
            ClickUtils.disableTemporarily(v, 800);

            // 유효성 검사
            if (!validateInputs())
                return;

            // 서버 전송
            sendFilterToServer();
        });

        touchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
    }

    // ---------------------------------------------------------------
    // ✅ UI 설정 및 리스너 (키보드, 인셋, 스크롤)
    // ---------------------------------------------------------------
    private void setupScrollAndInsets() {
        backBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400))
                return;
            finish();
        });

        // 스크롤뷰 터치 처리 (외부 클릭 시 키보드 내림)
        scrollView.setOnTouchListener((v, ev) -> {
            Rect btnRect = new Rect();
            boolean btnVisible = backBtn.getGlobalVisibleRect(btnRect);
            if (!btnVisible)
                return false;

            float rawX = ev.getRawX();
            float rawY = ev.getRawY();

            boolean hitBack = btnRect.contains((int) rawX, (int) rawY);
            if (!hitBack)
                return false;
            if (isCoveredByScrollContent(backBtn, contentContainer))
                return true;

            int[] btnLoc = new int[2];
            backBtn.getLocationOnScreen(btnLoc);

            MotionEvent forwarded = MotionEvent.obtain(ev);
            forwarded.offsetLocation(-btnLoc[0], -btnLoc[1]);
            boolean handled = backBtn.dispatchTouchEvent(forwarded);
            forwarded.recycle();

            return handled || ev.getAction() == MotionEvent.ACTION_UP;
        });

        // 상단 여백 계산
        final int[] overlayHeights = new int[1];
        Runnable recomputeOverlay = () -> {
            int topH = topArea.getHeight();
            int photoH = photoView.getHeight();
            if (topH <= 0)
                topH = dp(60);
            if (photoH <= 0)
                photoH = dp(300);
            overlayHeights[0] = topH + photoH;
        };
        scrollView.post(recomputeOverlay);

        // WindowInsets 적용
        final View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());

            topArea.setPadding(topArea.getPaddingLeft(), sys.top, topArea.getPaddingRight(),
                    topArea.getPaddingBottom());

            int topPad = overlayHeights[0] + sys.top;
            int bottomPad = Math.max(sys.bottom, ime.bottom);

            scrollView.setPadding(scrollView.getPaddingLeft(), topPad, scrollView.getPaddingRight(), bottomPad);
            return insets;
        });

        // 키보드 감지 및 앵커 이동
        root.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            root.getWindowVisibleDisplayFrame(lastVisibleFrame);
            int screenHeight = root.getRootView().getHeight();
            int visibleHeight = lastVisibleFrame.height();
            int diff = screenHeight - visibleHeight;

            keyboardVisible = diff > dp(100);
            keyboardHeight = keyboardVisible ? diff : 0;

            if (keyboardVisible) {
                if (pendingAnchor != Anchor.NONE) {
                    alignToPendingAnchor();
                }
            } else {
                if (wasKeyboardVisible) {
                    clearAllEditFocus();
                    pendingAnchor = Anchor.NONE;
                }
            }
            wasKeyboardVisible = keyboardVisible;
        });
    }

    // ---------------------------------------------------------------
    // ✅ EditText 리스너 설정 (제목, 태그, 가격)
    // ---------------------------------------------------------------
    private void setupInputListeners() {
        // --- 제목 입력 ---
        titleEditText.setOnClickListener(v -> {
            focusWithAnchor(titleEditText, Anchor.TAG_TXT, false);
            setEdit(true, titleEditText);
        });
        titleEditText.setOnFocusChangeListener((v, hasFocus) -> {
            setEdit(hasFocus, titleEditText);
            if (hasFocus) {
                validateTitle();
                focusWithAnchor(titleEditText, Anchor.TAG_TXT, false);
            }
        });
        titleEditText.setOnEditorActionListener((v, actionId, event) -> {
            hideKeyboardAndClearFocus();
            return true;
        });
        titleEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                validateTitle();
                setEdit(true, titleEditText);
                setRegisterBtn();
            }
        });

        // --- 태그 입력 ---
        tagEditText.setOnClickListener(v -> {
            focusWithAnchor(tagEditText, Anchor.SALE_TXT, false);
            setEdit(true, tagEditText);
        });
        tagEditText.setOnFocusChangeListener((v, hasFocus) -> {
            setEdit(hasFocus, tagEditText);
            if (hasFocus)
                focusWithAnchor(tagEditText, Anchor.SALE_TXT, false);
            if (!hasFocus)
                applyFinalHashTagFix();
        });
        tagEditText.setFilters(new InputFilter[] { singleSpaceFilter, tagCharFilter });
        tagEditText.setOnEditorActionListener((v, actionId, event) -> {
            applyFinalHashTagFix();
            hideKeyboardAndClearFocus();
            return false;
        });
        tagEditText.addTextChangedListener(new TextWatcher() {
            private boolean self = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (self)
                    return;
                // 태그 자동 # 붙이기 로직
                String text = s.toString();
                if (text.endsWith(" ") && text.length() > 1) {
                    String full = text.trim();
                    int lastSpace = full.lastIndexOf(' ');
                    String lastTag = (lastSpace == -1) ? full : full.substring(lastSpace + 1);
                    if (!lastTag.startsWith("#")) {
                        String newTag = "#" + lastTag;
                        String prefix = (lastSpace == -1) ? "" : full.substring(0, lastSpace + 1);
                        String result = prefix + newTag + " ";
                        self = true;
                        tagEditText.setText(result);
                        tagEditText.setSelection(result.length());
                        self = false;
                    }
                }

                String str = s.toString().trim();
                String[] tags = str.isEmpty() ? new String[] {} : str.split(" ");

                if (tags.length > 5) {
                    alertTxt2.setText("태그는 최대 5개까지 입력가능합니다.\n\n");
                    alertTxt2.setTextColor(Color.parseColor("#FF5C8A"));
                } else {
                    alertTxt2.setText("태그는 띄어쓰기로 구분해 주세요.\n각 10자 이하로 5개까지 가능하며,\n한글, 영문, 숫자, 밑줄(_)만 입력 가능해요.");
                    alertTxt2.setTextColor(Color.parseColor("#8090989F"));
                }

                if (tagEditText.hasFocus()) {
                    setEdit(true, tagEditText);
                }
                setRegisterBtn();
            }
        });

        // --- 가격 입력 ---
        priceEditText.setOnClickListener(v -> focusWithAnchor(priceEditText, Anchor.REGISTER_BTN, false));
        priceEditText.setOnFocusChangeListener((v, hasFocus) -> {
            setPriceEdit(hasFocus);
            if (hasFocus)
                focusWithAnchor(priceEditText, Anchor.REGISTER_BTN, false);
        });
        priceEditText.setOnEditorActionListener((v, actionId, event) -> {
            hideKeyboardAndClearFocus();
            return true;
        });
        priceEditText.addTextChangedListener(new TextWatcher() {
            private boolean selfChange = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (selfChange || isFree)
                    return;

                String t = s.toString();
                if (!isFree && t.length() > 1 && t.startsWith("0")) {
                    String newText = t.replaceFirst("^0+(?=\\d)", "");
                    if (newText.isEmpty())
                        newText = "0";
                    selfChange = true;
                    priceEditText.setText(newText);
                    priceEditText.setSelection(newText.length());
                    selfChange = false;
                    isPriceFirstEdited = true;
                    return;
                }

                try {
                    int price = Integer.parseInt(t);
                    if (price < 10 || price > 300 || price % 10 != 0) {
                        alertTxt3.setTextColor(Color.parseColor("#FF5C8A"));
                        alertTxt3.setText("판매 불가한 가격입니다.");
                        alertTxt3.setVisibility(View.VISIBLE);
                    } else {
                        alertTxt3.setVisibility(View.INVISIBLE);
                    }
                } catch (NumberFormatException e) {
                    alertTxt3.setTextColor(Color.parseColor("#FF5C8A"));
                    alertTxt3.setText("판매 불가한 가격입니다.");
                    alertTxt3.setVisibility(View.VISIBLE);
                }

                setPriceEdit(true);
                setRegisterBtn();
            }
        });
    }

    // ---------------------------------------------------------------
    // ✅ 가격 설정 버튼 리스너 (무료/유료)
    // ---------------------------------------------------------------
    private void setupPriceButtons() {
        free.setOnClickListener(v -> {
            isSelectPrice = true;
            free.setBackgroundResource(R.drawable.btn_free_yes);
            pay.setBackgroundResource(R.drawable.btn_pay_no);
            hideKeyboardAndClearFocus();

            isFree = true;
            priceEditText.setText("0");
            priceEditText.setTextColor(Color.parseColor("#8090989F"));
            priceEditText.setEnabled(false);
            alertTxt3.setText("무료 필터의 경우 가격을 측정할 수 없습니다.");
            alertTxt3.setTextColor(Color.parseColor("#FF5C8A"));
            alertTxt3.setVisibility(View.VISIBLE);

            setPriceEdit(false);
            setRegisterBtn();
        });

        pay.setOnClickListener(v -> {
            isSelectPrice = true;
            pay.setBackgroundResource(R.drawable.btn_pay_yes);
            free.setBackgroundResource(R.drawable.btn_free_no);

            isFree = false;
            isPriceFirstEdited = false;
            priceEditText.setTextColor(Color.BLACK);
            priceEditText.setEnabled(true);
            priceEditText.setText("0");
            priceEditText.setSelection(priceEditText.getText().length());

            setPriceEdit(true);
            setRegisterBtn();
        });
    }

    // ---------------------------------------------------------------
    // ✅ 유효성 검사 및 서버 전송
    // ---------------------------------------------------------------
    private boolean validateInputs() {
        String title = titleEditText.getText().toString().trim();
        String tagStr = tagEditText.getText().toString().trim().replace("#", "");
        String[] tags = tagStr.isEmpty() ? new String[] {} : tagStr.split("\\s+");

        if (title.isEmpty() || title.length() > 15) {
            focusWithAnchor(titleEditText, Anchor.TAG_TXT, true);
            return false;
        } else if (tags.length > 5) {
            focusWithAnchor(tagEditText, Anchor.SALE_TXT, true);
            return false;
        } else if (!isSelectPrice) {
            alertTxt3.setText("가격을 선택해주세요.");
            alertTxt3.setTextColor(Color.parseColor("#FF5C8A"));
            alertTxt3.setVisibility(View.VISIBLE);
            anchor(priceEditText, Anchor.REGISTER_BTN, true);
            return false;
        } else if (!isFree) {
            try {
                int price = Integer.parseInt(priceEditText.getText().toString().trim());
                if (price < 10 || price > 300 || price % 10 != 0) {
                    focusWithAnchor(priceEditText, Anchor.REGISTER_BTN, true);
                    return false;
                }
            } catch (NumberFormatException e) {
                focusWithAnchor(priceEditText, Anchor.REGISTER_BTN, true);
                return false;
            }
        }
        return true;
    }

    // ✅ 최종 서버 등록 요청
    private void sendFilterToServer() {
        if (filterData == null) {
            Toast.makeText(this, "데이터 오류 발생", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. 사용자 입력값 반영 (기존 로직 유지)
        filterData.name = titleEditText.getText().toString().trim();
        String tagRaw = tagEditText.getText().toString().trim().replace("#", "");
        if (!tagRaw.isEmpty()) {
            filterData.tags = new ArrayList<>(java.util.Arrays.asList(tagRaw.split("\\s+")));
        } else {
            filterData.tags = new ArrayList<>();
        }

        if (isFree) {
            filterData.price = 0;
        } else {
            try {
                filterData.price = Integer.parseInt(priceEditText.getText().toString().trim());
            } catch (NumberFormatException e) {
                filterData.price = 0;
            }
        }

        // 2. DTO 생성
        FilterDtoCreateRequest request = filterData.toDto();

        Log.d("Register", "보내는 데이터: " + new Gson().toJson(request));

        // 3. API 호출
        FilterApi api = AppRetrofitClient.getInstance(this).create(FilterApi.class);

        // Call<FilterResponse> 타입 사용
        api.uploadFilter(request).enqueue(new Callback<FilterResponse>() {
            @Override
            public void onResponse(Call<FilterResponse> call, Response<FilterResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d("Register", "필터 등록 성공");

                    // ✅ 서버에서 받은 응답 객체
                    FilterResponse filterResponse = response.body();

                    // 다음 화면으로 데이터 전달하며 이동
                    moveToFilterInfo(response.body());

                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Error";
                        Log.e("Register", "등록 실패: " + response.code() + ", " + errorBody);
                        Toast.makeText(RegisterActivity.this, "등록 실패", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<FilterResponse> call, Throwable t) {
                Log.e("Register", "네트워크 오류", t);
                Toast.makeText(RegisterActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ✅ [추가] 필터 상세 화면으로 이동하는 메서드
    private void moveToFilterInfo(FilterResponse response) {
        Intent intent = new Intent(RegisterActivity.this, FilterInfoActivity.class);
        // 뒤로가기 했을 때 다시 등록화면으로 오지 않게 플래그 설정 (선택사항)
        // intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // ★ 서버 응답 객체를 통째로 넘김
        intent.putExtra("filter_response", response);

        startActivity(intent);
        finish(); // 등록 화면 종료
    }

    private void moveToMain() { /// ??? 언제 main으로 가는거지
        Intent mainIntent = new Intent(RegisterActivity.this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(mainIntent);
        finish();
    }

    // ---------------------------------------------------------------
    // ✅ Helper Methods (키보드, UI, 포커스 등)
    // ---------------------------------------------------------------

    private boolean isCoveredByScrollContent(View target, View scrollContent) {
        Rect targetRect = new Rect();
        Rect contentRect = new Rect();
        boolean tOk = target.getGlobalVisibleRect(targetRect);
        boolean cOk = scrollContent.getGlobalVisibleRect(contentRect);
        if (!tOk || !cOk)
            return false;
        return Rect.intersects(targetRect, contentRect);
    }

    private final InputFilter singleSpaceFilter = (source, start, end, dest, dstart, dend) -> {
        if (source == null || start >= end)
            return null;
        String in = source.subSequence(start, end).toString();
        String compact = in.replaceAll("\\s{2,}", " ");
        StringBuilder sb = new StringBuilder(compact);
        if (sb.length() > 0 && sb.charAt(0) == ' ' && dstart > 0 && dest.charAt(dstart - 1) == ' ') {
            sb.deleteCharAt(0);
        }
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == ' ' && dend < dest.length() && dest.charAt(dend) == ' ') {
            sb.deleteCharAt(sb.length() - 1);
        }
        if (sb.length() == 1 && sb.charAt(0) == ' ' && dstart > 0 && dest.charAt(dstart - 1) == ' ') {
            return "";
        }
        String out = sb.toString();
        return out.equals(in) ? null : out;
    };

    private final InputFilter tagCharFilter = (source, start, end, dest, dstart, dend) -> {
        String input = source.subSequence(start, end).toString();
        if (input.equals("#"))
            return "";
        String filtered = input.replaceAll("[^\\u1100-\\u11FF\\u3131-\\u318F\\uAC00-\\uD7A3a-zA-Z0-9_ #]", "");
        return !input.equals(filtered) ? filtered : null;
    };

    private void applyFinalHashTagFix() {
        Editable s = tagEditText.getText();
        if (s == null)
            return;
        String text = s.toString().trim();
        if (text.isEmpty())
            return;

        int lastSpace = text.lastIndexOf(' ');
        String lastTag = (lastSpace == -1) ? text : text.substring(lastSpace + 1);
        if (lastTag.startsWith("#"))
            return;

        String newTag = "#" + lastTag;
        String prefix = (lastSpace == -1) ? "" : text.substring(0, lastSpace + 1);
        String result = prefix + newTag;
        tagEditText.setText(result);
        tagEditText.setSelection(result.length());
    }

    private void anchor(EditText editText, Anchor anchor, boolean forceScroll) {
        if (editText == null)
            return;
        showKeyboard(editText);
        pendingAnchor = anchor;
        this.forceScroll = forceScroll;
        if (keyboardVisible)
            new Handler().postDelayed(this::alignToPendingAnchor, 16);
    }

    private void focusWithAnchor(EditText editText, Anchor anchor, boolean forceScroll) {
        if (editText == null)
            return;
        editText.requestFocus();
        showKeyboard(editText);
        pendingAnchor = anchor;
        this.forceScroll = forceScroll;
        if (keyboardVisible)
            new Handler().postDelayed(this::alignToPendingAnchor, 16);
    }

    private void alignToPendingAnchor() {
        if (pendingAnchor == Anchor.NONE)
            return;
        View target = null;
        switch (pendingAnchor) {
            case TAG_TXT:
                target = tagTxt;
                break;
            case SALE_TXT:
                target = saleTxt;
                break;
            case REGISTER_BTN:
                target = registerBtn;
                break;
        }
        alignKeyboardTopToViewTop(target);
    }

    private void alignKeyboardTopToViewTop(View target) {
        if (target == null)
            return;
        final int SAFE_SLACK_PX = dp(6);
        int[] loc = new int[2];
        target.getLocationInWindow(loc);
        int targetTop = loc[1];
        int keyboardTop = lastVisibleFrame.bottom;
        int dy = targetTop - keyboardTop;
        if (forceScroll) {
            if (Math.abs(dy) > dp(1))
                scrollView.smoothScrollBy(0, dy);
        } else {
            if (dy > SAFE_SLACK_PX)
                scrollView.smoothScrollBy(0, dy);
        }
    }

    private void showKeyboard(View view) {
        view.post(() -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null)
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        });
    }

    private boolean isPointInsideView(MotionEvent ev, View v) {
        if (v == null)
            return false;
        Rect r = new Rect();
        if (!v.getGlobalVisibleRect(r))
            return false;
        return r.contains((int) ev.getRawX(), (int) ev.getRawY());
    }

    private boolean isPointInsideAnyEditText(MotionEvent ev) {
        return isPointInsideView(ev, titleEditText) || isPointInsideView(ev, tagEditText)
                || isPointInsideView(ev, priceEditText);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = ev.getRawX();
                downY = ev.getRawY();
                maybeTap = true;
                break;
            case MotionEvent.ACTION_MOVE:
                if (Math.abs(ev.getRawX() - downX) > touchSlop || Math.abs(ev.getRawY() - downY) > touchSlop) {
                    maybeTap = false;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (maybeTap) {
                    View focused = getCurrentFocus();
                    if (focused instanceof EditText && !isPointInsideAnyEditText(ev)
                            && !isPointInsideView(ev, registerBtn)) {
                        hideKeyboardAndClearFocus();
                    }
                }
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    private void hideKeyboardAndClearFocus() {
        View v = getCurrentFocus();
        if (v == null)
            v = scrollView;
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && v != null)
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        clearAllEditFocus();
        pendingAnchor = Anchor.NONE;
        forceScroll = false;
    }

    private void clearAllEditFocus() {
        if (titleEditText != null)
            titleEditText.clearFocus();
        if (tagEditText != null)
            tagEditText.clearFocus();
        if (priceEditText != null)
            priceEditText.clearFocus();
    }

    private int dp(int value) {
        return (int) (getResources().getDisplayMetrics().density * value);
    }

    private void validateTitle() {
        String t = titleEditText.getText().toString().trim();
        if (t.isEmpty()) {
            alertTxt1.setText("필터 이름을 입력해주세요.");
            alertTxt1.setTextColor(Color.parseColor("#FF5C8A"));
            alertTxt1.setVisibility(View.VISIBLE);
        } else if (t.length() > 15) {
            alertTxt1.setText("작성 가능한 이름은 최대 15자 입니다.");
            alertTxt1.setTextColor(Color.parseColor("#FF5C8A"));
            alertTxt1.setVisibility(View.VISIBLE);
        } else {
            alertTxt1.setVisibility(View.INVISIBLE);
        }
    }

    private void setEdit(boolean hasFocus, EditText et) {
        String text = et.getText().toString().trim();
        boolean valid = true;
        if (et == titleEditText)
            valid = !text.isEmpty() && text.length() <= 15;
        else if (et == tagEditText)
            valid = text.isEmpty() || text.split(" ").length <= 5;

        int bgRes = hasFocus ? (valid ? R.drawable.border_edit_focus : R.drawable.border_edit_alert)
                : (valid ? R.drawable.border_edit : R.drawable.border_edit_alert);
        et.setBackgroundResource(bgRes);
    }

    private void setPriceEdit(boolean hasFocus) {
        if (isFree) {
            priceBox.setBackgroundResource(R.drawable.border_price);
            return;
        }
        String t = priceEditText.getText().toString().trim();
        boolean valid = false;
        try {
            int price = Integer.parseInt(t);
            valid = (price >= 10 && price <= 300 && price % 10 == 0);
        } catch (Exception ignored) {
        }

        int bgRes = hasFocus ? (valid ? R.drawable.border_price_focus : R.drawable.border_price_alert)
                : (valid ? R.drawable.border_price : R.drawable.border_price_alert);
        priceBox.setBackgroundResource(bgRes);
    }

    private void setRegisterBtn() {
        boolean titleValid = !titleEditText.getText().toString().trim().isEmpty()
                && titleEditText.getText().length() <= 15;
        String tagStr = tagEditText.getText().toString().trim();
        boolean tagValid = tagStr.isEmpty() || tagStr.split(" ").length <= 5;
        boolean priceValid = isFree;
        if (!isFree) {
            try {
                int p = Integer.parseInt(priceEditText.getText().toString().trim());
                priceValid = (p >= 10 && p <= 300 && p % 10 == 0);
            } catch (Exception e) {
                priceValid = false;
            }
        }

        if (titleValid && tagValid && priceValid) {
            registerBtn.setBackgroundResource(R.drawable.btn_register_yes);
            registerBtn.setTextColor(Color.WHITE);
        } else {
            registerBtn.setBackgroundResource(R.drawable.btn_register_no);
            registerBtn.setTextColor(Color.parseColor("#90989F"));
        }
    }
}