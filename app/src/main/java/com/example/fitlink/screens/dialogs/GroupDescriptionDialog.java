package com.example.fitlink.screens.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.fitlink.R;
import com.example.fitlink.models.Group;
import com.example.fitlink.models.SportType;
import com.example.fitlink.utils.ImageUtil;

public class GroupDescriptionDialog {

    public interface AdminActionsListener {
        void onEdit(Group group);
        void onDelete(Group group);
    }

    private final Context context;
    private final Group group;
    private boolean isAdminPanel = false;
    private AdminActionsListener adminListener;

    // בנאי רגיל למשתמשים (כמו שהיה עד עכשיו)
    public GroupDescriptionDialog(Context context, Group group) {
        this.context = context;
        this.group = group;
    }

    // בנאי חדש למנהלים בלבד
    public GroupDescriptionDialog(Context context, Group group, boolean isAdminPanel, AdminActionsListener adminListener) {
        this.context = context;
        this.group = group;
        this.isAdminPanel = isAdminPanel;
        this.adminListener = adminListener;
    }

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

        ImageView imgIcon = dialog.findViewById(R.id.img_dialog_group_icon);
        TextView tvName = dialog.findViewById(R.id.tv_dialog_group_name);
        TextView tvDescription = dialog.findViewById(R.id.tv_dialog_group_description);
        Button btnClose = dialog.findViewById(R.id.btn_dialog_close);

        LinearLayout layoutAdminActions = dialog.findViewById(R.id.layout_dialog_group_admin_actions);
        Button btnEdit = dialog.findViewById(R.id.btn_dialog_group_edit);
        Button btnDelete = dialog.findViewById(R.id.btn_dialog_group_delete);

        tvName.setText(group.getName());

        String description = group.getDescription();
        if (description == null || description.trim().isEmpty()) {
            description = "No description available for this group.";
        }
        tvDescription.setText(description);

        String base64Image = group.getGroupImage();
        int sportIconRes = getSportIconResource(group.getSportType());

        if (base64Image != null && !base64Image.isEmpty()) {
            Bitmap bitmap = ImageUtil.convertFrom64base(base64Image);
            if (bitmap != null) {
                imgIcon.setImageBitmap(bitmap);
                imgIcon.setPadding(0, 0, 0, 0);
                imgIcon.setImageTintList(null);
            } else {
                setFallbackIcon(imgIcon, sportIconRes);
            }
        } else {
            setFallbackIcon(imgIcon, sportIconRes);
        }

        // אם פתחנו את הדיאלוג דרך פאנל הניהול, נציג את כפתורי העריכה והמחיקה
        if (isAdminPanel) {
            layoutAdminActions.setVisibility(View.VISIBLE);
            btnEdit.setOnClickListener(v -> {
                dialog.dismiss();
                if (adminListener != null) adminListener.onEdit(group);
            });
            btnDelete.setOnClickListener(v -> {
                dialog.dismiss();
                if (adminListener != null) adminListener.onDelete(group);
            });
        } else {
            layoutAdminActions.setVisibility(View.GONE);
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void setFallbackIcon(ImageView imgIcon, int sportIconRes) {
        imgIcon.setImageResource(sportIconRes);
        int padding = (int) (12 * context.getResources().getDisplayMetrics().density);
        imgIcon.setPadding(padding, padding, padding, padding);
        imgIcon.setImageTintList(ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.fitlinkPrimary)
        ));
    }

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