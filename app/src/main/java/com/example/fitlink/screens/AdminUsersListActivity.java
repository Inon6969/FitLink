package com.example.fitlink.screens;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitlink.R;
import com.example.fitlink.adapters.UserAdapter;
import com.example.fitlink.models.User;
import com.example.fitlink.screens.dialogs.AddUserDialog;
import com.example.fitlink.screens.dialogs.DeleteUserDialog;
import com.example.fitlink.screens.dialogs.EditUserDialog;
import com.example.fitlink.screens.dialogs.UserFilterDialog;
import com.example.fitlink.services.DatabaseService;
import com.example.fitlink.utils.SharedPreferencesUtil;
import com.google.android.material.button.MaterialButton;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AdminUsersListActivity extends BaseActivity {

    private static final String TAG = "UsersListActivity";

    // UI Elements
    private UserAdapter userAdapter;
    private TextView tvUserCount;
    private EditText etSearch;
    private MaterialButton btnFilterUsers;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private MaterialButton btnAddUser;

    // Data Elements
    private List<User> allUsers = null;

    // שמירת קריטריוני הסינון הנוכחיים מהדיאלוג
    private String currentFilterRole = null;
    private String currentFilterEmail = "";
    private String currentFilterPhone = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_users_list);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupSearchLogic();
        setupAddUserButton();
    }

    private void initViews() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvUserCount = findViewById(R.id.tv_user_count);
        etSearch = findViewById(R.id.edit_UsersTable_search);
        btnFilterUsers = findViewById(R.id.btn_filter_admin_users);
        progressBar = findViewById(R.id.progress_bar);
        emptyState = findViewById(R.id.empty_state);
        btnAddUser = findViewById(R.id.btn_UsersTable_add_user);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }
    }

    private void setupRecyclerView() {
        RecyclerView usersList = findViewById(R.id.rv_users_list);
        usersList.setLayoutManager(new LinearLayoutManager(this));

        userAdapter = new UserAdapter(new UserAdapter.OnUserClickListener() {
            @Override
            public void onUserClick(User user) {
                Intent intent = new Intent(AdminUsersListActivity.this, UserProfileActivity.class);
                intent.putExtra("USER_ID", user.getId());
                startActivity(intent);
            }

            @Override
            public void onEditUser(User user) {
                new EditUserDialog(AdminUsersListActivity.this, user, () -> loadUsers()).show();
            }

            @Override
            public void onToggleAdmin(User user) {
                handleToggleAdmin(user);
            }

            @Override
            public void onDeleteUser(User user) {
                handleDeleteUser(user);
            }

            @Override
            public boolean isCurrentUser(User user) {
                return user.getId().equals(SharedPreferencesUtil.getUserId(AdminUsersListActivity.this));
            }
        });
        usersList.setAdapter(userAdapter);
    }

    private void setupSearchLogic() {
        // האזנה להקלדה בשורת החיפוש (מחפש רק לפי שם)
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFullFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // פתיחת דיאלוג הסינון המתקדם
        btnFilterUsers.setOnClickListener(v -> {
            UserFilterDialog filterDialog = new UserFilterDialog(this);
            // העברת הקריטריונים הנוכחיים
            filterDialog.setInitialCriteria(currentFilterRole, currentFilterEmail, currentFilterPhone);

            filterDialog.setListener((role, email, phone) -> {
                currentFilterRole = role;
                currentFilterEmail = email;
                currentFilterPhone = phone;

                // הפעלת הסינון המלא
                applyFullFilter();
            });
            filterDialog.show();
        });
    }

    // פונקציה שאוספת את כל פרמטרי הסינון ומריצה אותם
    private void applyFullFilter() {
        String query = etSearch.getText().toString();
        filterUsers(query, currentFilterRole, currentFilterEmail, currentFilterPhone);
    }

    private void filterUsers(String query, String role, String filterEmail, String filterPhone) {
        if (allUsers == null) return;

        List<User> filteredList = allUsers.stream().filter(user -> {

            // 1. סינון לפי תפקיד (Role)
            if (role != null) {
                boolean isAdmin = user.getIsAdmin();
                if (role.equals("Admin") && !isAdmin) return false;
                if (role.equals("Regular") && isAdmin) return false;
            }

            // 2. סינון לפי אימייל (מהדיאלוג)
            if (!filterEmail.isEmpty()) {
                if (user.getEmail() == null || !user.getEmail().toLowerCase().contains(filterEmail.toLowerCase())) {
                    return false;
                }
            }

            // 3. סינון לפי טלפון (מהדיאלוג)
            if (!filterPhone.isEmpty()) {
                if (user.getPhone() == null || !user.getPhone().contains(filterPhone)) {
                    return false;
                }
            }

            // 4. סינון לפי טקסט חופשי (חיפוש גלובלי - כעת רק לפי שם המשתמש)
            if (!query.trim().isEmpty()) {
                String q = query.toLowerCase().trim();
                if (user.getFullName() == null || !user.getFullName().toLowerCase().contains(q)) {
                    return false;
                }
            }

            // אם המשתמש עבר את כל הסינונים בהצלחה
            return true;
        }).collect(Collectors.toList());

        updateListDisplay(filteredList);
    }

    private void setupAddUserButton() {
        btnAddUser.setOnClickListener(v -> new AddUserDialog(this, newUser -> loadUsers()).show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUsers();
    }

    private void loadUsers() {
        progressBar.setVisibility(View.VISIBLE);
        databaseService.getUserList(new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(List<User> users) {
                allUsers = (users != null) ? users : new ArrayList<>();
                // מריץ את כל הסינונים כדי לשמור על המצב הקודם גם אחרי רענון הרשימה
                applyFullFilter();
            }

            @Override
            public void onFailed(Exception e) {
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Failed to get users list", e);
                Toast.makeText(AdminUsersListActivity.this, "Error loading users", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateListDisplay(List<User> listToDisplay) {
        progressBar.setVisibility(View.GONE);
        userAdapter.setUserList(listToDisplay);
        tvUserCount.setText(MessageFormat.format("Total users: {0}", listToDisplay.size()));

        if (listToDisplay.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
        } else {
            emptyState.setVisibility(View.GONE);
        }
    }

    private void handleToggleAdmin(User user) {
        boolean newRole = !user.getIsAdmin();
        databaseService.updateUserAdminStatus(user.getId(), newRole, new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(Void object) {
                Toast.makeText(AdminUsersListActivity.this, "Status updated", Toast.LENGTH_SHORT).show();
                loadUsers();
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(AdminUsersListActivity.this, "Update failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleDeleteUser(User user) {
        boolean isSelf = user.getId().equals(SharedPreferencesUtil.getUserId(this));

        new DeleteUserDialog(this, user, deleteGroups -> {
            executeUserDeletion(user, isSelf);
        }).show();
    }

    private void executeUserDeletion(User user, boolean isSelf) {
        progressBar.setVisibility(View.VISIBLE);

        databaseService.deleteUserCompletely(user, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                progressBar.setVisibility(View.GONE);

                if (isSelf) {
                    SharedPreferencesUtil.signOutUser(AdminUsersListActivity.this);
                    Intent intent = new Intent(AdminUsersListActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    return;
                }

                Toast.makeText(AdminUsersListActivity.this, "User entirely deleted", Toast.LENGTH_SHORT).show();
                loadUsers();
            }

            @Override
            public void onFailed(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AdminUsersListActivity.this, "Failed to delete user", Toast.LENGTH_SHORT).show();
            }
        });
    }
}