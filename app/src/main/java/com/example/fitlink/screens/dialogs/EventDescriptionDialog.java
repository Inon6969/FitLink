package com.example.fitlink.screens.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.fitlink.R;
import com.example.fitlink.models.Event;
import com.example.fitlink.models.SportType;

public class EventDescriptionDialog {

    private final Context context;
    private final Event event;

    // בנאי (Constructor) שמקבל את ההקשר (Context) ואת האירוע שעליו לחצו
    public EventDescriptionDialog(Context context, Event event) {
        this.context = context;
        this.event = event;
    }

    // הפונקציה שבונה ומציגה את הדיאלוג
    public void show() {
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_event_description); // טעינת העיצוב שיצרנו

        // הגדרת רקע שקוף לדיאלוג כדי שהפינות המעוגלות של ה-CardView יראו טוב
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        // אתחול הרכיבים מה-XML
        ImageView imgIcon = dialog.findViewById(R.id.img_dialog_event_icon);
        TextView tvTitle = dialog.findViewById(R.id.tv_dialog_event_title);
        TextView tvDescription = dialog.findViewById(R.id.tv_dialog_event_description);
        Button btnClose = dialog.findViewById(R.id.btn_dialog_close);

        // השמת הנתונים של האירוע בתוך רכיבי התצוגה
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

        // סגירת הדיאלוג כשלוחצים על כפתור הסגירה
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    // פונקציית עזר שמתאימה את האייקון הנכון לסוג הספורט
    private int getSportIconResource(SportType type) {
        if (type == SportType.RUNNING) {
            return R.drawable.ic_running;
        } else if (type == SportType.SWIMMING) {
            return R.drawable.ic_swimming;
        } else if (type == SportType.CYCLING) {
            return R.drawable.ic_cycling;
        }
        return R.drawable.ic_sport; // אייקון ברירת מחדל
    }
}