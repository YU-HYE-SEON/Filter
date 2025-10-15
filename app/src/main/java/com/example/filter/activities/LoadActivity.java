package com.example.filter.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.example.filter.R;
import com.example.filter.dialogs.FilterEixtDialog;
import com.example.filter.etc.ClickUtils;
import com.example.filter.etc.LassoOverlayView;
import com.example.filter.etc.ShapeOverlayView;
import com.example.filter.etc.ShapeOverlayView.ShapeType;
import com.example.filter.etc.StickerItem;
import com.example.filter.etc.StickerStore;
import com.example.filter.fragments.MyStickersFragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class LoadActivity extends BaseActivity {
    private ConstraintLayout topArea;
    private ImageView loadImage;
    private ShapeOverlayView shapeOverlay;
    private LassoOverlayView lassoOverlay;
    private ImageView lassoCut, shapeCut;
    private ImageView squareCut, starCut, triangleCut, circleCut, heartCut;
    private ImageButton cancelBtn, checkBtn;
    private boolean shapeModeOn = false;
    private boolean lassoModeOn = false;
    private final Matrix imgMatrix = new Matrix();
    private final Matrix savedMatrix = new Matrix();
    private final float[] mvals = new float[9];
    private float minScale = 1f;
    private float maxScale = 4f;

    private enum Mode {NONE, DRAG, ZOOM}

    private Mode touchMode = Mode.NONE;
    private float startX, startY;
    private float pinchStartDist = 0f;
    private float pinchMidX = 0f, pinchMidY = 0f;
    private ScaleGestureDetector scaleDetector;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_filter_load);

        topArea = findViewById(R.id.topArea);
        loadImage = findViewById(R.id.loadImage);
        shapeOverlay = findViewById(R.id.shapeOverlay);
        lassoOverlay = findViewById(R.id.lassoOverlay);
        lassoCut = findViewById(R.id.lassoCut);
        shapeCut = findViewById(R.id.shapeCut);
        squareCut = findViewById(R.id.squareCut);
        starCut = findViewById(R.id.starCut);
        triangleCut = findViewById(R.id.triangleCut);
        circleCut = findViewById(R.id.circleCut);
        heartCut = findViewById(R.id.heartCut);
        cancelBtn = findViewById(R.id.cancelBtn);
        checkBtn = findViewById(R.id.checkBtn);

        Window window = getWindow();
        window.setNavigationBarColor(Color.BLACK);

        Uri photoUri = getIntent().getData();
        if (photoUri != null) {
            loadImageFromUri(photoUri);
        } else {
            finish();
            return;
        }

        loadImage.post(() -> {
            if (loadImage.getDrawable() == null) return;

            int vw = loadImage.getWidth();
            int vh = loadImage.getHeight();
            int dw = loadImage.getDrawable().getIntrinsicWidth();
            int dh = loadImage.getDrawable().getIntrinsicHeight();

            if (vw == 0 || vh == 0 || dw <= 0 || dh <= 0) return;

            float s = Math.min(vw / (float) dw, vh / (float) dh);
            minScale = s;

            float dx = (vw - dw * s) * 0.5f;
            float dy = (vh - dh * s) * 0.5f;

            imgMatrix.reset();
            imgMatrix.postScale(s, s);
            imgMatrix.postTranslate(dx, dy);
            loadImage.setImageMatrix(imgMatrix);

            updateOverlaysImageBounds();

            RectF imgRect = getImageDisplayRect(loadImage);
            if (imgRect != null) {
                shapeOverlay.setImageBounds(imgRect);
                lassoOverlay.setImageBounds(imgRect);
            }
        });

        scaleDetector = new ScaleGestureDetector(this,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        float factor = detector.getScaleFactor();
                        imgMatrix.getValues(mvals);
                        float curScale = mvals[Matrix.MSCALE_X];
                        float target = curScale * factor;

                        if (target < minScale) factor = minScale / curScale;
                        else if (target > maxScale) factor = maxScale / curScale;

                        imgMatrix.postScale(factor, factor, pinchMidX, pinchMidY);
                        constrainTranslation();
                        loadImage.setImageMatrix(imgMatrix);
                        updateOverlaysImageBounds();
                        return true;
                    }
                });

        loadImage.setOnTouchListener((v, ev) -> {
            switch (ev.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    savedMatrix.set(imgMatrix);
                    startX = ev.getX();
                    startY = ev.getY();
                    touchMode = Mode.DRAG;
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    if (ev.getPointerCount() >= 2) {
                        pinchStartDist = spacing(ev);
                        midPoint(ev, out -> {
                            pinchMidX = out[0];
                            pinchMidY = out[1];
                        });
                        savedMatrix.set(imgMatrix);
                        touchMode = Mode.ZOOM;
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (touchMode == Mode.DRAG) {
                        imgMatrix.set(savedMatrix);
                        float dx = ev.getX() - startX;
                        float dy = ev.getY() - startY;
                        imgMatrix.postTranslate(dx, dy);
                        constrainTranslation();
                        loadImage.setImageMatrix(imgMatrix);
                        updateOverlaysImageBounds();
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_POINTER_UP:
                    touchMode = Mode.NONE;
                    break;
            }
            if (ev.getPointerCount() >= 2) scaleDetector.onTouchEvent(ev);
            else if (ev.getActionMasked() == MotionEvent.ACTION_MOVE)
                scaleDetector.onTouchEvent(ev);
            return true;
        });

        shapeOverlay.setMaskScale(0.3f);

        lassoCut.setOnClickListener(v -> {
            if (lassoModeOn) {
                lassoModeOn = false;
                lassoCut.setImageResource(R.drawable.icon_rotation_no);
                lassoOverlay.setLassoVisible(false);
            } else {
                lassoModeOn = true;
                lassoCut.setImageResource(R.drawable.icon_rotation_yes);
                shapeCut.setImageResource(R.drawable.icon_rotation_no);
                shapeOverlay.setShape(ShapeType.NONE);
                updateOverlaysImageBounds();
                lassoOverlay.setLassoVisible(true);
                lassoOverlay.getViewTreeObserver().addOnGlobalLayoutListener(
                        new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                lassoOverlay.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                lassoOverlay.beginFullSelection();
                            }
                        });
                visibleOff();
            }
        });

        shapeCut.setOnClickListener(v -> {
            if (shapeModeOn) {
                shapeModeOn = false;
                shapeCut.setImageResource(R.drawable.icon_rotation_no);
                shapeOverlay.setShape(ShapeType.NONE);
                updateShapeIcons(null);
                visibleOff();
            } else {
                shapeModeOn = true;
                shapeCut.setImageResource(R.drawable.icon_rotation_yes);
                lassoCut.setImageResource(R.drawable.icon_rotation_no);
                lassoOverlay.setLassoVisible(false);
                visibleOn();
                shapeOverlay.setShape(ShapeType.NONE);
                updateShapeIcons(null);
            }
        });

        squareCut.setOnClickListener(v -> {
            toggleShape(ShapeType.SQUARE);
        });

        starCut.setOnClickListener(v -> {
            toggleShape(ShapeType.STAR);
        });

        triangleCut.setOnClickListener(v -> {
            toggleShape(ShapeType.TRIANGLE);
        });

        circleCut.setOnClickListener(v -> {
            toggleShape(ShapeType.CIRCLE);
        });

        heartCut.setOnClickListener(v -> {
            toggleShape(ShapeType.HEART);
        });

        cancelBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(500)) return;
            finish();
        });

        checkBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(500)) return;

            setAllControlsEnabled(false);

            //선택한 영역만 png로 잘라서 MyStickersFragment의 myStickers 목록에 추가

            final Bitmap src = getBitmapFromImageView(loadImage);
            final Matrix viewToBmp = new Matrix();
            final Matrix imgMatrix = new Matrix(loadImage.getImageMatrix());
            final boolean lassoVisible = (lassoOverlay.getVisibility() == View.VISIBLE);
            final boolean shapeVisible = (shapeOverlay.getVisibility() == View.VISIBLE);
            final List<Path> lassoShapes = lassoVisible ? new ArrayList<>(lassoOverlay.getShapes()) : new ArrayList<>();
            final boolean hasShape = shapeVisible && shapeOverlay.hasActiveShape();
            final RectF maskRectView = hasShape ? shapeOverlay.getMaskRectOnView() : null;
            final Bitmap maskBmp = hasShape ? shapeOverlay.getMaskBitmapForExport() : null;

            if (src == null || !imgMatrix.invert(viewToBmp) ||
                    (!lassoVisible && !hasShape) ||
                    (lassoVisible && lassoShapes.isEmpty() && !hasShape)) {
                showToast("내 스티커에 저장을 실패했습니다");
                return;
            }

            new Thread(() -> {
                ArrayList<File> created = new ArrayList<>();
                String msg = "내 스티커에 저장을 실패했습니다";

                try {
                    if (lassoVisible && !lassoShapes.isEmpty()) {
                        Bitmap out = cropByPathsUnion(src, lassoShapes, viewToBmp);
                        if (out != null) {
                            File f = savePngToStickers(out, LoadActivity.this);
                            created.add(f);
                            out.recycle();
                        }
                    }
                    if (hasShape && maskBmp != null && maskRectView != null) {
                        Bitmap out = cropByMask(src, maskBmp, maskRectView, viewToBmp);
                        if (out != null) {
                            File f = savePngToStickers(out, LoadActivity.this);
                            created.add(f);
                            out.recycle();
                        }
                    }
                    if (!created.isEmpty()) {
                        for (File f : created) {
                            StickerStore.get().enqueuePending(StickerItem.fromFile(f.getAbsolutePath()));
                        }
                        msg = "내 스티커에 저장을 완료했습니다";
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }

                final String uiMsg = msg;
                runOnUiThread(() -> {
                    showToast(uiMsg);

                    if ("내 스티커에 저장을 완료했습니다".equals(uiMsg)) {
                        getSupportFragmentManager().beginTransaction()
                                .setCustomAnimations(R.anim.slide_up, 0)
                                .replace(R.id.bottomArea2, new MyStickersFragment())
                                .commit();
                    }
                });
            }).start();
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showExitConfirmDialog();
            }
        });
    }

    private void loadImageFromUri(Uri photoUri) {
        if (loadImage == null) {
            return;
        }

        try {
            InputStream inputStream = getContentResolver().openInputStream(photoUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) inputStream.close();
            if (bitmap == null) {
                return;
            }

            InputStream exifInputStream = getContentResolver().openInputStream(photoUri);
            ExifInterface exif = new ExifInterface(exifInputStream);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            float r = 0;
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    r = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    r = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    r = 270;
                    break;
            }

            if (exifInputStream != null) exifInputStream.close();
            if (r != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(r);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            }
            loadImage.setImageBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private RectF getImageDisplayRect(ImageView imageView) {
        if (imageView.getDrawable() == null) return null;

        Matrix matrix = imageView.getImageMatrix();
        RectF drawableRect = new RectF(0, 0,
                imageView.getDrawable().getIntrinsicWidth(),
                imageView.getDrawable().getIntrinsicHeight());
        matrix.mapRect(drawableRect);
        return drawableRect;
    }

    private void visibleOn() {
        squareCut.setVisibility(View.VISIBLE);
        starCut.setVisibility(View.VISIBLE);
        triangleCut.setVisibility(View.VISIBLE);
        circleCut.setVisibility(View.VISIBLE);
        heartCut.setVisibility(View.VISIBLE);
    }

    private void visibleOff() {
        squareCut.setVisibility(View.INVISIBLE);
        starCut.setVisibility(View.INVISIBLE);
        triangleCut.setVisibility(View.INVISIBLE);
        circleCut.setVisibility(View.INVISIBLE);
        heartCut.setVisibility(View.INVISIBLE);

        squareCut.setImageResource(R.drawable.icon_square_no);
        starCut.setImageResource(R.drawable.icon_star_no);
        triangleCut.setImageResource(R.drawable.icon_triangle_no);
        circleCut.setImageResource(R.drawable.icon_circle_no);
        heartCut.setImageResource(R.drawable.icon_heart_no);

        shapeOverlay.setShape(ShapeType.NONE);
    }

    private void updateShapeIcons(ShapeType type) {
        squareCut.setImageResource(type == ShapeType.SQUARE ? R.drawable.icon_square_yes : R.drawable.icon_square_no);
        starCut.setImageResource(type == ShapeType.STAR ? R.drawable.icon_star_yes : R.drawable.icon_star_no);
        triangleCut.setImageResource(type == ShapeType.TRIANGLE ? R.drawable.icon_triangle_yes : R.drawable.icon_triangle_no);
        circleCut.setImageResource(type == ShapeType.CIRCLE ? R.drawable.icon_circle_yes : R.drawable.icon_circle_no);
        heartCut.setImageResource(type == ShapeType.HEART ? R.drawable.icon_heart_yes : R.drawable.icon_heart_no);
    }

    private void toggleShape(ShapeType target) {
        ShapeType cur = shapeOverlay.getCurrentShape();
        if (cur == target) {
            shapeOverlay.setShape(ShapeType.NONE);
            updateShapeIcons(null);
        } else {
            shapeOverlay.setShape(target);
            updateShapeIcons(target);
        }
    }

    private Bitmap getBitmapFromImageView(ImageView iv) {
        if (iv == null || iv.getDrawable() == null) return null;
        if (iv.getDrawable() instanceof android.graphics.drawable.BitmapDrawable) {
            return ((android.graphics.drawable.BitmapDrawable) iv.getDrawable()).getBitmap();
        }
        Bitmap b = Bitmap.createBitmap(
                iv.getDrawable().getIntrinsicWidth(),
                iv.getDrawable().getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888
        );
        Canvas c = new Canvas(b);
        iv.getDrawable().setBounds(0, 0, c.getWidth(), c.getHeight());
        iv.getDrawable().draw(c);
        return b;
    }

    private Bitmap cropByPathsUnion(Bitmap src, List<Path> pathsInView, Matrix viewToBmp) {
        if (src == null || pathsInView == null || pathsInView.isEmpty())
            return null;

        Path unionBmp = new Path();
        boolean first = true;
        for (Path pv : pathsInView) {
            Path p = new Path(pv);
            p.transform(viewToBmp);
            if (first) {
                unionBmp.set(p);
                first = false;
            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
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

    private Bitmap cropByMask(Bitmap src, Bitmap maskBmp, RectF maskRectView, Matrix viewToBmp) {
        if (src == null || maskBmp == null || maskRectView == null) return null;

        RectF maskRectBmp = new RectF(maskRectView);
        viewToBmp.mapRect(maskRectBmp);

        int outW = Math.max(1, Math.round(maskRectBmp.width()));
        int outH = Math.max(1, Math.round(maskRectBmp.height()));

        Bitmap out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        c.translate(-maskRectBmp.left, -maskRectBmp.top);

        c.drawBitmap(src, 0, 0, null);

        Paint pm = new Paint();
        pm.setAntiAlias(false);
        pm.setFilterBitmap(false);
        pm.setDither(false);
        pm.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        c.drawBitmap(maskBmp, null, maskRectBmp, pm);
        pm.setXfermode(null);

        cleanupAlpha(out, 160);

        return out;
    }

    private void cleanupAlpha(Bitmap bmp, int threshold) {
        int w = bmp.getWidth(), h = bmp.getHeight();
        int[] row = new int[w];

        int th = Math.max(0, Math.min(255, threshold));

        for (int y = 0; y < h; y++) {
            bmp.getPixels(row, 0, w, 0, y, w, 1);
            for (int x = 0; x < w; x++) {
                int c = row[x];
                int a = (c >>> 24) & 0xFF;
                if (a == 0) {
                    row[x] = 0;
                } else if (a < th) {
                    row[x] = 0;
                } else {
                    row[x] = (0xFF << 24) | (c & 0x00FFFFFF);
                }
            }
            bmp.setPixels(row, 0, w, 0, y, w, 1);
        }
    }

    private File savePngToStickers(Bitmap bmp, android.content.Context ctx) throws
            IOException {
        File dir = new File(ctx.getFilesDir(), "stickers");
        if (!dir.exists()) dir.mkdirs();
        String name = "sticker_" + System.currentTimeMillis() + ".png";
        File f = new java.io.File(dir, name);
        FileOutputStream fos = new FileOutputStream(f);
        bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
        fos.flush();
        fos.close();
        return f;
    }

    private void showToast(String message) {
        View old = topArea.findViewWithTag("inline_banner");
        if (old != null) topArea.removeView(old);

        TextView tv = new TextView(this);
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

        setAllControlsEnabled(true);

        tv.postDelayed(() -> tv.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> {
                    if (tv.getParent() == topArea) topArea.removeView(tv);
                })
                .start(), 2000);
    }

    private int dp(int dp) {
        return Math.round(getResources().getDisplayMetrics().density * dp);
    }

    private void setAllControlsEnabled(boolean enabled) {
        View[] cuts = new View[]{
                cancelBtn, checkBtn, lassoCut, shapeCut, squareCut, starCut, triangleCut, circleCut, heartCut
        };
        for (View v : cuts) {
            if (v == null) continue;
            v.setEnabled(enabled);
            v.setClickable(enabled);
            v.setAlpha(enabled ? 1f : 0.4f);
        }
    }

    private void showExitConfirmDialog() {
        new FilterEixtDialog(this, new FilterEixtDialog.FilterEixtDialogListener() {
            @Override
            public void onKeep() {
            }

            @Override
            public void onExit() {
                Intent i = new Intent(LoadActivity.this, FilterActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                i.putExtra("EXIT_BY_CHILD", true);
                startActivity(i);
                finish();
            }
        }).withMessage("편집한 내용을 저장하지 않고\n종료하시겠습니까?").withButton1Text("예").withButton2Text("아니오").show();
    }

    private float spacing(MotionEvent e) {
        if (e.getPointerCount() < 2) return 0f;
        float dx = e.getX(0) - e.getX(1);
        float dy = e.getY(0) - e.getY(1);
        return (float) Math.hypot(dx, dy);
    }

    private interface MidOut {
        void onMid(float[] xy);
    }

    private void midPoint(MotionEvent e, MidOut cb) {
        if (e.getPointerCount() < 2) return;
        cb.onMid(new float[]{(e.getX(0) + e.getX(1)) / 2f,
                (e.getY(0) + e.getY(1)) / 2f});
    }

    private void constrainTranslation() {
        if (loadImage.getDrawable() == null) return;

        int vw = loadImage.getWidth();
        int vh = loadImage.getHeight();
        int dw = loadImage.getDrawable().getIntrinsicWidth();
        int dh = loadImage.getDrawable().getIntrinsicHeight();

        imgMatrix.getValues(mvals);
        float curScale = mvals[Matrix.MSCALE_X];
        float clampScale = Math.max(minScale, Math.min(curScale, maxScale));
        if (Math.abs(clampScale - curScale) > 1e-6f) {
            float cx = vw * 0.5f, cy = vh * 0.5f;
            imgMatrix.postScale(clampScale / curScale, clampScale / curScale, cx, cy);
            imgMatrix.getValues(mvals);
        }

        RectF rect = new RectF(0, 0, dw, dh);
        Matrix m = new Matrix(imgMatrix);
        m.mapRect(rect);

        float dx = 0f, dy = 0f;

        if (rect.width() <= vw) {
            dx = vw / 2f - rect.centerX();
        } else {
            if (rect.left > 0) dx = -rect.left;
            else if (rect.right < vw) dx = vw - rect.right;
        }

        if (rect.height() <= vh) {
            dy = vh / 2f - rect.centerY();
        } else {
            if (rect.top > 0) dy = -rect.top;
            else if (rect.bottom < vh) dy = vh - rect.bottom;
        }

        if (dx != 0f || dy != 0f) imgMatrix.postTranslate(dx, dy);
    }

    private void updateOverlaysImageBounds() {
        RectF r = getImageDisplayRect(loadImage);
        if (r != null) {
            shapeOverlay.setImageBounds(r);
            lassoOverlay.setImageBounds(r);
        }
    }
}