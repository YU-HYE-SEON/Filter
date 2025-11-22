package com.example.filter.items;

import com.example.filter.api_datas.request_dto.FilterDtoCreateRequest;
import com.example.filter.api_datas.FaceStickerData;
import com.example.filter.api_datas.response_dto.FilterListResponse;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.logging.Filter;

public class FilterListItem implements Serializable {
    public final Long id; // db의 pk
    public final String filterTitle;
    public final String thumbmailUrl;
    public final String nickname;

    public final int price;

    public final Long useCount; // 사용수
    public final boolean usage; // 현재 로그인한 유저가 구매/사용 했는지 여부 (price가 0보다 크고 usage가 true라면 구매완료 표기하면 됨)
    public final boolean bookmark; // 현재 로그인한 유저가 북마크 했는지 여부

    // 생성자 정의
    public FilterListItem(long id, String filterTitle, String thumbmailUrl, String nickname, int price, long useCount, boolean usage, boolean bookmark) {
        this.id = id;
        this.filterTitle = filterTitle;
        this.thumbmailUrl = thumbmailUrl;
        this.nickname = nickname;
        this.price = price;
        this.useCount = useCount;
        this.usage = usage;
        this.bookmark = bookmark;
    }

    public FilterListItem convertFromDto(FilterListResponse response) {
        FilterListItem item = new FilterListItem(
                response.id, response.name, response.thumbnailUrl,
                response.creator, response.pricePoint, response.useCount,
                response.usage, response.bookmark);
        return item;
    }

}