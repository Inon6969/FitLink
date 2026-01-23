package com.example.fitlink.screens.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.fitlink.R;
import com.example.fitlink.models.User;
import com.example.fitlink.services.DatabaseService;
import com.example.fitlink.utils.Validator;

public class EditUserDialog {
    private final Context context;
    private final User user;
    private final Runnable onSuccess;

    public EditUserDialog(Context context, User user, Runnable onSuccess) {
        this.context = context;
        this.user = user;
        this.onSuccess = onSuccess;
    }

    public void show() {
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_edit_user);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        EditText inputFirstName = dialog.findViewById(R.id.inputEditUserFirstName);
        EditText inputLastName = dialog.findViewById(R.id.inputEditUserLastName);
        EditText inputEmail = dialog.findViewById(R.id.inputEditUserEmail);
        EditText inputPhone = dialog.findViewById(R.id.inputEditUserPhone);
        EditText inputPassword = dialog.findViewById(R.id.inputEditUserPassword);

        Button btnSave = dialog.findViewById(R.id.btnEditUserSave);
        Button btnCancel = dialog.findViewById(R.id.btnEditUserCancel);

        // טעינת נתונים קיימים
        inputFirstName.setText(user.getFirstName());
        inputLastName.setText(user.getLastName());
        inputEmail.setText(user.getEmail());
        inputPhone.setText(user.getPhone());
        inputPassword.setText(user.getPassword());

        btnSave.setOnClickListener(v -> {
            String fName = inputFirstName.getText().toString().trim();
            String lName = inputLastName.getText().toString().trim();
            String newEmail = inputEmail.getText().toString().trim();
            String phone = inputPhone.getText().toString().trim();
            String pass = inputPassword.getText().toString().trim();

            // 1. וולידציה בסיסית של השדות
            if (!Validator.isNameValid(fName)) {
                inputFirstName.setError("שם פרטי קצר מדי");
                inputFirstName.requestFocus();
                return;
            }
            if (!Validator.isNameValid(lName)) {
                inputLastName.setError("שם משפחה קצר מדי");
                inputLastName.requestFocus();
                return;
            }
            if (!Validator.isEmailValid(newEmail)) {
                inputEmail.setError("כתובת אימייל לא תקינה");
                inputEmail.requestFocus();
                return;
            }
            if (!Validator.isPhoneValid(phone)) {
                inputPhone.setError("מספר טלפון לא תקין");
                inputPhone.requestFocus();
                return;
            }
            if (!Validator.isPasswordValid(pass)) {
                inputPassword.setError("הסיסמה חייבת להכיל לפחות 6 תווים");
                inputPassword.requestFocus();
                return;
            }

            // 2. בדיקה האם האימייל השתנה
            if (!newEmail.equalsIgnoreCase(user.getEmail())) {
                // האימייל שונה - יש לבדוק אם הוא קיים במערכת
                DatabaseService.getInstance().checkIfEmailExists(newEmail, new DatabaseService.DatabaseCallback<Boolean>() {
                    @Override
                    public void onCompleted(Boolean exists) {
                        if (exists) {
                            inputEmail.setError("אימייל זה כבר קיים במערכת");
                            inputEmail.requestFocus();
                        } else {
                            // האימייל החדש פנוי - ממשיכים לעדכון
                            performUpdate(fName, lName, newEmail, phone, pass, dialog);
                        }
                    }

                    @Override
                    public void onFailed(Exception e) {
                        Toast.makeText(context, "שגיאה בבדיקת אימייל", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                // האימייל לא השתנה - מעדכנים ישירות את שאר הפרטים
                performUpdate(fName, lName, newEmail, phone, pass, dialog);
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // פונקציית עזר לביצוע העדכון ב-Database
    private void performUpdate(String fName, String lName, String email, String phone, String pass, Dialog dialog) {
        user.setFirstName(fName);
        user.setLastName(lName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPassword(pass);

        DatabaseService.getInstance().updateUser(user, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                Toast.makeText(context, "הפרטים עודכנו בהצלחה!", Toast.LENGTH_SHORT).show();
                if (onSuccess != null) onSuccess.run();
                dialog.dismiss();
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(context, "שגיאה בעדכון: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}