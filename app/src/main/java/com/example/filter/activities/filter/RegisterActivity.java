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
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;

import androidx.annotation.Nullable;

import com.example.filter.R;
import com.example.filter.activities.BaseActivity;
import com.example.filter.activities.MainActivity;
import com.example.filter.activities.filterinfo.FilterInfoActivity;
import com.example.filter.etc.ClickUtils;
import com.example.filter.apis.service.FilterApi;
import com.example.filter.apis.dto.FilterDtoCreateRequest;
import com.example.filter.etc.FaceStickerData;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RegisterActivity extends BaseActivity {
    private String filterId, originalPath, newImagePath, imagePath, title, tagStr, priceStr,
            brushImagePath, stickerImagePath, brushPath, stickerImageNoFacePath;
    private float cropN_l, cropN_t, cropN_r, cropN_b;
    private int accumRotationDeg;
    private boolean accumFlipH, accumFlipV;
    private View topArea, photoView, contentContainer;
    private ImageView photo;
    private NestedScrollView scrollView;
    private LinearLayout priceBox;
    private EditText titleEditText, tagEditText, priceEditText;
    private TextView alertTxt1, alertTxt2, alertTxt3, tagTxt, saleTxt;
    private ImageButton free, pay, backBtn;
    private AppCompatButton registerBtn;
    private boolean isFree = true;
    private boolean isSelectPrice = false;
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
    private FilterDtoCreateRequest.ColorAdjustments adj;
    private ArrayList<FaceStickerData> faceStickers;

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
            if (ClickUtils.isFastClick(v, 400)) return;
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

        cropN_l = getIntent().getFloatExtra("cropRectN_l", -1f);
        cropN_t = getIntent().getFloatExtra("cropRectN_t", -1f);
        cropN_r = getIntent().getFloatExtra("cropRectN_r", -1f);
        cropN_b = getIntent().getFloatExtra("cropRectN_b", -1f);

        accumRotationDeg = getIntent().getIntExtra("accumRotationDeg", 0);
        accumFlipH = getIntent().getBooleanExtra("accumFlipH", false);
        accumFlipV = getIntent().getBooleanExtra("accumFlipV", false);

        adj = (FilterDtoCreateRequest.ColorAdjustments) getIntent().getSerializableExtra("color_adjustments");
        brushImagePath = getIntent().getStringExtra("brush_image_path");
        stickerImagePath = getIntent().getStringExtra("sticker_image_path");

        /// 얼굴인식스티커 정보 받기 ///
        stickerImageNoFacePath = getIntent().getStringExtra("stickerImageNoFacePath");
        faceStickers = (ArrayList<FaceStickerData>) getIntent().getSerializableExtra("face_stickers");
        /*if (faceStickers != null && !faceStickers.isEmpty()) {
            for (FaceStickerData d : faceStickers) {
                Log.d("StickerFlow", String.format(
                        "[RegisterActivity] 받은 FaceStickerData → relX=%.4f, relY=%.4f, relW=%.4f, relH=%.4f, rot=%.4f, groupId=%d",
                        d.relX, d.relY, d.relW, d.relH, d.rot, d.groupId
                ));
            }
        } else {
            Log.d("StickerFlow", "[RegisterActivity] faceStickers가 비어있음 혹은 null입니다.");
        }*/

        if (imagePath != null) {
            finalBitmap = BitmapFactory.decodeFile(imagePath);
            if (finalBitmap != null) {
                photo.setImageBitmap(finalBitmap);
            }
        }

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

        tagEditText.setOnClickListener(v -> {
            focusWithAnchor(tagEditText, Anchor.SALE_TXT, false);
            setEdit(true, tagEditText);
        });
        tagEditText.setOnFocusChangeListener((v, hasFocus) -> {
            setEdit(hasFocus, tagEditText);
            if (hasFocus) focusWithAnchor(tagEditText, Anchor.SALE_TXT, false);

            if (!hasFocus) {
                applyFinalHashTagFix();
            }
        });
        tagEditText.setFilters(new InputFilter[]{singleSpaceFilter, tagCharFilter});
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
                if (self) return;

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
                String[] tags = str.isEmpty() ? new String[]{} : str.split(" ");

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
        priceEditText.setOnClickListener(v -> {
            focusWithAnchor(priceEditText, Anchor.REGISTER_BTN, false);
        });
        priceEditText.setOnFocusChangeListener((v, hasFocus) -> {
            setPriceEdit(hasFocus);
            if (hasFocus) {
                focusWithAnchor(priceEditText, Anchor.REGISTER_BTN, false);
            }
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
                if (selfChange || isFree) return;

                String t = s.toString();

                if (!isFree && t.length() > 1 && t.startsWith("0")) {
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

        ClickUtils.clickDim(registerBtn);
        /// 중첩 클릭되면 안 됨 ///
        registerBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            ClickUtils.disableTemporarily(v, 800);

            title = titleEditText.getText().toString().trim();
            tagStr = tagEditText.getText().toString().trim();
            tagStr = tagStr.replace("#", "");
            String[] tags = tagStr.isEmpty() ? new String[]{} : tagStr.split("\\s+");
            priceStr = priceEditText.getText().toString().trim();

            if (title.isEmpty() || title.length() > 15) {
                focusWithAnchor(titleEditText, Anchor.TAG_TXT, true);
                return;
            } else if (tags.length > 5) {
                focusWithAnchor(tagEditText, Anchor.SALE_TXT, true);
                return;
            } else if (!isSelectPrice) {
                alertTxt3.setText("가격을 선택해주세요.");
                alertTxt3.setTextColor(Color.parseColor("#FF5C8A"));
                alertTxt3.setVisibility(View.VISIBLE);
                anchor(priceEditText, Anchor.REGISTER_BTN, true);
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

            filterId = UUID.randomUUID().toString();
            String uniqueFileName = "filter_" + filterId + ".png";
            String brushFileName = "brush_" + filterId + ".png";
            String stickerFileName = "sticker_" + filterId + ".png";

            File imageFile = new File(getCacheDir(), uniqueFileName);
            File brushFile = new File(getCacheDir(), brushFileName);
            File stickerFile = new File(getCacheDir(), stickerFileName);

            newImagePath = imageFile.getAbsolutePath();
            brushPath = brushFile.getAbsolutePath();

            try (FileOutputStream out = new FileOutputStream(imageFile)) {
                finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            Bitmap brushBitmap = BitmapFactory.decodeFile(brushImagePath);
            Bitmap stickerBitmap = BitmapFactory.decodeFile(stickerImagePath);
            try {
                if (brushBitmap != null) {
                    try (FileOutputStream out = new FileOutputStream(brushFile)) {
                        brushBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                    }
                }

                if (stickerBitmap != null) {
                    try (FileOutputStream out = new FileOutputStream(stickerFile)) {
                        stickerBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            String idToken = getSharedPreferences("Auth", MODE_PRIVATE).getString("idToken", null);
            if (idToken != null) {
                FilterDtoCreateRequest request = new FilterDtoCreateRequest();
                request.colorAdjustments = adj;
                sendFilterToServer(idToken, title, tagStr, isFree ? "0" : priceStr, imageFile, request);
            } else {
                Log.e("스티커테스트", "idToken이 없습니다. 로그인 필요");
            }

            /// 홈화면에도 필터 정보 전달 ///
            Intent mainIntent = new Intent(RegisterActivity.this, MainActivity.class);
            /// FLAG_ACTIVITY_CLEAR_TOP과 FLAG_ACTIVITY_SINGLE_TOP을 사용하여 MainActivity가 onNewIntent를 받도록 합니다. ///
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            putFilterInfo(mainIntent);
            startActivity(mainIntent);

            /// 필터상세화면에도 필터 정보 전달 ///
            Intent detailIntent = new Intent(RegisterActivity.this, FilterInfoActivity.class);
            putFilterInfo(detailIntent);
            startActivity(detailIntent);
            finish();
        });

        touchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
    }

    private void putFilterInfo(Intent intent) {
        intent.putExtra("filterId", filterId);
        //intent.putExtra("nickname", "@" + "닉네임");
        intent.putExtra("original_image_path", originalPath);
        intent.putExtra("imgUrl", newImagePath);
        intent.putExtra("filterTitle", title);
        intent.putExtra("tags", tagStr);
        intent.putExtra("price", isFree ? "0" : priceStr);

        intent.putExtra("cropRectN_l", cropN_l);
        intent.putExtra("cropRectN_t", cropN_t);
        intent.putExtra("cropRectN_r", cropN_r);
        intent.putExtra("cropRectN_b", cropN_b);

        intent.putExtra("accumRotationDeg", accumRotationDeg);
        intent.putExtra("accumFlipH", accumFlipH);
        intent.putExtra("accumFlipV", accumFlipV);

        intent.putExtra("color_adjustments", adj);
        intent.putExtra("brush_image_path", brushPath);

        ///  얼굴인식스티커 정보 전달 ///
        intent.putExtra("stickerImageNoFacePath", stickerImageNoFacePath);
        intent.putExtra("face_stickers", new ArrayList<>(this.faceStickers));

        List<FilterDtoCreateRequest.FaceSticker> faceStickers = new ArrayList<>();
        for (FaceStickerData d : this.faceStickers) {
            FilterDtoCreateRequest.FaceSticker s = new FilterDtoCreateRequest.FaceSticker();
            s.placementType = "face";
            s.x = d.relX;
            s.y = d.relY;
            s.scale = (d.relW + d.relH) / 2f;
            //s.relW = d.relW;
            //s.relH = d.relH;
            s.rotation = d.rot;
            s.stickerId = d.groupId;
            faceStickers.add(s);

            /*Log.d("StickerFlow", String.format(
                    "[RegisterActivity] 전달 준비 → relX=%.4f, relY=%.4f, relW=%.4f, relH=%.4f, rot=%.4f, groupId=%d",
                    d.relX, d.relY, d.relW, d.relH, d.rot, d.groupId
            ));*/
        }
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

    private final InputFilter tagCharFilter = (source, start, end, dest, dstart, dend) -> {
        String input = source.subSequence(start, end).toString();

        if (input.equals("#")) {
            return "";
        }

        String filtered = input.replaceAll("[^\\u1100-\\u11FF\\u3131-\\u318F\\uAC00-\\uD7A3a-zA-Z0-9_ #]", "");

        if (!input.equals(filtered)) {
            return filtered;
        }

        return null;
    };

    private void applyFinalHashTagFix() {
        Editable s = tagEditText.getText();
        if (s == null) return;

        String text = s.toString().trim();
        if (text.isEmpty()) return;

        int lastSpace = text.lastIndexOf(' ');
        String lastTag = (lastSpace == -1) ? text : text.substring(lastSpace + 1);

        if (lastTag.startsWith("#")) return;

        String newTag = "#" + lastTag;
        String prefix = (lastSpace == -1) ? "" : text.substring(0, lastSpace + 1);
        String result = prefix + newTag;

        tagEditText.setText(result);
        tagEditText.setSelection(result.length());
    }

    private void anchor(EditText editText, Anchor anchor, boolean forceScroll) {
        if (editText == null) return;
        showKeyboard(editText);
        pendingAnchor = anchor;
        this.forceScroll = forceScroll;

        if (keyboardVisible) {
            new Handler().postDelayed(this::alignToPendingAnchor, 16);
        }
    }

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

        boolean valid;
        if (et == titleEditText) {
            valid = !text.isEmpty() && text.length() <= 15;

        } else if (et == tagEditText) {
            String[] tags = text.isEmpty() ? new String[]{} : text.split(" ");
            valid = tags.length <= 5;

        } else {
            valid = true;
        }

        if (hasFocus) {
            if (valid) {
                et.setBackgroundResource(R.drawable.border_edit_focus);
            } else {
                et.setBackgroundResource(R.drawable.border_edit_alert);
            }
        } else {
            if (valid) {
                et.setBackgroundResource(R.drawable.border_edit);
            } else {
                et.setBackgroundResource(R.drawable.border_edit_alert);
            }
        }
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
        } catch (Exception e) {
            valid = false;
        }

        if (hasFocus) {
            if (valid) {
                priceBox.setBackgroundResource(R.drawable.border_price_focus);
            } else {
                priceBox.setBackgroundResource(R.drawable.border_price_alert);
            }
        } else {
            if (valid) {
                priceBox.setBackgroundResource(R.drawable.border_price);
            } else {
                priceBox.setBackgroundResource(R.drawable.border_price_alert);
            }
        }
    }

    private boolean isTitleValid() {
        String t = titleEditText.getText().toString().trim();
        return !t.isEmpty() && t.length() <= 15;
    }

    private boolean isTagValid() {
        String str = tagEditText.getText().toString().trim();
        if (str.isEmpty()) return true;
        String[] tags = str.split(" ");
        return tags.length <= 5;
    }

    private boolean isPriceValid() {
        if (!isSelectPrice) return false;
        if (isFree) return true;

        try {
            int price = Integer.parseInt(priceEditText.getText().toString().trim());
            return price >= 10 && price <= 300 && price % 10 == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void setRegisterBtn() {
        boolean titleValid = isTitleValid();
        boolean tagValid = isTagValid();
        boolean priceValid = isPriceValid();

        if (titleValid && tagValid && priceValid) {
            registerBtn.setBackgroundResource(R.drawable.btn_register_yes);
            registerBtn.setTextColor(Color.WHITE);
        } else {
            registerBtn.setBackgroundResource(R.drawable.btn_register_no);
            registerBtn.setTextColor(Color.parseColor("#90989F"));
        }
    }

    private void sendFilterToServer(String idToken, String title, String tags, String price, File imageFile, FilterDtoCreateRequest request) {
        request.name = title;
        request.price = Integer.parseInt(price);
        request.originalImageUrl = originalPath;
        request.editedImageUrl = imagePath;
        request.tags = List.of(tags.split("\\s+"));
        //request.aspectX = 4;
        //request.aspectY = 5;

        Log.d("스티커테스트", "보내는 데이터: " + new Gson().toJson(request));

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

        String accessToken = getSharedPreferences("Auth", MODE_PRIVATE)
                .getString("accessToken", "");

        Log.d("스티커테스트", "accessToken: " + accessToken);

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