package com.example.fitlink.screens;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.fitlink.R;
import com.example.fitlink.models.Event;
import com.example.fitlink.models.User;
import com.example.fitlink.services.DatabaseService;
import com.example.fitlink.utils.EventReminderScheduler;
import com.example.fitlink.utils.ImageUtil;
import com.example.fitlink.utils.SharedPreferencesUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends BaseActivity implements View.OnClickListener, NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";

    private MaterialButton btnJoinorCreateGroup;
    private MaterialButton btnJoinorCreateEvent;
    private MaterialButton btnMyGroups;
    private MaterialButton btnMyCalendar;
    private MaterialButton btnAdminPanel;

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // מאזין לכל ה-DrawerLayout כדי שנוכל לשלוט גם במסך וגם בתפריט
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // 1. מחיל את הריווח על המסך המרכזי (CoordinatorLayout)
            findViewById(R.id.main).setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);

            // 2. מחיל את הריווח רק מלמעלה ומלמטה על תפריט הצד (NavigationView)
            // זה ידחוף את ה-Header בדיוק אל מתחת ל-Status Bar מבלי לפגוע בעיצוב!
            findViewById(R.id.nav_view).setPadding(0, systemBars.top, 0, systemBars.bottom);

            return insets;
        });

        // קבלת המשתמש המחובר כרגע
        User user = SharedPreferencesUtil.getUser(this);
        Log.d(TAG, "User: " + user);

        // Find views
        btnJoinorCreateGroup = findViewById(R.id.btn_join_or_create_group);
        btnJoinorCreateEvent = findViewById(R.id.btn_join_or_create_event);
        btnMyGroups = findViewById(R.id.btn_my_groups);
        btnMyCalendar = findViewById(R.id.btn_my_calendar);
        btnAdminPanel = findViewById(R.id.btn_admin_panel);

        // Set click listeners for main buttons
        btnJoinorCreateGroup.setOnClickListener(this);
        btnJoinorCreateEvent.setOnClickListener(this);
        btnMyGroups.setOnClickListener(this);
        btnMyCalendar.setOnClickListener(this);
        btnAdminPanel.setOnClickListener(this);

        // Show admin card only if user is admin
        if (user != null && user.getIsAdmin()) {
            findViewById(R.id.admin_card).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.admin_card).setVisibility(View.GONE);
        }

        // מעביר את אובייקט המשתמש להגדרת התפריט כדי שנוכל להציג את פרטיו
        setupNavigationDrawer(user);

        // מבקש הרשאת התראות באנדרואיד 13+ (אם עדיין אין)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        // --- ניהול והפעלת התראות ---
        String currentUserId = SharedPreferencesUtil.getUserId(this);
        if (currentUserId != null) {
            // 1. האזנה לבקשות הצטרפות לקבוצות (בזמן אמת)
            DatabaseService.getInstance().listenForNewJoinRequests(currentUserId, this);

            // 2. האזנה להודעות חדשות בצ'אט הקבוצתי (בזמן אמת)
            DatabaseService.getInstance().listenForNewChatMessages(currentUserId, this);

            // 3. האזנה לאירועים חדשים בקבוצות (בזמן אמת)
            DatabaseService.getInstance().listenForNewEvents(currentUserId, this);

            // 4. תזמון מחדש של התראות תזכורת לכל האירועים העתידיים של המשתמש
            scheduleFutureEventReminders(currentUserId);
        }
    }

    /**
     * פונקציית עזר העוברת על כל האירועים של המשתמש ומתזמנת עבורם
     * התראה ל-24 שעות לפני תחילת האירוע באמצעות WorkManager
     */
    private void scheduleFutureEventReminders(String userId) {
        DatabaseService.getInstance().getUser(userId, new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(User currentUser) {
                if (currentUser != null && currentUser.getEventIds() != null) {
                    for (String eventId : currentUser.getEventIds().keySet()) {
                        DatabaseService.getInstance().getEvent(eventId, new DatabaseService.DatabaseCallback<>() {
                            @Override
                            public void onCompleted(Event event) {
                                // מתזמן רק אירועים שעדיין לא התחילו
                                if (event != null && event.getStartTimestamp() > System.currentTimeMillis()) {
                                    EventReminderScheduler.scheduleReminder(MainActivity.this, event);
                                }
                            }

                            @Override
                            public void onFailed(Exception e) {
                                Log.e(TAG, "Failed to load event for scheduling reminder", e);
                            }
                        });
                    }
                }
            }

            @Override
            public void onFailed(Exception e) {
                Log.e(TAG, "Failed to load user for scheduling event reminders", e);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // מוודא שהתפריט מסמן את "Home" בכל פעם שחוזרים למסך הזה
        NavigationView navigationView = findViewById(R.id.nav_view);
        if (navigationView != null) {
            navigationView.setCheckedItem(R.id.nav_home);
        }
    }

    private void setupNavigationDrawer(User user) {
        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.nav_home);

        // קריאה לפונקציה שמעדכנת את תצוגת המשתמש ב-Header של התפריט
        updateNavHeader(user, navigationView);

        // --- צביעת כפתור ה-Log Out באדום ---
        android.view.Menu menu = navigationView.getMenu();
        android.view.MenuItem logoutItem = menu.findItem(R.id.nav_logout);

        if (logoutItem != null) {
            android.text.SpannableString s = new android.text.SpannableString(logoutItem.getTitle());
            s.setSpan(new android.text.style.ForegroundColorSpan(android.graphics.Color.parseColor("#FF4C4C")), 0, s.length(), 0);
            logoutItem.setTitle(s);
        }
    }

    private void updateNavHeader(User user, NavigationView navigationView) {
        if (user == null) return;

        // משיג את ה-View של ה-Header מתוך התפריט
        View headerView = navigationView.getHeaderView(0);

        ImageView imgProfile = headerView.findViewById(R.id.img_header_logo);
        TextView tvName = headerView.findViewById(R.id.tv_header_title);
        TextView tvEmail = headerView.findViewById(R.id.tv_header_subtitle);

        // עדכון שם המשתמש
        if (user.getFullName() != null && !user.getFullName().isEmpty()) {
            tvName.setText(user.getFullName());
        }

        // עדכון אימייל
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            tvEmail.setText(user.getEmail());
        }

        // עדכון תמונת פרופיל (אם קיימת)
        String base64Image = user.getProfileImage();
        if (base64Image != null && !base64Image.isEmpty()) {
            Bitmap bmp = ImageUtil.convertFrom64base(base64Image);
            if (bmp != null) {
                imgProfile.setImageBitmap(bmp);
            } else {
                imgProfile.setImageResource(R.drawable.ic_user); // תמונת ברירת מחדל
            }
        } else {
            imgProfile.setImageResource(R.drawable.ic_user); // תמונת ברירת מחדל
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            // כבר במסך הבית, רק נסגור את התפריט
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        }

        if (id == R.id.nav_account) {
            Log.d(TAG, "Account clicked from nav");
            Intent intent = new Intent(this, UserProfileActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        } else if (id == R.id.nav_contact) {
            Log.d(TAG, "Contact clicked from nav");
            Intent intent = new Intent(this, ContactActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        } else if (id == R.id.nav_logout) {
            Log.d(TAG, "Sign out clicked from nav");
            logout();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        // אם התפריט פתוח, סגור אותו קודם. אחרת, צא מהאפליקציה כרגיל
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == btnJoinorCreateGroup.getId()) {
            startActivity(new Intent(this, GroupsListActivity.class));
            return;
        }
        if (id == btnJoinorCreateEvent.getId()) {
            startActivity(new Intent(this, EventsListActivity.class));
            return;
        }
        if (id == btnMyGroups.getId()) {
            startActivity(new Intent(this, MyGroupsActivity.class));
            return;
        }
        if (id == btnMyCalendar.getId()) {
            startActivity(new Intent(this, MyCalendarActivity.class));
            return;
        }
        if (id == btnAdminPanel.getId()) {
            startActivity(new Intent(this, AdminActivity.class));
            return;
        }
    }
}