package com.example.fitlink.screens.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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
        AutoCompleteTextView inputCountryCode = dialog.findViewById(R.id.inputEditUserCountryCode);
        EditText inputPassword = dialog.findViewById(R.id.inputEditUserPassword);

        Button btnSave = dialog.findViewById(R.id.btnEditUserSave);
        Button btnCancel = dialog.findViewById(R.id.btnEditUserCancel);

        // הגדרת תפריט הקידומות
        String[] countryCodes = new String[]{
                "+972", // ישראל
                "+1",   // ארה"ב וקנדה
                "+44",  // בריטניה
                "+91",  // הודו
                "+86",  // סין
                "+33",  // צרפת
                "+49",  // גרמניה
                "+34",  // ספרד
                "+39",  // איטליה
                "+55",  // ברזיל
                "+61",  // אוסטרליה
                "+7",   // רוסיה / קזחסטן
                "+52",  // מקסיקו
                "+81",  // יפן
                "+82",  // דרום קוריאה
                "+31",  // הולנד
                "+41",  // שוויץ
                "+46",  // שוודיה
                "+27",  // דרום אפריקה
                "+971", // איחוד האמירויות
                "+65",  // סינגפור
                "+60",  // מלזיה
                "+62",  // אינדונזיה
                "+63",  // הפיליפינים
                "+90"   // טורקיה
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_dropdown_item_1line, countryCodes);
        inputCountryCode.setAdapter(adapter);

        // פיצול מספר הטלפון הקיים לקידומת ומספר
        String fullPhone = user.getPhone();
        String currentPrefix = "+972";
        String currentPhoneRaw = fullPhone != null ? fullPhone : "";

        if (fullPhone != null) {
            for (String code : countryCodes) {
                if (fullPhone.startsWith(code)) {
                    currentPrefix = code;
                    currentPhoneRaw = fullPhone.substring(code.length());
                    break;
                }
            }
        }

        // טעינת הנתונים
        inputFirstName.setText(user.getFirstName());
        inputLastName.setText(user.getLastName());
        inputEmail.setText(user.getEmail());
        inputCountryCode.setText(currentPrefix, false);
        inputPhone.setText(currentPhoneRaw);
        inputPassword.setText(user.getPassword());

        btnSave.setOnClickListener(v -> {
            String fName = inputFirstName.getText().toString().trim();
            String lName = inputLastName.getText().toString().trim();
            String newEmail = inputEmail.getText().toString().trim();
            String pass = inputPassword.getText().toString().trim();

            String prefix = inputCountryCode.getText().toString().trim();
            String rawPhone = inputPhone.getText().toString().trim();
            if (rawPhone.startsWith("0")) {
                rawPhone = rawPhone.substring(1);
            }
            String newFullPhone = prefix + rawPhone;

            // 1. Basic field validation
            if (!Validator.isNameValid(fName)) {
                inputFirstName.setError("First name is too short");
                return;
            }
            if (!Validator.isNameValid(lName)) {
                inputLastName.setError("Last name is too short");
                return;
            }
            if (!Validator.isEmailValid(newEmail)) {
                inputEmail.setError("Invalid email address");
                return;
            }
            if (!Validator.isPhoneValid(newFullPhone)) {
                inputPhone.setError("Invalid phone number");
                return;
            }
            if (!Validator.isPasswordValid(pass)) {
                inputPassword.setError("Password must be at least 6 characters");
                return;
            }

            // 2. Hierarchical availability check (Phone -> Email -> Update)
            checkPhoneAvailability(fName, lName, newEmail, newFullPhone, pass, dialog, inputEmail, inputPhone);
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void checkPhoneAvailability(String fName, String lName, String email, String phone, String pass, Dialog dialog, EditText inputEmail, EditText inputPhone) {
        if (!phone.equals(user.getPhone())) {
            DatabaseService.getInstance().checkIfPhoneExists(phone, new DatabaseService.DatabaseCallback<Boolean>() {
                @Override
                public void onCompleted(Boolean exists) {
                    if (exists) {
                        inputPhone.setError("This phone number is already registered");
                        inputPhone.requestFocus();
                    } else {
                        checkEmailAvailability(fName, lName, email, phone, pass, dialog, inputEmail);
                    }
                }

                @Override
                public void onFailed(Exception e) {
                    Toast.makeText(context, "Error verifying phone number", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            checkEmailAvailability(fName, lName, email, phone, pass, dialog, inputEmail);
        }
    }

    private void checkEmailAvailability(String fName, String lName, String email, String phone, String pass, Dialog dialog, EditText inputEmail) {
        if (!email.equalsIgnoreCase(user.getEmail())) {
            DatabaseService.getInstance().checkIfEmailExists(email, new DatabaseService.DatabaseCallback<Boolean>() {
                @Override
                public void onCompleted(Boolean exists) {
                    if (exists) {
                        inputEmail.setError("This email is already registered");
                        inputEmail.requestFocus();
                    } else {
                        performUpdate(fName, lName, email, phone, pass, dialog);
                    }
                }

                @Override
                public void onFailed(Exception e) {
                    Toast.makeText(context, "Error verifying email", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            performUpdate(fName, lName, email, phone, pass, dialog);
        }
    }

    private void performUpdate(String fName, String lName, String email, String phone, String pass, Dialog dialog) {
        user.setFirstName(fName);
        user.setLastName(lName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPassword(pass);

        DatabaseService.getInstance().updateUser(user, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                if (onSuccess != null) onSuccess.run();
                dialog.dismiss();
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(context, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}