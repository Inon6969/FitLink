package com.example.fitlink.screens.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.fitlink.R;
import com.example.fitlink.models.User;

public class DeleteUserDialog extends Dialog {

    private final User user;
    private final OnDeleteOptionSelected listener;

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
        txtMessage.setText("You are about to entirely delete " + user.getFullName() + ".\nAll their created groups and events will be permanently removed. Are you sure?");

        // מעביר תמיד true כדי לבצע מחיקה הרמטית של המשתמש והקבוצות שלו
        findViewById(R.id.btnConfirmDelete).setOnClickListener(v -> {
            listener.onDeleteOption(true);
            dismiss();
        });

        findViewById(R.id.btnDeleteCancel).setOnClickListener(v -> dismiss());
    }

    public interface OnDeleteOptionSelected {
        void onDeleteOption(boolean deleteGroups);
    }
}