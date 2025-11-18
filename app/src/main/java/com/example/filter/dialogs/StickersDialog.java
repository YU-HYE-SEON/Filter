package com.example.filter.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;

import com.example.filter.R;
import com.example.filter.etc.ClickUtils;

public class StickersDialog extends Dialog {
    public interface StickersDialogListener {
        void onKeep();

        void onChange();
    }

    private StickersDialogListener listener;

    public StickersDialog(@NonNull Context context, StickersDialogListener listener) {
        super(context);
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_sticker);

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

        AppCompatButton changeBtn = findViewById(R.id.changeBtn);
        AppCompatButton keepBtn = findViewById(R.id.keepBtn);

        ClickUtils.clickDim(keepBtn);
        ClickUtils.clickDim(changeBtn);

        keepBtn.setOnClickListener(v -> {
            dismiss();
            if (listener != null) listener.onKeep();
        });

        changeBtn.setOnClickListener(v -> {
            dismiss();
            if (listener != null) listener.onChange();
        });
    }
}