package com.example.fitlink.screens.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.fitlink.R;
import com.example.fitlink.models.User;
import com.example.fitlink.services.DatabaseService;
import com.example.fitlink.utils.Validator;

public class AddUserDialog {
    private static final String TAG = "AddUserDialog";
    private final Context context;
    private final AddUserListener listener;
    private final DatabaseService databaseService;
    public AddUserDialog(Context context, AddUserListener listener) {
        this.context = context;
        this.listener = listener;
        this.databaseService = DatabaseService.getInstance();
    }

    public void show() {
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_add_user);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        EditText inputFirstName = dialog.findViewById(R.id.inputAddUserFirstName);
        EditText inputLastName = dialog.findViewById(R.id.inputAddUserLastName);
        EditText inputEmail = dialog.findViewById(R.id.inputAddUserEmail);
        EditText inputPassword = dialog.findViewById(R.id.inputAddUserPassword);
        EditText inputPhone = dialog.findViewById(R.id.inputAddUserPhone); // הוספת שדה טלפון

        Button btnAdd = dialog.findViewById(R.id.btnAddUserSave);
        Button btnCancel = dialog.findViewById(R.id.btnAddUserCancel);

        btnAdd.setOnClickListener(v -> {
            String fName = inputFirstName.getText().toString().trim();
            String lName = inputLastName.getText().toString().trim();
            String email = inputEmail.getText().toString().trim();
            String password = inputPassword.getText().toString().trim();
            String phone = inputPhone.getText().toString().trim();

            // וולידציה לפי התבנית של RegisterActivity
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
            if (!Validator.isEmailValid(email)) {
                inputEmail.setError("כתובת אימייל לא תקינה");
                inputEmail.requestFocus();
                return;
            }
            if (!Validator.isPhoneValid(phone)) {
                inputPhone.setError("מספר טלפון לא תקין");
                inputPhone.requestFocus();
                return;
            }
            if (!Validator.isPasswordValid(password)) {
                inputPassword.setError("הסיסמה חייבת להכיל לפחות 6 תווים");
                inputPassword.requestFocus();
                return;
            }

            Log.d(TAG, "Adding user: " + email);
            String uid = databaseService.generateUserId();
            User user = new User(uid, email, password, fName, lName, phone, false, null);

            // בדיקת כפילות אימייל בדיוק כמו ב-RegisterActivity
            databaseService.checkIfEmailExists(email, new DatabaseService.DatabaseCallback<Boolean>() {
                @Override
                public void onCompleted(Boolean exists) {
                    if (exists) {
                        Toast.makeText(context, "אימייל זה כבר קיים במערכת", Toast.LENGTH_SHORT).show();
                    } else {
                        // יצירת המשתמש
                        databaseService.createNewUser(user, new DatabaseService.DatabaseCallback<Void>() {
                            @Override
                            public void onCompleted(Void unused) {
                                Log.d(TAG, "User created successfully via dialog");
                                if (listener != null) {
                                    listener.onUserAdded(user);
                                }
                                Toast.makeText(context, "משתמש נוסף בהצלחה", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            }

                            @Override
                            public void onFailed(Exception e) {
                                Log.e(TAG, "Failed to create user", e);
                                Toast.makeText(context, "שגיאה בשמירה", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }

                @Override
                public void onFailed(Exception e) {
                    Log.e(TAG, "Failed to check email existence", e);
                    Toast.makeText(context, "שגיאה בבדיקת אימייל", Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    public interface AddUserListener {
        void onUserAdded(User newUser);
    }
}