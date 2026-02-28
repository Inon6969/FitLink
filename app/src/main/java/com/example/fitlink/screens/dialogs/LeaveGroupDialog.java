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
    private final Runnable onConfirm;

    public LeaveGroupDialog(Context context, Group group, Runnable onConfirm) {
        this.context = context;
        this.group = group;
        this.onConfirm = onConfirm;
    }

    public void show() {
        Dialog dialog = new Dialog(context);

        // טוען את קובץ העיצוב העצמאי החדש שלנו
        dialog.setContentView(R.layout.dialog_leave_group);
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCancelable(true);

        // חיבור לאלמנטים מקובץ ה-XML החדש
        TextView txtTitle = dialog.findViewById(R.id.txtLeaveGroupTitle);
        TextView txtMessage = dialog.findViewById(R.id.txtLeaveGroupMessage);
        Button btnConfirm = dialog.findViewById(R.id.btnLeaveGroupConfirm);
        Button btnCancel = dialog.findViewById(R.id.btnLeaveGroupCancel);

        // הגדרת הטקסט המדויק עם שם הקבוצה
        txtTitle.setText("Leave Group");
        txtMessage.setText("Are you sure you want to leave '" + group.getName() + "'?");

        btnConfirm.setOnClickListener(v -> {
            onConfirm.run();
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}