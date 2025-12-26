package com.example.fitlink.screens.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.widget.Button;
import android.widget.TextView;

import com.example.fitlink.R;

public class LogoutDialog {
    private final Context context;
    private final Runnable onConfirm;

    public LogoutDialog(Context context, Runnable onConfirm) {
        this.context = context;
        this.onConfirm = onConfirm;
    }

    public void show() {
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_logout);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.setCancelable(true);

        TextView txtTitle = dialog.findViewById(R.id.txtDialogTitle);
        TextView txtMessage = dialog.findViewById(R.id.txtDialogMessage);
        Button btnConfirm = dialog.findViewById(R.id.btnLogoutConfirm);
        Button btnCancel = dialog.findViewById(R.id.btnLogoutCancel);

        txtTitle.setText("התנתקות");
        txtMessage.setText("האם ברצונך להתנתק?");

        btnConfirm.setOnClickListener(v -> {
            onConfirm.run();
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}
