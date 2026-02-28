package com.example.fitlink.screens;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitlink.R;
import com.example.fitlink.adapters.GroupAdapter;
import com.example.fitlink.models.DifficultyLevel;
import com.example.fitlink.models.Group;
import com.example.fitlink.models.SportType;
import com.example.fitlink.screens.dialogs.AddGroupDialog;
import com.example.fitlink.screens.dialogs.GroupDescriptionDialog;
import com.example.fitlink.screens.dialogs.LeaveGroupDialog; // הייבוא של הדיאלוג החדש
import com.example.fitlink.services.DatabaseService;
import com.example.fitlink.utils.SharedPreferencesUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class GroupsListActivity extends BaseActivity {

    private static final String TAG = "GroupsListActivity";

    private GroupAdapter groupAdapter;
    private TextView tvGroupCount;
    private EditText etSearch;
    private Spinner spinnerSearchType, spinnerSearchOptions;
    private TextInputLayout layoutSearchText;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private MaterialButton btnCreateGroup;

    private List<Group> allGroups = new ArrayList<>();
    private AddGroupDialog currentAddGroupDialog;

    private final ActivityResultLauncher<Intent> mapPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String address = result.getData().getStringExtra("address");
                    double lat = result.getData().getDoubleExtra("lat", 0);
                    double lng = result.getData().getDoubleExtra("lng", 0);
                    if (currentAddGroupDialog != null && currentAddGroupDialog.isShowing()) {
                        currentAddGroupDialog.updateLocationDetails(address, lat, lng);
                    }
                }
            }
    );

    public ActivityResultLauncher<Intent> getMapPickerLauncher() { return mapPickerLauncher; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_groups_list);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupSearchLogic();
        setupCreateGroupButton();
    }

    private void initViews() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_groups_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvGroupCount = findViewById(R.id.tv_group_count);
        etSearch = findViewById(R.id.edit_GroupsList_search);
        spinnerSearchType = findViewById(R.id.spinner_groups_search_type);
        spinnerSearchOptions = findViewById(R.id.spinner_search_options);
        layoutSearchText = findViewById(R.id.layout_search_text);
        progressBar = findViewById(R.id.groups_progress_bar);
        emptyState = findViewById(R.id.groups_empty_state);
        btnCreateGroup = findViewById(R.id.btn_GroupsList_create_group);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_groups);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }
    }

    private void setupRecyclerView() {
        RecyclerView rvGroups = findViewById(R.id.rv_groups_list);
        rvGroups.setLayoutManager(new LinearLayoutManager(this));

        String currentUserId = SharedPreferencesUtil.getUserId(this);

        groupAdapter = new GroupAdapter(new ArrayList<>(), true, currentUserId, new GroupAdapter.OnGroupClickListener() {
            @Override
            public void onJoinClick(Group group) { handleJoinGroup(group); }

            @Override
            public void onLeaveClick(Group group) { handleLeaveGroup(group); }

            @Override
            public void onGroupClick(Group group) {
                new GroupDescriptionDialog(GroupsListActivity.this, group).show();
            }
        });
        rvGroups.setAdapter(groupAdapter);
    }

    private void setupSearchLogic() {
        String[] searchOptions = {"Name", "Sport Type", "Level", "Location"};
        spinnerSearchType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, searchOptions));

        DifficultyLevel[] levels = DifficultyLevel.values();
        String[] levelNames = new String[levels.length];
        for(int i=0; i<levels.length; i++) levelNames[i] = levels[i].getDisplayName();
        ArrayAdapter<String> levelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, levelNames);

        SportType[] sports = SportType.values();
        String[] sportNames = new String[sports.length];
        for(int i=0; i<sports.length; i++) sportNames[i] = sports[i].getDisplayName();
        ArrayAdapter<String> sportAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sportNames);

        spinnerSearchType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedType = searchOptions[position];

                if (selectedType.equals("Level")) {
                    layoutSearchText.setVisibility(View.GONE);
                    spinnerSearchOptions.setVisibility(View.VISIBLE);
                    spinnerSearchOptions.setAdapter(levelAdapter);
                } else if (selectedType.equals("Sport Type")) {
                    layoutSearchText.setVisibility(View.GONE);
                    spinnerSearchOptions.setVisibility(View.VISIBLE);
                    spinnerSearchOptions.setAdapter(sportAdapter);
                } else {
                    layoutSearchText.setVisibility(View.VISIBLE);
                    spinnerSearchOptions.setVisibility(View.GONE);
                }
                executeSearch();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerSearchOptions.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { executeSearch(); }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { executeSearch(); }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void executeSearch() {
        if (allGroups == null) return;
        String searchType = spinnerSearchType.getSelectedItem() != null ? spinnerSearchType.getSelectedItem().toString() : "Name";
        List<Group> filteredList;

        if (searchType.equals("Level")) {
            if (spinnerSearchOptions.getSelectedItem() == null) return;
            String selectedLvl = spinnerSearchOptions.getSelectedItem().toString();
            filteredList = allGroups.stream()
                    .filter(g -> g.getLevel() != null && g.getLevel().getDisplayName().equals(selectedLvl))
                    .collect(Collectors.toList());
        } else if (searchType.equals("Sport Type")) {
            if (spinnerSearchOptions.getSelectedItem() == null) return;
            String selectedSport = spinnerSearchOptions.getSelectedItem().toString();
            filteredList = allGroups.stream()
                    .filter(g -> g.getSportType() != null && g.getSportType().getDisplayName().equals(selectedSport))
                    .collect(Collectors.toList());
        } else {
            String query = etSearch.getText().toString().toLowerCase().trim();
            if (query.isEmpty()) {
                updateListDisplay(allGroups);
                return;
            }
            filteredList = allGroups.stream().filter(group -> {
                if (searchType.equals("Name")) return group.getName() != null && group.getName().toLowerCase().contains(query);
                if (searchType.equals("Location")) return group.getLocation() != null && group.getLocation().getAddress() != null && group.getLocation().getAddress().toLowerCase().contains(query);
                return false;
            }).collect(Collectors.toList());
        }
        updateListDisplay(filteredList);
    }

    private void setupCreateGroupButton() {
        btnCreateGroup.setOnClickListener(v -> {
            currentAddGroupDialog = new AddGroupDialog(this);
            currentAddGroupDialog.setOnDismissListener(d -> {
                loadGroups();
                currentAddGroupDialog = null;
            });
            currentAddGroupDialog.show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadGroups();
    }

    private void loadGroups() {
        progressBar.setVisibility(View.VISIBLE);
        databaseService.getAllGroups(new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(List<Group> groups) {
                progressBar.setVisibility(View.GONE);
                allGroups = (groups != null) ? groups : new ArrayList<>();
                executeSearch();
            }
            @Override
            public void onFailed(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(GroupsListActivity.this, "Error loading groups", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateListDisplay(List<Group> listToDisplay) {
        if (groupAdapter != null) groupAdapter.updateList(listToDisplay);
        tvGroupCount.setText(MessageFormat.format("Showing {0} groups", listToDisplay.size()));
        emptyState.setVisibility(listToDisplay.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void handleJoinGroup(Group group) {
        String currentUserId = SharedPreferencesUtil.getUserId(this);
        if (group.getMembers() != null && group.getMembers().containsKey(currentUserId)) {
            Toast.makeText(this, "You are already a member of this group", Toast.LENGTH_SHORT).show();
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        databaseService.joinGroup(group.getId(), Objects.requireNonNull(currentUserId), new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(Void object) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(GroupsListActivity.this, "Joined successfully!", Toast.LENGTH_SHORT).show();
                loadGroups(); // מרענן את הרשימה ומחליף את הכפתור ל-LEAVE
            }
            @Override
            public void onFailed(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(GroupsListActivity.this, "Failed to join group", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleLeaveGroup(Group group) {
        String currentUserId = SharedPreferencesUtil.getUserId(this);

        // מונע מיוצר הקבוצה לעזוב אותה
        if (group.getAdminId() != null && group.getAdminId().equals(currentUserId)) {
            Toast.makeText(this, "As the creator of the group, you cannot leave it.", Toast.LENGTH_SHORT).show();
            return;
        }

        // קריאה לדיאלוג המעוצב שיצרנו!
        new LeaveGroupDialog(this, group, () -> {
            progressBar.setVisibility(View.VISIBLE);
            databaseService.leaveGroup(group.getId(), Objects.requireNonNull(currentUserId), new DatabaseService.DatabaseCallback<Void>() {
                @Override
                public void onCompleted(Void object) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(GroupsListActivity.this, "Left group successfully", Toast.LENGTH_SHORT).show();
                    loadGroups(); // מרענן את הרשימה ומחליף את הכפתור בחזרה ל-JOIN
                }

                @Override
                public void onFailed(Exception e) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(GroupsListActivity.this, "Failed to leave group", Toast.LENGTH_SHORT).show();
                }
            });
        }).show();
    }
}