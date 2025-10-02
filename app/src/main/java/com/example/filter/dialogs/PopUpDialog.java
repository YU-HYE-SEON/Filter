package com.example.filter.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.filter.R;

public class PopUpDialog extends Dialog {
    public interface PopUpDialogListener {
        void onClose();
    }

    private PopUpDialogListener listener;
    private CharSequence titleText = null;
    private CharSequence messageText = null;

    public PopUpDialog(@NonNull Context context, PopUpDialogListener listener) {
        super(context);
        this.listener = listener;
    }

    public PopUpDialog withTitle(CharSequence text) {
        this.titleText = text;
        return this;
    }

    public PopUpDialog withMessage(CharSequence text) {
        this.messageText = text;
        return this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_pop_up);

        setCancelable(true);
        setCanceledOnTouchOutside(true);

        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(getWindow().getAttributes());

        lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.dimAmount = 0.4f;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        getWindow().setAttributes(lp);

        Button btn = findViewById(R.id.btn);
        TextView title = findViewById(R.id.title);
        TextView message = findViewById(R.id.message);

        if (title != null) title.setText(titleText);
        if (message != null) message.setText(messageText);

        btn.setOnClickListener(v -> {
            dismiss();
            if (listener != null) listener.onClose();
        });
    }
}