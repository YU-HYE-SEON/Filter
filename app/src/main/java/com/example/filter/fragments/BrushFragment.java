package com.example.filter.fragments;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Color;
import android.graphics.Matrix;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BrushFragment extends Fragment {
    private ImageButton pen, glowPen, mosaic, eraser;
    private ImageButton cancelBtn, checkBtn;
    private View currentToolPanel;
    private LayoutInflater inflater;
    private FrameLayout brushPanel;
    private boolean suppress = false;
    private static final ExecutorService PALETTE_EXEC = Executors.newSingleThreadExecutor();
    private static volatile Bitmap PALETTE_CACHE_FAST;
    private int lastPenColor = 0xFFFFFFFF;
    private int lastPenSizePx = 0;
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

        int prefColor = BrushPrefs.getColor(requireContext(), brushState.color);
        int prefSize = BrushPrefs.getSize(requireContext(), brushState.sizePx > 0 ? brushState.sizePx : dp(4));

        if (savedInstanceState != null) {
            lastPenColor = savedInstanceState.getInt("lastPenColor", prefColor);
            lastPenSizePx = savedInstanceState.getInt("lastPenSizePx", prefSize);
        } else {
            lastPenColor = prefColor;
            lastPenSizePx = prefSize;
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

            showPenPanel();
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

    private void showPenPanel() {
        if (brushPanel == null || isPenPanelOpen) return;
        isPenPanelOpen = true;

        final float backupHue = lastHue;
        final float backupSat = lastSat;
        final boolean backupHasHS = hasLastHS;

        if (brushDraw != null) brushDraw.setDrawingEnabled(false);

        View panel = inflater.inflate(R.layout.v_pen, brushPanel, false);

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
        SeekBar satSeekbar = panel.findViewById(R.id.saturationSeekbar);
        EditText satValue = panel.findViewById(R.id.saturationValue);
        SeekBar alphaSeekbar = panel.findViewById(R.id.alphaSeekbar);
        EditText alphaValue = panel.findViewById(R.id.alphaValue);
        SeekBar sizeSeekbar = panel.findViewById(R.id.sizeSeekbar);

        fillEditorsFromColor(panel, lastPenColor);

        final float[] baseHS = new float[]{0f, 1f, 1f};
        {
            float[] hsvInit = new float[3];
            Color.colorToHSV(0xFF000000 | (lastPenColor & 0x00FFFFFF), hsvInit);
            if (hsvInit[2] == 0f && hasLastHS) {
                baseHS[0] = lastHue;
                baseHS[1] = lastSat;
            } else {
                baseHS[0] = hsvInit[0];
                baseHS[1] = hsvInit[1];
            }
        }
        final int[] sizeDiameterPx = {dp(4)};

        int initDia = (lastPenSizePx > 0) ? lastPenSizePx : dp(4);
        sizeDiameterPx[0] = initDia;

        if (sizeSeekbar != null) {
            int min = dp(4), max = dp(40);
            int initProgress = clamp(Math.round((initDia - min) * 100f / (max - min)), 0, 100);
            sizeSeekbar.setProgress(initProgress);
        }

        int initColor = gatherCurrentARGB(rCode, gCode, bCode, alphaValue);
        updateSizePreview(panel, initColor, initDia);

        attachEmptyFallback(satValue, "0");
        attachEmptyFallback(alphaValue, "0");
        attachEmptyFallback(rCode, "0");
        attachEmptyFallback(gCode, "0");
        attachEmptyFallback(bCode, "0");

        if (colorPalette != null) {
            colorPalette.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    colorPalette.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                    int w = Math.max(1, colorPalette.getWidth());
                    int h = Math.max(1, colorPalette.getHeight());

                    Bitmap hd = makeRectHSVPalette(w, h);
                    colorPalette.setPaletteDrawable(new BitmapDrawable(getResources(), hd));

                    colorPalette.post(() -> {
                        int init = gatherCurrentARGB(rCode, gCode, bCode, alphaValue);
                        pushColorToUI(panel, colorPalette, init, null, true);
                    });
                }
            });

            colorPalette.setColorListener((ColorEnvelopeListener) (envelope, fromUser) -> {
                if (!fromUser || suppress) return;

                suppress = true;

                int picked = envelope.getColor();
                float[] hsvTmp = new float[3];
                Color.colorToHSV(0xFF000000 | (picked & 0x00FFFFFF), hsvTmp);

                lastHue = hsvTmp[0];
                lastSat = hsvTmp[1];
                hasLastHS = true;

                baseHS[0] = hsvTmp[0];
                baseHS[1] = hsvTmp[1];

                int vPct = clamp(parseIntEmptyZeroSafe(satValue), 0, 100);
                float v = vPct / 100f;

                int aPct = clamp(parseIntEmptyZeroSafe(alphaValue), 0, 100);
                int a = clamp(Math.round(aPct * 2.55f), 0, 255);

                int rgbScaled = Color.HSVToColor(new float[]{baseHS[0], baseHS[1], v}) & 0x00FFFFFF;
                int argb = (a << 24) | rgbScaled;

                pushColorToUI(panel, colorPalette, argb, null, true);
                suppress = false;
            });
        }

        if (hexCode != null) {
            hexCode.setFilters(new InputFilter[]{
                    new InputFilter.AllCaps(),
                    hexLengthFilter(6),
                    hexCharsOnly()
            });
        }
        setIfEmpty(hexCode, "FFFFFF");
        setIfEmpty(rCode, "255");
        setIfEmpty(gCode, "255");
        setIfEmpty(bCode, "255");
        setIfEmpty(satValue, "100");
        setIfEmpty(alphaValue, "100");

        if (satSeekbar != null)
            satSeekbar.setProgress(clamp(parseIntEmptyZero(satValue.getText().toString()), 0, 100));
        if (alphaSeekbar != null)
            alphaSeekbar.setProgress(clamp(parseIntEmptyZero(alphaValue.getText().toString()), 0, 100));

        if (hexCode != null) hexCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (suppress) return;
                String hex = s.toString().trim();
                if (hex.length() != 6) return;
                int rgb = parseHexRGB(hex);
                int aPct = parseIntSafe(alphaValue, 100);
                int a = clamp(Math.round(aPct * 2.55f), 0, 255);
                int argb = (a << 24) | (rgb & 0x00FFFFFF);

                float[] hsvTmp = new float[3];
                Color.colorToHSV(0xFF000000 | (rgb & 0x00FFFFFF), hsvTmp);
                float useH = (hsvTmp[2] == 0f && hasLastHS) ? lastHue : hsvTmp[0];
                float useS = (hsvTmp[2] == 0f && hasLastHS) ? lastSat : hsvTmp[1];
                baseHS[0] = useH;
                baseHS[1] = useS;
                if (satValue != null) {
                    setTextIfChangedKeepCursor(satValue, String.valueOf(Math.round(hsvTmp[2] * 100)));
                }

                suppress = true;
                pushColorToUI(panel, colorPalette, argb, hexCode, true);
                suppress = false;
            }
        });

        TextWatcher rgbTw = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (suppress) return;
                int r = clamp(parseIntEmptyZeroSafe(rCode), 0, 255);
                int g = clamp(parseIntEmptyZeroSafe(gCode), 0, 255);
                int b = clamp(parseIntEmptyZeroSafe(bCode), 0, 255);
                int aPct = clamp(parseIntEmptyZeroSafe(alphaValue), 0, 100);
                int a = clamp(Math.round(aPct * 2.55f), 0, 255);

                float[] hsvTmp = new float[3];
                Color.colorToHSV(0xFF000000 | ((r << 16) | (g << 8) | b), hsvTmp);
                float useH = (hsvTmp[2] == 0f && hasLastHS) ? lastHue : hsvTmp[0];
                float useS = (hsvTmp[2] == 0f && hasLastHS) ? lastSat : hsvTmp[1];
                baseHS[0] = useH;
                baseHS[1] = useS;
                if (satValue != null) {
                    setTextIfChangedKeepCursor(satValue, String.valueOf(Math.round(hsvTmp[2] * 100)));
                }

                int argb = (a << 24) | (r << 16) | (g << 8) | b;

                suppress = true;
                pushColorToUI(panel, colorPalette, argb, (EditText) getView().findFocus(), true);
                suppress = false;
            }
        };

        if (rCode != null) rCode.addTextChangedListener(rgbTw);
        if (gCode != null) gCode.addTextChangedListener(rgbTw);
        if (bCode != null) bCode.addTextChangedListener(rgbTw);

        if (satSeekbar != null)
            satSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (!fromUser || suppress) return;

                    updateSatSeekbarAppearance(seekBar, baseHS[0], baseHS[1], progress);
                    if (alphaSeekbar != null) {
                        float vNow = progress / 100f;
                        int aPctNow = clamp(parseIntEmptyZeroSafe(alphaValue), 0, 100);
                        updateAlphaSeekbarAppearance(alphaSeekbar, baseHS[0], baseHS[1], vNow, aPctNow);
                    }

                    if (pendingSatUi != null) ui.removeCallbacks(pendingSatUi);
                    pendingSatUi = () -> { if (satValue != null) satValue.setText(String.valueOf(progress)); };
                    ui.postDelayed(pendingSatUi, SEEKBAR_THROTTLE_MS);
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {
                    if (satValue != null) satValue.setText(String.valueOf(seekBar.getProgress()));
                }
            });

        if (satValue != null) satValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (suppress) return;

                String raw = s == null ? "" : s.toString().trim();
                int rawVal = parseIntEmptyZero(raw);
                if (rawVal > 100) {
                    suppress = true;
                    setTextIfChangedKeepCursor(satValue, "100");
                    suppress = false;
                    rawVal = 100;
                }

                int vPct = clamp(rawVal, 0, 100);
                if (satSeekbar != null) satSeekbar.setProgress(vPct);

                int cur = gatherCurrentARGB(rCode, gCode, bCode, alphaValue);
                int a = Color.alpha(cur);
                float v = vPct / 100f;
                int rgbScaled = Color.HSVToColor(new float[]{baseHS[0], baseHS[1], v}) & 0x00FFFFFF;
                int argb = (a << 24) | rgbScaled;

                suppress = true;
                pushColorToUI(panel, colorPalette, argb, satValue, false);
                suppress = false;

                if (alphaSeekbar != null) {
                    int aPctNow = clamp(parseIntEmptyZeroSafe(alphaValue), 0, 100);
                    updateAlphaSeekbarAppearance(alphaSeekbar, baseHS[0], baseHS[1], v, aPctNow);
                }

                if (sizeSeekbar != null) {
                    int vPctNow = clamp(parseIntEmptyZeroSafe(satValue), 0, 100);
                    float vFor = vPctNow / 100f;
                    int rgbScaled2 = Color.HSVToColor(new float[]{baseHS[0], baseHS[1], vFor}) & 0x00FFFFFF;
                    int a2 = clamp(Math.round(clamp(parseIntEmptyZeroSafe(alphaValue), 0, 100) * 2.55f), 0, 255);
                    updateSizeSeekbarAppearance(sizeSeekbar, (a2 << 24) | rgbScaled2);
                }
            }
        });

        if (alphaSeekbar != null)
            alphaSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (!fromUser || suppress) return;

                    float vNow = clamp(parseIntEmptyZeroSafe(satValue), 0, 100) / 100f;
                    updateAlphaSeekbarAppearance(seekBar, baseHS[0], baseHS[1], vNow, progress);

                    if (pendingAlphaUi != null) ui.removeCallbacks(pendingAlphaUi);
                    pendingAlphaUi = () -> { if (alphaValue != null) alphaValue.setText(String.valueOf(progress)); };
                    ui.postDelayed(pendingAlphaUi, SEEKBAR_THROTTLE_MS);
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {
                    if (alphaValue != null) alphaValue.setText(String.valueOf(seekBar.getProgress()));
                }
            });

        if (alphaValue != null) alphaValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (suppress) return;

                String raw = s == null ? "" : s.toString().trim();
                int rawVal = parseIntEmptyZero(raw);
                if (rawVal > 100) {
                    suppress = true;
                    setTextIfChangedKeepCursor(alphaValue, "100");
                    suppress = false;
                    rawVal = 100;
                }

                int aPct = clamp(rawVal, 0, 100);
                if (alphaSeekbar != null) alphaSeekbar.setProgress(aPct);
                int cur = gatherCurrentARGB(rCode, gCode, bCode, alphaValue);
                int rgb = cur & 0x00FFFFFF;
                int a = clamp(Math.round(aPct * 2.55f), 0, 255);
                int argb = (a << 24) | rgb;
                suppress = true;
                pushColorToUI(panel, colorPalette, argb, alphaValue, false);
                suppress = false;

                if (alphaSeekbar != null) {
                    float vNow = clamp(parseIntEmptyZeroSafe(satValue), 0, 100) / 100f;
                    updateAlphaSeekbarAppearance(alphaSeekbar, baseHS[0], baseHS[1], vNow, aPct);
                }


                float vNow2 = clamp(parseIntEmptyZeroSafe(satValue), 0, 100) / 100f;
                int rgbScaled2 = Color.HSVToColor(new float[]{baseHS[0], baseHS[1], vNow2}) & 0x00FFFFFF;
                int a2 = clamp(Math.round(aPct * 2.55f), 0, 255);
                updateSizeSeekbarAppearance(sizeSeekbar, (a2 << 24) | rgbScaled2);
            }
        });

        if (sizeSeekbar != null) {
            sizeSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    int min = dp(4), max = dp(40);
                    int dia = min + Math.round((max - min) * (progress / 100f));
                    sizeDiameterPx[0] = dia;

                    int curArgb = gatherCurrentARGB(rCode, gCode, bCode, alphaValue);
                    updateSizePreview(panel, curArgb, dia);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
        }

        View penCancel = panel.findViewById(R.id.cancelBtn);
        View penComplete = panel.findViewById(R.id.completeBtn);
        if (penCancel != null) penCancel.setOnClickListener(v -> {
            lastHue = backupHue;
            lastSat = backupSat;
            hasLastHS = backupHasHS;

            hideToolPanel(true);
        });
        if (penComplete != null) penComplete.setOnClickListener(v -> {
            int chosen = gatherCurrentARGB(
                    (EditText) panel.findViewById(R.id.RCode),
                    (EditText) panel.findViewById(R.id.GCode),
                    (EditText) panel.findViewById(R.id.BCode),
                    (EditText) panel.findViewById(R.id.alphaValue)
            );

            lastPenColor = chosen;
            tintPenButton(chosen);

            lastPenSizePx = sizeDiameterPx[0];

            hasLastHS = true;

            brushState.color = lastPenColor;
            brushState.sizePx = lastPenSizePx;

            BrushPrefs.save(requireContext(), lastPenColor, lastPenSizePx);

            if (brushDraw != null) brushDraw.setBrush(lastPenColor, lastPenSizePx);

            hideToolPanel(true);
        });
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

    private void pushColorToUI(View panel, ColorPickerView colorPalette, int argb, EditText sourceEt, boolean updateSelector) {
        if (updateSelector && colorPalette != null) {
            moveSelectorToColor(colorPalette, argb);

            float[] hsv = new float[3];
            Color.colorToHSV(0xFF000000 | (argb & 0x00FFFFFF), hsv);
            float hPreview = (hsv[2] == 0f && hasLastHS) ? lastHue : hsv[0];
            float sPreview = (hsv[2] == 0f && hasLastHS) ? lastSat : hsv[1];

            int preview = Color.HSVToColor(new float[]{hPreview, sPreview, 1f});
            setSelectorColor(colorPalette, preview);
        }

        int fallbackDia = dp(4);
        View size = panel.findViewById(R.id.size);
        int curDia = (size != null && size.getLayoutParams() != null)
                ? size.getLayoutParams().width : fallbackDia;

        updateSizePreview(panel, argb, curDia);

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

    private void updateSizePreview(View panel, int argb, int diameterPx) {
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

    private Bitmap getFastRectPalette(Resources res) {
        Bitmap cached = PALETTE_CACHE_FAST;
        if (cached != null && !cached.isRecycled()) return cached;
        Bitmap bmp = makeRectHSVPalette(128, 64);
        PALETTE_CACHE_FAST = bmp;
        return bmp;
    }

    private void upgradePaletteAsync(ColorPickerView view, int w, int h, Runnable afterSet) {
        PALETTE_EXEC.execute(() -> {
            Bitmap hd = makeRectHSVPalette(w, h);
            new Handler(Looper.getMainLooper()).post(() -> {
                if (view.getWindowToken() != null) {
                    view.setPaletteDrawable(new BitmapDrawable(getResources(), hd));

                    if (afterSet != null) afterSet.run();
                }
            });
        });
    }

    private int colorAtFullSV(int argb) {
        float[] hsv = new float[3];
        Color.colorToHSV(0xFF000000 | (argb & 0x00FFFFFF), hsv);
        hsv[2] = 1f;
        int rgb = Color.HSVToColor(hsv);
        return rgb;
    }

    private void tintPenButton(int argb) {
        if (pen == null) return;
        int opaque = (argb & 0x00FFFFFF) | 0xFF000000;
        ImageViewCompat.setImageTintList(pen, ColorStateList.valueOf(opaque));
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

        SeekBar sSat = panel.findViewById(R.id.saturationSeekbar);
        sSat.setProgressDrawable(satTrack);
        sSat.setThumb(satThumb);

        alphaChecker = buildTiledCheckerboard(dp(999));
        alphaTrack = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.WHITE, Color.WHITE});
        alphaTrack.setCornerRadius(dp(999));
        alphaLayer = new LayerDrawable(new Drawable[]{alphaChecker, alphaTrack});
        alphaThumb = (GradientDrawable) buildRoundThumb(SAT_THUMB_DIAMETER_DP, Color.WHITE, SAT_THUMB_STROKE_DP);

        SeekBar sAlpha = panel.findViewById(R.id.alphaSeekbar);
        sAlpha.setProgressDrawable(alphaLayer);
        sAlpha.setThumb(alphaThumb);

        sizeChecker = buildTiledCheckerboard(dp(999));
        sizeTrack = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.WHITE, Color.WHITE});
        sizeTrack.setCornerRadius(dp(999));
        sizeLayer = new LayerDrawable(new Drawable[]{sizeChecker, sizeTrack});
        sizeThumb = (GradientDrawable) buildRoundThumb(SAT_THUMB_DIAMETER_DP, Color.WHITE, SAT_THUMB_STROKE_DP);

        SeekBar sSize = panel.findViewById(R.id.sizeSeekbar);
        sSize.setProgressDrawable(sizeLayer);
        sSize.setThumb(sizeThumb);
    }

    private void setupSeekbarStaticPadding(View panel) {
        //int pad = dp(SAT_THUMB_DIAMETER_DP / 2);
        for (int id : new int[]{R.id.saturationSeekbar, R.id.alphaSeekbar, R.id.sizeSeekbar}) {
            SeekBar b = panel.findViewById(id);
            b.setPadding(0, 0, 0, 0);
            try {
                b.setThumbOffset(0);
            } catch (Throwable ignore) {}
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("lastPenColor", lastPenColor);
        outState.putInt("lastPenSizePx", lastPenSizePx);
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