package com.example.filter.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.filter.R;

public class SearchFragment extends Fragment {
    private RecyclerView recyclerView;
    private TextView textView;
    private static final String ARG_KEYWORD = "search_keyword";
    private String keyword;

    public static SearchFragment newInstance(String keyword) {
        SearchFragment fragment = new SearchFragment();
        Bundle args = new Bundle();
        args.putString(ARG_KEYWORD, keyword);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            keyword = getArguments().getString(ARG_KEYWORD);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f_search, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        textView = view.findViewById(R.id.textView);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (keyword == null || keyword.isEmpty()) {
            textView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            textView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}
