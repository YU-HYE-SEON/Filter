package com.example.filter.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;

import com.example.filter.R;
import com.example.filter.etc.ClickUtils;

public class SignUpDialog extends Dialog {
    public interface SignUpDialogListener {
        void onClose();

        void onSignUp();

        void onPopUp1();

        void onPopUp2();
    }

    private SignUpDialogListener listener;

    public SignUpDialog(@NonNull Context context, SignUpDialogListener listener) {
        super(context);
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_sign_up);

        setCancelable(true);
        setCanceledOnTouchOutside(true);

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

        AppCompatButton closeBtn = findViewById(R.id.closeBtn);
        AppCompatButton signUpBtn = findViewById(R.id.signUpBtn);
        TextView txt2 = findViewById(R.id.txt2);
        TextView txt4 = findViewById(R.id.txt4);

        ClickUtils.clickDim(closeBtn);
        ClickUtils.clickDim(signUpBtn);

        closeBtn.setOnClickListener(v -> {
            dismiss();
            if (listener != null) listener.onClose();
        });

        signUpBtn.setOnClickListener(v -> {
            dismiss();
            if (listener != null) listener.onSignUp();
        });

        txt2.setOnClickListener(v -> {
            if (listener != null) listener.onPopUp1();
        });

        txt4.setOnClickListener(v -> {
            if (listener != null) listener.onPopUp2();
        });
    }
}