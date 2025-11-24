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

public class FaceStickerDeleteDialog extends Dialog {
    public interface FaceStickerDeleteDialogListener {
        void onNo();

        void onYes();
    }

    private FaceStickerDeleteDialogListener listener;

    public FaceStickerDeleteDialog(@NonNull Context context, FaceStickerDeleteDialogListener listener) {
        super(context);
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_delete_face_sticker);

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

        Button noBtn = findViewById(R.id.noBtn);
        Button yesBtn = findViewById(R.id.yesBtn);

        ClickUtils.clickDim(noBtn);
        ClickUtils.clickDim(yesBtn);

        noBtn.setOnClickListener(v -> {
            dismiss();
            if (listener != null) listener.onNo();
        });

        yesBtn.setOnClickListener(v -> {
            dismiss();
            if (listener != null) listener.onYes();
        });
    }
}