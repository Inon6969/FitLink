package com.example.fitlink.screens.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.fitlink.R;
import com.example.fitlink.models.Event;
import com.example.fitlink.models.SportType;
import com.example.fitlink.utils.SharedPreferencesUtil; // ייבוא לקבלת המשתמש הנוכחי

public class EventDescriptionDialog {

    private final Context context;
    private final Event event;
    private OnEditListener editListener; // מאזין ללחיצה על עריכה

    // ממשק להעברת אירוע העריכה למסך שפתח את הדיאלוג
    public interface OnEditListener {
        void onEditClick(Event event);
    }

    public EventDescriptionDialog(Context context, Event event) {
        this.context = context;
        this.event = event;
    }

    public void setOnEditListener(OnEditListener listener) {
        this.editListener = listener;
    }

    public void show() {
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_event_description);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        ImageView imgIcon = dialog.findViewById(R.id.img_dialog_event_icon);
        TextView tvTitle = dialog.findViewById(R.id.tv_dialog_event_title);
        TextView tvDescription = dialog.findViewById(R.id.tv_dialog_event_description);
        Button btnClose = dialog.findViewById(R.id.btn_dialog_close);
        Button btnEdit = dialog.findViewById(R.id.btn_dialog_edit_event); // הכפתור החדש

        if (event.getSportType() != null) {
            imgIcon.setImageResource(getSportIconResource(event.getSportType()));
        } else {
            imgIcon.setImageResource(R.drawable.ic_sport);
        }

        tvTitle.setText(event.getTitle());

        String description = event.getDescription();
        if (description == null || description.trim().isEmpty()) {
            description = "No description available for this event.";
        }
        tvDescription.setText(description);

        // --- לוגיקת הצגת כפתור העריכה ---
        String currentUserId = SharedPreferencesUtil.getUserId(context);
        boolean isCreator = currentUserId != null && currentUserId.equals(event.getCreatorId());

        if (btnEdit != null) {
            if (isCreator) {
                btnEdit.setVisibility(View.VISIBLE);
                btnEdit.setOnClickListener(v -> {
                    dialog.dismiss();
                    if (editListener != null) {
                        editListener.onEditClick(event);
                    }
                });
            } else {
                btnEdit.setVisibility(View.GONE);
            }
        }
        // ----------------------------------

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private int getSportIconResource(SportType type) {
        if (type == SportType.RUNNING) {
            return R.drawable.ic_running;
        } else if (type == SportType.SWIMMING) {
            return R.drawable.ic_swimming;
        } else if (type == SportType.CYCLING) {
            return R.drawable.ic_cycling;
        }
        return R.drawable.ic_sport;
    }
}