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
        setContentView(R.layout.d_stickers);

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

        Button keepBtn = findViewById(R.id.keepBtn);
        Button changeBtn = findViewById(R.id.changeBtn);

        keepBtn.setOnClickListener(v -> {
            dismiss();
            listener.onKeep();
        });

        changeBtn.setOnClickListener(v -> {
            dismiss();
            listener.onChange();
        });
    }
}
