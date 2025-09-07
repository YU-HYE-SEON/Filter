package com.example.filter.fragments;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.filter.R;
import com.example.filter.activities.FilterActivity;
import com.example.filter.etc.BrushOverlayView;
import com.example.filter.etc.BrushPrefs;
import com.example.filter.etc.BrushStateViewModel;
import com.example.filter.etc.ClickUtils;
import com.skydoves.colorpickerview.ColorPickerView;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;

import java.util.Locale;

public class BrushFragment extends Fragment {
    private ImageButton pen, glowPen, mosaic, eraser;
    private ImageButton cancelBtn, checkBtn;
    private View currentToolPanel;
    private LayoutInflater inflater;
    private FrameLayout brushPanel;
    private boolean suppress = false;
    private float curHue = 0f;
    private float curSat = 1f;
    private float curVal = 1f;
    private int curAlphaPct = 100;
    private int lastPenColor = 0xFFFFFFFF;
    private int lastPenSizePx = 0;
    private int lastGlowColor = 0xFFFFFFFF;
    private int lastGlowSizePx = 0;
    private boolean isPenPanelOpen = false;
    private FrameLayout overlayStack;
    private BrushOverlayView brushDraw;
    private BrushStateViewModel brushState;
    private View.OnLayoutChangeListener brushClipListener;
    private float lastHue = 0f;
    private float lastSat = 1f;
    private boolean hasLastHS = false;
    private static final int SAT_THUMB_DIAMETER_DP = 22;
    private static final int SAT_THUMB_STROKE_DP = 2;
    private GradientDrawable satTrack;
    private GradientDrawable satThumb;
    private Drawable alphaChecker;
    private GradientDrawable alphaTrack;
    private LayerDrawable alphaLayer;
    private GradientDrawable alphaThumb;
    private Drawable sizeChecker;
    private GradientDrawable sizeTrack;
    private LayerDrawable sizeLayer;
    private GradientDrawable sizeThumb;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private Runnable pendingSatUi, pendingAlphaUi;
    private static final long SEEKBAR_THROTTLE_MS = 16;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        brushState = new ViewModelProvider(requireActivity()).get(BrushStateViewModel.class);

        int prefPenColor = BrushPrefs.getPenColor(requireContext(), brushState.color);
        int prefPenSize = BrushPrefs.getPenSize(requireContext(), brushState.sizePx > 0 ? brushState.sizePx : dp(4));
        int prefGlowColor = BrushPrefs.getGlowColor(requireContext(), brushState.color);
        int prefGlowSize = BrushPrefs.getGlowSize(requireContext(), brushState.sizePx > 0 ? brushState.sizePx : dp(4));

        if (savedInstanceState != null) {
            lastPenColor = savedInstanceState.getInt("lastPenColor", prefPenColor);
            lastPenSizePx = savedInstanceState.getInt("lastPenSizePx", prefPenSize);
            lastGlowColor = savedInstanceState.getInt("lastGlowColor", prefGlowColor);
            lastGlowSizePx = savedInstanceState.getInt("lastGlowSizePx", prefGlowSize);
        } else {
            lastPenColor = prefPenColor;
            lastPenSizePx = prefPenSize;
            lastGlowColor = prefGlowColor;
            lastGlowSizePx = prefGlowSize;
        }

