package com.example.fitlink.screens.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.fitlink.R;
import com.example.fitlink.models.Group;

import java.util.Objects;

public class CancelJoinRequestDialog {
    private final Context context;
    private final Group group;
    private final Runnable onConfirm;

    public CancelJoinRequestDialog(Context context, Group group, Runnable onConfirm) {
        this.context = context;
        this.group = group;
        this.onConfirm = onConfirm;
    }

    public void show() {
        Dialog dialog = new Dialog(context);

        dialog.setContentView(R.layout.dialog_cancel_request);
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCancelable(true);

        TextView txtTitle = dialog.findViewById(R.id.txtCancelRequestTitle);
        TextView txtMessage = dialog.findViewById(R.id.txtCancelRequestMessage);
        Button btnConfirm = dialog.findViewById(R.id.btnCancelRequestYes);
        Button btnCancel = dialog.findViewById(R.id.btnCancelRequestNo);

        txtTitle.setText("Cancel Request");
        txtMessage.setText("Are you sure you want to cancel your request to join '" + group.getName() + "'?");

        btnConfirm.setOnClickListener(v -> {
            onConfirm.run();
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}