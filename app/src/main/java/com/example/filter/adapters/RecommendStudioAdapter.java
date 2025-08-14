package com.example.filter.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.filter.R;

import java.util.Arrays;
import java.util.List;

public class RecommendStudioAdapter extends RecyclerView.Adapter<RecommendStudioAdapter.StudioViewHolder> {
    private List<String> studioTitles;

    public RecommendStudioAdapter(List<String> studioTitles) {
        this.studioTitles = studioTitles;
    }

    @NonNull
    @Override
    public StudioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_studio, parent, false);
        return new StudioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudioViewHolder holder, int position) {
        String studioTitle = studioTitles.get(position);

        int imageRes = Color.parseColor("#BDBDBD");
        String intro = "스튜디오 소개";
        List<Integer> previewList = Arrays.asList(Color.parseColor("#BDBDBD"));

        switch (studioTitle) {
            case "취향저격 상점":
                imageRes = R.drawable.liking;
                intro = "내 취향에 정확히 맞는, 무...";
                previewList = Arrays.asList(R.drawable.liking1, R.drawable.liking2, R.drawable.liking3, R.drawable.liking4);
                break;
            case "파랑주의보":
                imageRes = R.drawable.blue_attention;
                intro = "감정이 푸르게 물드는 순간";
                previewList = Arrays.asList(R.drawable.blue_attention1, R.drawable.blue_attention2);
                break;
            case "오늘은 이 색깔":
                imageRes = R.drawable.today_color;
                intro = "오늘의 기분색상";
                previewList = Arrays.asList(R.drawable.today_color1);
                break;
            case "Zan#":
                imageRes = R.drawable.zan;
                intro = "잔잔바리샵";
                previewList = Arrays.asList(R.drawable.zan1);
                break;
            case "별사탕":
                imageRes = R.drawable.star_candy;
                intro = "귀여움 :)";
                previewList = Arrays.asList(R.drawable.star_candy1, R.drawable.star_candy2, R.drawable.star_candy3);
                break;
            case "Sticky":
                imageRes = R.drawable.sticky;
                intro = "페이스 필터만";
                previewList = Arrays.asList(R.drawable.sticky1, R.drawable.sticky2, R.drawable.sticky3);
                break;
            case "코랄선셋":
                imageRes = R.drawable.coral_sunset;
                intro = "코랄빛";
                previewList = Arrays.asList(R.drawable.coral_sunset1, R.drawable.coral_sunset2);
                break;
            case "오늘의 감정관":
                imageRes = R.drawable.today_mood;
                intro = "오늘의 감정";
                previewList = Arrays.asList(R.drawable.today_mood1, R.drawable.today_mood2);
                break;
            case "슈가코팅":
                imageRes = R.drawable.sugarcoating;
                intro = "설탕이없으면안돼";
                previewList = Arrays.asList(R.drawable.sugarcoating1, R.drawable.sugarcoating2, R.drawable.sugarcoating3, R.drawable.sugarcoating4);
                break;
            case "SOON":
                imageRes = R.drawable.soon;
                intro = "내가 사랑하는 일상과 순간";
                previewList = Arrays.asList(R.drawable.soon1, R.drawable.soon2, R.drawable.soon3);
                break;
            case "레트로콜라":
                imageRes = R.drawable.retro_cola;
                intro = "레트로한 무드";
                previewList = Arrays.asList(R.drawable.retro_cola1, R.drawable.retro_cola2, R.drawable.retro_cola3);
                break;
        }

        holder.studioImage.setImageResource(imageRes);
        holder.studioTitle.setText(studioTitle);
        holder.studioIntro.setText(intro);

        holder.studioImgPreview.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));
        StudioItemPreviewAdapter sipAdapter = new StudioItemPreviewAdapter(previewList);
        holder.studioImgPreview.setAdapter(sipAdapter);
        holder.studioImgPreview.setNestedScrollingEnabled(false);
    }

    @Override
    public int getItemCount() {
        return studioTitles.size();
    }

    public static class StudioViewHolder extends RecyclerView.ViewHolder {
        ImageView studioImage;
        TextView studioTitle;
        TextView studioIntro;
        RecyclerView studioImgPreview;

        public StudioViewHolder(@NonNull View itemView) {
            super(itemView);
            studioImage = itemView.findViewById(R.id.studioImage);
            studioTitle = itemView.findViewById(R.id.studioTitle);
            studioIntro = itemView.findViewById(R.id.studioIntro);
            studioImgPreview = itemView.findViewById(R.id.studioImgPreview);
        }
    }
}
