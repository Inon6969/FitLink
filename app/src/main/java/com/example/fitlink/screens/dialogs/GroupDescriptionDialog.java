package com.example.fitlink.screens.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.fitlink.R;
import com.example.fitlink.models.Group;
import com.example.fitlink.models.SportType;
import com.example.fitlink.utils.ImageUtil;

public class GroupDescriptionDialog {

    private final Context context;
    private final Group group;

    // בנאי (Constructor)
    public GroupDescriptionDialog(Context context, Group group) {
        this.context = context;
        this.group = group;
    }

    // הפונקציה שבונה ומציגה את הדיאלוג
    public void show() {
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_group_description);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        // אתחול הרכיבים
        ImageView imgIcon = dialog.findViewById(R.id.img_dialog_group_icon);
        TextView tvName = dialog.findViewById(R.id.tv_dialog_group_name);
        TextView tvDescription = dialog.findViewById(R.id.tv_dialog_group_description);
        Button btnClose = dialog.findViewById(R.id.btn_dialog_close);

        tvName.setText(group.getName());

        String description = group.getDescription();
        if (description == null || description.trim().isEmpty()) {
            description = "No description available for this group.";
        }
        tvDescription.setText(description);

        // --- הלוגיקה להצגת התמונה או האייקון ---
        String base64Image = group.getGroupImage();
        int sportIconRes = getSportIconResource(group.getSportType());

        if (base64Image != null && !base64Image.isEmpty()) {
            Bitmap bitmap = ImageUtil.convertFrom64base(base64Image);
            if (bitmap != null) {
                imgIcon.setImageBitmap(bitmap);
                imgIcon.setPadding(0, 0, 0, 0); // ביטול הריווח לתמונה מלאה
                imgIcon.setImageTintList(null); // ביטול הצביעה הכחולה
            } else {
                setFallbackIcon(imgIcon, sportIconRes);
            }
        } else {
            setFallbackIcon(imgIcon, sportIconRes);
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    // פונקציית עזר להגדרת אייקון ברירת המחדל (כולל צבע וריווח)
    private void setFallbackIcon(ImageView imgIcon, int sportIconRes) {
        imgIcon.setImageResource(sportIconRes);
        int padding = (int) (12 * context.getResources().getDisplayMetrics().density);
        imgIcon.setPadding(padding, padding, padding, padding);
        imgIcon.setImageTintList(ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.fitlinkPrimary)
        ));
    }

    // פונקציית עזר למציאת האייקון הנכון לפי סוג הספורט
    private int getSportIconResource(SportType type) {
        if (type == SportType.RUNNING) {
            return R.drawable.ic_running;
        } else if (type == SportType.SWIMMING) {
            return R.drawable.ic_swimming;
        } else if (type == SportType.CYCLING) {
            return R.drawable.ic_cycling;
        }
        return R.drawable.ic_sport;
    }
}