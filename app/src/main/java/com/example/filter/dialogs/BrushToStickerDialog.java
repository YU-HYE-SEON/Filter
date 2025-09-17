package com.example.filter.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.filter.R;

public class BrushToStickerDialog extends Dialog {
    public interface BrushToStickerDialogListener {
        void onYes();

        void onNo();
    }

    private BrushToStickerDialogListener listener;
    private CharSequence messageText = null;
    private CharSequence button1Text = null;
    private CharSequence button2Text = null;

    public BrushToStickerDialog(@NonNull Context context, BrushToStickerDialogListener listener) {
        super(context);
        this.listener = listener;
    }

    public BrushToStickerDialog withMessage(CharSequence text) {
        this.messageText = text;
        return this;
    }

    public BrushToStickerDialog withButton1Text(CharSequence text) {
        this.button1Text = text;
        return this;
    }

    public BrushToStickerDialog withButton2Text(CharSequence text) {
        this.button2Text = text;
        return this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog);

        setCancelable(false);
        setCanceledOnTouchOutside(false);

        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(getWindow().getAttributes());

        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        int dialogWidth = displayMetrics.widthPixels;
        lp.width = dialogWidth;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.dimAmount = 0.4f;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        getWindow().setAttributes(lp);

        TextView textView = findViewById(R.id.message);
        Button yesBtn = findViewById(R.id.button1);
        Button noBtn = findViewById(R.id.button2);

        if (textView != null && messageText != null) textView.setText(messageText);
        if (yesBtn != null && button1Text != null) yesBtn.setText(button1Text);
        if (noBtn != null && button2Text != null) noBtn.setText(button2Text);

        yesBtn.setOnClickListener(v -> {
            dismiss();
            if(listener != null) listener.onYes();
        });

        noBtn.setOnClickListener(v -> {
            dismiss();
            if (listener != null) listener.onNo();
        });
    }
}