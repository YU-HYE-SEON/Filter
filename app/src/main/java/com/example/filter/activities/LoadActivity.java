package com.example.filter.activities;

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
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.constraintlayout.widget.ConstraintLayout;

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

        Uri photoUri = getIntent().getData();
        if (photoUri != null) {
            loadImageFromUri(photoUri);
        } else {
            finish();
            return;
        }

        loadImage.post(() -> {
            RectF imgRect = getImageDisplayRect(loadImage);
            if (imgRect != null) {
                shapeOverlay.setImageBounds(imgRect);
                lassoOverlay.setImageBounds(imgRect);
            }
        });

        shapeOverlay.setMaskScale(0.3f);

        lassoCut.setOnClickListener(v -> {
            lassoCut.setImageResource(R.drawable.rotation_icon_yes);
            shapeCut.setImageResource(R.drawable.rotation_icon_no);

            shapeOverlay.setShape(ShapeType.NONE);
            lassoOverlay.setLassoVisible(true);

            lassoOverlay.beginFullSelection();

            visibleOff();
        });

        shapeCut.setOnClickListener(v -> {
            shapeCut.setImageResource(R.drawable.rotation_icon_yes);
            lassoCut.setImageResource(R.drawable.rotation_icon_no);

            lassoOverlay.setLassoVisible(false);

            visibleOn();
        });

        squareCut.setOnClickListener(v -> {
            shapeOverlay.setShape(ShapeType.SQUARE);
            updateShapeIcons("square");
        });

        starCut.setOnClickListener(v -> {
            shapeOverlay.setShape(ShapeType.STAR);
            updateShapeIcons("star");
        });

        triangleCut.setOnClickListener(v -> {
            shapeOverlay.setShape(ShapeType.TRIANGLE);
            updateShapeIcons("triangle");
        });

        circleCut.setOnClickListener(v -> {
            shapeOverlay.setShape(ShapeType.CIRCLE);
            updateShapeIcons("circle");
        });

        heartCut.setOnClickListener(v -> {
            shapeOverlay.setShape(ShapeType.HEART);
            updateShapeIcons("heart");
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

        squareCut.setImageResource(R.drawable.square_no);
        starCut.setImageResource(R.drawable.star_no);
        triangleCut.setImageResource(R.drawable.triangle_no);
        circleCut.setImageResource(R.drawable.circle_no);
        heartCut.setImageResource(R.drawable.heart_no);

        shapeOverlay.setShape(ShapeType.NONE);
    }

    private void updateShapeIcons(String shape) {
        squareCut.setImageResource("square".equals(shape) ? R.drawable.square_yes : R.drawable.square_no);
        starCut.setImageResource("star".equals(shape) ? R.drawable.star_yes : R.drawable.star_no);
        triangleCut.setImageResource("triangle".equals(shape) ? R.drawable.triangle_yes : R.drawable.triangle_no);
        circleCut.setImageResource("circle".equals(shape) ? R.drawable.circle_yes : R.drawable.circle_no);
        heartCut.setImageResource("heart".equals(shape) ? R.drawable.heart_yes : R.drawable.heart_no);
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

    private Bitmap cropByMask(Bitmap src, Bitmap maskBmp, RectF
            maskRectView, Matrix
                                      viewToBmp) {
        if (src == null || maskBmp == null || maskRectView == null) return null;

        RectF maskRectBmp = new RectF(maskRectView);
        viewToBmp.mapRect(maskRectBmp);

        int outW = Math.max(1, Math.round(maskRectBmp.width()));
        int outH = Math.max(1, Math.round(maskRectBmp.height()));

        Bitmap out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        c.translate(-maskRectBmp.left, -maskRectBmp.top);

        c.drawBitmap(src, 0, 0, null);

        Paint pm = new Paint(Paint.ANTI_ALIAS_FLAG);
        pm.setFilterBitmap(true);
        pm.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        c.drawBitmap(maskBmp, null, maskRectBmp, pm);
        pm.setXfermode(null);

        return out;
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
        tv.setTextSize(14);
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
}