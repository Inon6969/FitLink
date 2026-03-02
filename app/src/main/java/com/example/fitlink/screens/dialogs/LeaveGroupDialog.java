package com.example.fitlink.screens.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.fitlink.R;
import com.example.fitlink.models.Group;

import java.util.Objects;

public class LeaveGroupDialog {
    private final Context context;
    private final Group group;
    private final String currentUserId;
    private final Runnable onConfirm;

    // הוספנו את currentUserId לבנאי כדי לדעת מי מנסה לעזוב
    public LeaveGroupDialog(Context context, Group group, String currentUserId, Runnable onConfirm) {
        this.context = context;
        this.group = group;
        this.currentUserId = currentUserId;
        this.onConfirm = onConfirm;
    }

    public void show() {
        Dialog dialog = new Dialog(context);

        dialog.setContentView(R.layout.dialog_leave_group);
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCancelable(true);

        TextView txtTitle = dialog.findViewById(R.id.txtLeaveGroupTitle);
        TextView txtMessage = dialog.findViewById(R.id.txtLeaveGroupMessage);
        Button btnConfirm = dialog.findViewById(R.id.btnLeaveGroupConfirm);
        Button btnCancel = dialog.findViewById(R.id.btnLeaveGroupCancel);

        txtTitle.setText("Leave Group");

        // לוגיקה לבחירת ההודעה המתאימה לפי התפקיד של המשתמש
        boolean isCreator = group.getCreatorId() != null && group.getCreatorId().equals(currentUserId);
        boolean isManager = group.getManagers() != null && group.getManagers().containsKey(currentUserId);

        if (isCreator) {
            txtMessage.setText("As the creator of '" + group.getName() + "', you can take a break and leave. You will retain your creator status and can rejoin at any time. Leave group?");
        } else if (isManager) {
            txtMessage.setText("You are a manager of '" + group.getName() + "'. Leaving the group will revoke your management permissions. Are you sure you want to leave?");
        } else {
            txtMessage.setText("Are you sure you want to leave '" + group.getName() + "'?");
        }

        btnConfirm.setOnClickListener(v -> {
            onConfirm.run();
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}