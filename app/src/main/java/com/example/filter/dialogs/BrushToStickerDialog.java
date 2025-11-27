package com.example.filter.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.Button;

import androidx.annotation.NonNull;

import com.example.filter.R;

public class BrushToStickerDialog extends Dialog {
    public interface BrushToStickerDialogListener {
        void onCancel();

        void onAdd();
    }

    private BrushToStickerDialogListener listener;

    public BrushToStickerDialog(@NonNull Context context, BrushToStickerDialogListener listener) {
        super(context);
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_brush_to_sticker);

        setCancelable(false);
        setCanceledOnTouchOutside(false);

        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(getWindow().getAttributes());

        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        int dialogWidth = displayMetrics.widthPixels;
        lp.width = dialogWidth;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.dimAmount = 0.7f;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        getWindow().setAttributes(lp);

        Button cancelBtn = findViewById(R.id.cancelBtn);
        Button addBtn = findViewById(R.id.addBtn);

        cancelBtn.setOnClickListener(v -> {
            dismiss();
            if (listener != null) listener.onCancel();
        });

        addBtn.setOnClickListener(v -> {
            dismiss();
            if (listener != null) listener.onAdd();
        });
    }
}