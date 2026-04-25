package com.example.fitlink.screens;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.fitlink.R;
import com.example.fitlink.dialogs.CreateEventDialog;
import com.example.fitlink.dialogs.DeleteGroupDialog;
import com.example.fitlink.dialogs.EditGroupDialog;
import com.example.fitlink.dialogs.LeaveGroupDialog;
import com.example.fitlink.dialogs.ProfileImageDialog;
import com.example.fitlink.models.Group;
import com.example.fitlink.services.DatabaseService;
import com.example.fitlink.utils.ImageUtil;
import com.example.fitlink.utils.SharedPreferencesUtil;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

public class GroupDashboardActivity extends BaseActivity {

    private static final int REQ_CAMERA = 100;
    private static final int REQ_GALLERY = 200;
    private static final int REQ_CAMERA_PERMISSION = 101;

    private Group currentGroup;
    private ImageView imgGroupPhoto;
    private Chip chipGroupLevel;
    private Chip chipGroupType;
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
    private ValueEventListener groupListener;
    private boolean isInitialized = false;

    public ActivityResultLauncher<Intent> getMapPickerLauncher() {
        return mapPickerLauncher;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_group_dashboard);

        String groupId = getIntent().getStringExtra("GROUP_ID");

        if (groupId == null || groupId.isEmpty()) {
            Toast.makeText(this, "Error: Group ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String currentUserId = SharedPreferencesUtil.getUserId(this);

        groupListener = DatabaseService.getInstance().listenToGroup(groupId, new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(Group group) {
                if (group == null) {
                    Toast.makeText(GroupDashboardActivity.this, "This group no longer exists.", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                if (group.getMembers() == null || !group.getMembers().containsKey(currentUserId)) {
                    Toast.makeText(GroupDashboardActivity.this, "You are no longer a member of this group.", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }

                currentGroup = group;

                boolean isCreator = currentGroup.getCreatorId() != null && currentGroup.getCreatorId().equals(currentUserId);
                boolean isManager = currentGroup.getManagers() != null && currentGroup.getManagers().containsKey(currentUserId);

                if (!isCreator && !isManager) {
                    if (currentCreateEventDialog != null && currentCreateEventDialog.isShowing()) {
                        currentCreateEventDialog.dismiss();
                        Toast.makeText(GroupDashboardActivity.this, "Your manager permissions were removed.", Toast.LENGTH_SHORT).show();
                    }
                }

                if (!isCreator) {
                    if (currentEditGroupDialog != null && currentEditGroupDialog.isShowing()) {
                        currentEditGroupDialog.dismiss();
                        Toast.makeText(GroupDashboardActivity.this, "You no longer have permission to edit.", Toast.LENGTH_SHORT).show();
                    }
                }

                if (!isInitialized) {
                    initViews();
                    isInitialized = true;
                } else {
                    updateUI();
                }
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(GroupDashboardActivity.this, "Failed to load group details", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (currentCreateEventDialog != null && currentCreateEventDialog.isShowing()) {
            currentCreateEventDialog.dismiss();
        }
        if (currentEditGroupDialog != null && currentEditGroupDialog.isShowing()) {
            currentEditGroupDialog.dismiss();
        }

        if (currentGroup != null && groupListener != null && currentGroup.getId() != null) {
            DatabaseService.getInstance().removeGroupListener(currentGroup.getId(), groupListener);
        }
    }

    private void initViews() {
        View root = findViewById(R.id.main_group_dashboard);
        AppBarLayout appBarLayout = findViewById(R.id.toolbar_group_dashboard).getParent() instanceof AppBarLayout
                ? (AppBarLayout) findViewById(R.id.toolbar_group_dashboard).getParent()
                : null;

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);

            if (appBarLayout != null) {
                appBarLayout.setPadding(0, systemBars.top, 0, 0);
            }

            return insets;
        });

        root.post(() -> ViewCompat.requestApplyInsets(root));

        Toolbar toolbar = findViewById(R.id.toolbar_group_dashboard);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }

        imgGroupPhoto = findViewById(R.id.img_dashboard_group_photo);
        chipGroupLevel = findViewById(R.id.chip_dashboard_level);
        chipGroupType = findViewById(R.id.chip_dashboard_type);

        LinearLayout layoutLocation = findViewById(R.id.layout_dashboard_location);
        TextView tvLocation = findViewById(R.id.tv_dashboard_location);
        tvLocation.setPaintFlags(tvLocation.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);

        layoutLocation.setOnClickListener(v -> {
            if (currentGroup.getLocation() != null) {
                String address = currentGroup.getLocation().getAddress();
                double lat = currentGroup.getLocation().getLatitude();
                double lng = currentGroup.getLocation().getLongitude();

                android.net.Uri locationUri = android.net.Uri.parse("geo:0,0?q=" + lat + "," + lng + "(" + android.net.Uri.encode(address) + ")");
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, locationUri);
                Intent chooser = Intent.createChooser(mapIntent, "Navigate to group location with...");
                try {
                    startActivity(chooser);
                } catch (Exception e) {
                    Toast.makeText(GroupDashboardActivity.this, "No navigation app found", Toast.LENGTH_SHORT).show();
                }
            }
        });

        imgGroupPhoto.setOnClickListener(v -> {
            if (currentGroup != null && currentGroup.getGroupImage() != null && !currentGroup.getGroupImage().isEmpty()) {
                showFullImageDialog();
            }
        });

        findViewById(R.id.btn_dashboard_change_photo).setOnClickListener(v -> openImagePicker());

        findViewById(R.id.btn_dashboard_requests).setOnClickListener(v -> {
            Intent intent = new Intent(GroupDashboardActivity.this, JoinRequestsActivity.class);
            intent.putExtra("GROUP_ID", currentGroup.getId());
            startActivity(intent);
        });

        findViewById(R.id.btn_dashboard_schedule).setOnClickListener(v -> {
            currentCreateEventDialog = new CreateEventDialog(this, currentGroup);
            currentCreateEventDialog.show();
        });

        findViewById(R.id.btn_dashboard_edit).setOnClickListener(v -> {
            currentEditGroupDialog = new EditGroupDialog(this, currentGroup, updatedGroup -> {
            });
            currentEditGroupDialog.show();
        });

        findViewById(R.id.btn_dashboard_delete).setOnClickListener(v -> new DeleteGroupDialog(this, () -> {
            Toast.makeText(this, "Deleting group...", Toast.LENGTH_SHORT).show();
            DatabaseService.getInstance().deleteGroup(currentGroup.getId(), new DatabaseService.DatabaseCallback<>() {
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
        }).show());

        findViewById(R.id.btn_dashboard_leave).setOnClickListener(v -> {
            String currentUserId = SharedPreferencesUtil.getUserId(this);
            boolean isCreator = currentGroup.getCreatorId() != null && currentGroup.getCreatorId().equals(currentUserId);
            if (isCreator && (currentGroup.getManagers() == null || currentGroup.getManagers().isEmpty())) {
                Toast.makeText(this, "You must appoint at least one Manager before leaving the group.", Toast.LENGTH_LONG).show();
                return;
            }

            new LeaveGroupDialog(this, currentGroup, currentUserId, () -> DatabaseService.getInstance().leaveGroup(currentGroup.getId(), currentUserId, new DatabaseService.DatabaseCallback<>() {
                @Override
                public void onCompleted(Void object) {
                    Toast.makeText(GroupDashboardActivity.this, "Left group successfully", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onFailed(Exception e) {
                    Toast.makeText(GroupDashboardActivity.this, "Failed to leave group", Toast.LENGTH_SHORT).show();
                }
            })).show();
        });

        findViewById(R.id.btn_dashboard_members).setOnClickListener(v -> {
            Intent intent = new Intent(GroupDashboardActivity.this, MembersListActivity.class);
            intent.putExtra("GROUP_ID", currentGroup.getId());
            startActivity(intent);
        });

        findViewById(R.id.btn_dashboard_chat).setOnClickListener(v -> {
            Intent intent = new Intent(GroupDashboardActivity.this, GroupChatActivity.class);
            intent.putExtra("GROUP_ID", currentGroup.getId());
            startActivity(intent);
        });

        findViewById(R.id.btn_dashboard_calendar).setOnClickListener(v -> {
            Intent intent = new Intent(GroupDashboardActivity.this, GroupCalendarActivity.class);
            intent.putExtra("GROUP_ID", currentGroup.getId());
            startActivity(intent);
        });

        updateUI();
    }

    private void updateUI() {
        if (getSupportActionBar() != null) getSupportActionBar().setTitle(currentGroup.getName());
        TextView tvTitle = findViewById(R.id.tv_dashboard_title);
        tvTitle.setText(currentGroup.getName());

        if (currentGroup.getSportType() != null) {
            chipGroupType.setText(currentGroup.getSportType().toString());
            chipGroupType.setVisibility(View.VISIBLE);
        } else {
            chipGroupType.setVisibility(View.GONE);
        }

        if (currentGroup.getLevel() != null) {
            chipGroupLevel.setText(currentGroup.getLevel().getDisplayName());
            chipGroupLevel.setVisibility(View.VISIBLE);
        } else {
            chipGroupLevel.setVisibility(View.GONE);
        }

        LinearLayout layoutLocation = findViewById(R.id.layout_dashboard_location);
        TextView tvLocation = findViewById(R.id.tv_dashboard_location);
        if (currentGroup.getLocation() != null && currentGroup.getLocation().getAddress() != null && !currentGroup.getLocation().getAddress().isEmpty()) {
            tvLocation.setText(currentGroup.getLocation().getAddress());
            layoutLocation.setVisibility(View.VISIBLE);
        } else {
            layoutLocation.setVisibility(View.GONE);
        }

        TextView tvDescription = findViewById(R.id.tv_dashboard_description);
        if (currentGroup.getDescription() != null && !currentGroup.getDescription().trim().isEmpty()) {
            tvDescription.setText(currentGroup.getDescription());
        } else {
            tvDescription.setText("No description provided for this group.");
        }

        if (currentGroup.getGroupImage() != null && !currentGroup.getGroupImage().isEmpty()) {
            Bitmap bmp = ImageUtil.convertFrom64base(currentGroup.getGroupImage());
            if (bmp != null) imgGroupPhoto.setImageBitmap(bmp);
        } else {
            imgGroupPhoto.setImageResource(R.drawable.ic_sport);
        }

        String currentUserId = SharedPreferencesUtil.getUserId(this);
        boolean isCreator = currentGroup.getCreatorId() != null && currentGroup.getCreatorId().equals(currentUserId);
        boolean isManager = currentGroup.getManagers() != null && currentGroup.getManagers().containsKey(currentUserId);

        findViewById(R.id.btn_dashboard_change_photo).setVisibility(isCreator ? View.VISIBLE : View.GONE);
        findViewById(R.id.card_dashboard_schedule).setVisibility((isCreator || isManager) ? View.VISIBLE : View.GONE);
        findViewById(R.id.card_dashboard_requests).setVisibility((isCreator || isManager) ? View.VISIBLE : View.GONE);

        MaterialCardView cardEdit = findViewById(R.id.card_dashboard_edit);
        MaterialButton btnDelete = findViewById(R.id.btn_dashboard_delete);
        MaterialButton btnLeave = findViewById(R.id.btn_dashboard_leave);

        if (isCreator) {
            cardEdit.setVisibility(View.VISIBLE);
            btnDelete.setVisibility(View.VISIBLE);
            btnLeave.setVisibility(View.VISIBLE);
        } else {
            cardEdit.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
            btnLeave.setVisibility(View.VISIBLE);
        }
    }

    private void showFullImageDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_full_image);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        ImageView dialogImage = dialog.findViewById(R.id.dialogImage);
        dialogImage.setImageDrawable(imgGroupPhoto.getDrawable());

        View btnClose = dialog.findViewById(R.id.card_close_full_image);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void openImagePicker() {
        boolean hasImage = currentGroup.getGroupImage() != null && !currentGroup.getGroupImage().isEmpty();

        new ProfileImageDialog(this, hasImage, true, new ProfileImageDialog.ImagePickerListener() {
            @Override
            public void onCamera() {
                // בדיקה האם יש הרשאת מצלמה
                if (ContextCompat.checkSelfPermission(GroupDashboardActivity.this, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {

                    // בקשת הרשאה מהמשתמש בזמן ריצה
                    ActivityCompat.requestPermissions(GroupDashboardActivity.this,
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
                deleteGroupImage();
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
        DatabaseService.getInstance().updateGroup(currentGroup, new DatabaseService.DatabaseCallback<>() {
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