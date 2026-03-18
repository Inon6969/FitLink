package com.example.fitlink.screens;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
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
import com.example.fitlink.models.Group;
import com.example.fitlink.models.User;
import com.example.fitlink.screens.dialogs.AddUserDialog;
import com.example.fitlink.screens.dialogs.DeleteUserDialog;
import com.example.fitlink.screens.dialogs.EditUserDialog;
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
    private Spinner spinnerSearchType;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private MaterialButton btnAddUser;

    // התיקון: אתחול כ-null כדי למנוע העלמה מוקדמת של ה-ProgressBar והופעת מצב ריק בטעות
    private List<User> allUsers = null;

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
        spinnerSearchType = findViewById(R.id.spinner_UsersTable_search_type);
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
                // לחיצה על כל הפריט מעבירה לפרופיל המשתמש
                Intent intent = new Intent(AdminUsersListActivity.this, UserProfileActivity.class);
                intent.putExtra("USER_ID", user.getId());
                startActivity(intent);
            }

            @Override
            public void onEditUser(User user) {
                // לחיצה על כפתור העריכה פותחת את דיאלוג העריכה
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
        String[] searchOptions = {"Name", "Email", "Phone"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, searchOptions);
        spinnerSearchType.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void filterUsers(String query) {
        // מונע מהחיפוש לנסות לסנן רשימה שעדיין לא נטענה
        if (allUsers == null) return;

        if (query.isEmpty()) {
            updateListDisplay(allUsers);
            return;
        }

        String searchType = spinnerSearchType.getSelectedItem().toString();
        List<User> filteredList;

        filteredList = allUsers.stream().filter(user -> {
            String q = query.toLowerCase();
            switch (searchType) {
                case "Name":
                    return user.getFullName().toLowerCase().contains(q);
                case "Email":
                    return user.getEmail().toLowerCase().contains(q);
                case "Phone":
                    return user.getPhone() != null && user.getPhone().contains(q);
                default:
                    return false;
            }
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
                // הסרנו את הסתרת ה-ProgressBar מכאן
                allUsers = (users != null) ? users : new ArrayList<>();
                filterUsers(etSearch.getText().toString());
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
        // התיקון: מוודא שה-ProgressBar נעלם רק כשהרשימה מוכנה לתצוגה
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
            executeUserDeletion(user, isSelf, deleteGroups);
        }).show();
    }

    private void executeUserDeletion(User user, boolean isSelf, boolean deleteGroups) {
        progressBar.setVisibility(View.VISIBLE);

        if (deleteGroups) {
            databaseService.getAllGroups(new DatabaseService.DatabaseCallback<List<Group>>() {
                @Override
                public void onCompleted(List<Group> allGroups) {
                    List<Group> userGroups = new ArrayList<>();
                    if (allGroups != null) {
                        for (Group g : allGroups) {
                            if (g.getCreatorId() != null && g.getCreatorId().equals(user.getId())) {
                                userGroups.add(g);
                            }
                        }
                    }

                    if (userGroups.isEmpty()) {
                        performFinalUserDeletion(user, isSelf);
                    } else {
                        int[] deletedCount = {0};
                        boolean[] hasFailed = {false};
                        for (Group g : userGroups) {
                            databaseService.deleteGroup(g.getId(), new DatabaseService.DatabaseCallback<Void>() {
                                @Override
                                public void onCompleted(Void object) {
                                    deletedCount[0]++;
                                    if (deletedCount[0] == userGroups.size() && !hasFailed[0]) {
                                        performFinalUserDeletion(user, isSelf);
                                    }
                                }

                                @Override
                                public void onFailed(Exception e) {
                                    if (!hasFailed[0]) {
                                        hasFailed[0] = true;
                                        progressBar.setVisibility(View.GONE);
                                        Toast.makeText(AdminUsersListActivity.this, "Failed to delete user's groups", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }
                    }
                }

                @Override
                public void onFailed(Exception e) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(AdminUsersListActivity.this, "Failed to fetch groups", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            performFinalUserDeletion(user, isSelf);
        }
    }

    private void performFinalUserDeletion(User user, boolean isSelf) {
        databaseService.deleteUser(user.getId(), new DatabaseService.DatabaseCallback<Void>() {
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
                Toast.makeText(AdminUsersListActivity.this, "User deleted successfully", Toast.LENGTH_SHORT).show();
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