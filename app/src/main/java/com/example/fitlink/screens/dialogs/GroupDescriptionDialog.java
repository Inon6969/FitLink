package com.example.fitlink.screens.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.example.fitlink.R;
import com.example.fitlink.models.Group;
import com.example.fitlink.models.SportType;
import com.example.fitlink.screens.MembersListActivity;
import com.example.fitlink.utils.ImageUtil;
import com.google.android.material.chip.Chip;

public class GroupDescriptionDialog {

    private final Context context;
    private final Group group;
    private boolean isAdminPanel = false;
    private AdminActionsListener adminListener;

    public GroupDescriptionDialog(Context context, Group group) {
        this.context = context;
        this.group = group;
    }

    public GroupDescriptionDialog(Context context, Group group, boolean isAdminPanel, AdminActionsListener adminListener) {
        this.context = context;
        this.group = group;
        this.isAdminPanel = isAdminPanel;
        this.adminListener = adminListener;
    }

    public void show() {
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_group_description);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        // חילוץ ה-Views הרגילים
        ImageView imgIcon = dialog.findViewById(R.id.img_dialog_group_icon);
        TextView tvName = dialog.findViewById(R.id.tv_dialog_group_name);
        TextView tvDescription = dialog.findViewById(R.id.tv_dialog_group_description);
        Button btnClose = dialog.findViewById(R.id.btn_dialog_close);

        // חילוץ ה-Views החדשים של פרטי הקבוצה (מעוצבים בדומה ל-item_group)
        ImageView imgSportMini = dialog.findViewById(R.id.img_dialog_sport_mini);
        TextView tvSport = dialog.findViewById(R.id.tv_dialog_group_sport);
        TextView tvLocation = dialog.findViewById(R.id.tv_dialog_group_location);
        TextView tvMembers = dialog.findViewById(R.id.tv_dialog_group_members);
        Chip chipLevel = dialog.findViewById(R.id.chip_dialog_group_level);
        LinearLayout layoutLocation = dialog.findViewById(R.id.layout_dialog_location);

        LinearLayout layoutAdminActions = dialog.findViewById(R.id.layout_dialog_group_admin_actions);
        Button btnEdit = dialog.findViewById(R.id.btn_dialog_group_edit);
        Button btnDelete = dialog.findViewById(R.id.btn_dialog_group_delete);

        // עדכון שם ותיאור
        tvName.setText(group.getName());
        String description = group.getDescription();
        if (description == null || description.trim().isEmpty()) {
            description = "No description available for this group.";
        }
        tvDescription.setText(description);

        // --- עדכון המידע החדש ---

        // קביעת האייקון הדינמי (מתאים לספורט הספציפי)
        int sportIconRes = getSportIconResource(group.getSportType());
        imgSportMini.setImageResource(sportIconRes);

        // סוג הספורט
        if (group.getSportType() != null) {
            String sportName = formatEnumName(group.getSportType().name());
            tvSport.setText(sportName);
        } else {
            tvSport.setText("N/A");
        }

        // רמת קושי (באמצעות Chip)
        if (group.getLevel() != null) {
            String levelName = formatEnumName(group.getLevel().name());
            chipLevel.setText(levelName);
            chipLevel.setVisibility(View.VISIBLE);
        } else {
            chipLevel.setVisibility(View.GONE);
        }

        // מיקום (לחיץ - פותח ניווט)
        if (group.getLocation() != null && group.getLocation().getAddress() != null && !group.getLocation().getAddress().isEmpty()) {
            tvLocation.setText(group.getLocation().getAddress());

            // הוספת קו תחתון כדי לסמן שזה לחיץ
            tvLocation.setPaintFlags(tvLocation.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);

            // אירוע לחיצה לפתיחת המפה
            tvLocation.setOnClickListener(v -> openNavigationApp());

            // עשינו גם את האייקון לחיץ דרך ה-Layout
            layoutLocation.setOnClickListener(v -> openNavigationApp());

        } else {
            layoutLocation.setVisibility(View.GONE);
        }

        // מספר משתתפים (לחיץ - מעביר למסך חברי הקבוצה)
        int membersCount = group.getMembers() != null ? group.getMembers().size() : 0;
        tvMembers.setText(membersCount + " Members");

        // הוספת קו תחתון לטקסט
        tvMembers.setPaintFlags(tvMembers.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);

        // אירוע לחיצה למעבר לרשימת המשתתפים
        tvMembers.setOnClickListener(v -> {
            dialog.dismiss(); // סוגר את הדיאלוג לפני המעבר
            Intent intent = new Intent(context, MembersListActivity.class);
            intent.putExtra("GROUP_ID", group.getId());
            context.startActivity(intent);
        });

        // --- סוף עדכון המידע החדש ---

        // טיפול בתמונה הראשית
        String base64Image = group.getGroupImage();

        if (base64Image != null && !base64Image.isEmpty()) {
            Bitmap bitmap = ImageUtil.convertFrom64base(base64Image);
            if (bitmap != null) {
                imgIcon.setImageBitmap(bitmap);
                imgIcon.setPadding(0, 0, 0, 0);
                imgIcon.setImageTintList(null);
            } else {
                setFallbackIcon(imgIcon, sportIconRes);
            }
        } else {
            setFallbackIcon(imgIcon, sportIconRes);
        }

        if (isAdminPanel) {
            layoutAdminActions.setVisibility(View.VISIBLE);
            btnEdit.setOnClickListener(v -> {
                dialog.dismiss();
                if (adminListener != null) adminListener.onEdit(group);
            });
            btnDelete.setOnClickListener(v -> {
                dialog.dismiss();
                if (adminListener != null) adminListener.onDelete(group);
            });
        } else {
            layoutAdminActions.setVisibility(View.GONE);
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void openNavigationApp() {
        if (group.getLocation() == null) return;

        String address = group.getLocation().getAddress();
        double lat = group.getLocation().getLatitude();
        double lng = group.getLocation().getLongitude();

        Uri locationUri = Uri.parse("geo:0,0?q=" + lat + "," + lng + "(" + Uri.encode(address) + ")");
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, locationUri);
        Intent chooser = Intent.createChooser(mapIntent, "Navigate to group location with...");
        try {
            context.startActivity(chooser);
        } catch (Exception e) {
            Toast.makeText(context, "No navigation app found", Toast.LENGTH_SHORT).show();
        }
    }

    private void setFallbackIcon(ImageView imgIcon, int sportIconRes) {
        imgIcon.setImageResource(sportIconRes);
        int padding = (int) (12 * context.getResources().getDisplayMetrics().density);
        imgIcon.setPadding(padding, padding, padding, padding);
        imgIcon.setImageTintList(ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.fitlinkPrimary)
        ));
    }

    private int getSportIconResource(SportType type) {
        if (type == null) return R.drawable.ic_sport;

        if (type == SportType.RUNNING) {
            return R.drawable.ic_running;
        } else if (type == SportType.SWIMMING) {
            return R.drawable.ic_swimming;
        } else if (type == SportType.CYCLING) {
            return R.drawable.ic_cycling;
        }
        return R.drawable.ic_sport;
    }

    private String formatEnumName(String name) {
        if (name == null || name.isEmpty()) return "";
        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }

    public interface AdminActionsListener {
        void onEdit(Group group);

        void onDelete(Group group);
    }
}