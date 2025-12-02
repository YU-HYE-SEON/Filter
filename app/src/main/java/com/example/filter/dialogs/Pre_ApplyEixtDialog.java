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
import com.example.filter.etc.ClickUtils;

public class Pre_ApplyEixtDialog extends Dialog {
    public interface Pre_ApplyEixtDialogListener {
        void onKeep();

        void onExit();
    }

    private Pre_ApplyEixtDialogListener listener;

    public Pre_ApplyEixtDialog(@NonNull Context context, Pre_ApplyEixtDialogListener listener) {
        super(context);
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_exit_pre_apply);

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

        Button keepBtn = findViewById(R.id.keepBtn);
        Button exitBtn = findViewById(R.id.exitBtn);

        ClickUtils.clickDim(keepBtn);
        ClickUtils.clickDim(exitBtn);

        keepBtn.setOnClickListener(v -> {
            dismiss();
            if (listener != null) listener.onKeep();
        });

        exitBtn.setOnClickListener(v -> {
            dismiss();
            if (listener != null) listener.onExit();
        });
    }
}