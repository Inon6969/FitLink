package com.example.fitlink.screens;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.fitlink.R;
import com.example.fitlink.models.Group;
import com.example.fitlink.screens.dialogs.LeaveGroupDialog;
import com.example.fitlink.services.DatabaseService;
import com.example.fitlink.utils.SharedPreferencesUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class GroupDashboardActivity extends BaseActivity {

    private Group currentGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_group_dashboard);

        // מקבל את אובייקט הקבוצה מהמסך הקודם
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

        // כותרת
        TextView tvTitle = findViewById(R.id.tv_dashboard_title);
        tvTitle.setText(currentGroup.getName());

        // תיאור הקבוצה
        TextView tvDescription = findViewById(R.id.tv_dashboard_description);
        if (currentGroup.getDescription() != null && !currentGroup.getDescription().trim().isEmpty()) {
            tvDescription.setText(currentGroup.getDescription());
        } else {
            tvDescription.setText("No description provided for this group.");
        }

        MaterialButton btnMembers = findViewById(R.id.btn_dashboard_members);
        MaterialButton btnChat = findViewById(R.id.btn_dashboard_chat);
        MaterialButton btnCalendar = findViewById(R.id.btn_dashboard_calendar);
        MaterialButton btnSchedule = findViewById(R.id.btn_dashboard_schedule);
        MaterialButton btnLeave = findViewById(R.id.btn_dashboard_leave);
        MaterialCardView cardSchedule = findViewById(R.id.card_dashboard_schedule);

        String currentUserId = SharedPreferencesUtil.getUserId(this);

        // מציג את כפתור היצירה/קביעת אירוע רק אם המשתמש הוא היוצר של הקבוצה.
        // בנוסף, נסתיר ממנו את אפשרות ה"עזיבה" (כפי שהגדרנו גם במסך הרשימה).
        if (currentGroup.getAdminId() != null && currentGroup.getAdminId().equals(currentUserId)) {
            cardSchedule.setVisibility(View.VISIBLE);
            btnLeave.setVisibility(View.GONE); // מנהל לא יכול לעזוב את הקבוצה של עצמו
        } else {
            cardSchedule.setVisibility(View.GONE);
            btnLeave.setVisibility(View.VISIBLE); // משתמש רגיל יכול לעזוב
        }

        // הגדרת פעולת עזיבה
        btnLeave.setOnClickListener(v -> {
            new LeaveGroupDialog(this, currentGroup, () -> {
                DatabaseService.getInstance().leaveGroup(currentGroup.getId(), currentUserId, new DatabaseService.DatabaseCallback<Void>() {
                    @Override
                    public void onCompleted(Void object) {
                        Toast.makeText(GroupDashboardActivity.this, "Left group successfully", Toast.LENGTH_SHORT).show();
                        finish(); // סוגר את הדשבורד וחוזר אחורה למסך הקבוצות
                    }

                    @Override
                    public void onFailed(Exception e) {
                        Toast.makeText(GroupDashboardActivity.this, "Failed to leave group", Toast.LENGTH_SHORT).show();
                    }
                });
            }).show();
        });

        // הגדרת פעולות לכפתורים - ישמש כהכנה למסכים העתידיים
        btnMembers.setOnClickListener(v -> Toast.makeText(this, "Opening Members...", Toast.LENGTH_SHORT).show());
        btnChat.setOnClickListener(v -> Toast.makeText(this, "Opening Chat...", Toast.LENGTH_SHORT).show());
        btnCalendar.setOnClickListener(v -> Toast.makeText(this, "Opening Calendar...", Toast.LENGTH_SHORT).show());
        btnSchedule.setOnClickListener(v -> Toast.makeText(this, "Opening Scheduler...", Toast.LENGTH_SHORT).show());
    }
}