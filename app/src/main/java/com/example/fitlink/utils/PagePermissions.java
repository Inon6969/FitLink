package com.example.fitlink.utils;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import com.example.fitlink.models.User;
import com.example.fitlink.screens.AdminActivity;
import com.example.fitlink.screens.LoginActivity;
import com.example.fitlink.screens.MainActivity;

public class PagePermissions {
    //1) כניסה רק למנהלים
    public static void checkAdminPage(Activity activity) {
        User user = SharedPreferencesUtil.getUser(activity);

        if (!SharedPreferencesUtil.isUserLoggedIn(activity)) {
            Intent intent = new Intent(activity, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity.startActivity(intent);
        } else if (!user.isAdmin()) {
            Toast.makeText(activity, "אין לך גישה לדף זה", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(activity, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity.startActivity(intent);
        }
    }

    //2) דף שמיועד למשתמשים רגילים
    public static void checkUserPage(Activity activity) {
        User user = SharedPreferencesUtil.getUser(activity);

        if (!SharedPreferencesUtil.isUserLoggedIn(activity)) {
            Intent intent = new Intent(activity, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity.startActivity(intent);
        }
    }

    //3) אם כבר מחובר – שלח אותו לדף המתאים במקום Login/Register
    public static void redirectIfLoggedIn(Activity activity) {
        User user = SharedPreferencesUtil.getUser(activity);

        if (SharedPreferencesUtil.isUserLoggedIn(activity)) {
            Intent intent;
            if (user.isAdmin()) {
                intent = new Intent(activity, AdminActivity.class);
            } else {
                intent = new Intent(activity, MainActivity.class);
            }

            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity.startActivity(intent);
        }
    }
}