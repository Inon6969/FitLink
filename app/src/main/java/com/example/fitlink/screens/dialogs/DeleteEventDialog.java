package com.example.fitlink.screens.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.fitlink.R;

public class DeleteEventDialog {

    private final Context context;
    private final OnDeleteConfirmListener listener;

    public interface OnDeleteConfirmListener {
        void onDeleteConfirm();
    }

    public DeleteEventDialog(Context context, OnDeleteConfirmListener listener) {
        this.context = context;
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
}