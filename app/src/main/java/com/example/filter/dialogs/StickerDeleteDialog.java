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

public class StickerDeleteDialog extends Dialog {
    public interface StickerDeleteDialogListener {
        void onKeep();

        void onDelete();
    }

    private StickerDeleteDialogListener listener;
    private CharSequence messageText = null;
    private CharSequence button1Text = null;
    private CharSequence button2Text = null;

    public StickerDeleteDialog(@NonNull Context context, StickerDeleteDialogListener listener) {
        super(context);
        this.listener = listener;
    }

    public StickerDeleteDialog withMessage(CharSequence text) {
        this.messageText = text;
        return this;
    }

    public StickerDeleteDialog withButton1Text(CharSequence text) {
        this.button1Text = text;
        return this;
    }

    public StickerDeleteDialog withButton2Text(CharSequence text) {
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
        lp.dimAmount = 0.7f;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        getWindow().setAttributes(lp);

        TextView textView = findViewById(R.id.message);
        Button deleteBtn = findViewById(R.id.button1);
        Button keepBtn = findViewById(R.id.button2);

        if (textView != null && messageText != null) textView.setText(messageText);
        if (deleteBtn != null && button1Text != null) deleteBtn.setText(button1Text);
        if (keepBtn != null && button2Text != null) keepBtn.setText(button2Text);

        keepBtn.setOnClickListener(v -> {
            dismiss();
            if (listener != null) listener.onKeep();
        });

        deleteBtn.setOnClickListener(v -> {
            dismiss();
            if (listener != null) listener.onDelete();
        });
    }
}