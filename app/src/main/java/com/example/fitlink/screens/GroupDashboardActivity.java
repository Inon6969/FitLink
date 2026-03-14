package com.example.fitlink.screens;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
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
import com.example.fitlink.screens.dialogs.ProfileImageDialog;
import com.example.fitlink.services.DatabaseService;
import com.example.fitlink.utils.ImageUtil;
import com.example.fitlink.utils.SharedPreferencesUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.Objects;

public class GroupDashboardActivity extends BaseActivity {

    private static final int REQ_CAMERA = 100;
    private static final int REQ_GALLERY = 200;

    private Group currentGroup;
    private ImageView imgGroupPhoto;
    private CreateEventDialog currentCreateEventDialog;
    private EditGroupDialog currentEditGroupDialog;

    private final ActivityResultLauncher<Intent> mapPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String address = result.getData().getStringExtra("address");
                    double lat = result.getData().getDoubleExtra("lat", 0);
                    double lng = result.getData().getDoubleExtra("lng", 0);

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

        // שולפים את ה-ID מהאינטנט במקום מ-Holder!
        String groupId = getIntent().getStringExtra("GROUP_ID");

        if (groupId == null || groupId.isEmpty()) {
            Toast.makeText(this, "Error: Group ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // מושכים את הקבוצה הטרייה מהדאטהבייס לפני שבונים את הממשק
        DatabaseService.getInstance().getGroup(groupId, new DatabaseService.DatabaseCallback<Group>() {
            @Override
            public void onCompleted(Group group) {
                if (group == null) {
                    Toast.makeText(GroupDashboardActivity.this, "Group not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                currentGroup = group;
                initViews(); // רק אחרי שיש לנו את המידע, אנחנו מרנדרים את המסך
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(GroupDashboardActivity.this, "Failed to load group details", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
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

        imgGroupPhoto = findViewById(R.id.img_dashboard_group_photo);
        MaterialButton btnChangePhoto = findViewById(R.id.btn_dashboard_change_photo);

        MaterialButton btnMembers = findViewById(R.id.btn_dashboard_members);
        MaterialButton btnChat = findViewById(R.id.btn_dashboard_chat);
        MaterialButton btnCalendar = findViewById(R.id.btn_dashboard_calendar);

        MaterialCardView cardRequests = findViewById(R.id.card_dashboard_requests);
        MaterialButton btnRequests = findViewById(R.id.btn_dashboard_requests);

        MaterialCardView cardSchedule = findViewById(R.id.card_dashboard_schedule);
        MaterialButton btnSchedule = findViewById(R.id.btn_dashboard_schedule);

        MaterialCardView cardEdit = findViewById(R.id.card_dashboard_edit);
        MaterialButton btnEdit = findViewById(R.id.btn_dashboard_edit);
        MaterialButton btnDelete = findViewById(R.id.btn_dashboard_delete);
        MaterialButton btnLeave = findViewById(R.id.btn_dashboard_leave);

        String currentUserId = SharedPreferencesUtil.getUserId(this);

        boolean isCreator = currentGroup.getCreatorId() != null && currentGroup.getCreatorId().equals(currentUserId);
        boolean isManager = currentGroup.getManagers() != null && currentGroup.getManagers().containsKey(currentUserId);

        if (currentGroup.getGroupImage() != null && !currentGroup.getGroupImage().isEmpty()) {
            Bitmap bmp = ImageUtil.convertFrom64base(currentGroup.getGroupImage());
            if (bmp != null) imgGroupPhoto.setImageBitmap(bmp);
        }

        if (isCreator) {
            btnChangePhoto.setVisibility(View.VISIBLE);
            btnChangePhoto.setOnClickListener(v -> openImagePicker());
        } else {
            btnChangePhoto.setVisibility(View.GONE);
        }

        if (isCreator || isManager) {
            cardSchedule.setVisibility(View.VISIBLE);
            cardRequests.setVisibility(View.VISIBLE);
        } else {
            cardSchedule.setVisibility(View.GONE);
            cardRequests.setVisibility(View.GONE);
        }

        if (isCreator) {
            cardEdit.setVisibility(View.VISIBLE);
            btnDelete.setVisibility(View.VISIBLE);
            btnLeave.setVisibility(View.VISIBLE);
        } else {
            cardEdit.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
            btnLeave.setVisibility(View.VISIBLE);
        }

        // --- כאן אנחנו מעבירים את ה-ID הלאה לשאר המסכים ---

        btnRequests.setOnClickListener(v -> {
            Intent intent = new Intent(GroupDashboardActivity.this, JoinRequestsActivity.class);
            intent.putExtra("GROUP_ID", currentGroup.getId());
            startActivity(intent);
        });

        btnSchedule.setOnClickListener(v -> {
            currentCreateEventDialog = new CreateEventDialog(this, currentGroup);
            currentCreateEventDialog.show();
        });

        btnEdit.setOnClickListener(v -> {
            currentEditGroupDialog = new EditGroupDialog(this, currentGroup, updatedGroup -> {
                currentGroup = updatedGroup;
                if (getSupportActionBar() != null) getSupportActionBar().setTitle(currentGroup.getName());
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
            new DeleteGroupDialog(this, () -> {
                Toast.makeText(this, "Deleting group...", Toast.LENGTH_SHORT).show();
                DatabaseService.getInstance().deleteGroup(currentGroup.getId(), new DatabaseService.DatabaseCallback<Void>() {
                    @Override
                    public void onCompleted(Void object) {
                        Toast.makeText(GroupDashboardActivity.this, "Group deleted successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onFailed(Exception e) {
                        Toast.makeText(GroupDashboardActivity.this, "Failed to delete group", Toast.LENGTH_SHORT).show();
                    }
                });
            }).show();
        });

        btnLeave.setOnClickListener(v -> {
            if (isCreator && (currentGroup.getManagers() == null || currentGroup.getManagers().isEmpty())) {
                Toast.makeText(this, "You must appoint at least one Manager before leaving the group.", Toast.LENGTH_LONG).show();
                return;
            }

            new LeaveGroupDialog(this, currentGroup, currentUserId, () -> {
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
            intent.putExtra("GROUP_ID", currentGroup.getId());
            startActivity(intent);
        });

        btnChat.setOnClickListener(v -> {
            Intent intent = new Intent(GroupDashboardActivity.this, GroupChatActivity.class);
            intent.putExtra("GROUP_ID", currentGroup.getId());
            startActivity(intent);
        });

        btnCalendar.setOnClickListener(v -> {
            Intent intent = new Intent(GroupDashboardActivity.this, GroupCalendarActivity.class);
            intent.putExtra("GROUP_ID", currentGroup.getId());
            startActivity(intent);
        });
    }

    private void openImagePicker() {
        boolean hasImage = currentGroup.getGroupImage() != null && !currentGroup.getGroupImage().isEmpty();

        new ProfileImageDialog(this, hasImage, true, new ProfileImageDialog.ImagePickerListener() {
            @Override
            public void onCamera() {
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, REQ_CAMERA);
            }

            @Override
            public void onGallery() {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, REQ_GALLERY);
            }

            @Override
            public void onDelete() {
                deleteGroupImage();
            }
        }).show();
    }

    private void deleteGroupImage() {
        currentGroup.setGroupImage(null);
        imgGroupPhoto.setImageResource(R.drawable.ic_sport);
        saveGroupImage("Group photo removed");
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
            imgGroupPhoto.setImageBitmap(bitmap);

            String base64 = ImageUtil.convertTo64Base(imgGroupPhoto);
            currentGroup.setGroupImage(base64);

            saveGroupImage("Group photo updated!");
        }
    }

    private void saveGroupImage(String successMessage) {
        DatabaseService.getInstance().updateGroup(currentGroup, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                Toast.makeText(GroupDashboardActivity.this, successMessage, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(GroupDashboardActivity.this, "Failed to update photo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}