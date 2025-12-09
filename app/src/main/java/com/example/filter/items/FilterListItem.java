package com.example.filter.items;

import com.example.filter.api_datas.request_dto.FilterDtoCreateRequest;
import com.example.filter.api_datas.FaceStickerData;
import com.example.filter.api_datas.response_dto.FilterListResponse;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.logging.Filter;

public class FilterListItem implements Serializable {
    public Long reviewId;

    public final Long id; // db의 pk
    public final String filterTitle;
    public final String thumbmailUrl;
    public final String nickname;

    public final Long useCount; // 사용수
    // public final boolean usage; // 현재 로그인한 유저가 구매/사용 했는지 여부 (price가 0보다 크고 usage가 true라면 구매완료 표기하면 됨)
    public final PriceDisplayEnum type;
    public final int price;
    public final boolean bookmark; // 현재 로그인한 유저가 북마크 했는지 여부

    public FilterListItem() {
        this.id = -1L;
        this.filterTitle = "";
        this.thumbmailUrl = "";
        this.nickname = "";
        this.price = -1;
        this.useCount = -1L;
        this.type = PriceDisplayEnum.NONE;
        this.bookmark = false;
    }

    public FilterListItem(Long reviewId, Long id, String filterTitle,
                          String thumbmailUrl, String nickname, int price,
                          Long useCount, PriceDisplayEnum type, boolean bookmark) {
        this.reviewId = reviewId; // reviewId
        this.id = id;             // filterId
        this.filterTitle = filterTitle;
        this.thumbmailUrl = thumbmailUrl;
        this.nickname = nickname;
        this.price = price;
        this.useCount = useCount;
        this.type = type;
        this.bookmark = bookmark;
    }


    // 생성자 정의
    public FilterListItem(Long id, String filterTitle, String thumbmailUrl,
                          String nickname, int price, Long useCount,
                          PriceDisplayEnum type, boolean bookmark) {
        this.id = id;
        this.filterTitle = filterTitle;
        this.thumbmailUrl = thumbmailUrl;
        this.nickname = nickname;
        this.price = price;
        this.useCount = useCount;
        this.type = type;
        this.bookmark = bookmark;
    }

    public static FilterListItem convertFromDto(FilterListResponse response) {
        // type 변환
        PriceDisplayEnum displayType = PriceDisplayEnum.fromString(response.priceDisplayType);

        FilterListItem item = new FilterListItem(
                response.id, response.name, response.thumbnailUrl,
                response.creator, response.pricePoint, response.useCount,
                displayType, response.bookmark);
        return item;
    }
}