        brushState.color = lastPenColor;
        brushState.sizePx = lastPenSizePx;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f_brush, container, false);
        this.inflater = inflater;

        pen = view.findViewById(R.id.pen);
        glowPen = view.findViewById(R.id.glowPen);
        mosaic = view.findViewById(R.id.mosaic);
        eraser = view.findViewById(R.id.eraser);
        cancelBtn = view.findViewById(R.id.cancelBtn);
        checkBtn = view.findViewById(R.id.checkBtn);

        brushPanel = requireActivity().findViewById(R.id.brushPanel);
        overlayStack = requireActivity().findViewById(R.id.overlayStack);

        tintPenButton(lastPenColor);
        tintGlowButton(lastGlowColor);

        if (overlayStack != null && brushDraw == null) {
            brushDraw = new BrushOverlayView(requireContext());
            overlayStack.addView(brushDraw,
                    new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT));

            int initialSize = (lastPenSizePx > 0) ? lastPenSizePx : dp(4);
            brushDraw.setBrush(lastPenColor, initialSize);
            brushDraw.setDrawingEnabled(!isPenPanelOpen);

            Activity act0 = getActivity();
            if (act0 instanceof FilterActivity && brushDraw != null) {
                ((FilterActivity) act0).applyBrushClipRect(brushDraw);
            }

            brushClipListener = (v, a, b, c, d, e, f, g, h) -> {
                if (!isAdded()) return;
                Activity act = getActivity();
                if (act instanceof FilterActivity && brushDraw != null) {
                    ((FilterActivity) act).applyBrushClipRect(brushDraw);
                }
            };
            overlayStack.addOnLayoutChangeListener(brushClipListener);
        }

        pen.setOnClickListener(v -> {
            if (isPenPanelOpen) return;
            if (brushDraw != null) {
                brushDraw.setBrush(lastPenColor, lastPenSizePx, BrushOverlayView.BrushMode.NORMAL);
                brushDraw.setDrawingEnabled(true);
                brushDraw.bringToFront();
            }
            showPanel(BrushOverlayView.BrushMode.NORMAL);
        });

        glowPen.setOnClickListener(v -> {
            if (isPenPanelOpen) return;
            if (brushDraw != null) {
                brushDraw.setBrush(lastGlowColor, lastGlowSizePx, BrushOverlayView.BrushMode.GLOW);
                brushDraw.setDrawingEnabled(true);
                brushDraw.bringToFront();
            }
            showPanel(BrushOverlayView.BrushMode.GLOW);
        });

        eraser.setOnClickListener(v -> {
            if (isPenPanelOpen) return;
            if (brushDraw != null) {
                int lastEraserSizePx = BrushPrefs.getEraserSize(requireContext(), dp(20));
                brushDraw.setBrush(Color.TRANSPARENT, lastEraserSizePx, BrushOverlayView.BrushMode.ERASER);
                brushDraw.setDrawingEnabled(true);
                brushDraw.bringToFront();
            }
            showEraserPanel();
        });

        cancelBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(500)) return;

            if (currentToolPanel != null) hideToolPanel(false);
            if (brushDraw != null) {
                brushDraw.clear();
                brushDraw.setDrawingEnabled(false);
            }
            isPenPanelOpen = false;

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.slide_up, 0)
                    .replace(R.id.bottomArea2, new StickersFragment())
                    .commit();
        });

        checkBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(500)) return;

            if (currentToolPanel != null) hideToolPanel(false);
            if (brushDraw != null) {
                brushDraw.setDrawingEnabled(false);
            }
            isPenPanelOpen = false;

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.slide_up, 0)
                    .replace(R.id.bottomArea2, new StickersFragment())
                    .commit();
        });

        return view;
    }

    private void hideToolPanel(boolean enableDrawingAfterClose) {
        if (brushPanel == null) return;
        if (currentToolPanel == null) {
            brushPanel.setVisibility(View.GONE);
            isPenPanelOpen = false;

            if (brushDraw != null) {
                brushDraw.setDrawingEnabled(enableDrawingAfterClose);
                if (enableDrawingAfterClose) brushDraw.bringToFront();
            }

            return;
        }

        View panel = currentToolPanel;
        panel.animate().alpha(0f).translationY(dp(16)).setDuration(120)
                .withEndAction(() -> {
                    brushPanel.removeAllViews();
                    brushPanel.setVisibility(View.GONE);
                    currentToolPanel = null;
                    isPenPanelOpen = false;

                    if (brushDraw != null) brushDraw.setDrawingEnabled(enableDrawingAfterClose);
                }).start();
    }

    private Bitmap makeRectHSVPalette(int width, int height) {
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        float[] hsv = new float[]{0f, 1f, 1f};
        for (int y = 0; y < height; y++) {
            float s = (float) y / (float) (height - 1);
            for (int x = 0; x < width; x++) {
                float h = (float) x / (float) (width - 1) * 360f;
                hsv[0] = h;
                hsv[1] = s;
                hsv[2] = 1f;
                bmp.setPixel(x, y, Color.HSVToColor(hsv));
            }
        }
        return bmp;
    }

    private void setSelectorColor(ColorPickerView colorPalette, int fillColor) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(fillColor);
        d.setSize(dp(22), dp(22));

        int strokeColor = Color.WHITE;
        d.setStroke(dp(2), strokeColor);

        colorPalette.setSelectorDrawable(d);
    }

    private void pushColorToUI(View panel, ColorPickerView colorPalette, int argb, EditText sourceEt, boolean updateSelector, BrushOverlayView.BrushMode mode) {
        if (updateSelector && colorPalette != null) {
            moveSelectorToColor(colorPalette, argb);

            float[] hsv = new float[3];
            Color.colorToHSV(0xFF000000 | (argb & 0x00FFFFFF), hsv);
            float hPreview = (hsv[2] == 0f && hasLastHS) ? lastHue : hsv[0];
            float sPreview = (hsv[2] == 0f && hasLastHS) ? lastSat : hsv[1];

            int preview = Color.HSVToColor(new float[]{hPreview, sPreview, 1f});
            setSelectorColor(colorPalette, preview);
        }

        int curDia = getDiameterFromSeekbar(panel);

        if (mode == BrushOverlayView.BrushMode.GLOW) {
            updateGlowSizePreview(panel, argb, curDia);
        } else {
            updatePenSizePreview(panel, argb, curDia);
        }

        EditText hexCode = panel.findViewById(R.id.HexCode);
        EditText rCode = panel.findViewById(R.id.RCode);
        EditText gCode = panel.findViewById(R.id.GCode);
        EditText bCode = panel.findViewById(R.id.BCode);
        EditText satValue = panel.findViewById(R.id.saturationValue);
        EditText alphaValue = panel.findViewById(R.id.alphaValue);

        int r = Color.red(argb), g = Color.green(argb), b = Color.blue(argb);
        int a = Color.alpha(argb);
        float[] hsv = new float[3];
        Color.colorToHSV(0xFF000000 | (argb & 0x00FFFFFF), hsv);

        if (hsv[2] > 0f) {
            lastHue = hsv[0];
            lastSat = hsv[1];
            hasLastHS = true;
        }

        if (rCode != sourceEt) setTextSkipIfEmptyAndZero(rCode, r);
        if (gCode != sourceEt) setTextSkipIfEmptyAndZero(gCode, g);
        if (bCode != sourceEt) setTextSkipIfEmptyAndZero(bCode, b);
        if (hexCode != sourceEt)
            setTextIfChangedKeepCursor(hexCode, String.format(Locale.US, "%02X%02X%02X", r, g, b));
        if (satValue != sourceEt)
            setTextIfChangedKeepCursor(satValue, String.valueOf(Math.round(hsv[2] * 100)));
        if (alphaValue != sourceEt)
            setTextIfChangedKeepCursor(alphaValue, String.valueOf(Math.round(a / 2.55f)));

        SeekBar satSeekbar = panel.findViewById(R.id.saturationSeekbar);
        SeekBar alphaSeekbar = panel.findViewById(R.id.alphaSeekbar);
        SeekBar sizeSeekbar = panel.findViewById(R.id.sizeSeekbar);
        if (satSeekbar != null) {
            float hForTrack = (hsv[2] == 0f && hasLastHS) ? lastHue : hsv[0];
            float sForTrack = (hsv[2] == 0f && hasLastHS) ? lastSat : hsv[1];

            int vPct = Math.round(hsv[2] * 100f);
            updateSatSeekbarAppearance(satSeekbar, hForTrack, sForTrack, vPct);

            if (alphaSeekbar != null) {
                updateAlphaSeekbarAppearance(alphaSeekbar, hForTrack, sForTrack, hsv[2], Math.round(a / 2.55f));
            }

            if (sizeSeekbar != null) {
                updateSizeSeekbarAppearance(sizeSeekbar, argb);
            }
        }
    }

    private int parseHexRGB(String hexCode) {
        try {
            return (int) Long.parseLong(hexCode, 16);
        } catch (Exception e) {
            return 0xFFFFFF;
        }
    }

    private InputFilter hexLengthFilter(int max) {
        return (source, start, end, dest, dstart, dend) -> {
            int newLen = dest.length() - (dend - dstart) + (end - start);
            if (newLen > max) return "";
            return null;
        };
    }

    private InputFilter hexCharsOnly() {
        return (source, start, end, dest, dstart, dend) -> {
            for (int i = start; i < end; i++) {
                char c = source.charAt(i);
                boolean ok = (c >= '0' && c <= '9') ||
                        (c >= 'a' && c <= 'f') ||
                        (c >= 'A' && c <= 'F');
                if (!ok) return "";
            }
            return null;
        };
    }

    private void setIfEmpty(EditText et, String def) {
        if (et != null && et.getText().length() == 0) et.setText(def);
    }

    private int gatherCurrentARGB(EditText rCode, EditText gCode, EditText bCode, EditText alphaValue) {
        int r = clamp(parseIntEmptyZeroSafe(rCode), 0, 255);
        int g = clamp(parseIntEmptyZeroSafe(gCode), 0, 255);
        int b = clamp(parseIntEmptyZeroSafe(bCode), 0, 255);
        int aPct = clamp(parseIntEmptyZeroSafe(alphaValue), 0, 100);
        int a = clamp(Math.round(aPct * 2.55f), 0, 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private int parseIntSafe(EditText et, int def) {
        return parseInt(et != null ? et.getText().toString() : null, def);
    }

    private int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s == null ? "" : s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private void moveSelectorToColor(ColorPickerView colorPalette, int argb) {
        if (colorPalette == null) return;

        float[] hsv = new float[3];
        Color.colorToHSV(0xFF000000 | (argb & 0x00FFFFFF), hsv);

        if (hsv[2] == 0f && hasLastHS) {
            hsv[0] = lastHue;
            hsv[1] = lastSat;
        } else {
            lastHue = hsv[0];
            lastSat = hsv[1];
            hasLastHS = true;
        }

        if (hsv[0] >= 360f) hsv[0] = 0f;

        int vw = Math.max(2, colorPalette.getWidth());
        int vh = Math.max(2, colorPalette.getHeight());

        int x = (int) Math.floor((hsv[0] / 360f) * (vw - 2)) + 1;
        int y = (int) Math.floor((hsv[1]) * (vh - 2)) + 1;

        x = Math.max(0, Math.min(vw - 1, x));
        y = Math.max(0, Math.min(vh - 1, y));

        try {
            colorPalette.setSelectorPoint(x, y);
        } catch (IllegalArgumentException e) {
            int cx = Math.max(0, Math.min(vw - 1, vw / 2));
            int cy = Math.max(0, Math.min(vh - 1, vh / 2));
            colorPalette.setSelectorPoint(cx, cy);
        }
    }

    private void setTextIfChangedKeepCursor(EditText et, String newText) {
        if (et == null) return;
        String cur = et.getText() == null ? "" : et.getText().toString();
        if (cur.equals(newText)) return;

        int selStart = et.getSelectionStart();
        et.setText(newText);

        int len = newText.length();
        int newSel = Math.min(len, Math.max(0, selStart));
        try {
            et.setSelection(newSel);
        } catch (Exception ignore) {
        }
    }

    private void updatePenSizePreview(View panel, int argb, int diameterPx) {
        View size = panel.findViewById(R.id.size);

        if (size == null) return;

        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(argb);
        d.setSize(diameterPx, diameterPx);

        ViewGroup.LayoutParams lp = size.getLayoutParams();
        if (lp != null) {
            lp.width = diameterPx;
            lp.height = diameterPx;
            size.setLayoutParams(lp);
        }
        size.setBackground(d);
    }

    private void updateGlowSizePreview(View panel, int argb, int diameterPx) {
        ImageView iv = panel.findViewById(R.id.size);
        if (iv == null) return;

        final float glowScale = 1.8f;
        final float blurDp = 12f;
        final float blurPx = dp((int) blurDp);

        float coreR = diameterPx / 2f;
        float glowR = (diameterPx * glowScale) / 2f;
        float half = glowR + blurPx + 2f;

        int bmW = (int) Math.ceil(half * 2f);
        int bmH = bmW;

        Bitmap bmp = Bitmap.createBitmap(bmW, bmH, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        float cx = bmW / 2f, cy = bmH / 2f;

        Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
        glow.setStyle(Paint.Style.FILL);
        int a = Color.alpha(argb);
        int glowA = Math.round(a * 0.5f);
        int glowColor = (glowA << 24) | (argb & 0x00FFFFFF);
        glow.setColor(glowColor);
        glow.setMaskFilter(new BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL));
        c.drawCircle(cx, cy, glowR, glow);

        Paint core = new Paint(Paint.ANTI_ALIAS_FLAG);
        core.setStyle(Paint.Style.FILL);
        core.setColor(argb);
        c.drawCircle(cx, cy, coreR, core);

        ViewGroup.LayoutParams lp = iv.getLayoutParams();
        if (lp != null) {
            lp.width = bmW;
            lp.height = bmH;
            iv.setLayoutParams(lp);
        }
        iv.setScaleType(ImageView.ScaleType.CENTER);
        iv.setImageBitmap(bmp);
        iv.setBackground(null);
    }

    private int getDiameterFromSeekbar(View panel) {
        SeekBar sizeSeekbar = panel.findViewById(R.id.sizeSeekbar);
        if (sizeSeekbar == null) return dp(4);
        int min = dp(4), max = dp(40);
        int progress = sizeSeekbar.getProgress();
        return min + Math.round((max - min) * (progress / 100f));
    }

    private int parseIntEmptyZero(String s) {
        if (s == null) return 0;
        String t = s.trim();
        if (t.length() == 0) return 0;
        try {
            return Integer.parseInt(t);
        } catch (Exception e) {
            return 0;
        }
    }

    private int parseIntEmptyZeroSafe(EditText et) {
        return parseIntEmptyZero(et == null ? null : et.getText().toString());
    }

    private void setTextSkipIfEmptyAndZero(EditText et, int numeric) {
        if (et == null) return;
        String cur = et.getText() == null ? "" : et.getText().toString();
        if (cur.length() == 0 && numeric == 0) return;
        setTextIfChangedKeepCursor(et, String.valueOf(numeric));
    }

    private void applyFallbackIfEmpty(EditText et, String fallback) {
        if (et == null) return;
        CharSequence cs = et.getText();
        String cur = cs == null ? "" : cs.toString().trim();
        if (cur.length() == 0) {
            suppress = true;
            setTextIfChangedKeepCursor(et, fallback);
            suppress = false;
        }
    }

    private void attachEmptyFallback(EditText et, String fallback) {
        if (et == null) return;
        et.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) applyFallbackIfEmpty(et, fallback);
        });
        et.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE
                    || actionId == EditorInfo.IME_ACTION_NEXT
                    || actionId == EditorInfo.IME_ACTION_GO
                    || actionId == EditorInfo.IME_ACTION_SEND) {
                applyFallbackIfEmpty(et, fallback);
            }
            return false;
        });
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private int pxToDp(int px) {
        return Math.round(px / getResources().getDisplayMetrics().density);
    }

    private void tintPenButton(int argb) {
        if (pen == null) return;
        int opaque = (argb & 0x00FFFFFF) | 0xFF000000;
        ImageViewCompat.setImageTintList(pen, ColorStateList.valueOf(opaque));
    }

    private void tintGlowButton(int argb) {
        if (glowPen == null) return;
        int opaque = (argb & 0x00FFFFFF) | 0xFF000000;
        ImageViewCompat.setImageTintList(glowPen, ColorStateList.valueOf(opaque));
    }

    private void fillEditorsFromColor(View panel, int argb) {
        EditText hexCode = panel.findViewById(R.id.HexCode);
        EditText rCode = panel.findViewById(R.id.RCode);
        EditText gCode = panel.findViewById(R.id.GCode);
        EditText bCode = panel.findViewById(R.id.BCode);
        EditText alphaVal = panel.findViewById(R.id.alphaValue);
        EditText satVal = panel.findViewById(R.id.saturationValue);

        int r = Color.red(argb), g = Color.green(argb), b = Color.blue(argb);
        int aPct = Math.round(Color.alpha(argb) / 2.55f);

        float[] hsv = new float[3];
        Color.colorToHSV(0xFF000000 | (argb & 0x00FFFFFF), hsv);
        int vPct = Math.round(hsv[2] * 100f);

        if (hexCode != null) hexCode.setText(String.format(Locale.US, "%02X%02X%02X", r, g, b));
        if (rCode != null) rCode.setText(String.valueOf(r));
        if (gCode != null) gCode.setText(String.valueOf(g));
        if (bCode != null) bCode.setText(String.valueOf(b));
        if (alphaVal != null) alphaVal.setText(String.valueOf(aPct));
        if (satVal != null) satVal.setText(String.valueOf(vPct));
    }

    private void updateSatSeekbarAppearance(SeekBar bar, float hue, float sat, int vPct) {
        int end = Color.HSVToColor(new float[]{hue, sat, 1f});
        satTrack.setColors(new int[]{Color.BLACK, end});

        float v = Math.max(0f, Math.min(1f, vPct / 100f));
        int thumbArgb = Color.HSVToColor(new float[]{hue, sat, v});
        satThumb.setColor(thumbArgb);
        bar.invalidate();
    }

    private void updateAlphaSeekbarAppearance(SeekBar bar, float hue, float sat, float v, int aPct) {
        int opaque = Color.HSVToColor(new float[]{hue, sat, Math.max(0f, Math.min(1f, v))});
        int rgb = opaque & 0x00FFFFFF;

        alphaTrack.setColors(new int[]{rgb, (rgb | 0xFF000000)});

        int a = clamp(Math.round(aPct * 2.55f), 0, 255);
        int thumbArgb = (a << 24) | rgb;
        alphaThumb.setColor(thumbArgb);
        bar.invalidate();
    }

    private void updateSizeSeekbarAppearance(SeekBar bar, int argb) {
        sizeTrack.setColors(new int[]{argb, argb});
        sizeThumb.setColor(argb);
        bar.invalidate();
    }

    private void updateEraserSizePreview(ImageView eraserSizeView, int sizeDp) {
        if (eraserSizeView == null) return;

        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(Color.WHITE);
        d.setSize(dp(sizeDp), dp(sizeDp));

        ViewGroup.LayoutParams lp = eraserSizeView.getLayoutParams();
        if (lp != null) {
            lp.width = dp(sizeDp);
            lp.height = dp(sizeDp);
            eraserSizeView.setLayoutParams(lp);
        }
        eraserSizeView.setBackground(d);
    }

    private Drawable buildRoundThumb(int diameterDp, int fillColor, int strokeDp) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(fillColor);
        d.setStroke(dp(strokeDp), Color.WHITE);
        d.setSize(dp(diameterDp), dp(diameterDp));
        return d;
    }

    private Drawable buildTiledCheckerboard(int cornerPx) {
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.transparent_bg);
        BitmapShader shader = new BitmapShader(bmp, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);

        Matrix m = new Matrix();
        float scale = 0.35f;
        m.setScale(scale, scale);
        shader.setLocalMatrix(m);

        float r = cornerPx;
        float[] radii = new float[]{r, r, r, r, r, r, r, r};
        ShapeDrawable sd = new ShapeDrawable(new RoundRectShape(radii, null, null));
        sd.getPaint().setAntiAlias(true);
        sd.getPaint().setShader(shader);
        return sd;
    }

    private void initSeekbarDrawables(View panel) {
        satTrack = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.BLACK, Color.WHITE});
        satTrack.setCornerRadius(dp(999));
        satThumb = (GradientDrawable) buildRoundThumb(SAT_THUMB_DIAMETER_DP, Color.WHITE, SAT_THUMB_STROKE_DP);
        satTrack.mutate();
        satThumb.mutate();

        SeekBar sSat = panel.findViewById(R.id.saturationSeekbar);
        sSat.setProgressDrawable(satTrack);
        sSat.setThumb(satThumb);

        alphaChecker = buildTiledCheckerboard(dp(999));
        alphaTrack = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.WHITE, Color.WHITE});
        alphaTrack.setCornerRadius(dp(999));
        alphaLayer = new LayerDrawable(new Drawable[]{alphaChecker, alphaTrack});
        alphaThumb = (GradientDrawable) buildRoundThumb(SAT_THUMB_DIAMETER_DP, Color.WHITE, SAT_THUMB_STROKE_DP);
        alphaTrack.mutate();
        alphaThumb.mutate();

        SeekBar sAlpha = panel.findViewById(R.id.alphaSeekbar);
        sAlpha.setProgressDrawable(alphaLayer);
        sAlpha.setThumb(alphaThumb);

        sizeChecker = buildTiledCheckerboard(dp(999));
        sizeTrack = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.WHITE, Color.WHITE});
        sizeTrack.setCornerRadius(dp(999));
        sizeLayer = new LayerDrawable(new Drawable[]{sizeChecker, sizeTrack});
        sizeThumb = (GradientDrawable) buildRoundThumb(SAT_THUMB_DIAMETER_DP, Color.WHITE, SAT_THUMB_STROKE_DP);
        sizeTrack.mutate();
        sizeThumb.mutate();

        SeekBar sSize = panel.findViewById(R.id.sizeSeekbar);
        sSize.setProgressDrawable(sizeLayer);
        sSize.setThumb(sizeThumb);
    }

    private void setupSeekbarStaticPadding(View panel) {
        int padding = dp(SAT_THUMB_DIAMETER_DP / 2);
        for (int id : new int[]{R.id.saturationSeekbar, R.id.alphaSeekbar, R.id.sizeSeekbar}) {
            SeekBar b = panel.findViewById(id);
            b.setPadding(0, 0, padding, 0);
            try {
                b.setThumbOffset(0);
            } catch (Throwable ignore) {
            }
        }
    }

    private int getLastColor(BrushOverlayView.BrushMode mode) {
        return (mode == BrushOverlayView.BrushMode.GLOW) ? lastGlowColor : lastPenColor;
    }

    private int getLastSize(BrushOverlayView.BrushMode mode) {
        return (mode == BrushOverlayView.BrushMode.GLOW) ? lastGlowSizePx : lastPenSizePx;
    }

    private void setLastColor(BrushOverlayView.BrushMode mode, int argb) {
        if (mode == BrushOverlayView.BrushMode.GLOW) lastGlowColor = argb;
        else lastPenColor = argb;
    }

    private void setLastSize(BrushOverlayView.BrushMode mode, int px) {
        if (mode == BrushOverlayView.BrushMode.GLOW) lastGlowSizePx = px;
        else lastPenSizePx = px;
    }

    private void updateSizePreviewByMode(View panel, int argb, int diameterPx, BrushOverlayView.BrushMode mode) {
        if (mode == BrushOverlayView.BrushMode.GLOW) {
            updateGlowSizePreview(panel, argb, diameterPx);
        } else {
            updatePenSizePreview(panel, argb, diameterPx);
        }
    }

    private void showPanel(BrushOverlayView.BrushMode mode) {
        if (brushPanel == null || isPenPanelOpen) return;
        isPenPanelOpen = true;

        final float backupHue = lastHue;
        final float backupSat = lastSat;
        final boolean backupHasHS = hasLastHS;

        if (brushDraw != null) brushDraw.setDrawingEnabled(false);

        int layout = (mode == BrushOverlayView.BrushMode.GLOW) ? R.layout.v_glow : R.layout.v_pen;
        View panel = inflater.inflate(layout, brushPanel, false);

        initSeekbarDrawables(panel);
        setupSeekbarStaticPadding(panel);

        brushPanel.setVisibility(View.VISIBLE);
        brushPanel.removeAllViews();
        brushPanel.addView(panel);
        currentToolPanel = panel;

        ColorPickerView colorPalette = panel.findViewById(R.id.colorPalette);
        EditText hexCode = panel.findViewById(R.id.HexCode);
        EditText rCode = panel.findViewById(R.id.RCode);
        EditText gCode = panel.findViewById(R.id.GCode);
        EditText bCode = panel.findViewById(R.id.BCode);
        SeekBar satSeek = panel.findViewById(R.id.saturationSeekbar);
        EditText satVal = panel.findViewById(R.id.saturationValue);
        SeekBar aSeek = panel.findViewById(R.id.alphaSeekbar);
        EditText aVal = panel.findViewById(R.id.alphaValue);
        SeekBar sizeSeek = panel.findViewById(R.id.sizeSeekbar);

        int lastColor = getLastColor(mode);
        int lastSize = Math.max(getLastSize(mode), dp(4));
        fillEditorsFromColor(panel, lastColor);

        final float[] baseHS = new float[]{0f, 1f, 1f};
        {
            float[] hsv = new float[3];
            Color.colorToHSV(0xFF000000 | (lastColor & 0x00FFFFFF), hsv);
            curHue = (hsv[2] == 0f && hasLastHS) ? lastHue : hsv[0];
            curSat = (hsv[2] == 0f && hasLastHS) ? lastSat : hsv[1];
            curVal = hsv[2];
            curAlphaPct = Math.round(Color.alpha(lastColor) / 2.55f);
        }

        if (sizeSeek != null) {
            int min = dp(4), max = dp(40);
            int initProgress = clamp(Math.round((lastSize - min) * 100f / (max - min)), 0, 100);
            sizeSeek.setProgress(initProgress);
        }

        int initColor = gatherCurrentARGB(rCode, gCode, bCode, aVal);
        updateSizePreviewByMode(panel, initColor, lastSize, mode);

        attachEmptyFallback(satVal, "0");
        attachEmptyFallback(aVal, "0");
        attachEmptyFallback(rCode, "0");
        attachEmptyFallback(gCode, "0");
        attachEmptyFallback(bCode, "0");

        if (colorPalette != null) {
            colorPalette.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    colorPalette.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    Bitmap hd = makeRectHSVPalette(Math.max(1, colorPalette.getWidth()), Math.max(1, colorPalette.getHeight()));
                    colorPalette.setPaletteDrawable(new BitmapDrawable(getResources(), hd));
                    colorPalette.post(() -> pushColorToUI(panel, colorPalette, initColor, null, true, mode));
                }
            });

            colorPalette.setColorListener((ColorEnvelopeListener) (env, fromUser) -> {
                if (!fromUser || suppress) return;
                int picked = env.getColor();
                float[] hsv = new float[3];
                Color.colorToHSV(0xFF000000 | (picked & 0x00FFFFFF), hsv);
                curHue = hsv[0];
                curSat = hsv[1];
                lastHue = curHue;
                lastSat = curSat;
                hasLastHS = true;

                int argb = argbFromCur();
                pushColorToUI(panel, colorPalette, argb, null, true, mode);
                syncEditorsFromCur(hexCode, rCode, gCode, bCode, satVal, aVal);
            });
        }

        if (hexCode != null) {
            hexCode.setFilters(new InputFilter[]{new InputFilter.AllCaps(), hexLengthFilter(6), hexCharsOnly()});
            if (hexCode.length() == 0) hexCode.setText("FFFFFF");
        }
        setIfEmpty(rCode, "255");
        setIfEmpty(gCode, "255");
        setIfEmpty(bCode, "255");
        setIfEmpty(satVal, "100");
        setIfEmpty(aVal, "100");
        if (satSeek != null)
            satSeek.setProgress(clamp(parseIntEmptyZero(satVal.getText().toString()), 0, 100));
        if (aSeek != null)
            aSeek.setProgress(clamp(parseIntEmptyZero(aVal.getText().toString()), 0, 100));

        TextWatcher hexWatcher = new SimpleTextWatcher(() -> {
            if (suppress) return;
            String hex = hexCode.getText().toString().trim();
            if (hex.length() != 6) return;
            int rgb = parseHexRGB(hex);
            int aPct = parseIntSafe(aVal, 100);
            int a = clamp(Math.round(aPct * 2.55f), 0, 255);
            int argb = (a << 24) | (rgb & 0x00FFFFFF);

            float[] hsv = new float[3];
            Color.colorToHSV(0xFF000000 | (rgb & 0x00FFFFFF), hsv);
            baseHS[0] = (hsv[2] == 0f && hasLastHS) ? lastHue : hsv[0];
            baseHS[1] = (hsv[2] == 0f && hasLastHS) ? lastSat : hsv[1];
            if (satVal != null)
                setTextIfChangedKeepCursor(satVal, String.valueOf(Math.round(hsv[2] * 100)));

            suppress = true;
            pushColorToUI(panel, colorPalette, argb, hexCode, true, mode);
            suppress = false;
        });

        TextWatcher rgbWatcher = new SimpleTextWatcher(() -> {
            if (suppress) return;
            int r = clamp(parseIntEmptyZeroSafe(rCode), 0, 255);
            int g = clamp(parseIntEmptyZeroSafe(gCode), 0, 255);
            int b = clamp(parseIntEmptyZeroSafe(bCode), 0, 255);
            int aPct = clamp(parseIntEmptyZeroSafe(aVal), 0, 100);
            int a = clamp(Math.round(aPct * 2.55f), 0, 255);

            float[] hsv = new float[3];
            Color.colorToHSV(0xFF000000 | ((r << 16) | (g << 8) | b), hsv);
            baseHS[0] = (hsv[2] == 0f && hasLastHS) ? lastHue : hsv[0];
            baseHS[1] = (hsv[2] == 0f && hasLastHS) ? lastSat : hsv[1];
            if (satVal != null)
                setTextIfChangedKeepCursor(satVal, String.valueOf(Math.round(hsv[2] * 100)));

            int argb = (a << 24) | (r << 16) | (g << 8) | b;
            suppress = true;
            pushColorToUI(panel, colorPalette, argb, (EditText) getView().findFocus(), true, mode);
            suppress = false;
        });

        if (hexCode != null) hexCode.addTextChangedListener(hexWatcher);
        if (rCode != null) rCode.addTextChangedListener(rgbWatcher);
        if (gCode != null) gCode.addTextChangedListener(rgbWatcher);
        if (bCode != null) bCode.addTextChangedListener(rgbWatcher);

        if (satSeek != null)
            satSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                    if (!fromUser || suppress) return;
                    curVal = p / 100f;
                    refreshAllSeekbars(s, aSeek, sizeSeek);
                    if (pendingSatUi != null) ui.removeCallbacks(pendingSatUi);
                    pendingSatUi = () -> setTextIfChangedKeepCursor(satVal, String.valueOf(p));
                    ;
                    ui.postDelayed(pendingSatUi, SEEKBAR_THROTTLE_MS);
                }

                @Override
                public void onStartTrackingTouch(SeekBar s) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar s) {
                    setTextIfChangedKeepCursor(satVal, String.valueOf(s.getProgress()));
                }
            });

        if (satVal != null) satVal.addTextChangedListener(new SimpleTextWatcher(() -> {
            if (suppress) return;
            int raw = clamp(parseIntEmptyZero(satVal.getText().toString()), 0, 100);
            if (satSeek != null) satSeek.setProgress(raw);

            curVal = raw / 100f;
            refreshAllSeekbars(satSeek, aSeek, sizeSeek);
            suppress = true;
            pushColorToUI(panel, colorPalette, argbFromCur(), satVal, false, mode);
            suppress = false;
        }));

        if (aSeek != null) aSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (!fromUser || suppress) return;
                curAlphaPct = p;
                refreshAllSeekbars(satSeek, s, sizeSeek);
                if (pendingAlphaUi != null) ui.removeCallbacks(pendingAlphaUi);
                pendingAlphaUi = () -> {
                    setTextIfChangedKeepCursor(aVal, String.valueOf(p));
                };
                ui.postDelayed(pendingAlphaUi, SEEKBAR_THROTTLE_MS);
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar s) {
                setTextIfChangedKeepCursor(aVal, String.valueOf(s.getProgress()));
            }
        });

        if (aVal != null) aVal.addTextChangedListener(new SimpleTextWatcher(() -> {
            if (suppress) return;
            int aPct = clamp(parseIntEmptyZeroSafe(aVal), 0, 100);
            if (aSeek != null) aSeek.setProgress(aPct);

            curAlphaPct = aPct;
            refreshAllSeekbars(satSeek, aSeek, sizeSeek);
            suppress = true;
            pushColorToUI(panel, colorPalette, argbFromCur(), aVal, false, mode);
            suppress = false;
        }));

        if (sizeSeek != null)
            sizeSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                    int min = dp(4), max = dp(40);
                    int dia = min + Math.round((max - min) * (p / 100f));
                    int curArgb = gatherCurrentARGB(rCode, gCode, bCode, aVal);
                    updateSizePreviewByMode(panel, curArgb, dia, mode);
                }

                @Override
                public void onStartTrackingTouch(SeekBar s) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar s) {
                }
            });

        View cancelPanel = panel.findViewById(R.id.cancelBtn);
        View completePanel = panel.findViewById(R.id.completeBtn);
        if (cancelPanel != null) cancelPanel.setOnClickListener(v -> {
            lastHue = backupHue;
            lastSat = backupSat;
            hasLastHS = backupHasHS;
            hideToolPanel(true);
        });
        if (completePanel != null) completePanel.setOnClickListener(v -> {
            int chosen = gatherCurrentARGB(rCode, gCode, bCode, aVal);
            int dia = getDiameterFromSeekbar(panel);
            setLastColor(mode, chosen);
            setLastSize(mode, dia);
            if (mode == BrushOverlayView.BrushMode.GLOW) tintGlowButton(chosen);
            else tintPenButton(chosen);

            brushState.color = chosen;
            brushState.sizePx = dia;
            if (mode == BrushOverlayView.BrushMode.GLOW) {
                BrushPrefs.saveGlow(requireContext(), chosen, dia);
                if (brushDraw != null)
                    brushDraw.setBrush(chosen, dia, BrushOverlayView.BrushMode.GLOW);
            } else {
                BrushPrefs.savePen(requireContext(), chosen, dia);
                if (brushDraw != null)
                    brushDraw.setBrush(chosen, dia, BrushOverlayView.BrushMode.NORMAL);
            }
            hideToolPanel(true);
        });
    }

    private void showEraserPanel() {
        if (brushPanel == null || isPenPanelOpen) return;
        isPenPanelOpen = true;

        if (brushDraw != null) brushDraw.setDrawingEnabled(false);

        View panel = inflater.inflate(R.layout.v_eraser, brushPanel, false);
        brushPanel.setVisibility(View.VISIBLE);
        brushPanel.removeAllViews();
        brushPanel.addView(panel);
        currentToolPanel = panel;

        ImageView eraserSizeView = panel.findViewById(R.id.eraserSize);
        SeekBar sizeSeekbar = panel.findViewById(R.id.sizeSeekbar);

        int lastSizePx = BrushPrefs.getEraserSize(requireContext(), 20);
        int lastSizeDp = Math.max(10, Math.min(100, pxToDp(lastSizePx)));
        int progress = Math.round((lastSizeDp - 10) * 100f / 90f);
        sizeSeekbar.setProgress(progress);

        updateEraserSizePreview(eraserSizeView, lastSizeDp);

        sizeSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int p, boolean fromUser) {
                int sizeDp = 10 + Math.round(90f * (p / 100f));
                updateEraserSizePreview(eraserSizeView, sizeDp);
                if (brushDraw != null) {
                    brushDraw.setBrush(Color.TRANSPARENT, dp(sizeDp), BrushOverlayView.BrushMode.ERASER);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        View cancelPanel = panel.findViewById(R.id.cancelBtn);
        View completePanel = panel.findViewById(R.id.completeBtn);

        if (cancelPanel != null) cancelPanel.setOnClickListener(v -> hideToolPanel(true));

        if (completePanel != null) completePanel.setOnClickListener(v -> {
            int sizeDp = 10 + Math.round(90f * (sizeSeekbar.getProgress() / 100f));
            int sizePx = dp(sizeDp);
            BrushPrefs.saveEraser(requireContext(), sizePx);
            if (brushDraw != null) {
                brushDraw.setBrush(Color.TRANSPARENT, sizePx, BrushOverlayView.BrushMode.ERASER);
            }
            hideToolPanel(true);
        });
    }


    private int argbFromCur() {
        int rgb = Color.HSVToColor(new float[]{curHue, curSat, Math.max(0f, Math.min(1f, curVal))}) & 0x00FFFFFF;
        int a = clamp(Math.round(curAlphaPct * 2.55f), 0, 255);
        return (a << 24) | rgb;
    }

    private void refreshAllSeekbars(SeekBar satSeek, SeekBar alphaSeek, SeekBar sizeSeek) {
        int vPct = Math.round(curVal * 100f);
        updateSatSeekbarAppearance(satSeek, curHue, curSat, vPct);
        if (alphaSeek != null)
            updateAlphaSeekbarAppearance(alphaSeek, curHue, curSat, curVal, curAlphaPct);
        if (sizeSeek != null) updateSizeSeekbarAppearance(sizeSeek, argbFromCur());
    }

    private void syncEditorsFromCur(EditText hex, EditText r, EditText g, EditText b, EditText v, EditText a) {
        int argb = argbFromCur();
        int rr = Color.red(argb), gg = Color.green(argb), bb = Color.blue(argb);
        setTextIfChangedKeepCursor(hex, String.format(Locale.US, "%02X%02X%02X", rr, gg, bb));
        setTextIfChangedKeepCursor(r, String.valueOf(rr));
        setTextIfChangedKeepCursor(g, String.valueOf(gg));
        setTextIfChangedKeepCursor(b, String.valueOf(bb));
        setTextIfChangedKeepCursor(v, String.valueOf(Math.round(curVal * 100)));
        setTextIfChangedKeepCursor(a, String.valueOf(curAlphaPct));
    }

    private static class SimpleTextWatcher implements TextWatcher {
        private final Runnable after;

        SimpleTextWatcher(Runnable after) {
            this.after = after;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int a, int b, int c) {
        }

        @Override
        public void onTextChanged(CharSequence s, int a, int b, int c) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (after != null) after.run();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("lastPenColor", lastPenColor);
        outState.putInt("lastPenSizePx", lastPenSizePx);
        outState.putInt("lastGlowColor", lastGlowColor);
        outState.putInt("lastGlowSizePx", lastGlowSizePx);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (overlayStack != null && brushClipListener != null) {
            overlayStack.removeOnLayoutChangeListener(brushClipListener);
            brushClipListener = null;
        }
        if (brushDraw != null) brushDraw.setDrawingEnabled(false);
    }
}