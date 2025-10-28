package com.example.filter.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.filter.R;

public class SearchMainFragment extends Fragment {
    private EditText searchTxt;
    private ImageButton searchBtn;
    private ConstraintLayout searchMain, searchHistory;
    private TextView keywordTxt;
    private TextView keyword1, keyword2, keyword3, keyword4, keyword5, keyword6, keyword7, keyword8, keyword9, keyword10;
    private FrameLayout searchFrame;
    private boolean maybeTap = false;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f_search_main, container, false);

        searchTxt = view.findViewById(R.id.searchTxt);
        searchBtn = view.findViewById(R.id.searchBtn);
        searchHistory = view.findViewById(R.id.searchHistory);
        keywordTxt = view.findViewById(R.id.keywordTxt);
        keyword1 = view.findViewById(R.id.keyword1);
        keyword2 = view.findViewById(R.id.keyword2);
        keyword3 = view.findViewById(R.id.keyword3);
        keyword4 = view.findViewById(R.id.keyword4);
        keyword5 = view.findViewById(R.id.keyword5);
        keyword6 = view.findViewById(R.id.keyword6);
        keyword7 = view.findViewById(R.id.keyword7);
        keyword8 = view.findViewById(R.id.keyword8);
        keyword9 = view.findViewById(R.id.keyword9);
        keyword10 = view.findViewById(R.id.keyword10);
        searchMain = view.findViewById(R.id.searchMain);
        searchFrame = view.findViewById(R.id.searchFrame);

        View root = requireActivity().findViewById(android.R.id.content);
        root.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            root.getWindowVisibleDisplayFrame(r);
            int screenHeight = root.getRootView().getHeight();
            int keypadHeight = screenHeight - r.bottom;
            boolean keypadVisible = keypadHeight > screenHeight * 0.15;
            if (!keypadVisible) searchTxt.clearFocus();
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getChildFragmentManager().getBackStackEntryCount() > 0) {
                    getChildFragmentManager().popBackStack();
                } else {
                    setEnabled(false);
                    requireActivity().onBackPressed();
                }
            }
        });

        getChildFragmentManager().addOnBackStackChangedListener(() -> {
            if (getChildFragmentManager().getBackStackEntryCount() == 0) {
                goBackToMainSearch();
            }
        });

        searchBtn.setOnClickListener(v -> handleSearch());

        searchTxt.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                handleSearch();
                return true;
            }
            return false;
        });

        searchHistory.setOnClickListener(v -> {
            String keyword = keywordTxt.getText().toString();
            navigateToSearch(keyword);
        });

        View.OnClickListener keywordClickListener = v -> {
            TextView clickedTextView = (TextView) v;
            String keyword = clickedTextView.getText().toString();
            navigateToSearch(keyword);
        };

        keyword1.setOnClickListener(keywordClickListener);
        keyword2.setOnClickListener(keywordClickListener);
        keyword3.setOnClickListener(keywordClickListener);
        keyword4.setOnClickListener(keywordClickListener);
        keyword5.setOnClickListener(keywordClickListener);
        keyword6.setOnClickListener(keywordClickListener);
        keyword7.setOnClickListener(keywordClickListener);
        keyword8.setOnClickListener(keywordClickListener);
        keyword9.setOnClickListener(keywordClickListener);
        keyword10.setOnClickListener(keywordClickListener);
    }

    private void handleSearch() {
        String keyword = searchTxt.getText().toString();
        navigateToSearch(keyword);
    }

    private void navigateToSearch(String keyword) {
        hideKeypadAndClearFocus();

        searchTxt.setText(keyword);
        searchTxt.setSelection(keyword.length());

        if (searchMain != null) {
            searchMain.setVisibility(View.GONE);
        }
        if (searchFrame != null) {
            searchFrame.setVisibility(View.VISIBLE);
        }

        if (getChildFragmentManager().getBackStackEntryCount() > 0) {
            getChildFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }

        SearchFragment searchFragment = SearchFragment.newInstance(keyword);

        getChildFragmentManager().beginTransaction()
                .replace(R.id.searchFrame, searchFragment)
                .addToBackStack(null)
                .commit();
    }

    private void goBackToMainSearch() {
        searchTxt.setText(null);
        if (searchMain != null) {
            searchMain.setVisibility(View.VISIBLE);
        }
        if (searchFrame != null) {
            searchFrame.setVisibility(View.GONE);
        }
        hideKeypadAndClearFocus();
    }

    private boolean isPoint(MotionEvent ev) {
        if (searchTxt == null) return false;
        Rect r = new Rect();
        boolean visible = searchTxt.getGlobalVisibleRect(r);
        if (!visible) return false;
        final int x = (int) ev.getRawX();
        final int y = (int) ev.getRawY();
        return r.contains(x, y);
    }

    private void hideKeypadAndClearFocus() {
        View v = requireActivity().getCurrentFocus();
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && v != null) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
        if (searchTxt != null) searchTxt.clearFocus();
    }

    public void onParentTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                maybeTap = true;
                break;
            }
            case MotionEvent.ACTION_UP: {
                if (maybeTap) {
                    View focused = requireActivity().getCurrentFocus();
                    boolean focusedIsEdit = focused instanceof EditText;
                    boolean tapInsideEdit = isPoint(ev);

                    if (focusedIsEdit && !tapInsideEdit) {
                        hideKeypadAndClearFocus();
                    }
                }
                break;
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        requireActivity().findViewById(R.id.frameLayout).setVisibility(View.GONE);
        requireActivity().findViewById(R.id.mainActivity).setVisibility(View.VISIBLE);
    }
}