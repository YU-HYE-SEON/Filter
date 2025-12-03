package com.example.filter.api_datas.response_dto;

import java.util.List;

public class PageResponse<T> {
    // response json에서 content라는 이름의 리스트로 담겨옴
    public List<T> content;

    public List<T> getContent() {
        return content;
    }

    // 페이징 메타데이터
    public int totalPages;
    public long totalElements;
    public int size;
    public int number; // 현재 페이지 번호 (0부터 시작함)
    public boolean last; // 마지막 페이지 여부
    public boolean first; // 첫 페이지 여부
    public boolean empty; // 데이터 없음 여부
}
