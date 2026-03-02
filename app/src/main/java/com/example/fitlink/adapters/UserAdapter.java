package com.example.fitlink.adapters;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitlink.R;
import com.example.fitlink.models.User;
import com.example.fitlink.utils.ImageUtil;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    private final List<User> userList;
    private final OnUserClickListener onUserClickListener;

    // משתנים למצב קבוצה (תמיכה במנהלים וביוצר)
    private boolean isGroupMode = false;
    private boolean isCurrentUserGroupCreator = false;
    private boolean isCurrentUserGroupManager = false;
    private String groupCreatorId = null;
    private Map<String, Boolean> groupManagers = new HashMap<>();

    // הקונסטרקטור
    public UserAdapter(@Nullable final OnUserClickListener onUserClickListener) {
        this.userList = new ArrayList<>();
        this.onUserClickListener = onUserClickListener;
    }

    // מתודה להפעלת מצב קבוצה המקבלת את מפת המנהלים והרשאות המשתמש הנוכחי
    public void setGroupMode(boolean isGroupMode, boolean isCurrentUserGroupCreator, boolean isCurrentUserGroupManager, String groupCreatorId, Map<String, Boolean> groupManagers) {
        this.isGroupMode = isGroupMode;
        this.isCurrentUserGroupCreator = isCurrentUserGroupCreator;
        this.isCurrentUserGroupManager = isCurrentUserGroupManager;
        this.groupCreatorId = groupCreatorId;
        this.groupManagers = groupManagers != null ? groupManagers : new HashMap<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = userList.get(position);
        if (user == null) return;

        // הגדרת נתונים בסיסיים
        holder.tvName.setText(user.getFullName());
        holder.tvEmail.setText(user.getEmail());

        if (user.getPhone() != null && !user.getPhone().isEmpty()) {
            holder.tvPhone.setText(user.getPhone());
        } else {
            holder.tvPhone.setText("No phone");
        }

        if (user.getPassword() != null) {
            holder.tvPassword.setText("Password: " + user.getPassword());
        } else {
            holder.tvPassword.setText("Password: *****");
        }

        // טיפול בתמונת פרופיל
        String base64Image = user.getProfileImage();
        if (base64Image != null && !base64Image.isEmpty()) {
            Bitmap bmp = ImageUtil.convertFrom64base(base64Image);
            if (bmp != null) {
                holder.imgProfile.setImageBitmap(bmp);
            } else {
                holder.imgProfile.setImageResource(R.drawable.ic_user);
            }
        } else {
            holder.imgProfile.setImageResource(R.drawable.ic_user);
        }

        // בדיקה האם זה המשתמש המחובר ("אני")
        boolean isSelf = user.getId() != null && onUserClickListener != null && onUserClickListener.isCurrentUser(user);

        // הלוגיקה להצגת תגית "You"
        if (isSelf) {
            holder.chipIsMe.setVisibility(View.VISIBLE);
        } else {
            holder.chipIsMe.setVisibility(View.GONE);
        }

        // הסתרת שדה הסיסמה והאייקון שלו כאשר צופים ברשימת חברי קבוצה
        View passwordLayout = (View) holder.tvPassword.getParent();
        if (isGroupMode) {
            passwordLayout.setVisibility(View.GONE);
        } else {
            passwordLayout.setVisibility(View.VISIBLE);
        }

        // לוגיקת הרשאות וניהול תגיות וכפתורים
        if (isGroupMode) {
            // === לוגיקה למסך חברי הקבוצה (MembersListActivity) ===
            boolean isCreator = groupCreatorId != null && groupCreatorId.equals(user.getId());
            boolean isManager = groupManagers.containsKey(user.getId());

            // 1. קביעת התגית: יוצר / מנהל / מוסתר
            if (isCreator) {
                holder.chipRole.setText("Creator");
                holder.chipRole.setVisibility(View.VISIBLE);
            } else if (isManager) {
                holder.chipRole.setText("Manager");
                holder.chipRole.setVisibility(View.VISIBLE);
            } else {
                holder.chipRole.setVisibility(View.GONE);
            }

            // 2. כפתור מינוי/הסרת מנהלים (מוצג רק ליוצר, ועבור כולם חוץ מעצמו)
            if (isCurrentUserGroupCreator && !isCreator) {
                holder.btnToggleAdmin.setVisibility(View.VISIBLE);
                if (isManager) {
                    holder.btnToggleAdmin.setImageResource(R.drawable.ic_remove_admin); // כבר מנהל -> אפשרות הסרה
                } else {
                    holder.btnToggleAdmin.setImageResource(R.drawable.ic_add_admin); // לא מנהל -> אפשרות הוספה
                }
            } else {
                holder.btnToggleAdmin.setVisibility(View.GONE);
            }

            // 3. כפתור מחיקת משתמש (הסרה מקבוצה)
            if (isCurrentUserGroupCreator && !isCreator) {
                // יוצר יכול להסיר את כולם חוץ מעצמו
                holder.btnDeleteUser.setVisibility(View.VISIBLE);
            } else if (isCurrentUserGroupManager && !isCreator && !isManager) {
                // מנהל יכול להסיר רק משתמשים רגילים (לא את היוצר ולא מנהלים אחרים)
                holder.btnDeleteUser.setVisibility(View.VISIBLE);
            } else {
                holder.btnDeleteUser.setVisibility(View.GONE);
            }

        } else {
            // === לוגיקה למסך הניהול הראשי (UsersListActivity) ===
            if (user.getIsAdmin()) {
                holder.chipRole.setText("Admin");
                holder.chipRole.setVisibility(View.VISIBLE);
            } else {
                holder.chipRole.setVisibility(View.GONE);
            }

            if (isSelf) {
                holder.btnToggleAdmin.setVisibility(View.GONE);
            } else {
                holder.btnToggleAdmin.setVisibility(View.VISIBLE);
                if (user.getIsAdmin()) {
                    holder.btnToggleAdmin.setImageResource(R.drawable.ic_remove_admin);
                } else {
                    holder.btnToggleAdmin.setImageResource(R.drawable.ic_add_admin);
                }

                holder.btnToggleAdmin.setOnClickListener(v -> {
                    if (onUserClickListener != null) onUserClickListener.onToggleAdmin(user);
                });
            }

            // כפתור מחיקה גלוי תמיד למנהל הראשי
            holder.btnDeleteUser.setVisibility(View.VISIBLE);
        }

        // הגדרת לחיצה על כפתור המחיקה (משמש גם למחיקת משתמש וגם להסרה מקבוצה)
        holder.btnDeleteUser.setOnClickListener(v -> {
            if (onUserClickListener != null) onUserClickListener.onDeleteUser(user);
        });

        // במצב קבוצה (כפתור מינוי מנהלים) או במצב רגיל (מנהלי אפליקציה)
        holder.btnToggleAdmin.setOnClickListener(v -> {
            if (onUserClickListener != null) onUserClickListener.onToggleAdmin(user);
        });

        // לחיצה על כל השורה
        holder.itemView.setOnClickListener(v -> {
            if (onUserClickListener != null) onUserClickListener.onUserClick(user);
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    // מתודות לניהול הרשימה
    public void setUserList(List<User> users) {
        userList.clear();
        userList.addAll(users);
        notifyDataSetChanged();
    }

    public void addUser(User user) {
        userList.add(user);
        notifyItemInserted(userList.size() - 1);
    }

    public void updateUser(User user) {
        int index = userList.indexOf(user);
        if (index == -1) return;
        userList.set(index, user);
        notifyItemChanged(index);
    }

    public void removeUser(User user) {
        int index = userList.indexOf(user);
        if (index == -1) return;
        userList.remove(index);
        notifyItemRemoved(index);
    }

    public interface OnUserClickListener {
        void onUserClick(User user);
        void onToggleAdmin(User user);
        void onDeleteUser(User user);
        boolean isCurrentUser(User user);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName;
        final TextView tvEmail;
        final TextView tvPhone;
        final TextView tvPassword;
        final ImageButton btnDeleteUser;
        final ImageButton btnToggleAdmin;
        final ImageView imgProfile;

        // התגיות שלנו מ-XML
        final Chip chipRole;
        final Chip chipIsMe;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_item_user_name);
            tvEmail = itemView.findViewById(R.id.tv_item_user_email);
            tvPhone = itemView.findViewById(R.id.tv_item_user_phone);
            tvPassword = itemView.findViewById(R.id.tv_item_user_password);
            btnDeleteUser = itemView.findViewById(R.id.btn_item_user_delete);
            btnToggleAdmin = itemView.findViewById(R.id.btn_item_user_toggleAdmin);
            imgProfile = itemView.findViewById(R.id.img_item_user_profile);

            // קישור התגיות
            chipRole = itemView.findViewById(R.id.chip_user_role);
            chipIsMe = itemView.findViewById(R.id.chip_user_is_me);
        }
    }
}