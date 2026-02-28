package com.example.fitlink.screens.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.example.fitlink.R;
import com.example.fitlink.models.User;
import com.google.android.material.button.MaterialButton;

public class DeleteUserDialog extends Dialog {

    private final User user;
    private final OnDeleteOptionSelected listener;

    public interface OnDeleteOptionSelected {
        void onDeleteOption(boolean deleteGroups);
    }

    public DeleteUserDialog(@NonNull Context context, User user, OnDeleteOptionSelected listener) {
        super(context);
        this.user = user;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_delete_user);
        if (getWindow() != null) {
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView txtMessage = findViewById(R.id.txtDeleteUserMessage);
        txtMessage.setText("You are about to delete " + user.getFullName() + ".\nDo you also want to delete their groups?");

        findViewById(R.id.btnDeleteUserAndGroups).setOnClickListener(v -> {
            listener.onDeleteOption(true);
            dismiss();
        });

        findViewById(R.id.btnDeleteUserOnly).setOnClickListener(v -> {
            listener.onDeleteOption(false);
            dismiss();
        });

        findViewById(R.id.btnDeleteCancel).setOnClickListener(v -> dismiss());
    }
}