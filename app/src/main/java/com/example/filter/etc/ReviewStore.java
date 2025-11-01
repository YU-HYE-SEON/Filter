package com.example.filter.etc;

import android.app.Application;

import com.example.filter.items.ReviewItem;

import java.util.*;

public class ReviewStore extends Application {
    private static final Map<String, List<ReviewItem>> reviewMap = new HashMap<>();

    public static List<ReviewItem> getReviews(String key) {
        return reviewMap.computeIfAbsent(key, k -> new ArrayList<>());
    }

    public static void addReview(String key, ReviewItem item) {
        List<ReviewItem> list = reviewMap.computeIfAbsent(key, k -> new ArrayList<>());
        list.add(0, item);
    }

    public static void removeReview(String key, String imageUrl) {
        List<ReviewItem> list = reviewMap.get(key);
        if (list == null) return;

        Iterator<ReviewItem> iterator = list.iterator();
        while (iterator.hasNext()) {
            ReviewItem item = iterator.next();
            if (item.imageUrl.equals(imageUrl)) {
                iterator.remove();
                break;
            }
        }
    }
}