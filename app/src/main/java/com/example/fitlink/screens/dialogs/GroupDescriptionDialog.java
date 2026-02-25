package com.example.fitlink.screens.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.fitlink.R;
import com.example.fitlink.models.Group;
import com.example.fitlink.models.SportType;

public class GroupDescriptionDialog {

    private final Context context;
    private final Group group;

    // בנאי (Constructor) שמקבל את ההקשר (Context) ואת הקבוצה שעליה לחצו
    public GroupDescriptionDialog(Context context, Group group) {
        this.context = context;
        this.group = group;
    }

    // הפונקציה שבונה ומציגה את הדיאלוג
    public void show() {
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_group_description); // טעינת העיצוב שיצרנו

        // הגדרת רקע שקוף לדיאלוג כדי שהפינות המעוגלות של ה-CardView יראו טוב
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        // אתחול הרכיבים מה-XML
        ImageView imgIcon = dialog.findViewById(R.id.img_dialog_group_icon);
        TextView tvName = dialog.findViewById(R.id.tv_dialog_group_name);
        TextView tvDescription = dialog.findViewById(R.id.tv_dialog_group_description);
        Button btnClose = dialog.findViewById(R.id.btn_dialog_close);

        // השמת הנתונים של הקבוצה בתוך רכיבי התצוגה
        imgIcon.setImageResource(getSportIconResource(group.getSportType()));
        tvName.setText(group.getName());

        String description = group.getDescription();
        if (description == null || description.trim().isEmpty()) {
            description = "No description available for this group.";
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
        return R.drawable.ic_sport;
    }
}