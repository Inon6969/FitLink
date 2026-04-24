package com.example.fitlink.utils;

import android.util.Patterns;

import androidx.annotation.Nullable;

/// Validator class to validate user input.
/// This class contains static methods to validate user input,
/// like email, password, phone, name etc.
public class Validator {

    /// Check if the email is valid and not empty
    ///
    /// @param email email to validate
    /// @return true if the email is valid, false otherwise
    /// @see Patterns#EMAIL_ADDRESS
    public static boolean isEmailValid(@Nullable String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches();
    }

    /// Check if the password is valid (at least 6 characters)
    ///
    /// @param password password to validate
    /// @return true if the password is valid, false otherwise
    public static boolean isPasswordValid(@Nullable String password) {
        if (password == null || password.trim().isEmpty()) {
            return false;
        }
        return password.trim().length() >= 6;
    }

    /// Check if the phone number is valid according to the international E.164 standard.
    /// Handles international formats (e.g. +972501234567)
    ///
    /// @param phone phone number to validate
    /// @return true if the phone number is valid, false otherwise
    public static boolean isPhoneValid(@Nullable String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }

        // שלב 1: ניקוי המספר מרווחים ומקפים שהמשתמש אולי הקליד מתוך הרגל
        String cleanPhone = phone.trim().replaceAll("[\\s\\-]", "");

        // שלב 2: אימות לפי התקן הבינלאומי המחמיר (E.164)
        // ^\+         -> חייב להתחיל בפלוס
        // [1-9]       -> הספרה הראשונה אחרי הפלוס חייבת להיות בין 1 ל-9 (אסור 0)
        // \d{7,14}$   -> לאחריה בין 7 ל-14 ספרות בדיוק (מה שנותן בסך הכל 8 עד 15 ספרות למספר)
        return cleanPhone.matches("^\\+[1-9]\\d{7,14}$");
    }

    /// Check if the name is valid
    /// Requires at least 2 characters and only letters (English/Hebrew), spaces, or hyphens.
    ///
    /// @param name name to validate
    /// @return true if the name is valid, false otherwise
    public static boolean isNameValid(@Nullable String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        String cleanName = name.trim();
        return cleanName.length() >= 2 && cleanName.matches("^[a-zA-Zא-ת\\s\\-]+$");
    }
}