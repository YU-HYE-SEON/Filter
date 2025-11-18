package com.example.filter.etc;

import android.content.Context;
import android.content.SharedPreferences;

/// 사용자가 나인지 타인인지 구분하기 위한 테스트용 임시 클래스 ///
public class UserManager {
    private static UserManager instance;
    private SharedPreferences sp;

    private UserManager(Context ctx) {
        sp = ctx.getSharedPreferences("User", Context.MODE_PRIVATE);
    }

    public static UserManager get(Context ctx) {
        if (instance == null) instance = new UserManager(ctx);
        return instance;
    }

    public void setNickname(String nickname) {
        sp.edit().putString("nickname", nickname).apply();
    }

    public String getNickname() {
        return sp.getString("nickname", null);
    }
}
