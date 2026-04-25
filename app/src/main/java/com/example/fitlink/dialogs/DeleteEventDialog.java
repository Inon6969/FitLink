package com.example.fitlink.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.fitlink.R;

public class DeleteEventDialog {

    private final Context context;
    private final OnDeleteConfirmListener listener;
    private String customTitle = null;
    private String customMessage = null;

    // בנאי רגיל למחיקת אירוע בודד
    public DeleteEventDialog(Context context, OnDeleteConfirmListener listener) {
        this.context = context;
        this.listener = listener;
    }

    // בנאי חדש למחיקה המונית (או לכל מצב שדורש טקסט מותאם)
    public DeleteEventDialog(Context context, String title, String message, OnDeleteConfirmListener listener) {
        this.context = context;
        this.customTitle = title;
        this.customMessage = message;
        this.listener = listener;
    }

    public void show() {
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_delete_event);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        // אם הועברו טקסטים מותאמים אישית, נעדכן אותם
        if (customTitle != null || customMessage != null) {
            TextView tvTitle = dialog.findViewById(R.id.tv_delete_event_title);
            TextView tvMessage = dialog.findViewById(R.id.tv_delete_event_message);

            if (customTitle != null && tvTitle != null) {
                tvTitle.setText(customTitle);
            }
            if (customMessage != null && tvMessage != null) {
                tvMessage.setText(customMessage);
            }
        }

        Button btnCancel = dialog.findViewById(R.id.btn_delete_event_cancel);
        Button btnConfirm = dialog.findViewById(R.id.btn_delete_event_confirm);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteConfirm();
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    public interface OnDeleteConfirmListener {
        void onDeleteConfirm();
    }
}