package com.example.filter.activities;

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
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import android.animation.ValueAnimator;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import androidx.annotation.Nullable;

import com.example.filter.R;
import com.example.filter.etc.ClickUtils;
import com.example.filter.etc.FilterApi;
import com.example.filter.etc.FilterDtoCreateRequest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RegisterActivity extends BaseActivity {
    String originalPath, imagePath;
    private View topArea, photoView, contentContainer;
    private ImageView photo;
    private NestedScrollView scrollView;
    private EditText titleEditText, tagEditText, priceEditText;
    private TextView alertTxt1, alertTxt2, alertTxt3, tagTxt, saleTxt;
    private RadioGroup saleRadioGroup;
    private RadioButton freeRadio, paidRadio;
    private ImageButton registerBtn, backBtn;
    private boolean isFree = true;
    private boolean isPriceFirstEdited = false;

    private enum Anchor {NONE, TAG_TXT, SALE_TXT, REGISTER_BTN}

    private Anchor pendingAnchor = Anchor.NONE;
    private boolean keyboardVisible = false;
    private boolean wasKeyboardVisible = false;
    private final Rect lastVisibleFrame = new Rect();
    private int keyboardHeight = 0;
    private float downX, downY;
    private boolean maybeTap = false;
    private int touchSlop;
    private boolean forceScroll = false;
    private Bitmap finalBitmap;
    private List<FilterDtoCreateRequest.Sticker> stickerList;
    private FilterDtoCreateRequest.ColorAdjustments adj;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_register);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        topArea = findViewById(R.id.topArea);
        photoView = findViewById(R.id.photoView);
        photo = findViewById(R.id.photo);
        scrollView = findViewById(R.id.scrollView);
        contentContainer = findViewById(R.id.contentContainer);
        titleEditText = findViewById(R.id.titleEditText);
        tagEditText = findViewById(R.id.tagEditText);
        priceEditText = findViewById(R.id.priceEditText);
        alertTxt1 = findViewById(R.id.alertTxt1);
        alertTxt2 = findViewById(R.id.alertTxt2);
        alertTxt3 = findViewById(R.id.alertTxt3);
        tagTxt = findViewById(R.id.tagTxt);
        saleTxt = findViewById(R.id.saleTxt);
        saleRadioGroup = findViewById(R.id.saleRadioGroup);
        freeRadio = findViewById(R.id.freeRadio);
        paidRadio = findViewById(R.id.paidRadio);
        registerBtn = findViewById(R.id.registerBtn);
        backBtn = findViewById(R.id.backBtn);

        adj = (FilterDtoCreateRequest.ColorAdjustments) getIntent().getSerializableExtra("color_adjustments");
        stickerList = (List<FilterDtoCreateRequest.Sticker>) getIntent().getSerializableExtra("stickers");

        if (adj != null) {
            Log.d("ColorAdjustments",
                    "[등록화면]\n" +
                            "밝기: " + adj.brightness + " 노출: " + adj.exposure +
                            " 대비: " + adj.contrast + " 하이라이트: " + adj.highlight +
                            " 그림자: " + adj.shadow + " 온도: " + adj.temperature +
                            " 색조: " + adj.hue + " 채도: " + adj.saturation +
                            " 선명하게: " + adj.sharpen + " 흐리게: " + adj.blur +
                            " 비네트: " + adj.vignette + " 노이즈: " + adj.noise);
        }

        scrollView.setOnTouchListener((v, ev) -> {
            Rect btnRect = new Rect();
            boolean btnVisible = backBtn.getGlobalVisibleRect(btnRect);
            if (!btnVisible) return false;

            float rawX = ev.getRawX();
            float rawY = ev.getRawY();

            boolean hitBack = btnRect.contains((int) rawX, (int) rawY);

            if (!hitBack) {
                return false;
            }
            if (isCoveredByScrollContent(backBtn, contentContainer)) {
                return true;
            }

            int[] btnLoc = new int[2];
            backBtn.getLocationOnScreen(btnLoc);

            MotionEvent forwarded = MotionEvent.obtain(ev);
            forwarded.offsetLocation(-btnLoc[0], -btnLoc[1]);
            boolean handled = backBtn.dispatchTouchEvent(forwarded);
            forwarded.recycle();

            return handled || ev.getAction() == MotionEvent.ACTION_UP;
        });

        backBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(500)) return;
            finish();
        });

        scrollView.post(() -> {
            int topH = topArea.getHeight();
            int photoH = photoView.getHeight();

            if (topH <= 0) topH = dp(60);
            if (photoH <= 0) photoH = dp(300);

            int offset = topH + photoH;
            scrollView.setPadding(
                    scrollView.getPaddingLeft(),
                    offset,
                    scrollView.getPaddingRight(),
                    scrollView.getPaddingBottom()
            );
        });

        final int[] overlayHeights = new int[1];
        Runnable recomputeOverlay = () -> {
            int topH = topArea.getHeight();
            int photoH = photoView.getHeight();
            if (topH <= 0) topH = dp(60);
            if (photoH <= 0) photoH = dp(300);
            overlayHeights[0] = topH + photoH;
        };
        scrollView.post(recomputeOverlay);

        final View root = findViewById(android.R.id.content);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());

            topArea.setPadding(
                    topArea.getPaddingLeft(),
                    sys.top,
                    topArea.getPaddingRight(),
                    topArea.getPaddingBottom()
            );

            int topPad = overlayHeights[0] + sys.top;
            int bottomPad = Math.max(sys.bottom, ime.bottom);

            scrollView.setPadding(
                    scrollView.getPaddingLeft(),
                    topPad,
                    scrollView.getPaddingRight(),
                    bottomPad
            );
            return insets;
        });

        ViewGroup.MarginLayoutParams scv = (ViewGroup.MarginLayoutParams) scrollView.getLayoutParams();
        final int bottomMargin = scv.bottomMargin;
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            Insets nav = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.navigationBars());

            int bottomInset = Math.max(Math.max(sys.bottom, nav.bottom), ime.bottom);

            scv.bottomMargin = bottomMargin + bottomInset;
            scrollView.setLayoutParams(scv);

            return insets;
        });

        View.OnLayoutChangeListener relayout = (view, l, t, r, b, ol, ot, or, ob) -> {
            int old = overlayHeights[0];
            recomputeOverlay.run();
            if (overlayHeights[0] != old) {
                ViewCompat.requestApplyInsets(root);
            }
        };
        topArea.addOnLayoutChangeListener(relayout);
        photoView.addOnLayoutChangeListener(relayout);

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

        originalPath = getIntent().getStringExtra("original_image_path");
        imagePath = getIntent().getStringExtra("final_image");

        if (imagePath != null) {
            finalBitmap = BitmapFactory.decodeFile(imagePath);
            if (finalBitmap != null) {
                photo.setImageBitmap(finalBitmap);
            }
        }

        titleEditText.setOnClickListener(v -> focusWithAnchor(titleEditText, Anchor.TAG_TXT, false));
        titleEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) focusWithAnchor(titleEditText, Anchor.TAG_TXT, false);
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
                if (s.length() > 15) {
                    alertTxt1.setText("작성 가능한 이름은 최대 15자 입니다.");
                    alertTxt1.setVisibility(View.VISIBLE);
                } else {
                    alertTxt1.setVisibility(View.INVISIBLE);
                }
            }
        });

        tagEditText.setOnClickListener(v -> focusWithAnchor(tagEditText, Anchor.SALE_TXT, false));
        tagEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) focusWithAnchor(tagEditText, Anchor.SALE_TXT, false);
        });
        tagEditText.setFilters(new InputFilter[]{singleSpaceFilter});
        tagEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String str = s.toString().trim();
                String[] tags = str.isEmpty() ? new String[]{} : str.split(" ");
                if (tags.length > 5) {
                    alertTxt2.setVisibility(View.VISIBLE);
                } else {
                    alertTxt2.setVisibility(View.INVISIBLE);
                }
            }
        });

        saleRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            hideKeyboardAndClearFocus();

            if (checkedId == R.id.freeRadio) {
                isFree = true;
                priceEditText.setText("0");
                priceEditText.setTextColor(Color.parseColor("#888888"));
                priceEditText.setEnabled(false);
                alertTxt3.setText("무료 필터의 경우 가격을 측정할 수 없습니다.");
                alertTxt3.setVisibility(View.VISIBLE);
            } else if (checkedId == R.id.paidRadio) {
                isFree = false;
                isPriceFirstEdited = false;
                priceEditText.setTextColor(Color.BLACK);
                priceEditText.setEnabled(true);
                priceEditText.setText("0");
                priceEditText.setSelection(priceEditText.getText().length());
            }
        });

        priceEditText.setOnClickListener(v -> {
            if (!priceEditText.isEnabled()) {
                paidRadio.setChecked(true);
            }
            focusWithAnchor(priceEditText, Anchor.REGISTER_BTN, false);
        });
        priceEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                if (!priceEditText.isEnabled()) {
                    paidRadio.setChecked(true);
                }
                focusWithAnchor(priceEditText, Anchor.REGISTER_BTN, false);
            }
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
                if (selfChange || isFree) return;

                String t = s.toString();

                if (paidRadio.isChecked() && t.length() > 1 && t.startsWith("0")) {
                    String newText = t.replaceFirst("^0+(?=\\d)", "");
                    if (newText.isEmpty()) newText = "0";
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
            if (ClickUtils.isFastClick(500)) return;

            String title = titleEditText.getText().toString().trim();
            String tagStr = tagEditText.getText().toString().trim();
            String[] tags = tagStr.isEmpty() ? new String[]{} : tagStr.split("\\s+");
            String priceStr = priceEditText.getText().toString().trim();

            if (title.isEmpty() || title.length() > 15) {
                alertTxt1.setText(title.isEmpty() ? "필터 이름을 입력해주세요." : "작성 가능한 이름은 최대 15자 입니다.");
                alertTxt1.setVisibility(View.VISIBLE);
                focusWithAnchor(titleEditText, Anchor.TAG_TXT, true);
                return;
            } else if (tags.length > 5) {
                focusWithAnchor(tagEditText, Anchor.SALE_TXT, true);
                return;
            } else if (!isFree) {
                try {
                    int price = Integer.parseInt(priceStr);
                    if (price < 10 || price > 300 || price % 10 != 0) {
                        focusWithAnchor(priceEditText, Anchor.REGISTER_BTN, true);
                        return;
                    }
                } catch (NumberFormatException e) {
                    focusWithAnchor(priceEditText, Anchor.REGISTER_BTN, true);
                    return;
                }
            }

            String uniqueFileName = "filter_" + UUID.randomUUID().toString() + ".png";
            File imageFile = new File(getCacheDir(), uniqueFileName);
            String newImagePath = imageFile.getAbsolutePath();

            try (FileOutputStream out = new FileOutputStream(imageFile)) {
                finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            String idToken = getSharedPreferences("Auth", MODE_PRIVATE).getString("idToken", null);
            if (idToken != null) {
                FilterDtoCreateRequest request = new FilterDtoCreateRequest();
                request.colorAdjustments = adj;
                request.stickers = stickerList;
                sendFilterToServer(idToken, title, tagStr, isFree ? "0" : priceStr, imageFile, request);
            } else {
                Log.e("스티커테스트", "idToken이 없습니다. 로그인 필요");
            }

            Intent mainIntent = new Intent(RegisterActivity.this, MainActivity.class);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            //mainIntent.putExtra("new_filter_nickname", "@" + "닉네임");
            mainIntent.putExtra("new_filter_image", newImagePath);
            mainIntent.putExtra("new_filter_title", title);
            mainIntent.putExtra("new_filter_tags", tagStr);
            mainIntent.putExtra("new_filter_price", isFree ? "0" : priceStr);

            if (stickerList != null) {
                mainIntent.putExtra("new_filter_stickers",(Serializable) stickerList);
            }
            if (adj != null) {
                mainIntent.putExtra("color_adjustments", adj);
            }

            startActivity(mainIntent);

            Intent detailIntent = new Intent(RegisterActivity.this, FilterDetailActivity.class);
            detailIntent.putExtra("filterId", UUID.randomUUID().toString());
            //detailIntent.putExtra("nickname", "@" + "닉네임");
            detailIntent.putExtra("imgUrl", newImagePath);
            detailIntent.putExtra("filterTitle", title);
            detailIntent.putExtra("tags", tagStr);
            //detailIntent.putExtra("price", isFree ? "0" : priceStr);

            if (stickerList != null) {
                detailIntent.putExtra("stickers", (Serializable) stickerList);
            }
            if (adj != null) {
                detailIntent.putExtra("color_adjustments", adj);
            }

            detailIntent.putExtra("original_image_path", originalPath);

            startActivity(detailIntent);

            finish();
        });

        touchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
    }

    private boolean isCoveredByScrollContent(View target, View scrollContent) {
        Rect targetRect = new Rect();
        Rect contentRect = new Rect();

        boolean tOk = target.getGlobalVisibleRect(targetRect);
        boolean cOk = scrollContent.getGlobalVisibleRect(contentRect);

        if (!tOk || !cOk) return false;

        return Rect.intersects(targetRect, contentRect);
    }

    private final InputFilter singleSpaceFilter = (source, start, end, dest, dstart, dend) -> {
        if (source == null || start >= end) return null;

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


    private void focusWithAnchor(EditText editText, Anchor anchor, boolean forceScroll) {
        if (editText == null) return;
        editText.requestFocus();
        showKeyboard(editText);
        pendingAnchor = anchor;
        this.forceScroll = forceScroll;

        if (keyboardVisible) {
            new Handler().postDelayed(this::alignToPendingAnchor, 16);
        }
    }

    private void alignToPendingAnchor() {
        if (pendingAnchor == Anchor.NONE) return;

        View target;
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
            default:
                return;
        }
        alignKeyboardTopToViewTop(target);
    }

    private void alignKeyboardTopToViewTop(View target) {
        if (target == null) return;

        final int SAFE_SLACK_PX = dp(6);

        int[] loc = new int[2];
        target.getLocationInWindow(loc);
        int targetTop = loc[1];

        int keyboardTop = lastVisibleFrame.bottom;
        int dy = targetTop - keyboardTop;

        if (forceScroll) {
            if (Math.abs(dy) > dp(1)) {
                scrollView.smoothScrollBy(0, dy);
            }
        } else {
            if (dy > SAFE_SLACK_PX) {
                scrollView.smoothScrollBy(0, dy);
            }
        }
    }

    private void animateScrollToY(int dy, long durationMs) {
        if (Math.abs(dy) < dp(1)) return;

        final int startY = scrollView.getScrollY();
        final int targetY = Math.max(0, startY + dy);

        ValueAnimator va = ValueAnimator.ofInt(startY, targetY);
        va.setDuration(durationMs);
        va.setInterpolator(new FastOutSlowInInterpolator());
        va.addUpdateListener(a -> scrollView.scrollTo(0, (int) a.getAnimatedValue()));
        va.start();
    }

    private void showKeyboard(View view) {
        view.post(() -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    private boolean isPointInsideView(MotionEvent ev, View v) {
        if (v == null) return false;
        Rect r = new Rect();
        boolean visible = v.getGlobalVisibleRect(r);
        if (!visible) return false;
        final int x = (int) ev.getRawX();
        final int y = (int) ev.getRawY();
        return r.contains(x, y);
    }

    private boolean isPointInsideAnyEditText(MotionEvent ev) {
        return isPointInsideView(ev, titleEditText)
                || isPointInsideView(ev, tagEditText)
                || isPointInsideView(ev, priceEditText);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                downX = ev.getRawX();
                downY = ev.getRawY();
                maybeTap = true;
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                float dx = Math.abs(ev.getRawX() - downX);
                float dy = Math.abs(ev.getRawY() - downY);
                if (dx > touchSlop || dy > touchSlop) {
                    maybeTap = false;
                }
                break;
            }
            case MotionEvent.ACTION_UP: {
                if (maybeTap) {
                    View focused = getCurrentFocus();
                    boolean focusedIsEdit = focused instanceof EditText;
                    boolean tapInsideAnyEdit = isPointInsideAnyEditText(ev);
                    boolean tapRegister = isPointInsideView(ev, registerBtn);

                    if (focusedIsEdit && !tapInsideAnyEdit && !tapRegister) {
                        hideKeyboardAndClearFocus();
                    }
                }
                break;
            }
            default:
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    private void hideKeyboardAndClearFocus() {
        View v = getCurrentFocus();
        if (v == null) v = scrollView;
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && v != null) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
        clearAllEditFocus();
        pendingAnchor = Anchor.NONE;
        forceScroll = false;
    }

    private void clearAllEditFocus() {
        if (titleEditText != null) titleEditText.clearFocus();
        if (tagEditText != null) tagEditText.clearFocus();
        if (priceEditText != null) priceEditText.clearFocus();
    }

    private int dp(int value) {
        return (int) (getResources().getDisplayMetrics().density * value);
    }

    private void sendFilterToServer(String idToken, String title, String tags, String price, File imageFile, FilterDtoCreateRequest request) {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://13.124.105.243/")  // 서버 주소
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        FilterApi api = retrofit.create(FilterApi.class);

        request.name = title;
        request.price = Integer.parseInt(price);
        request.originalImageUrl = originalPath;
        request.editedImageUrl = imagePath;
        request.tags = List.of(tags.split("\\s+"));
        //request.aspectX = 4;
        //request.aspectY = 5;

        request.stickers = stickerList;

        String accessToken = getSharedPreferences("Auth", MODE_PRIVATE)
                .getString("accessToken", "");

        Call<ResponseBody> call = api.uploadFilter("Bearer " + accessToken, request);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d("스티커테스트", "필터 업로드 성공");
                } else {
                    try {
                        Log.e("스티커테스트", "서버 응답 에러: " + response.errorBody().string());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("스티커테스트", "Retrofit 업로드 실패", t);
            }
        });
    }
}