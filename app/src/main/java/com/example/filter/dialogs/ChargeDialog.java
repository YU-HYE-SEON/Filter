package com.example.filter.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;

import com.example.filter.R;
import com.example.filter.etc.ClickUtils;

public class ChargeDialog extends Dialog {
    public interface ChargeDialogListener {
        void onOK();
    }

    private ChargeDialogListener listener;
    private TextView message1, message2;
    private AppCompatButton okBtn;
    private Boolean isSuccess = null;

    public ChargeDialog(@NonNull Context context, ChargeDialogListener listener) {
        super(context);
        this.listener = listener;
    }

    public void setMessage(boolean isSuccess) {
        this.isSuccess = isSuccess;

        if (message1 != null && message2 != null) {
            applyMessage();
        }
    }

    private void applyMessage() {
        if (isSuccess == null) return;

        if (isSuccess) {
            message1.setText("충전에 성공했습니다");
            message2.setVisibility(View.GONE);
        } else {
            message1.setText("충전에 실패했습니다");
            message2.setVisibility(View.VISIBLE);
            message2.setText("다시 시도해주세요");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_charge);

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

        message1 = findViewById(R.id.message1);
        message2 = findViewById(R.id.message2);
        okBtn = findViewById(R.id.okBtn);

        ClickUtils.clickDim(okBtn);
        okBtn.setOnClickListener(v -> {
            dismiss();
            if (isSuccess) {
                if (listener != null) listener.onOK();
            }
        });

        applyMessage();
    }
}