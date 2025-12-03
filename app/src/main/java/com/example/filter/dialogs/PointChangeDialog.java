/*
package com.example.filter.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.filter.R;

public class PointChangeDialog extends Dialog {
    public interface PointChangeDialogListener {
        void onChange(int oldPrice, int newPrice);
    }

    private PointChangeDialogListener listener;
    private String filterTitle;
    private int currentPrice;

    public PointChangeDialog(@NonNull Context context, String filterTitle, int currentPrice, PointChangeDialogListener listener) {
        super(context);
        this.filterTitle = filterTitle;
        this.currentPrice = currentPrice;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.m_point_change);

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

        TextView filterTitle = findViewById(R.id.message);
        TextView price1 = findViewById(R.id.price1);
        EditText price2 = findViewById(R.id.price2);
        TextView txt2 = findViewById(R.id.txt2);
        Button btn = findViewById(R.id.btn);

        btn.setBackgroundColor(Color.TRANSPARENT);
        btn.setFocusable(true);
        btn.setClickable(true);

        if (filterTitle != null) filterTitle.setText(this.filterTitle);
        if (price1 != null) price1.setText(String.valueOf(currentPrice));
        if (price2 != null) price2.setText(String.valueOf(currentPrice));
        price2.setSelection(price2.getText().length());

        updateChangeRate(currentPrice, currentPrice, txt2);

        price2.addTextChangedListener(new TextWatcher() {
            private boolean selfChange = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (selfChange) return;

                String newPriceStr = s.toString().trim();
                int newPrice = 0;

                if (newPriceStr.length() > 1 && newPriceStr.startsWith("0")) {
                    String newText = newPriceStr.replaceFirst("^0+(?=\\d)", "");
                    if (newText.isEmpty()) newText = "0";
                    selfChange = true;
                    s.replace(0, s.length(), newText);
                    price2.setSelection(newText.length());
                    selfChange = false;
                    newPriceStr = newText;
                }


                if (newPriceStr.isEmpty()) {
                    txt2.setText("가격을 입력해주세요.");
                    btn.setEnabled(false);
                    btn.setAlpha(0.4f);
                    return;
                } else {
                    try {
                        newPrice = Integer.parseInt(newPriceStr);
                        if (newPrice < 0 || newPrice > 300 || (newPrice > 0 && newPrice % 10 != 0)) {
                            txt2.setText("0~300P까지 10P단위로만\n입력 가능합니다");
                            btn.setEnabled(false);
                            btn.setAlpha(0.4f);
                            return;
                        } else {
                            btn.setEnabled(true);
                            btn.setAlpha(1.0f);
                            updateChangeRate(currentPrice, newPrice, txt2);
                        }
                    } catch (NumberFormatException e) {
                        txt2.setText("0~300P까지 10P단위로만\n입력 가능합니다");
                        btn.setEnabled(false);
                        btn.setAlpha(0.4f);
                        return;
                    }
                }

            }
        });

        btn.setOnClickListener(v -> {
            String newPriceStr = price2.getText().toString().trim();
            if (newPriceStr.isEmpty()) {
                txt2.setText("가격을 입력해주세요.");
                return;
            }

            int newPrice = 0;
            newPrice = Integer.parseInt(newPriceStr);

            if (newPrice < 0 || newPrice > 300 || (newPrice > 0 && newPrice % 10 != 0)) {
                return;
            }

            dismiss();
            if (listener != null) listener.onChange(currentPrice, newPrice);
        });
    }

    private void updateChangeRate(int oldPrice, int newPrice, TextView txt) {
        if (oldPrice == newPrice) {
            txt.setText("가격 변동 없음");
        } else if (newPrice == 0) {
            if (oldPrice > 0) {
                txt.setText("가격을 무료로 전환했습니다.");
            } else {
                txt.setText("가격 변동 없음");
            }
        } else if (oldPrice == 0) {
            if (newPrice > 0) {
                txt.setText("가격을 새로 설정했습니다.");
            } else {
                txt.setText("가격 변동 없음");
            }
        } else {
            float change = (float) (newPrice - oldPrice);
            float rate = (change / oldPrice) * 100;
            int rateAbs = Math.round(Math.abs(rate));

            if (change > 0) {
                txt.setText("가격을 " + rateAbs + "% 올렸습니다.");
            } else {
                txt.setText("가격을 " + rateAbs + "% 낮췄습니다.");
            }
        }
    }
}*/
