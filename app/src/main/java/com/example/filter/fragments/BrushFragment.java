package com.example.filter.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Build;
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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.ViewCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.filter.R;
import com.example.filter.activities.FilterActivity;
import com.example.filter.dialogs.BrushToStickerDialog;
import com.example.filter.etc.BrushOverlayView;
import com.example.filter.etc.BrushPrefs;
import com.example.filter.etc.BrushStateViewModel;
import com.example.filter.etc.ClickUtils;
import com.example.filter.etc.LassoOverlayView;
import com.example.filter.etc.StickerItem;
import com.example.filter.etc.StickerStore;
import com.skydoves.colorpickerview.ColorPickerView;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BrushFragment extends Fragment {
    /// 관련 클래스 ///
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

    private static class Patch {
        Rect rect;
        Bitmap before;
    }

    private static class PendingErase {
        ImageView view;
        Path path = new Path();
        float width;
        ArrayList<Patch> patches = new ArrayList<>();
        boolean hadEffect = false;
    }

    /// UI ///
    private ConstraintLayout topArea;
    private ImageButton pen, glowPen, crayon, eraser;
    private TextView penTxt, glowPenTxt, crayonTxt, eraserTxt;
    private ImageButton cancelBtn, checkBtn;
    private FrameLayout brushPanel;
    private FrameLayout brushOverlay, stickerOverlay;
    private LassoOverlayView lassoOverlay;
    private LinearLayout brushToSticker;
    private ConstraintLayout bottomArea1;
    private ImageButton undoSticker, redoSticker, originalSticker;
    private CheckBox checkBox;

    /// 시스템 ///
    private LayoutInflater inflater;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private Runnable pendingSatUi, pendingAlphaUi;
    private static final long SEEKBAR_THROTTLE_MS = 16;

    /// 패널 ///
    private View currentToolPanel;
    private boolean isPenPanelOpen = false;
    private boolean suppress = false;

    /// 펜 ///
    private int lastPenColor = 0xFFFFFFFF;
    private int lastPenSizePx = 0;

    /// 글로우 펜 ///
    private int lastGlowColor = 0xFFFFFFFF;
    private int lastGlowSizePx = 0;

    /// 색연필 ///
    private int lastCrayonColor = 0xFFFFFFFF;
    private int lastCrayonSizePx = 0;
    private BitmapShader crayonPreviewShader;

    /// 지우개 ///
    private final ArrayList<FilterActivity.EraseOp> sessionEraseOps = new ArrayList<>();

    /// undoSticker, redoSticker, originalSticker 작업 ///
    private final Map<ImageView, PendingErase> activeErases = new HashMap<>();

    /// 색상 코드 변수 ///
    private float curHue = 0f, curSat = 1f, curVal = 1f;
    private boolean hasLastHS = false;

    /// 시크바 공통 변수 ///
    private static final int THUMB_DIAMETER_DP = 22;
    private static final int THUMB_STROKE_DP = 2;

    /// 채도 시크바 ///
    private float lastHue = 0f;
    private float lastSat = 1f;
    private GradientDrawable satTrack;
    private GradientDrawable satThumb;

    /// 투명도 시크바 ///
    private Drawable alphaChecker;
    private LayerDrawable alphaLayer;
    private GradientDrawable alphaTrack;
    private GradientDrawable alphaThumb;
    private int curAlphaPct = 100;

    /// 굵기 시크바 ///
    private Drawable sizeChecker;
    private LayerDrawable sizeLayer;
    private GradientDrawable sizeTrack;
    private GradientDrawable sizeThumb;

    /// 브러쉬 ///
    private BrushOverlayView brushDraw;
    private BrushStateViewModel brushState;
    private BrushOverlayView.BrushMode lastMode = BrushOverlayView.BrushMode.PEN;
    private View.OnLayoutChangeListener brushClipListener;
    private int baselineStrokeCount = 0;

    /// 스티커 ///
    private int baselineChildCount = 0;

    /// life cycle ///
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        brushState = new ViewModelProvider(requireActivity()).get(BrushStateViewModel.class);

        int prefPenColor = BrushPrefs.getPenColor(requireContext(), brushState.color);
        int prefPenSize = BrushPrefs.getPenSize(requireContext(), brushState.sizePx > 0 ? brushState.sizePx : dp(10));
        int prefGlowColor = BrushPrefs.getGlowColor(requireContext(), brushState.color);
        int prefGlowSize = BrushPrefs.getGlowSize(requireContext(), brushState.sizePx > 0 ? brushState.sizePx : dp(10));
        int prefCrayonColor = BrushPrefs.getCrayonColor(requireContext(), brushState.color);
        int prefCrayonSize = BrushPrefs.getCrayonSize(requireContext(), brushState.sizePx > 0 ? brushState.sizePx : dp(10));

        if (savedInstanceState != null) {
            lastPenColor = savedInstanceState.getInt("lastPenColor", prefPenColor);
            lastPenSizePx = savedInstanceState.getInt("lastPenSizePx", prefPenSize);
            lastGlowColor = savedInstanceState.getInt("lastGlowColor", prefGlowColor);
            lastGlowSizePx = savedInstanceState.getInt("lastGlowSizePx", prefGlowSize);
            lastCrayonColor = savedInstanceState.getInt("lastCrayonColor", prefCrayonColor);
            lastCrayonSizePx = savedInstanceState.getInt("lastCrayonSizePx", prefCrayonSize);
        } else {
            lastPenColor = prefPenColor;
            lastPenSizePx = prefPenSize;
            lastGlowColor = prefGlowColor;
            lastGlowSizePx = prefGlowSize;
            lastCrayonColor = prefCrayonColor;
            lastCrayonSizePx = prefCrayonSize;
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
        crayon = view.findViewById(R.id.crayon);
        eraser = view.findViewById(R.id.eraser);
        penTxt = view.findViewById(R.id.penTxt);
        glowPenTxt = view.findViewById(R.id.glowPenTxt);
        crayonTxt = view.findViewById(R.id.crayonTxt);
        eraserTxt = view.findViewById(R.id.eraserTxt);
        cancelBtn = view.findViewById(R.id.cancelBtn);
        checkBtn = view.findViewById(R.id.checkBtn);

        brushPanel = requireActivity().findViewById(R.id.brushPanel);
        brushOverlay = requireActivity().findViewById(R.id.brushOverlay);

        bottomArea1 = requireActivity().findViewById(R.id.bottomArea1);
        undoSticker = requireActivity().findViewById(R.id.undoSticker);
        redoSticker = requireActivity().findViewById(R.id.redoSticker);
        originalSticker = requireActivity().findViewById(R.id.originalSticker);
        brushToSticker = requireActivity().findViewById(R.id.brushToSticker);
        lassoOverlay = requireActivity().findViewById(R.id.lassoOverlay);
        checkBox = requireActivity().findViewById(R.id.checkBox);

        topArea = requireActivity().findViewById(R.id.topArea);
        stickerOverlay = requireActivity().findViewById(R.id.stickerOverlay);

        if (bottomArea1 != null) {
            undoSticker.setVisibility(View.INVISIBLE);
            redoSticker.setVisibility(View.INVISIBLE);
            originalSticker.setVisibility(View.INVISIBLE);
            bottomArea1.setVisibility(View.VISIBLE);
            brushToSticker.setVisibility(View.VISIBLE);
        }

        if (checkBox != null) {
            checkBox.setOnCheckedChangeListener((btn, isChecked) -> {
                if (isChecked) {
                    pen.setEnabled(false);
                    glowPen.setEnabled(false);
                    crayon.setEnabled(false);
                    eraser.setEnabled(false);

                    pen.setAlpha(0.2f);
                    glowPen.setAlpha(0.2f);
                    crayon.setAlpha(0.2f);
                    eraser.setAlpha(0.2f);

                    penTxt.setAlpha(0.2f);
                    glowPenTxt.setAlpha(0.2f);
                    crayonTxt.setAlpha(0.2f);
                    eraserTxt.setAlpha(0.2f);

                    lassoOverlay.setVisibility(View.VISIBLE);

                    FilterActivity act = (FilterActivity) requireActivity();
                    if (act.getRenderer() != null) {
                        int vx = act.getRenderer().getViewportX();
                        int vy = act.getRenderer().getViewportY();
                        int vw = act.getRenderer().getViewportWidth();
                        int vh = act.getRenderer().getViewportHeight();
                        lassoOverlay.setImageBounds(new RectF(vx, vy, vx + vw, vy + vh));
                        lassoOverlay.setLassoVisible(true);
                        lassoOverlay.clearAll();
                        lassoOverlay.setDrawingEnabled(true);
                    }
                    lassoOverlay.setLassoListener(() -> {
                        lassoOverlay.setDrawingEnabled(false);
                        showBrushToStickerDialog();
                    });
                } else {
                    lassoOverlay.setLassoListener(null);
                    lassoOverlay.setDrawingEnabled(false);
                    lassoOverlay.setLassoVisible(false);

                    pen.setEnabled(true);
                    glowPen.setEnabled(true);
                    crayon.setEnabled(true);
                    eraser.setEnabled(true);

                    setModeAlpha(lastMode);
                }
            });
        }

        tintPenButton(lastPenColor);
        tintGlowButton(lastGlowColor);
        tintCrayonButton(lastCrayonColor);

        if (brushOverlay != null) {
            brushDraw = pickupExistingBrushOverlay(brushOverlay);
            if (brushDraw == null) {
                brushDraw = new BrushOverlayView(requireContext());

                brushOverlay.addView(brushDraw,
                        new FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT));
            }
            baselineStrokeCount = brushDraw.getVisibleStrokeCount();
            if (stickerOverlay != null) baselineChildCount = stickerOverlay.getChildCount();

            if (brushDraw != null) {
                brushDraw.setOnStrokeProgressListener(new BrushOverlayView.OnStrokeProgressListener() {
                    @Override
                    public void onStrokeProgress(Path path, float strokeWidthPx, BrushOverlayView.BrushMode mode) {
                        if (mode != BrushOverlayView.BrushMode.ERASER) return;
                        if (stickerOverlay == null) return;

                        for (int i = 0; i < stickerOverlay.getChildCount(); i++) {
                            View child = stickerOverlay.getChildAt(i);
                            if (!(child instanceof ImageView)) continue;
                            if (!Boolean.TRUE.equals(child.getTag(R.id.tag_brush_layer))) continue;

                            ImageView iv = (ImageView) child;
                            Drawable d = iv.getDrawable();
                            if (!(d instanceof BitmapDrawable)) continue;

                            Bitmap bmp = ((BitmapDrawable) d).getBitmap();
                            if (bmp == null || bmp.isRecycled()) continue;

                            if (!bmp.isMutable()) {
                                bmp = bmp.copy(Bitmap.Config.ARGB_8888, true);
                                iv.setImageBitmap(bmp);
                            }

                            Path mappedDelta = mapPathFromBrushViewToBitmap(path, brushOverlay, iv, bmp.getWidth(), bmp.getHeight());

                            PendingErase pe = activeErases.get(iv);
                            if (pe == null) {
                                pe = new PendingErase();
                                pe.view = iv;
                                pe.width = strokeWidthPx;
                                activeErases.put(iv, pe);
                            }
                            pe.path.addPath(mappedDelta);

                            Rect rect = rectForPath(mappedDelta, strokeWidthPx, bmp.getWidth(), bmp.getHeight());
                            Bitmap before = null;
                            try {
                                before = Bitmap.createBitmap(bmp, rect.left, rect.top, rect.width(), rect.height());
                                Patch p = new Patch();
                                p.rect = rect;
                                p.before = before;
                                pe.patches.add(p);
                            } catch (Throwable ignore) {
                            }

                            Canvas c = new Canvas(bmp);
                            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                            p.setStyle(Paint.Style.STROKE);
                            p.setStrokeJoin(Paint.Join.ROUND);
                            p.setStrokeCap(Paint.Cap.ROUND);
                            p.setStrokeWidth(strokeWidthPx);
                            p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                            c.drawPath(mappedDelta, p);

                            iv.invalidate();

                            if (before != null && !before.isRecycled()) {
                                try {
                                    int w = rect.width(), h = rect.height();
                                    int[] afterRow = new int[w];
                                    int[] beforeRow = new int[w];
                                    boolean changed = false;
                                    for (int yy = 0; yy < h && !changed; yy++) {
                                        bmp.getPixels(afterRow, 0, w, rect.left, rect.top + yy, w, 1);
                                        before.getPixels(beforeRow, 0, w, 0, yy, w, 1);
                                        for (int xx = 0; xx < w; xx++) {
                                            int aB = (beforeRow[xx] >>> 24) & 0xFF;
                                            int aA = (afterRow[xx] >>> 24) & 0xFF;
                                            if (aA < aB) {
                                                changed = true;
                                                break;
                                            }
                                        }
                                    }
                                    if (changed) pe.hadEffect = true;
                                } catch (Throwable ignore) {
                                }
                            }
                        }
                    }

                    @Override
                    public void onStrokeEnd(Path fullPath, float strokeWidthPx, BrushOverlayView.BrushMode mode) {
                        if (mode == BrushOverlayView.BrushMode.ERASER && !activeErases.isEmpty()) {
                            boolean anyEffect = false;
                            for (PendingErase pe : activeErases.values()) {
                                if (pe.hadEffect) {
                                    anyEffect = true;
                                    break;
                                }
                            }

                            if (!anyEffect) {
                                for (PendingErase pe : activeErases.values()) {
                                    for (Patch pa : pe.patches) {
                                        if (pa.before != null && !pa.before.isRecycled())
                                            pa.before.recycle();
                                    }
                                    pe.patches.clear();
                                }
                                activeErases.clear();
                                return;
                            }

                            FilterActivity act = (FilterActivity) requireActivity();
                            ArrayList<FilterActivity.EraseOp> ops = new ArrayList<>();

                            for (PendingErase pe : activeErases.values()) {
                                if (!pe.hadEffect) {
                                    for (Patch pa : pe.patches) {
                                        if (pa.before != null && !pa.before.isRecycled())
                                            pa.before.recycle();
                                    }
                                    continue;
                                }

                                FilterActivity.EraseOp eo = new FilterActivity.EraseOp();
                                eo.view = pe.view;
                                eo.pathOnBitmap = new Path(pe.path);
                                eo.strokeWidthPx = pe.width;

                                for (Patch pa : pe.patches) {
                                    FilterActivity.ErasePatch ep = new FilterActivity.ErasePatch();
                                    ep.rect = new Rect(pa.rect);
                                    ep.before = pa.before;
                                    eo.patches.add(ep);
                                }
                                ops.add(eo);
                            }

                            sessionEraseOps.addAll(ops);
                            //if (!ops.isEmpty()) act.recordBrushErase(ops);

                            activeErases.clear();
                        }
                    }
                });
            }

            BrushOverlayView.BrushMode startMode
                    = BrushPrefs.getLastMode(requireContext(), BrushOverlayView.BrushMode.PEN);

            int color;
            int sizePx;
            switch (startMode) {
                case GLOW:
                    color = lastGlowColor;
                    sizePx = (lastGlowSizePx > 0) ? lastGlowSizePx : dp(10);
                    break;
                case CRAYON:
                    color = lastCrayonColor;
                    sizePx = (lastCrayonSizePx > 0) ? lastCrayonSizePx : dp(10);
                    break;
                case ERASER:
                    color = Color.TRANSPARENT;
                    sizePx = BrushPrefs.getEraserSize(requireContext(), dp(10));
                    break;
                case PEN:
                default:
                    color = lastPenColor;
                    sizePx = (lastPenSizePx > 0) ? lastPenSizePx : dp(10);
                    break;
            }
            brushDraw.setBrush(color, sizePx, startMode);
            brushDraw.setDrawingEnabled(!isPenPanelOpen);

            setModeAlpha(startMode);
            lastMode = startMode;

            brushClipListener = (v, a, b, c, d, e, f, g, h) -> {
                if (!isAdded()) return;
                Activity act = getActivity();
                if (act instanceof FilterActivity && brushDraw != null) {
                    ((FilterActivity) act).applyBrushClipRect(brushDraw);
                }
            };
            brushOverlay.addOnLayoutChangeListener(brushClipListener);
        }

        pen.setOnClickListener(v -> {
            if (isPenPanelOpen) return;
            if (brushDraw != null) {
                brushDraw.setBrush(lastPenColor, lastPenSizePx, BrushOverlayView.BrushMode.PEN);
                brushDraw.setDrawingEnabled(true);
            }
            lastMode = BrushOverlayView.BrushMode.PEN;
            setModeAlpha(lastMode);
            BrushPrefs.saveLastMode(requireContext(), lastMode);
            showPanel(lastMode);
        });

        glowPen.setOnClickListener(v -> {
            if (isPenPanelOpen) return;
            if (brushDraw != null) {
                brushDraw.setBrush(lastGlowColor, lastGlowSizePx, BrushOverlayView.BrushMode.GLOW);
                brushDraw.setDrawingEnabled(true);
            }
            lastMode = BrushOverlayView.BrushMode.GLOW;
            setModeAlpha(lastMode);
            BrushPrefs.saveLastMode(requireContext(), lastMode);
            showPanel(lastMode);
        });

        crayon.setOnClickListener(v -> {
            if (isPenPanelOpen) return;
            if (brushDraw != null) {
                brushDraw.setBrush(lastCrayonColor, lastCrayonSizePx, BrushOverlayView.BrushMode.CRAYON);
                brushDraw.setDrawingEnabled(true);
            }
            lastMode = BrushOverlayView.BrushMode.CRAYON;
            setModeAlpha(lastMode);
            BrushPrefs.saveLastMode(requireContext(), lastMode);
            showPanel(lastMode);
        });

        eraser.setOnClickListener(v -> {
            if (isPenPanelOpen) return;
            if (brushDraw != null) {
                int lastEraserSize = BrushPrefs.getEraserSize(requireContext(), dp(10));
                brushDraw.setBrush(Color.TRANSPARENT, lastEraserSize, BrushOverlayView.BrushMode.ERASER);
                brushDraw.setDrawingEnabled(true);
            }
            lastMode = BrushOverlayView.BrushMode.ERASER;
            setModeAlpha(lastMode);
            BrushPrefs.saveLastMode(requireContext(), lastMode);
            showEraserPanel();
        });

        cancelBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(500)) return;

            if (currentToolPanel != null) hideToolPanel(false);

            rollbackActiveErases();

            rollbackSessionErases();

            if (brushDraw != null) {
                brushDraw.trimToCount(baselineStrokeCount);
                brushDraw.setDrawingEnabled(false);
            }
            isPenPanelOpen = false;

            if (checkBox != null && checkBox.isChecked()) {
                checkBox.setChecked(false);
            }

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

                int from = baselineStrokeCount;
                int to = brushDraw.getVisibleStrokeCount();
                Bitmap sessionBmp = brushDraw.renderStrokes(from, to);
                if (sessionBmp != null) {
                    boolean hasPixel = hasAnyVisiblePixel(sessionBmp);
                    if (hasPixel && stickerOverlay != null) {
                        ImageView layer = new ImageView(requireContext());
                        layer.setImageBitmap(sessionBmp);
                        layer.setScaleType(ImageView.ScaleType.FIT_XY);
                        layer.setLayoutParams(new FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT));

                        layer.setClickable(false);
                        layer.setLongClickable(false);
                        layer.setFocusable(false);
                        layer.setFocusableInTouchMode(false);
                        layer.setEnabled(false);
                        layer.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                        layer.setOnTouchListener(null);
                        layer.setTag(R.id.tag_brush_layer, Boolean.TRUE);

                        float maxZ = 0f;
                        for (int i = 0; i < stickerOverlay.getChildCount(); i++) {
                            maxZ = Math.max(maxZ, ViewCompat.getZ(stickerOverlay.getChildAt(i)));
                        }
                        ViewCompat.setZ(layer, maxZ);

                        stickerOverlay.addView(layer);
                    } else {
                        if (sessionBmp != null && !sessionBmp.isRecycled()) sessionBmp.recycle();
                    }
                }

                brushDraw.trimToCount(baselineStrokeCount);
            }

            FilterActivity a = (FilterActivity) requireActivity();
            if (!sessionEraseOps.isEmpty()) {
                a.recordBrushErase(sessionEraseOps);
                sessionEraseOps.clear();
            }

            if (stickerOverlay != null) {
                a.recordStickerPlacement(baselineChildCount);
                baselineChildCount = stickerOverlay.getChildCount();
            }

            isPenPanelOpen = false;

            if (checkBox != null && checkBox.isChecked()) {
                checkBox.setChecked(false);
            }

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.slide_up, 0)
                    .replace(R.id.bottomArea2, new StickersFragment())
                    .commit();
        });

        return view;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("lastPenColor", lastPenColor);
        outState.putInt("lastPenSizePx", lastPenSizePx);
        outState.putInt("lastGlowColor", lastGlowColor);
        outState.putInt("lastGlowSizePx", lastGlowSizePx);
        outState.putInt("lastCrayonColor", lastCrayonColor);
        outState.putInt("lastCrayonSizePx", lastCrayonSizePx);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (brushOverlay != null && brushClipListener != null) {
            brushOverlay.removeOnLayoutChangeListener(brushClipListener);
            brushClipListener = null;
        }
        if (brushDraw != null) brushDraw.setDrawingEnabled(false);

        if (checkBox != null) {
            checkBox.setOnCheckedChangeListener(null);
            checkBox.setChecked(false);
        }
        if (lassoOverlay != null) {
            lassoOverlay.setLassoListener(null);
            lassoOverlay.clearAll();
            lassoOverlay.setDrawingEnabled(false);
            lassoOverlay.setVisibility(View.GONE);
        }
    }

    /// 패널 UI ///
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
        } else if (mode == BrushOverlayView.BrushMode.PEN) {
            updatePenSizePreview(panel, argb, curDia);
        } else {
            updateCrayonSizePreview(panel, argb, curDia);
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

    /// 컬러 팔레트 ///
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

    /// 색상 코드, RGB코드 ///
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

    private void syncEditorsFromCur(EditText hex, EditText r, EditText g, EditText b, EditText v, EditText a) {
        int argb = argbFromCur();
        int rr = Color.red(argb), gg = Color.green(argb), bb = Color.blue(argb);
        setTextIfChangedKeepCursor(hex, String.format(Locale.US, "%02X%02X%02X", rr, gg, bb));
        setTextSkipIfEmptyAndZero(r, rr);
        setTextSkipIfEmptyAndZero(g, gg);
        setTextSkipIfEmptyAndZero(b, bb);
        setTextIfChangedKeepCursor(v, String.valueOf(Math.round(curVal * 100)));
        setTextIfChangedKeepCursor(a, String.valueOf(curAlphaPct));
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

    private int parseHexRGB(String hexCode) {
        try {
            return (int) Long.parseLong(hexCode, 16);
        } catch (Exception e) {
            return 0xFFFFFF;
        }
    }

    private int gatherCurrentARGB(EditText rCode, EditText gCode, EditText bCode, EditText alphaValue) {
        int r = clamp(parseIntEmptyZeroSafe(rCode), 0, 255);
        int g = clamp(parseIntEmptyZeroSafe(gCode), 0, 255);
        int b = clamp(parseIntEmptyZeroSafe(bCode), 0, 255);
        int aPct = clamp(parseIntEmptyZeroSafe(alphaValue), 0, 100);
        int a = clamp(Math.round(aPct * 2.55f), 0, 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private int argbFromCur() {
        int rgb = Color.HSVToColor(new float[]{curHue, curSat, Math.max(0f, Math.min(1f, curVal))}) & 0x00FFFFFF;
        int a = clamp(Math.round(curAlphaPct * 2.55f), 0, 255);
        return (a << 24) | rgb;
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
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

    private void setIfEmpty(EditText et, String def) {
        if (et != null && et.getText().length() == 0) et.setText(def);
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

    /// 시크바 공통 ///
    private void refreshAllSeekbars(SeekBar satSeek, SeekBar alphaSeek, SeekBar sizeSeek) {
        int vPct = Math.round(curVal * 100f);
        updateSatSeekbarAppearance(satSeek, curHue, curSat, vPct);
        if (alphaSeek != null)
            updateAlphaSeekbarAppearance(alphaSeek, curHue, curSat, curVal, curAlphaPct);
        if (sizeSeek != null) updateSizeSeekbarAppearance(sizeSeek, argbFromCur());
    }

    private void initSeekbarDrawables(View panel) {
        satTrack = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.BLACK, Color.WHITE});
        satTrack.setCornerRadius(dp(999));
        satThumb = (GradientDrawable) buildRoundThumb(THUMB_DIAMETER_DP, Color.WHITE, THUMB_STROKE_DP);
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
        alphaThumb = (GradientDrawable) buildRoundThumb(THUMB_DIAMETER_DP, Color.WHITE, THUMB_STROKE_DP);
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
        sizeThumb = (GradientDrawable) buildRoundThumb(THUMB_DIAMETER_DP, Color.WHITE, THUMB_STROKE_DP);
        sizeTrack.mutate();
        sizeThumb.mutate();

        SeekBar sSize = panel.findViewById(R.id.sizeSeekbar);
        sSize.setProgressDrawable(sizeLayer);
        sSize.setThumb(sizeThumb);
    }

    private void setupSeekbarStaticPadding(View panel) {
        int padding = dp(THUMB_DIAMETER_DP / 2);
        for (int id : new int[]{R.id.saturationSeekbar, R.id.alphaSeekbar, R.id.sizeSeekbar}) {
            SeekBar b = panel.findViewById(id);
            b.setPadding(0, 0, padding, 0);
            try {
                b.setThumbOffset(0);
            } catch (Throwable ignore) {
            }
        }
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
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.brush_transparent_bg);
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

    /// 채도 시크바 ///
    private void updateSatSeekbarAppearance(SeekBar bar, float hue, float sat, int vPct) {
        int end = Color.HSVToColor(new float[]{hue, sat, 1f});
        satTrack.setColors(new int[]{Color.BLACK, end});

        float v = Math.max(0f, Math.min(1f, vPct / 100f));
        int thumbArgb = Color.HSVToColor(new float[]{hue, sat, v});
        satThumb.setColor(thumbArgb);
        bar.invalidate();
    }

    /// 투명도 시크바 ///
    private void updateAlphaSeekbarAppearance(SeekBar bar, float hue, float sat, float v, int aPct) {
        int opaque = Color.HSVToColor(new float[]{hue, sat, Math.max(0f, Math.min(1f, v))});
        int rgb = opaque & 0x00FFFFFF;

        alphaTrack.setColors(new int[]{rgb, (rgb | 0xFF000000)});

        int a = clamp(Math.round(aPct * 2.55f), 0, 255);
        int thumbArgb = (a << 24) | rgb;
        alphaThumb.setColor(thumbArgb);
        bar.invalidate();
    }

    /// 굵기 시크바 ///
    private void updateSizeSeekbarAppearance(SeekBar bar, int argb) {
        sizeTrack.setColors(new int[]{argb, argb});
        sizeThumb.setColor(argb);
        bar.invalidate();
    }

    /// 브러쉬 최종 이미지 미리보기 ///
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
        ImageView size = panel.findViewById(R.id.size);
        if (size == null) return;

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

        ViewGroup.LayoutParams lp = size.getLayoutParams();
        if (lp != null) {
            lp.width = bmW;
            lp.height = bmH;
            size.setLayoutParams(lp);
        }
        size.setScaleType(ImageView.ScaleType.CENTER);
        size.setImageBitmap(bmp);
        size.setBackground(null);
    }

    private BitmapShader ensureCrayonPreviewShader() {
        if (crayonPreviewShader != null) return crayonPreviewShader;

        Drawable d = requireContext().getDrawable(R.drawable.texture_crayon);
        if (d == null) return null;

        Bitmap src;
        if (d instanceof BitmapDrawable) {
            src = ((BitmapDrawable) d).getBitmap();
        } else {
            int w = Math.max(2, d.getIntrinsicWidth());
            int h = Math.max(2, d.getIntrinsicHeight());
            if (w <= 0) w = 256;
            if (h <= 0) h = 256;
            src = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas cc = new Canvas(src);
            d.setBounds(0, 0, w, h);
            d.draw(cc);
        }

        int w = src.getWidth(), h = src.getHeight();
        int[] in = new int[w * h];
        int[] out = new int[w * h];
        src.getPixels(in, 0, w, 0, 0, w, h);

        for (int i = 0; i < in.length; i++) {
            int c = in[i];
            int r = (c >> 16) & 0xFF;
            int g = (c >> 8) & 0xFF;
            int b = (c) & 0xFF;
            int luminance = (int) (0.299f * r + 0.587f * g + 0.114f * b);
            int alpha = 255 - luminance;
            if (alpha < 0) alpha = 0;
            if (alpha > 255) alpha = 255;
            out[i] = (alpha << 24) | 0x00FFFFFF;
        }

        Bitmap mask = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mask.setPixels(out, 0, w, 0, 0, w, h);

        crayonPreviewShader = new BitmapShader(mask, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        return crayonPreviewShader;
    }

    private void updateCrayonSizePreview(View panel, int argb, int diameterPx) {
        ImageView size = panel.findViewById(R.id.size);
        View wrap = panel.findViewById(R.id.sizePreviewWrap);
        if (size == null) return;

        Runnable draw = () -> {
            int bmW = (wrap != null && wrap.getWidth() > 0) ? wrap.getWidth() : dp(40);
            int bmH = bmW;

            Bitmap bmp = Bitmap.createBitmap(bmW, bmH, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bmp);
            float cx = bmW / 2f, cy = bmH / 2f;
            float r = diameterPx / 2f;

            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setStyle(Paint.Style.FILL);

            BitmapShader shader = ensureCrayonPreviewShader();
            if (shader != null) {
                p.setShader(shader);
                int opaqueColor = (argb | 0xFF000000);
                p.setColorFilter(new PorterDuffColorFilter(opaqueColor, PorterDuff.Mode.SRC_ATOP));
                p.setAlpha(Color.alpha(argb));
            } else {
                p.setColor(argb);
            }

            c.drawCircle(cx, cy, r, p);

            ViewGroup.LayoutParams lp = size.getLayoutParams();
            if (lp != null) {
                lp.width = bmW;
                lp.height = bmH;
                size.setLayoutParams(lp);
            }
            size.setScaleType(ImageView.ScaleType.CENTER);
            size.setImageBitmap(bmp);
            size.setBackground(null);
        };

        if (wrap == null || wrap.getWidth() == 0) {
            size.post(draw);
        } else {
            draw.run();
        }
    }

    private void updateEraserSizePreview(ImageView eraserSizeView, int sizePx) {
        if (eraserSizeView == null) return;

        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(Color.WHITE);
        d.setSize(dp(sizePx), dp(sizePx));

        ViewGroup.LayoutParams lp = eraserSizeView.getLayoutParams();
        if (lp != null) {
            lp.width = sizePx;
            lp.height = sizePx;
            eraserSizeView.setLayoutParams(lp);
        }
        eraserSizeView.setBackground(d);
    }

    private void updateSizePreviewByMode(View panel, int argb, int diameterPx, BrushOverlayView.BrushMode mode) {
        if (mode == BrushOverlayView.BrushMode.GLOW) {
            updateGlowSizePreview(panel, argb, diameterPx);
        } else if (mode == BrushOverlayView.BrushMode.CRAYON) {
            updateCrayonSizePreview(panel, argb, diameterPx);
        } else {
            updatePenSizePreview(panel, argb, diameterPx);
        }
    }

    /// 지우개 ///
    private int interpolateColor(int start, int end, float t) {
        int sa = (start >> 24) & 0xFF, sr = (start >> 16) & 0xFF, sg = (start >> 8) & 0xFF, sb = start & 0xFF;
        int ea = (end >> 24) & 0xFF, er = (end >> 16) & 0xFF, eg = (end >> 8) & 0xFF, eb = end & 0xFF;

        int a = (int) (sa + (ea - sa) * t);
        int r = (int) (sr + (er - sr) * t);
        int g = (int) (sg + (eg - sg) * t);
        int b = (int) (sb + (eb - sb) * t);
        return Color.argb(a, r, g, b);
    }

    private int getDiameterFromSeekbar(View panel) {
        SeekBar sizeSeekbar = panel.findViewById(R.id.sizeSeekbar);
        if (sizeSeekbar == null) return dp(10);
        int min = dp(10), max = dp(40);
        int progress = sizeSeekbar.getProgress();
        return min + Math.round((max - min) * (progress / 100f));
    }

    private Rect rectForPath(Path mapped, float strokeWidth, int bmpW, int bmpH) {
        RectF b = new RectF();
        mapped.computeBounds(b, true);
        int pad = Math.max(2, Math.round(strokeWidth / 2f + 2f));
        int l = clamp(Math.round(b.left) - pad, 0, bmpW - 1);
        int t = clamp(Math.round(b.top) - pad, 0, bmpH - 1);
        int r = clamp(Math.round(b.right) + pad, 0, bmpW - 1);
        int btm = clamp(Math.round(b.bottom) + pad, 0, bmpH - 1);
        return new Rect(l, t, Math.max(l + 1, r), Math.max(t + 1, btm));
    }

    private void rollbackActiveErases() {
        if (activeErases.isEmpty()) return;
        for (PendingErase pe : activeErases.values()) {
            if (!(pe.view.getDrawable() instanceof BitmapDrawable)) continue;
            Bitmap bmp = ((BitmapDrawable) pe.view.getDrawable()).getBitmap();
            if (bmp == null || bmp.isRecycled()) continue;

            for (Patch pa : pe.patches) {
                if (pa.before == null || pa.before.isRecycled()) continue;
                Canvas c = new Canvas(bmp);
                c.drawBitmap(pa.before, pa.rect.left, pa.rect.top, null);

                pa.before.recycle();
            }
            pe.patches.clear();
            pe.hadEffect = false;
            pe.path.reset();
            pe.view.invalidate();
        }
        activeErases.clear();
    }

    private void rollbackSessionErases() {
        if (sessionEraseOps.isEmpty()) return;
        for (FilterActivity.EraseOp eo : sessionEraseOps) {
            if (!(eo.view.getDrawable() instanceof BitmapDrawable)) continue;
            Bitmap bmp = ((BitmapDrawable) eo.view.getDrawable()).getBitmap();
            if (bmp == null || bmp.isRecycled()) continue;

            Canvas c = new Canvas(bmp);
            for (FilterActivity.ErasePatch ep : eo.patches) {
                if (ep.before == null || ep.before.isRecycled()) continue;
                c.drawBitmap(ep.before, ep.rect.left, ep.rect.top, null);

                ep.before.recycle();
                ep.before = null;
            }
            eo.view.invalidate();
        }
        sessionEraseOps.clear();
    }

    /// 패널 열기 ///
    private void showPanel(BrushOverlayView.BrushMode mode) {
        if (brushPanel == null || isPenPanelOpen) return;
        isPenPanelOpen = true;

        final float backupHue = lastHue;
        final float backupSat = lastSat;
        final boolean backupHasHS = hasLastHS;

        if (brushDraw != null) brushDraw.setDrawingEnabled(false);

        int layout = (mode == BrushOverlayView.BrushMode.GLOW)
                ? R.layout.v_glow : (mode == BrushOverlayView.BrushMode.CRAYON ? R.layout.v_crayon : R.layout.v_pen);
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
        int lastSize = Math.max(getLastSize(mode), dp(10));
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
            int min = dp(10), max = dp(40);
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
            //baseHS[0] = (hsv[2] == 0f && hasLastHS) ? lastHue : hsv[0];
            //baseHS[1] = (hsv[2] == 0f && hasLastHS) ? lastSat : hsv[1];

            if (satVal != null)
                setTextIfChangedKeepCursor(satVal, String.valueOf(Math.round(hsv[2] * 100)));

            float useHue = (hsv[2] == 0f && hasLastHS) ? lastHue : hsv[0];
            float useSat = (hsv[2] == 0f && hasLastHS) ? lastSat : hsv[1];

            curHue = useHue;
            curSat = useSat;
            curVal = hsv[2];
            curAlphaPct = aPct;

            lastHue = curHue;
            lastSat = curSat;
            hasLastHS = true;

            suppress = true;
            pushColorToUI(panel, colorPalette, argb, hexCode, true, mode);
            refreshAllSeekbars(satSeek, aSeek, sizeSeek);
            syncEditorsFromCur(hexCode, rCode, gCode, bCode, satVal, aVal);
            suppress = false;
        });

        TextWatcher rgbWatcher = new SimpleTextWatcher(() -> {
            if (suppress) return;
            int r = clamp(parseIntEmptyZeroSafe(rCode), 0, 255);
            int g = clamp(parseIntEmptyZeroSafe(gCode), 0, 255);
            int b = clamp(parseIntEmptyZeroSafe(bCode), 0, 255);
            int aPct = clamp(parseIntEmptyZeroSafe(aVal), 0, 100);
            int a = clamp(Math.round(aPct * 2.55f), 0, 255);
            int argb = (a << 24) | (r << 16) | (g << 8) | b;

            float[] hsv = new float[3];
            Color.colorToHSV(0xFF000000 | ((r << 16) | (g << 8) | b), hsv);
            //baseHS[0] = (hsv[2] == 0f && hasLastHS) ? lastHue : hsv[0];
            //baseHS[1] = (hsv[2] == 0f && hasLastHS) ? lastSat : hsv[1];

            if (satVal != null) {
                setTextIfChangedKeepCursor(satVal, String.valueOf(Math.round(hsv[2] * 100)));
            }

            float useHue = (hsv[2] == 0f && hasLastHS) ? lastHue : hsv[0];
            float useSat = (hsv[2] == 0f && hasLastHS) ? lastSat : hsv[1];

            curHue = useHue;
            curSat = useSat;
            curVal = hsv[2];
            curAlphaPct = aPct;

            lastHue = curHue;
            lastSat = curSat;
            hasLastHS = true;

            suppress = true;
            pushColorToUI(panel, colorPalette, argb, (EditText) getView().findFocus(), true, mode);
            refreshAllSeekbars(satSeek, aSeek, sizeSeek);
            syncEditorsFromCur(hexCode, rCode, gCode, bCode, satVal, aVal);
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
            int raw = parseIntEmptyZero(satVal.getText().toString());
            int clamped = clamp(raw, 0, 100);
            if (raw != clamped) {
                suppress = true;
                setTextIfChangedKeepCursor(satVal, String.valueOf(clamped));
                suppress = false;
            }

            if (satSeek != null) satSeek.setProgress(clamped);

            curVal = clamped / 100f;
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
            int raw = parseIntEmptyZeroSafe(aVal);
            int clamped = clamp(raw, 0, 100);
            if (raw != clamped) {
                suppress = true;
                setTextIfChangedKeepCursor(aVal, String.valueOf(clamped));
                suppress = false;
            }
            if (aSeek != null) aSeek.setProgress(clamped);

            curAlphaPct = clamped;
            refreshAllSeekbars(satSeek, aSeek, sizeSeek);
            suppress = true;
            pushColorToUI(panel, colorPalette, argbFromCur(), aVal, false, mode);
            suppress = false;
        }));

        if (sizeSeek != null)
            sizeSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                    int min = dp(10), max = dp(40);
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
            else if (mode == BrushOverlayView.BrushMode.CRAYON) tintCrayonButton(chosen);
            else tintPenButton(chosen);

            brushState.color = chosen;
            brushState.sizePx = dia;

            if (mode == BrushOverlayView.BrushMode.GLOW) {
                BrushPrefs.saveGlow(requireContext(), chosen, dia);
                if (brushDraw != null)
                    brushDraw.setBrush(chosen, dia, BrushOverlayView.BrushMode.GLOW);
            } else if (mode == BrushOverlayView.BrushMode.CRAYON) {
                BrushPrefs.saveCrayon(requireContext(), chosen, dia);
                if (brushDraw != null)
                    brushDraw.setBrush(chosen, dia, BrushOverlayView.BrushMode.CRAYON);
            } else {
                BrushPrefs.savePen(requireContext(), chosen, dia);
                if (brushDraw != null)
                    brushDraw.setBrush(chosen, dia, BrushOverlayView.BrushMode.PEN);
            }

            BrushPrefs.saveLastMode(requireContext(), mode);

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

        try {
            sizeSeekbar.setThumbOffset(0);
        } catch (Throwable ignore) {
        }

        final int minPx = dp(10);
        final int maxPx = dp(100);

        int lastSizePx = BrushPrefs.getEraserSize(requireContext(), dp(10));
        lastSizePx = Math.max(minPx, Math.min(maxPx, lastSizePx));

        int progress = Math.round((lastSizePx - minPx) * 100f / (maxPx - minPx));
        sizeSeekbar.setProgress(progress);

        GradientDrawable eraserTrack = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.parseColor("#007BFF"), Color.parseColor("#C2FA7A")}
        );
        eraserTrack.setCornerRadius(dp(999));
        eraserTrack.mutate();
        sizeSeekbar.setProgressDrawable(eraserTrack);

        GradientDrawable eraserThumb =
                (GradientDrawable) buildRoundThumb(THUMB_DIAMETER_DP, Color.BLACK, THUMB_STROKE_DP);
        eraserThumb.mutate();

        sizeSeekbar.setProgress(progress);
        float ratio = Math.max(0f, Math.min(1f, progress / 100f));
        int initColor = interpolateColor(Color.parseColor("#007BFF"), Color.parseColor("#C2FA7A"), ratio);
        eraserThumb.setColor(initColor);
        sizeSeekbar.setThumb(eraserThumb);

        updateEraserSizePreview(eraserSizeView, lastSizePx);

        sizeSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int p, boolean fromUser) {
                int sizePx = minPx + Math.round((maxPx - minPx) * (p / 100f));
                updateEraserSizePreview(eraserSizeView, sizePx);

                float ratio = Math.max(0f, Math.min(1f, p / 100f));
                int thumbColor = interpolateColor(Color.parseColor("#007BFF"), Color.parseColor("#C2FA7A"), ratio);
                eraserThumb.setColor(thumbColor);
                seekBar.invalidate();

                if (brushDraw != null) {
                    brushDraw.setBrush(Color.TRANSPARENT, sizePx, BrushOverlayView.BrushMode.ERASER);
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
            int sizePx = minPx + Math.round((maxPx - minPx) * (sizeSeekbar.getProgress() / 100f));
            BrushPrefs.saveEraser(requireContext(), sizePx);
            if (brushDraw != null) {
                brushDraw.setBrush(Color.TRANSPARENT, sizePx, BrushOverlayView.BrushMode.ERASER);
            }

            BrushPrefs.saveLastMode(requireContext(), BrushOverlayView.BrushMode.ERASER);

            hideToolPanel(true);
        });
    }

    /// 패널 닫기 ///
    private void hideToolPanel(boolean enableDrawingAfterClose) {
        if (brushPanel == null) return;
        if (currentToolPanel == null) {
            brushPanel.setVisibility(View.GONE);
            isPenPanelOpen = false;

            if (brushDraw != null) {
                brushDraw.setDrawingEnabled(enableDrawingAfterClose);
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

    /// 브러쉬 컬러 & 굵기 ///
    private int getLastColor(BrushOverlayView.BrushMode mode) {
        if (mode == BrushOverlayView.BrushMode.GLOW) return lastGlowColor;
        if (mode == BrushOverlayView.BrushMode.CRAYON) return lastCrayonColor;
        return lastPenColor;
    }

    private int getLastSize(BrushOverlayView.BrushMode mode) {
        if (mode == BrushOverlayView.BrushMode.GLOW) return lastGlowSizePx;
        if (mode == BrushOverlayView.BrushMode.CRAYON) return lastCrayonSizePx;
        return lastPenSizePx;
    }

    private void setLastColor(BrushOverlayView.BrushMode mode, int argb) {
        if (mode == BrushOverlayView.BrushMode.GLOW) lastGlowColor = argb;
        else if (mode == BrushOverlayView.BrushMode.CRAYON) lastCrayonColor = argb;
        else lastPenColor = argb;
    }

    private void setLastSize(BrushOverlayView.BrushMode mode, int px) {
        if (mode == BrushOverlayView.BrushMode.GLOW) lastGlowSizePx = px;
        else if (mode == BrushOverlayView.BrushMode.CRAYON) lastCrayonSizePx = px;
        else lastPenSizePx = px;
    }

    /// 브러쉬 오버레이 재사용 → 브러쉬 그림 여러 번 그리고 관리 가능 ///
    private BrushOverlayView pickupExistingBrushOverlay(@NonNull FrameLayout overlay) {
        BrushOverlayView keep = null;
        for (int i = 0; i < overlay.getChildCount(); i++) {
            View child = overlay.getChildAt(i);
            if (child instanceof BrushOverlayView) {
                if (keep == null) {
                    keep = (BrushOverlayView) child;
                } else {
                    overlay.removeView(child);
                    i--;
                }
            }
        }
        return keep;
    }

    /// undoSticker, redoSticker, originalSticker 작업 관련 ///
    private Path mapPathFromBrushViewToBitmap(Path src, View brushOverlay, ImageView targetView, int bmpW, int bmpH) {
        Path out = new Path(src);

        float bx = brushOverlay.getX(), by = brushOverlay.getY();
        Matrix m = new Matrix();
        m.postTranslate(bx, by);
        out.transform(m);

        float tx = targetView.getX(), ty = targetView.getY();
        m.reset();
        m.postTranslate(-tx, -ty);
        out.transform(m);

        float sx = (bmpW * 1f) / Math.max(1, targetView.getWidth());
        float sy = (bmpH * 1f) / Math.max(1, targetView.getHeight());
        m.reset();
        m.postScale(sx, sy);
        out.transform(m);

        return out;
    }

    /// 브러쉬 → 스티커 변환 & 올가미 ///
    private void showBrushToStickerDialog() {
        new BrushToStickerDialog(requireContext(), new BrushToStickerDialog.BrushToStickerDialogListener() {
            @Override
            public void onYes() {
                brushToSticker();
            }

            @Override
            public void onNo() {
                lassoOverlay.clearAll();
                lassoOverlay.setDrawingEnabled(true);
            }
        }
        ).withMessage("선택한 영역을 내 스티커에 추가하시겠습니까?")
                .withButton1Text("예")
                .withButton2Text("아니오")
                .show();
    }

    private void brushToSticker() {
        Bitmap overlayBmp = null;
        Bitmap cropped = null;
        try {
            if (brushOverlay == null || lassoOverlay == null) return;
            int w = Math.max(1, brushOverlay.getWidth());
            int h = Math.max(1, brushOverlay.getHeight());
            overlayBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(overlayBmp);
            brushOverlay.draw(canvas);

            FrameLayout stickerOverlay = requireActivity().findViewById(R.id.stickerOverlay);
            if (stickerOverlay != null) {
                for (int i = 0; i < stickerOverlay.getChildCount(); i++) {
                    View child = stickerOverlay.getChildAt(i);
                    if (!(child instanceof ImageView)) continue;
                    if (!Boolean.TRUE.equals(child.getTag(R.id.tag_brush_layer))) continue;

                    int save = canvas.save();
                    canvas.translate(child.getX(), child.getY());
                    child.draw(canvas);
                    canvas.restore();
                }
            }

            List<Path> shapes = lassoOverlay.getShapes();
            if (shapes == null || shapes.isEmpty()) {
                showToast("브러쉬를 감지하지 못했습니다");
                return;
            }

            cropped = cropByPathsUnion(overlayBmp, shapes, new Matrix());
            if (cropped == null) {
                showToast("내 스티커에 저장을 실패했습니다.");
                return;
            }

            if (!hasAnyVisiblePixel(cropped)) {
                showToast("브러쉬를 감지하지 못했습니다");
                return;
            }

            Bitmap trimmed = trimTransparentAndPad(cropped, 8, dp(10));
            if (trimmed != cropped) {
                cropped.recycle();
                cropped = trimmed;
            }

            File f = savePngToStickers(cropped, requireContext());
            StickerStore.get().enqueuePending(StickerItem.fromFile(f.getAbsolutePath()));
            showToast("내 스티커에 저장을 완료했습니다");
        } catch (Throwable t) {
            showToast("내 스티커에 저장을 실패했습니다");
        } finally {
            if (overlayBmp != null && !overlayBmp.isRecycled()) overlayBmp.recycle();
            if (cropped != null && !cropped.isRecycled()) cropped.recycle();
            if (lassoOverlay != null) {
                lassoOverlay.clearAll();
                lassoOverlay.setDrawingEnabled(true);
            }
        }
    }

    private Bitmap cropByPathsUnion(Bitmap src, List<Path> pathsInView, Matrix viewToBmp) {
        if (src == null || pathsInView == null || pathsInView.isEmpty()) return null;

        Path unionBmp = new Path();
        boolean first = true;
        for (Path pv : pathsInView) {
            Path p = new Path(pv);
            p.transform(viewToBmp);
            if (first) {
                unionBmp.set(p);
                first = false;
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                unionBmp.op(p, Path.Op.UNION);
            } else {
                unionBmp.addPath(p);
            }
        }
        unionBmp.setFillType(Path.FillType.WINDING);

        RectF b = new RectF();
        unionBmp.computeBounds(b, true);
        if (b.isEmpty()) return null;

        b.inset(-1f, -1f);

        int outW = Math.max(1, Math.round(b.width()));
        int outH = Math.max(1, Math.round(b.height()));
        Bitmap out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);

        c.translate(-b.left, -b.top);

        Paint pm = new Paint(Paint.ANTI_ALIAS_FLAG);
        pm.setStyle(Paint.Style.FILL);
        c.drawPath(unionBmp, pm);

        pm.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        c.drawBitmap(src, 0, 0, pm);
        pm.setXfermode(null);

        return out;
    }

    private File savePngToStickers(Bitmap bmp, Context c) throws IOException {
        File dir = new File(c.getFilesDir(), "stickers");
        if (!dir.exists()) dir.mkdirs();
        String name = "sticker_" + System.currentTimeMillis() + ".png";
        File f = new File(dir, name);
        FileOutputStream fos = new FileOutputStream(f);
        bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
        fos.flush();
        fos.close();
        return f;
    }

    private boolean hasAnyVisiblePixel(@NonNull Bitmap bmp) {
        int w = bmp.getWidth(), h = bmp.getHeight();
        int[] row = new int[w];
        for (int y = 0; y < h; y++) {
            bmp.getPixels(row, 0, w, 0, y, w, 1);
            for (int x = 0; x < w; x++) {
                if (((row[x] >>> 24) & 0xFF) != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private Bitmap trimTransparentAndPad(@NonNull Bitmap src, int alphaThreshold, int padPx) {
        int w = src.getWidth(), h = src.getHeight();
        int[] row = new int[w];
        int left = w, right = -1, top = h, bottom = -1;

        for (int y = 0; y < h; y++) {
            src.getPixels(row, 0, w, 0, y, w, 1);
            for (int x = 0; x < w; x++) {
                int a = (row[x] >>> 24) & 0xFF;
                if (a > alphaThreshold) {
                    if (x < left) left = x;
                    if (x > right) right = x;
                    if (y < top) top = y;
                    if (y > bottom) bottom = y;
                }
            }
        }

        if (right < left || bottom < top) return src;

        left = Math.max(0, left - padPx);
        top = Math.max(0, top - padPx);
        right = Math.min(w - 1, right + padPx);
        bottom = Math.min(h - 1, bottom + padPx);

        int outW = Math.max(1, right - left + 1);
        int outH = Math.max(1, bottom - top + 1);

        Bitmap out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        c.drawBitmap(src, -left, -top, null);
        return out;
    }

    private void showToast(String message) {
        View old = topArea.findViewWithTag("inline_banner");
        if (old != null) topArea.removeView(old);

        TextView tv = new TextView(requireContext());
        tv.setTag("inline_banner");
        tv.setText(message);
        tv.setTextColor(0XFFFFFFFF);
        tv.setTextSize(16);
        tv.setPadding(dp(14), dp(10), dp(14), dp(10));
        tv.setElevation(dp(4));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xCC222222);
        bg.setCornerRadius(dp(16));
        tv.setBackground(bg);

        ConstraintLayout.LayoutParams lp =
                new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT,
                        ConstraintLayout.LayoutParams.WRAP_CONTENT);
        lp.startToStart = topArea.getId();
        lp.endToEnd = topArea.getId();
        lp.topToTop = topArea.getId();
        lp.bottomToBottom = topArea.getId();
        tv.setLayoutParams(lp);

        tv.setAlpha(0f);
        topArea.addView(tv);
        tv.animate().alpha(1f).setDuration(150).start();

        tv.postDelayed(() -> tv.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> {
                    if (tv.getParent() == topArea) topArea.removeView(tv);
                })
                .start(), 2000);
    }

    /// 버튼 UI ///
    private void tintPenButton(int argb) {
        if (pen == null) return;
        int opaque = (argb & 0x00FFFFFF) | 0xFF000000;
        ImageViewCompat.setImageTintList(pen, ColorStateList.valueOf(opaque));
        penTxt.setTextColor(argb);
    }

    private void tintGlowButton(int argb) {
        if (glowPen == null) return;
        int opaque = (argb & 0x00FFFFFF) | 0xFF000000;
        ImageViewCompat.setImageTintList(glowPen, ColorStateList.valueOf(opaque));
        glowPenTxt.setTextColor(argb);
    }

    private void tintCrayonButton(int argb) {
        if (crayon == null) return;
        int opaque = (argb & 0x00FFFFFF) | 0xFF000000;
        ImageViewCompat.setImageTintList(crayon, ColorStateList.valueOf(opaque));
        crayonTxt.setTextColor(argb);
    }

    private void setModeAlpha(BrushOverlayView.BrushMode mode) {
        float on = 1f, off = 0.2f;
        if (pen != null) {
            pen.setAlpha(mode == BrushOverlayView.BrushMode.PEN ? on : off);
            penTxt.setAlpha(mode == BrushOverlayView.BrushMode.PEN ? on : off);
        }
        if (glowPen != null) {
            glowPen.setAlpha(mode == BrushOverlayView.BrushMode.GLOW ? on : off);
            glowPenTxt.setAlpha(mode == BrushOverlayView.BrushMode.GLOW ? on : off);
        }
        if (crayon != null) {
            crayon.setAlpha(mode == BrushOverlayView.BrushMode.CRAYON ? on : off);
            crayonTxt.setAlpha(mode == BrushOverlayView.BrushMode.CRAYON ? on : off);
        }
        if (eraser != null) {
            eraser.setAlpha(mode == BrushOverlayView.BrushMode.ERASER ? on : off);
            eraserTxt.setAlpha(mode == BrushOverlayView.BrushMode.ERASER ? on : off);
        }
    }

    /// 단위 변환 ///
    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}