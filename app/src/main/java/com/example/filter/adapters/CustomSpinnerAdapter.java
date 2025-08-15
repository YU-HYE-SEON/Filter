package com.example.filter.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.filter.R;

import java.util.List;

public class CustomSpinnerAdapter extends ArrayAdapter<String> {
    private final LayoutInflater inflater;
    private int selectedIndex = -1;

    public CustomSpinnerAdapter(@NonNull Context context, @NonNull List<String> items) {
        super(context, 0, items);
        inflater = LayoutInflater.from(context);
    }

    public void setSelectedIndex(int index) {
        this.selectedIndex = index;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.s_item, parent, false);
        }

        TextView spinnerText = convertView.findViewById(R.id.spinnerText);
        spinnerText.setText(getItem(position));

        return convertView;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.s_dropdown_item, parent, false);
        }

        TextView dropdownText = convertView.findViewById(R.id.dropdownText);
        ImageView checkIcon = convertView.findViewById(R.id.checkIcon);

        dropdownText.setText(getItem(position));

        if (position == selectedIndex) {
            checkIcon.setVisibility(View.VISIBLE);
        } else {
            checkIcon.setVisibility(View.GONE);
        }

        return convertView;
    }
}