package com.example.filter.items;

public enum PriceDisplayEnum {
    NONE,
    PURCHASED,
    NUMBER;

    public static PriceDisplayEnum fromString(String value) {
        for (PriceDisplayEnum type : PriceDisplayEnum.values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return NONE; // 기본값
    }
}
