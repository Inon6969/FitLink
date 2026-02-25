package com.example.fitlink.screens;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.fitlink.R;
import com.example.fitlink.models.User;
import com.example.fitlink.screens.dialogs.EditUserDialog;
import com.example.fitlink.screens.dialogs.ProfileImageDialog;
import com.example.fitlink.services.DatabaseService;
import com.example.fitlink.utils.ImageUtil;
import com.example.fitlink.utils.SharedPreferencesUtil;

import java.util.Objects;

public class UserProfileActivity extends BaseActivity {
    private static final int REQ_CAMERA = 100;
    private static final int REQ_GALLERY = 200;
    private TextView txtTitle, txtFirstName, txtLastName, txtEmail, txtPassword;
    private ImageView imgUserProfile;
    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.detailsAboutUserPage), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        user = SharedPreferencesUtil.getUser(this);

        // אתחול כפתורי הניווט בסרגל העליון
        Button btnToMain = findViewById(R.id.btn_DetailsAboutUser_to_main);
        Button btnToContact = findViewById(R.id.btn_DetailsAboutUser_to_contact);
        Button btnToExit = findViewById(R.id.btn_DetailsAboutUser_to_exit);

        btnToMain.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            // מנקה את כל המסכים שמעל מסך הבית ומונע יצירת כפילויות
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        // עדכון: הוספת מעבר לדף Contact עם ניהול מחסנית תקין
        btnToContact.setOnClickListener(v -> {
            Intent intent = new Intent(this, ContactActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        btnToExit.setOnClickListener(v -> logout());

        Button btnEditUser = findViewById(R.id.btn_DetailsAboutUser_edit_user);
        btnEditUser.setOnClickListener(v -> openEditDialog());

        imgUserProfile = findViewById(R.id.img_DetailsAboutUser_user_profile);
        Button btnChangePhoto = findViewById(R.id.btn_DetailsAboutUser_change_photo);
        btnChangePhoto.setOnClickListener(v -> openImagePicker());
        imgUserProfile.setOnClickListener(v -> {
            if (user.getProfileImage() != null) showFullImageDialog();
        });

        txtTitle = findViewById(R.id.txt_DetailsAboutUser_title);
        txtFirstName = findViewById(R.id.txt_DetailsAboutUser_first_name);
        txtLastName = findViewById(R.id.txt_DetailsAboutUser_last_name);
        txtEmail = findViewById(R.id.txt_DetailsAboutUser_email);
        txtPassword = findViewById(R.id.txt_DetailsAboutUser_password);

        loadUserDetailsFromSharedPref();
    }

    private void loadUserDetailsFromSharedPref() {
        txtTitle.setText(user.getFullName());
        txtFirstName.setText(user.getFirstName());
        txtLastName.setText(user.getLastName());
        txtEmail.setText(user.getEmail());
        txtPassword.setText(user.getPassword());

        if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
            Bitmap bmp = ImageUtil.convertFrom64base(user.getProfileImage());
            if (bmp != null) imgUserProfile.setImageBitmap(bmp);
        }
    }

    private void openEditDialog() {
        // שימוש במחלקה שיצרת במקום המימוש הידני הקודם
        EditUserDialog editDialog = new EditUserDialog(this, user, () -> {
            // קוד שירוץ לאחר עדכון מוצלח - רענון התצוגה ב-Activity
            loadUserDetailsFromSharedPref();
        });
        editDialog.show();
    }

    private void updateUserInDatabaseAndSharedPreference() {
        DatabaseService.getInstance().updateUser(user, new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(Void object) {
                txtFirstName.setText(user.getFirstName());
                txtLastName.setText(user.getLastName());
                txtPassword.setText(user.getPassword());
                txtTitle.setText(user.getFullName());

                SharedPreferencesUtil.saveUser(UserProfileActivity.this, user);

                Toast.makeText(UserProfileActivity.this, "הפרטים עודכנו בהצלחה!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(UserProfileActivity.this, "שגיאה בעדכון הנתונים: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openImagePicker() {
        boolean hasImage = user.getProfileImage() != null && !user.getProfileImage().isEmpty();

        new ProfileImageDialog(this, hasImage, new ProfileImageDialog.ImagePickerListener() {
            @Override
            public void onCamera() {
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, REQ_CAMERA);
            }

            @Override
            public void onGallery() {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, REQ_GALLERY);
            }

            @Override
            public void onDelete() {
                deleteProfileImage();
            }
        }).show();
    }

    private void deleteProfileImage() {
        user.setProfileImage(null);

        imgUserProfile.setImageResource(R.drawable.ic_user);

        databaseService.updateUser(user, new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(Void object) {
                SharedPreferencesUtil.saveUser(UserProfileActivity.this, user);
                Toast.makeText(UserProfileActivity.this, "תמונת הפרופיל נמחקה", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(UserProfileActivity.this, "שגיאה במחיקת התמונה", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK) return;

        Bitmap bitmap = null;

        if (requestCode == REQ_CAMERA && data != null) {
            bitmap = (Bitmap) Objects.requireNonNull(data.getExtras()).get("data");
        } else if (requestCode == REQ_GALLERY && data != null) {
            try {
                bitmap = BitmapFactory.decodeStream(
                        getContentResolver().openInputStream(Objects.requireNonNull(data.getData()))
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (bitmap != null) {
            imgUserProfile.setImageBitmap(bitmap);

            //המרה ל־Base64 ושמירה
            String base64 = ImageUtil.convertTo64Base(imgUserProfile);
            user.setProfileImage(base64);

            saveProfileImage();
        }
    }

    private void saveProfileImage() {
        DatabaseService.getInstance().updateUser(user, new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(Void object) {
                SharedPreferencesUtil.saveUser(UserProfileActivity.this, user);

                Toast.makeText(UserProfileActivity.this, "תמונת הפרופיל עודכנה!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(UserProfileActivity.this, "שגיאה בעדכון התמונה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showFullImageDialog() {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_full_image);

        ImageView dialogImage = dialog.findViewById(R.id.dialogImage);

        //מציב את התמונה שיש בתמונה המקורית
        dialogImage.setImageDrawable(imgUserProfile.getDrawable());

        //לוחצים על התמונה - יוצא
        dialogImage.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}