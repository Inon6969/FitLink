package com.example.fitlink.screens.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.example.fitlink.R;
import com.google.android.material.button.MaterialButton;

import java.util.Objects;

public class ProfileImageDialog {
    private final Context context;
    private final boolean hasImage;
    private final boolean isGroup;
    private final ImagePickerListener listener;

    public ProfileImageDialog(Context context, boolean hasImage, boolean isGroup, ImagePickerListener listener) {
        this.context = context;
        this.hasImage = hasImage;
        this.isGroup = isGroup;
        this.listener = listener;
    }

    public void show() {
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_profile_image);
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
        dialog.setCancelable(true);

        // עדכון הכותרת הראשית בהתאם להקשר
        TextView txtTitle = dialog.findViewById(R.id.txt_profile_image_dialog_title);
        if (txtTitle != null) {
            txtTitle.setText(isGroup ? "Update Group Photo" : "Update Profile Photo");
        }

        // עדכון הודעת ההסבר (Description) בהתאם להקשר
        TextView txtDesc = dialog.findViewById(R.id.txt_profile_image_dialog_desc);
        if (txtDesc != null) {
            txtDesc.setText(isGroup ? "How would you like to update the group's photo?" : "How would you like to update your photo?");
        }

        // מציאת הרכיבים
        MaterialButton btnCamera = dialog.findViewById(R.id.btn_profileImageDialog_camera);
        MaterialButton btnGallery = dialog.findViewById(R.id.btn_profileImageDialog_gallery);
        MaterialButton btnDelete = dialog.findViewById(R.id.btn_profileImageDialog_delete);
        MaterialButton btnCancel = dialog.findViewById(R.id.btn_profileImageDialog_cancel);

        // הצגת/הסתרת כפתור המחיקה בהתאם לקיום תמונה
        if (btnDelete != null) {
            btnDelete.setVisibility(hasImage ? View.VISIBLE : View.GONE);
        }

        // הגדרת מאזינים
        if (btnCamera != null) {
            btnCamera.setOnClickListener(v -> {
                listener.onCamera();
                dialog.dismiss();
            });
        }

        if (btnGallery != null) {
            btnGallery.setOnClickListener(v -> {
                listener.onGallery();
                dialog.dismiss();
            });
        }

        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> {
                listener.onDelete();
                dialog.dismiss();
            });
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    public interface ImagePickerListener {
        void onCamera();
        void onGallery();
        void onDelete();
    }
}