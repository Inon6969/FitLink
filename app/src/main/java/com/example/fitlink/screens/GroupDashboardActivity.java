package com.example.fitlink.screens;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.fitlink.R;
import com.example.fitlink.models.Group;
import com.example.fitlink.screens.dialogs.CreateEventDialog;
import com.example.fitlink.screens.dialogs.DeleteGroupDialog;
import com.example.fitlink.screens.dialogs.EditGroupDialog;
import com.example.fitlink.screens.dialogs.LeaveGroupDialog;
import com.example.fitlink.services.DatabaseService;
import com.example.fitlink.utils.SharedPreferencesUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class GroupDashboardActivity extends BaseActivity {

    private Group currentGroup;
    private CreateEventDialog currentCreateEventDialog;
    private EditGroupDialog currentEditGroupDialog; // המשתנה החדש לדיאלוג העריכה

    private final ActivityResultLauncher<Intent> mapPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String address = result.getData().getStringExtra("address");
                    double lat = result.getData().getDoubleExtra("lat", 0);
                    double lng = result.getData().getDoubleExtra("lng", 0);

                    // ניתוב התוצאה לדיאלוג הפתוח הנכון (יצירת אירוע או עריכת קבוצה)
                    if (currentCreateEventDialog != null && currentCreateEventDialog.isShowing()) {
                        currentCreateEventDialog.updateLocationDetails(address, lat, lng);
                    } else if (currentEditGroupDialog != null && currentEditGroupDialog.isShowing()) {
                        currentEditGroupDialog.updateLocationDetails(address, lat, lng);
                    }
                }
            }
    );

    public ActivityResultLauncher<Intent> getMapPickerLauncher() {
        return mapPickerLauncher;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_group_dashboard);

        currentGroup = (Group) getIntent().getSerializableExtra("GROUP_EXTRA");
        if (currentGroup == null) {
            Toast.makeText(this, "Error loading group details", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
    }

    private void initViews() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_group_dashboard), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbar_group_dashboard);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(currentGroup.getName());
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }

        TextView tvTitle = findViewById(R.id.tv_dashboard_title);
        tvTitle.setText(currentGroup.getName());

        TextView tvDescription = findViewById(R.id.tv_dashboard_description);
        if (currentGroup.getDescription() != null && !currentGroup.getDescription().trim().isEmpty()) {
            tvDescription.setText(currentGroup.getDescription());
        } else {
            tvDescription.setText("No description provided for this group.");
        }

        MaterialButton btnMembers = findViewById(R.id.btn_dashboard_members);
        MaterialButton btnChat = findViewById(R.id.btn_dashboard_chat);
        MaterialButton btnCalendar = findViewById(R.id.btn_dashboard_calendar);

        MaterialCardView cardSchedule = findViewById(R.id.card_dashboard_schedule);
        MaterialButton btnSchedule = findViewById(R.id.btn_dashboard_schedule);

        MaterialCardView cardEdit = findViewById(R.id.card_dashboard_edit);
        MaterialButton btnEdit = findViewById(R.id.btn_dashboard_edit);
        MaterialButton btnDelete = findViewById(R.id.btn_dashboard_delete);

        MaterialButton btnLeave = findViewById(R.id.btn_dashboard_leave);

        String currentUserId = SharedPreferencesUtil.getUserId(this);

        // הגדרת הרשאות ותצוגה: מנהל יראה אפשרויות ניהול, משתמש רגיל יראה רק אופציה לעזוב
        if (currentGroup.getAdminId() != null && currentGroup.getAdminId().equals(currentUserId)) {
            cardSchedule.setVisibility(View.VISIBLE);
            cardEdit.setVisibility(View.VISIBLE);
            btnDelete.setVisibility(View.VISIBLE);

            btnLeave.setVisibility(View.GONE);
        } else {
            cardSchedule.setVisibility(View.GONE);
            cardEdit.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);

            btnLeave.setVisibility(View.VISIBLE);
        }

        btnSchedule.setOnClickListener(v -> {
            currentCreateEventDialog = new CreateEventDialog(this, currentGroup);
            currentCreateEventDialog.show();
        });

        // פתיחת דיאלוג עריכת הקבוצה ועדכון התצוגה לאחר שמירה
        btnEdit.setOnClickListener(v -> {
            currentEditGroupDialog = new EditGroupDialog(this, currentGroup, updatedGroup -> {
                // מתרחש כשהשמירה מצליחה: מעדכן את האובייקט ואת התצוגה בממשק
                currentGroup = updatedGroup;

                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(currentGroup.getName());
                }
                tvTitle.setText(currentGroup.getName());

                if (currentGroup.getDescription() != null && !currentGroup.getDescription().trim().isEmpty()) {
                    tvDescription.setText(currentGroup.getDescription());
                } else {
                    tvDescription.setText("No description provided for this group.");
                }
            });
            currentEditGroupDialog.show();
        });

        btnDelete.setOnClickListener(v -> {
            // הצגת הדיאלוג המעוצב החדש במקום ה-AlertDialog הישן
            new DeleteGroupDialog(this, () -> {
                Toast.makeText(this, "Deleting group...", Toast.LENGTH_SHORT).show();

                // קריאה לפונקציית המחיקה ב-DatabaseService
                DatabaseService.getInstance().deleteGroup(currentGroup.getId(), new DatabaseService.DatabaseCallback<Void>() {
                    @Override
                    public void onCompleted(Void object) {
                        Toast.makeText(GroupDashboardActivity.this, "Group deleted successfully", Toast.LENGTH_SHORT).show();
                        finish(); // חזרה לרשימת הקבוצות
                    }

                    @Override
                    public void onFailed(Exception e) {
                        Toast.makeText(GroupDashboardActivity.this, "Failed to delete group", Toast.LENGTH_SHORT).show();
                    }
                });
            }).show();
        });

        btnLeave.setOnClickListener(v -> {
            new LeaveGroupDialog(this, currentGroup, () -> {
                DatabaseService.getInstance().leaveGroup(currentGroup.getId(), currentUserId, new DatabaseService.DatabaseCallback<Void>() {
                    @Override
                    public void onCompleted(Void object) {
                        Toast.makeText(GroupDashboardActivity.this, "Left group successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onFailed(Exception e) {
                        Toast.makeText(GroupDashboardActivity.this, "Failed to leave group", Toast.LENGTH_SHORT).show();
                    }
                });
            }).show();
        });

        btnMembers.setOnClickListener(v -> {
            Intent intent = new Intent(GroupDashboardActivity.this, MembersListActivity.class);
            intent.putExtra("GROUP_EXTRA", currentGroup);
            startActivity(intent);
        });

        btnChat.setOnClickListener(v -> Toast.makeText(this, "Opening Chat...", Toast.LENGTH_SHORT).show());

        btnCalendar.setOnClickListener(v -> {
            Intent intent = new Intent(GroupDashboardActivity.this, GroupCalendarActivity.class);
            intent.putExtra("GROUP_EXTRA", currentGroup);
            startActivity(intent);
        });
    }
}