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

public class FilterDeleteDialog extends Dialog {
    public interface FilterDeleteDialogListener {
        void onCancel();

        void onDelete();
    }

    private FilterDeleteDialogListener listener;

    public FilterDeleteDialog(@NonNull Context context, FilterDeleteDialogListener listener) {
        super(context);
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_filter_delete);

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

        Button cancelBtn = findViewById(R.id.cancelBtn);
        Button deleteBtn = findViewById(R.id.deleteBtn);

        cancelBtn.setOnClickListener(v -> {
            dismiss();
            if (listener != null) listener.onCancel();
        });

        deleteBtn.setOnClickListener(v -> {
            dismiss();
            if (listener != null) listener.onDelete();
        });
    }
}