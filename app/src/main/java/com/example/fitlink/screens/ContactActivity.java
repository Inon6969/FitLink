package com.example.fitlink.screens;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
import com.example.fitlink.models.User;
import com.example.fitlink.services.DatabaseService;
import com.example.fitlink.utils.ImageUtil;
import com.example.fitlink.utils.SharedPreferencesUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;

public class ContactActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {

    private TextInputEditText etMessage;
    private TextView tvSendingAs;
    private User currentUser;
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_contact);

        // Fetch the logged-in user details
        currentUser = SharedPreferencesUtil.getUser(this);

        initViews();
        setupNavigationDrawer(currentUser);
        setupClickListeners();

        // Display who the message will be sent as
        if (currentUser != null) {
            tvSendingAs.setText("Sending as: " + currentUser.getFullName() + " (" + currentUser.getEmail() + ")");
        } else {
            tvSendingAs.setText("Sending as: Unknown User");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // מוודא שהתפריט מסמן את "Contact Us" בכל פעם שחוזרים למסך הזה
        NavigationView navigationView = findViewById(R.id.nav_view);
        if (navigationView != null) {
            navigationView.setCheckedItem(R.id.nav_contact);
        }
    }

    private void initViews() {
        // מאזין לכל ה-DrawerLayout כדי שנוכל לשלוט בריווח ה-Status Bar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // מחיל על המסך המרכזי
            findViewById(R.id.main_contact_layout).setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);

            // מחיל על תפריט הצד
            View navView = findViewById(R.id.nav_view);
            if (navView != null) {
                navView.setPadding(0, systemBars.top, 0, systemBars.bottom);
            }
            return insets;
        });

        etMessage = findViewById(R.id.et_contact_message);
        tvSendingAs = findViewById(R.id.tv_contact_sending_as);
    }

    private void setupNavigationDrawer(User user) {
        Toolbar toolbar = findViewById(R.id.toolbar_contact);
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

        // סימון מסך יצירת הקשר בתפריט
        navigationView.setCheckedItem(R.id.nav_contact);

        // קריאה לפונקציה שמעדכנת את תצוגת המשתמש ב-Header של התפריט
        updateNavHeader(user, navigationView);

        // צביעת כפתור ה-Log Out באדום
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

        View headerView = navigationView.getHeaderView(0);
        ImageView imgProfile = headerView.findViewById(R.id.img_header_logo);
        TextView tvName = headerView.findViewById(R.id.tv_header_title);
        TextView tvEmail = headerView.findViewById(R.id.tv_header_subtitle);

        if (user.getFullName() != null && !user.getFullName().isEmpty()) {
            tvName.setText(user.getFullName());
        }
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            tvEmail.setText(user.getEmail());
        }

        String base64Image = user.getProfileImage();
        if (base64Image != null && !base64Image.isEmpty()) {
            Bitmap bmp = ImageUtil.convertFrom64base(base64Image);
            if (bmp != null) {
                imgProfile.setImageBitmap(bmp);
            } else {
                imgProfile.setImageResource(R.drawable.ic_user);
            }
        } else {
            imgProfile.setImageResource(R.drawable.ic_user);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        } else if (id == R.id.nav_account) {
            Intent intent = new Intent(this, UserProfileActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        } else if (id == R.id.nav_contact) {
            // כבר נמצאים במסך צור קשר
        } else if (id == R.id.nav_logout) {
            logout();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void setupClickListeners() {
        MaterialCardView cardEmail = findViewById(R.id.card_contact_email);
        MaterialCardView cardPhone = findViewById(R.id.card_contact_phone);
        MaterialButton btnSend = findViewById(R.id.btn_contact_send);

        cardEmail.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:support@fitlink.com"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Support Request from FitLink App");
            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show();
            }
        });

        cardPhone.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:+972501234567"));
            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Cannot open dialer", Toast.LENGTH_SHORT).show();
            }
        });

        btnSend.setOnClickListener(v -> handleSendMessage());
    }

    private void handleSendMessage() {
        String message = Objects.requireNonNull(etMessage.getText()).toString().trim();

        if (currentUser == null) {
            Toast.makeText(this, "Error: User identity not found. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (message.isEmpty()) {
            etMessage.setError("Please enter a message");
            etMessage.requestFocus();
            return;
        }

        String userId = currentUser.getId();
        String name = currentUser.getFullName();
        String email = currentUser.getEmail();
        String phone = (currentUser.getPhone() != null && !currentUser.getPhone().isEmpty()) ? currentUser.getPhone() : "No phone provided";

        databaseService.sendContactMessage(userId, name, email, phone, message, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                Toast.makeText(ContactActivity.this, "Message sent successfully!", Toast.LENGTH_LONG).show();
                etMessage.setText("");
                etMessage.clearFocus();
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(ContactActivity.this, "Failed to send: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}