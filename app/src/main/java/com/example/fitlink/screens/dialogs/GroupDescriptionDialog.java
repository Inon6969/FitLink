package com.example.fitlink.screens.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.fitlink.R;
import com.example.fitlink.models.Group;
import com.example.fitlink.models.SportType;
import com.example.fitlink.utils.SharedPreferencesUtil;

public class GroupDescriptionDialog {

    private final Context context;
    private final Group group;
    private OnEditListener editListener; // מאזין ללחיצה על עריכה

    // ממשק להעברת אירוע העריכה למסך שפתח את הדיאלוג
    public interface OnEditListener {
        void onEditClick(Group group);
    }

    // בנאי (Constructor)
    public GroupDescriptionDialog(Context context, Group group) {
        this.context = context;
        this.group = group;
    }

    public void setOnEditListener(OnEditListener listener) {
        this.editListener = listener;
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
        Button btnEdit = dialog.findViewById(R.id.btn_dialog_edit_group); // כפתור העריכה החדש

        imgIcon.setImageResource(getSportIconResource(group.getSportType()));
        tvName.setText(group.getName());

        String description = group.getDescription();
        if (description == null || description.trim().isEmpty()) {
            description = "No description available for this group.";
        }
        tvDescription.setText(description);

        // --- לוגיקת הצגת כפתור העריכה ---
        String currentUserId = SharedPreferencesUtil.getUserId(context);
        boolean isCreator = currentUserId != null && currentUserId.equals(group.getCreatorId());

        if (btnEdit != null) {
            if (isCreator) {
                btnEdit.setVisibility(View.VISIBLE);
                btnEdit.setOnClickListener(v -> {
                    dialog.dismiss();
                    if (editListener != null) {
                        editListener.onEditClick(group);
                    }
                });
            } else {
                btnEdit.setVisibility(View.GONE);
            }
        }
        // ----------------------------------

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    // פונקציית עזר למציאת האייקון הנכון
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