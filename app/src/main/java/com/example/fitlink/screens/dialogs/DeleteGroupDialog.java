package com.example.fitlink.screens.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.example.fitlink.R;

public class DeleteGroupDialog extends Dialog {

    private final Runnable onConfirm;

    public DeleteGroupDialog(@NonNull Context context, Runnable onConfirm) {
        super(context);
        this.onConfirm = onConfirm;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_delete_group);

        // הגדרת רקע שקוף כדי שהפינות המעוגלות של ה-CardView ייראו טוב
        if (getWindow() != null) {
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        findViewById(R.id.btnDeleteGroupCancel).setOnClickListener(v -> dismiss());

        findViewById(R.id.btnDeleteGroupConfirm).setOnClickListener(v -> {
            if (onConfirm != null) {
                onConfirm.run();
            }
            dismiss();
        });
    }
}