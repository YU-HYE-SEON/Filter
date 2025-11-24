package com.example.filter.fragments.archives;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.filter.R;
import com.example.filter.activities.MainActivity;
import com.example.filter.adapters.FilterListAdapter;
import com.example.filter.etc.GridSpaceItemDecoration;

public class ArchiveFragment extends Fragment {
    private ImageButton bookmark, buy, create, review;
    private RecyclerView recyclerView;
    private FilterListAdapter adapter;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f_archive, container, false);

        bookmark = view.findViewById(R.id.bookmark);
        buy = view.findViewById(R.id.buy);
        create = view.findViewById(R.id.create);
        review = view.findViewById(R.id.review);
        recyclerView = view.findViewById(R.id.recyclerView);

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int id = view.getId();
                switch (id) {
                    case R.id.bookmark:
                        setArchiveButtons(true, false, false, false);
                        break;
                    case R.id.buy:
                        setArchiveButtons(false, true, false, false);

                        break;
                    case R.id.create:
                        setArchiveButtons(false, false, true, false);
                        break;
                    case R.id.review:
                        setArchiveButtons(false, false, false, true);
                        break;
                }
            }
        };

        bookmark.setOnClickListener(listener);
        buy.setOnClickListener(listener);
        create.setOnClickListener(listener);
        review.setOnClickListener(listener);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        StaggeredGridLayoutManager sglm = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        sglm.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
        recyclerView.setLayoutManager(sglm);

        adapter = new FilterListAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new GridSpaceItemDecoration(2, dp(12), dp(18)));

        MainActivity activity = (MainActivity) requireActivity();

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
    }

    private void setArchiveButtons(boolean bm, boolean b, boolean c, boolean r) {
        bookmark.setBackgroundResource(bm ? R.drawable.btn_bookmark_yes : R.drawable.btn_bookmark_no);
        buy.setBackgroundResource(b ? R.drawable.btn_buy_yes : R.drawable.btn_buy_no);
        create.setBackgroundResource(c ? R.drawable.btn_create_yes : R.drawable.btn_create_no);
        review.setBackgroundResource(r ? R.drawable.btn_review_yes : R.drawable.btn_review_no);
    }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        requireActivity().findViewById(R.id.archiveFrame).setVisibility(View.GONE);
        requireActivity().findViewById(R.id.mainActivity).setVisibility(View.VISIBLE);
    }
}