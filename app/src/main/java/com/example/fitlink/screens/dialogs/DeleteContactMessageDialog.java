package com.example.fitlink.screens.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.example.fitlink.R;
import com.example.fitlink.models.ContactMessage;

public class DeleteContactMessageDialog extends Dialog {

    private final ContactMessage message;
    private final OnDeleteMessageListener listener;

    public interface OnDeleteMessageListener {
        void onDelete();
    }

    public DeleteContactMessageDialog(@NonNull Context context, ContactMessage message, OnDeleteMessageListener listener) {
        super(context);
        this.message = message;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_delete_contact_message);

        if (getWindow() != null) {
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView txtMessageContent = findViewById(R.id.txtDeleteMessageContent);

        // מציג את שם השולח באופן דינמי בתוך הודעת האזהרה
        String senderName = message.getName() != null ? message.getName() : "this user";
        txtMessageContent.setText("Are you sure you want to completely delete the message from " + senderName + "?");

        findViewById(R.id.btnConfirmDeleteMessage).setOnClickListener(v -> {
            listener.onDelete();
            dismiss();
        });

        findViewById(R.id.btnDeleteMessageCancel).setOnClickListener(v -> dismiss());
    }
}