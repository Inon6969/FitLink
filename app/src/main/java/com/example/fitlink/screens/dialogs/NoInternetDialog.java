package com.example.fitlink.screens.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.fitlink.R;

import java.util.Objects;

public class NoInternetDialog {
    private final Context context;
    private Dialog dialog;

    private String title;
    private String message;
    private String positiveText;
    private Runnable onPositive;
    private String negativeText;
    private Runnable onNegative;

    // קונסטרקטור מורחב שמאפשר לשלוט בכל הטקסטים והכפתורים (מעולה למצב אופליין)
    public NoInternetDialog(Context context, String title, String message,
                            String positiveText, Runnable onPositive,
                            String negativeText, Runnable onNegative) {
        this.context = context;
        this.title = title;
        this.message = message;
        this.positiveText = positiveText;
        this.onPositive = onPositive;
        this.negativeText = negativeText;
        this.onNegative = onNegative;
    }

    // קונסטרקטור ישן שתומך ב-SplashActivity (כדי שלא תצטרך לשנות שם קוד!)
    public NoInternetDialog(Context context, Runnable onRetry, Runnable onExit) {
        this(context,
                "No Internet Connection",
                "It looks like you are offline. Please check your internet connection and try again.",
                "Retry", onRetry,
                "Exit", onExit);
    }

    public void show() {
        if (dialog != null && dialog.isShowing()) {
            return;
        }

        dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_no_internet);
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
        dialog.setCancelable(false);

        TextView txtTitle = dialog.findViewById(R.id.txtDialogTitle);
        TextView txtMessage = dialog.findViewById(R.id.txtDialogMessage);
        Button btnPositive = dialog.findViewById(R.id.btnInternetRetry);
        Button btnNegative = dialog.findViewById(R.id.btnInternetExit);

        // הגדרת הטקסטים
        if (title != null) txtTitle.setText(title);
        if (message != null) txtMessage.setText(message);

        // הגדרת הכפתור הראשי (למשל Retry או Continue Offline)
        if (positiveText != null) {
            btnPositive.setText(positiveText);
            btnPositive.setVisibility(View.VISIBLE);
            btnPositive.setOnClickListener(v -> {
                if (onPositive != null) onPositive.run();
                dismiss();
            });
        } else {
            btnPositive.setVisibility(View.GONE);
        }

        // הגדרת הכפתור המשני (אם שלחנו null, הוא פשוט ייעלם והכפתור הראשי יתפוס את כל המקום)
        if (negativeText != null) {
            btnNegative.setText(negativeText);
            btnNegative.setVisibility(View.VISIBLE);
            btnNegative.setOnClickListener(v -> {
                if (onNegative != null) onNegative.run();
                dismiss();
            });
        } else {
            btnNegative.setVisibility(View.GONE);
        }

        dialog.show();
    }

    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }
}