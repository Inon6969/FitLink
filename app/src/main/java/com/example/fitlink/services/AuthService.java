package com.example.fitlink.services;

import android.content.Context;

import com.example.fitlink.models.User;
import com.example.fitlink.utils.SharedPreferencesUtil;

public class AuthService {
    private final Context context;

    public AuthService(Context context) {
        this.context = context.getApplicationContext();
    }

    //התנתקות
    public String logout() {
        User user = SharedPreferencesUtil.getUser(context);

        String email = user != null ? user.getEmail() : "";
        SharedPreferencesUtil.signOutUser(context);

        return email;
    }
}
