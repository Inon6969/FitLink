package com.example.fitlink.screens;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.fitlink.R;
import com.example.fitlink.models.Event;
import com.example.fitlink.models.User;
import com.example.fitlink.screens.dialogs.EditUserDialog;
import com.example.fitlink.screens.dialogs.ProfileImageDialog;
import com.example.fitlink.services.DatabaseService;
import com.example.fitlink.utils.ImageUtil;
import com.example.fitlink.utils.SharedPreferencesUtil;
import com.google.android.material.navigation.NavigationView;

import java.util.List;
import java.util.Objects;

public class UserProfileActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final int REQ_CAMERA = 100;
    private static final int REQ_GALLERY = 200;
    private static final int REQ_CAMERA_PERMISSION = 101; // קבוע עבור בקשת הרשאת מצלמה

    private TextView txtTitle, txtRank, txtFirstName, txtLastName, txtEmail, txtPhone, txtPassword;
    private TextView txtStatGroups, txtStatUpcoming, txtStatCompleted;
    private ImageView imgUserProfile, imgTogglePassword;

    private DrawerLayout drawerLayout;

    private User user;
    private String viewedUserId;
    private boolean isCurrentUser;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_profile);

        String currentUserId = SharedPreferencesUtil.getUserId(this);

        viewedUserId = getIntent().getStringExtra("USER_ID");
        if (viewedUserId == null || viewedUserId.isEmpty()) {
            viewedUserId = currentUserId;
        }

        isCurrentUser = viewedUserId.equals(currentUserId);

        initViews();
        loadUserData();

        User loggedInUser = SharedPreferencesUtil.getUser(this);
        setupNavigationDrawer(loggedInUser);
    }

    @Override
    protected void onResume() {
        super.onResume();
        NavigationView navigationView = findViewById(R.id.nav_view);
        if (navigationView != null && isCurrentUser) {
            navigationView.setCheckedItem(R.id.nav_account);
        }
    }

    private void initViews() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            findViewById(R.id.detailsAboutUserPage).setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            View navView = findViewById(R.id.nav_view);
            if (navView != null) {
                navView.setPadding(0, systemBars.top, 0, systemBars.bottom);
            }
            return insets;
        });

        Button btnEditUser = findViewById(R.id.btn_DetailsAboutUser_edit_user);
        Button btnChangePhoto = findViewById(R.id.btn_DetailsAboutUser_change_photo);

        txtTitle = findViewById(R.id.txt_DetailsAboutUser_title);
        txtRank = findViewById(R.id.txt_DetailsAboutUser_rank);
        txtFirstName = findViewById(R.id.txt_DetailsAboutUser_first_name);
        txtLastName = findViewById(R.id.txt_DetailsAboutUser_last_name);
        txtEmail = findViewById(R.id.txt_DetailsAboutUser_email);
        txtPhone = findViewById(R.id.txt_DetailsAboutUser_phone);
        txtPassword = findViewById(R.id.txt_DetailsAboutUser_password);

        txtStatGroups = findViewById(R.id.txt_stat_groups);
        txtStatUpcoming = findViewById(R.id.txt_stat_upcoming);
        txtStatCompleted = findViewById(R.id.txt_stat_completed);

        imgUserProfile = findViewById(R.id.img_DetailsAboutUser_user_profile);
        imgTogglePassword = findViewById(R.id.img_DetailsAboutUser_toggle_password);

        LinearLayout layoutPasswordContainer = findViewById(R.id.layout_DetailsAboutUser_password_container);

        if (!isCurrentUser) {
            btnEditUser.setVisibility(View.GONE);
            btnChangePhoto.setVisibility(View.GONE);
            layoutPasswordContainer.setVisibility(View.GONE);

            txtPhone.setPaintFlags(txtPhone.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
            txtPhone.setOnClickListener(v -> {
                if (user != null && user.getPhone() != null && !user.getPhone().isEmpty()) {
                    Intent dialIntent = new Intent(Intent.ACTION_DIAL);
                    dialIntent.setData(android.net.Uri.parse("tel:" + user.getPhone()));
                    try {
                        startActivity(dialIntent);
                    } catch (Exception e) {
                        Toast.makeText(this, "No dialer app found", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            txtEmail.setPaintFlags(txtEmail.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
            txtEmail.setOnClickListener(v -> {
                if (user != null && user.getEmail() != null && !user.getEmail().isEmpty()) {
                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                    emailIntent.setData(android.net.Uri.parse("mailto:" + user.getEmail()));
                    try {
                        startActivity(emailIntent);
                    } catch (Exception e) {
                        Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show();
                    }
                }
            });

        } else {
            btnEditUser.setOnClickListener(v -> openEditDialog());
            btnChangePhoto.setOnClickListener(v -> openImagePicker());

            txtPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            imgTogglePassword.setOnClickListener(v -> {
                isPasswordVisible = !isPasswordVisible;
                if (isPasswordVisible) {
                    txtPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    imgTogglePassword.setImageResource(R.drawable.ic_visibility);
                } else {
                    txtPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    imgTogglePassword.setImageResource(R.drawable.ic_visibility_off);
                }
            });
        }

        imgUserProfile.setOnClickListener(v -> {
            if (user != null && user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
                showFullImageDialog();
            }
        });
    }

    private void setupNavigationDrawer(User loggedInUser) {
        Toolbar toolbar = findViewById(R.id.toolbar_user_profile);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        if (!isCurrentUser) {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        } else {
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawerLayout, toolbar,
                    R.string.navigation_drawer_open,
                    R.string.navigation_drawer_close
            );
            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();

            navigationView.setNavigationItemSelectedListener(this);
            navigationView.setCheckedItem(R.id.nav_account);
            updateNavHeader(loggedInUser, navigationView);

            android.view.Menu menu = navigationView.getMenu();
            android.view.MenuItem logoutItem = menu.findItem(R.id.nav_logout);

            if (logoutItem != null) {
                android.text.SpannableString s = new android.text.SpannableString(logoutItem.getTitle());
                s.setSpan(new android.text.style.ForegroundColorSpan(android.graphics.Color.parseColor("#FF4C4C")), 0, s.length(), 0);
                logoutItem.setTitle(s);
            }
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
            // Already here
        } else if (id == R.id.nav_contact) {
            Intent intent = new Intent(this, ContactActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
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

    private void loadUserData() {
        if (isCurrentUser) {
            user = SharedPreferencesUtil.getUser(this);
            updateUI();

            DatabaseService.getInstance().getUser(viewedUserId, new DatabaseService.DatabaseCallback<>() {
                @Override
                public void onCompleted(User freshUser) {
                    if (freshUser != null) {
                        user = freshUser;
                        SharedPreferencesUtil.saveUser(UserProfileActivity.this, user);
                        updateUI();
                    }
                }

                @Override
                public void onFailed(Exception e) {
                }
            });
        } else {
            DatabaseService.getInstance().getUser(viewedUserId, new DatabaseService.DatabaseCallback<>() {
                @Override
                public void onCompleted(User fetchedUser) {
                    if (fetchedUser != null) {
                        user = fetchedUser;
                        updateUI();
                    } else {
                        Toast.makeText(UserProfileActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }

                @Override
                public void onFailed(Exception e) {
                    Toast.makeText(UserProfileActivity.this, "Failed to load user", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        }
    }

    private void updateUI() {
        if (user == null) return;

        if (getSupportActionBar() != null) {
            if (isCurrentUser) {
                getSupportActionBar().setTitle("My Profile");
            } else {
                getSupportActionBar().setTitle(user.getFirstName() + "'s Profile");
            }
        }

        txtTitle.setText(user.getFullName());
        txtFirstName.setText(user.getFirstName());
        txtLastName.setText(user.getLastName());
        txtEmail.setText(user.getEmail());
        txtPhone.setText(user.getPhone());

        if (isCurrentUser) {
            txtPassword.setText(user.getPassword());
        }

        if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
            Bitmap bmp = ImageUtil.convertFrom64base(user.getProfileImage());
            if (bmp != null) {
                imgUserProfile.setImageBitmap(bmp);
            } else {
                imgUserProfile.setImageResource(R.drawable.ic_user);
            }
        } else {
            imgUserProfile.setImageResource(R.drawable.ic_user);
        }

        loadUserStats();
    }

    private void loadUserStats() {
        int groupsCount = user.getGroupIds() != null ? user.getGroupIds().size() : 0;
        txtStatGroups.setText(String.valueOf(groupsCount));

        DatabaseService.getInstance().getAllEvents(new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(List<Event> events) {
                int upcomingCount = 0;
                int completedCount = user.getPastEventsCount();
                long currentTime = System.currentTimeMillis();

                if (events != null) {
                    for (Event event : events) {
                        if (event.getParticipants() != null && event.getParticipants().containsKey(user.getId())) {
                            if (event.getEndTimestamp() < currentTime) {
                                completedCount++;
                            } else {
                                upcomingCount++;
                            }
                        }
                    }
                }

                txtStatUpcoming.setText(String.valueOf(upcomingCount));
                txtStatCompleted.setText(String.valueOf(completedCount));

                updateUserRank(completedCount);
            }

            @Override
            public void onFailed(Exception e) {
                txtStatUpcoming.setText("-");
                txtStatCompleted.setText("-");
            }
        });
    }

    private void updateUserRank(int totalCompleted) {
        String rankText;
        if (totalCompleted == 0) {
            rankText = "🌱 Rookie";
        } else if (totalCompleted <= 3) {
            rankText = "🥉 Beginner";
        } else if (totalCompleted <= 10) {
            rankText = "🥈 Active Member";
        } else if (totalCompleted <= 25) {
            rankText = "🥇 Fitness Enthusiast";
        } else {
            rankText = "🏆 Pro Athlete";
        }

        txtRank.setText(rankText);
    }

    private void openEditDialog() {
        EditUserDialog editDialog = new EditUserDialog(this, user, this::updateUI);
        editDialog.show();
    }

    /**
     * פותח את דיאלוג בחירת התמונה ובודק הרשאות מצלמה במידת הצורך.
     */
    private void openImagePicker() {
        boolean hasImage = user.getProfileImage() != null && !user.getProfileImage().isEmpty();

        new ProfileImageDialog(this, hasImage, false, new ProfileImageDialog.ImagePickerListener() {
            @Override
            public void onCamera() {
                // בדיקה האם יש הרשאת מצלמה
                if (ContextCompat.checkSelfPermission(UserProfileActivity.this, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {

                    // בקשת הרשאה מהמשתמש בזמן ריצה
                    ActivityCompat.requestPermissions(UserProfileActivity.this,
                            new String[]{Manifest.permission.CAMERA},
                            REQ_CAMERA_PERMISSION);
                } else {
                    // אם כבר יש הרשאה, נפתח את המצלמה
                    openCameraIntent();
                }
            }

            @Override
            public void onGallery() {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, REQ_GALLERY);
            }

            @Override
            public void onDelete() {
                deleteProfileImage();
            }
        }).show();
    }

    /**
     * פונקציית עזר להפעלת ה-Intent של המצלמה.
     */
    private void openCameraIntent() {
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, REQ_CAMERA);
    }

    /**
     * מטפל בתשובת המשתמש לבקשת ההרשאה.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // המשתמש אישר, פותחים מצלמה
                openCameraIntent();
            } else {
                // המשתמש דחה
                Toast.makeText(this, "נדרשת הרשאת מצלמה כדי לצלם תמונה", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void deleteProfileImage() {
        user.setProfileImage(null);
        imgUserProfile.setImageResource(R.drawable.ic_user);

        DatabaseService.getInstance().updateUser(user, new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(Void object) {
                SharedPreferencesUtil.saveUser(UserProfileActivity.this, user);
                Toast.makeText(UserProfileActivity.this, "תמונת הפרופיל נמחקה", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(UserProfileActivity.this, "שגיאה במחיקת התמונה", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK) return;

        Bitmap bitmap = null;

        if (requestCode == REQ_CAMERA && data != null) {
            bitmap = (Bitmap) Objects.requireNonNull(data.getExtras()).get("data");
        } else if (requestCode == REQ_GALLERY && data != null) {
            try {
                bitmap = BitmapFactory.decodeStream(
                        getContentResolver().openInputStream(Objects.requireNonNull(data.getData()))
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (bitmap != null) {
            imgUserProfile.setImageBitmap(bitmap);
            String base64 = ImageUtil.convertTo64Base(imgUserProfile);
            user.setProfileImage(base64);
            saveProfileImage();
        }
    }

    private void saveProfileImage() {
        DatabaseService.getInstance().updateUser(user, new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(Void object) {
                SharedPreferencesUtil.saveUser(UserProfileActivity.this, user);
                Toast.makeText(UserProfileActivity.this, "תמונת הפרופיל עודכנה!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(UserProfileActivity.this, "שגיאה בעדכון התמונה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showFullImageDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_full_image);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        ImageView dialogImage = dialog.findViewById(R.id.dialogImage);
        dialogImage.setImageDrawable(imgUserProfile.getDrawable());

        View btnClose = dialog.findViewById(R.id.card_close_full_image);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }
}