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
import com.example.fitlink.models.User;
import com.example.fitlink.screens.dialogs.AddUserDialog;
import com.example.fitlink.screens.dialogs.EditUserDialog;
import com.example.fitlink.services.DatabaseService;
import com.example.fitlink.utils.SharedPreferencesUtil;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UsersListActivity extends BaseActivity {

    private static final String TAG = "UsersListActivity";

    // UI Elements
    private UserAdapter userAdapter;
    private TextView tvUserCount;
    private EditText etSearch;
    private Spinner spinnerSearchType;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private MaterialButton btnAddUser;

    // Data
    private List<User> allUsers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_users_list);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupSearchLogic();
        setupAddUserButton();
    }

    private void initViews() {
        // התאמת Insets למסך מלא
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
                new EditUserDialog(UsersListActivity.this, user, () -> {
                    loadUsers();
                }).show();
            }

            @Override
            public void onToggleAdmin(User user) {
                handleToggleAdmin(user); // פונקציה חדשה
            }

            @Override
            public void onDeleteUser(User user) {
                handleDeleteUser(user); // פונקציה חדשה
            }

            @Override
            public boolean isCurrentUser(User user) {
                return user.getId().equals(SharedPreferencesUtil.getUserId(UsersListActivity.this));
            }
        });
        usersList.setAdapter(userAdapter);
    }

    private void setupSearchLogic() {
        // הגדרת אפשרויות לסינון ב-Spinner
        String[] searchOptions = {"Name", "Email", "Phone"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, searchOptions);
        spinnerSearchType.setAdapter(adapter);

        // האזנה לשינויים בטקסט החיפוש
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
        btnAddUser.setOnClickListener(v -> {
            new AddUserDialog(this, newUser -> {
                loadUsers();
            }).show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUsers();
    }

    private void loadUsers() {
        progressBar.setVisibility(View.VISIBLE);
        databaseService.getUserList(new DatabaseService.DatabaseCallback<List<User>>() {
            @Override
            public void onCompleted(List<User> users) {
                progressBar.setVisibility(View.GONE);
                allUsers = (users != null) ? users : new ArrayList<>();
                filterUsers(etSearch.getText().toString()); // הפעלת פילטר קיים אם יש
            }

            @Override
            public void onFailed(Exception e) {
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Failed to get users list", e);
                Toast.makeText(UsersListActivity.this, "Error loading users", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateListDisplay(List<User> listToDisplay) {
        userAdapter.setUserList(listToDisplay);
        tvUserCount.setText("Total users: " + listToDisplay.size());

        if (listToDisplay.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
        } else {
            emptyState.setVisibility(View.GONE);
        }
    }

    private void handleToggleAdmin(User user) {
        boolean newRole = !user.getIsAdmin();
        databaseService.updateUserAdminStatus(user.getId(), newRole, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                Toast.makeText(UsersListActivity.this, "Status updated", Toast.LENGTH_SHORT).show();
                loadUsers(); // רענון הרשימה
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(UsersListActivity.this, "Update failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleDeleteUser(User user) {
        boolean isSelf = user.equals(SharedPreferencesUtil.getUser(UsersListActivity.this));

        databaseService.deleteUser(user.getId(), new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                if (isSelf) {
                    SharedPreferencesUtil.signOutUser(UsersListActivity.this);
                    Intent intent = new Intent(UsersListActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    return;
                }
                Toast.makeText(UsersListActivity.this, "User deleted", Toast.LENGTH_SHORT).show();
                loadUsers();
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(UsersListActivity.this, "Delete failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}