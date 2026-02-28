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
import java.util.List;
import java.util.Objects;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    private final List<User> userList;
    private final OnUserClickListener onUserClickListener;

    // דגלים למצב קבוצה
    private boolean isGroupMode = false;
    private boolean isCurrentUserGroupAdmin = false;

    // הקונסטרקטור
    public UserAdapter(@Nullable final OnUserClickListener onUserClickListener) {
        this.userList = new ArrayList<>();
        this.onUserClickListener = onUserClickListener;
    }

    // מתודה להפעלת מצב קבוצה והעברת הרשאות הניהול
    public void setGroupMode(boolean isGroupMode, boolean isGroupAdmin) {
        this.isGroupMode = isGroupMode;
        this.isCurrentUserGroupAdmin = isGroupAdmin;
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
        holder.tvPhone.setText(user.getPhone());
        holder.tvPassword.setText(user.getPassword());

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

        // בדיקה האם זה המשתמש המחובר
        boolean isSelf = user.getId() != null && Objects.requireNonNull(onUserClickListener).isCurrentUser(user);

        // הסתרת שדה הסיסמה והאייקון שלו כאשר צופים ברשימת חברי קבוצה
        View passwordLayout = (View) holder.tvPassword.getParent();
        if (isGroupMode) {
            passwordLayout.setVisibility(View.GONE);
            holder.chipRole.setVisibility(View.GONE); // מסתיר את תגית ה-Admin הכללית של האפליקציה בתוך קבוצה
        } else {
            passwordLayout.setVisibility(View.VISIBLE);
        }

        // לוגיקת הרשאות וניהול כפתורים
        if (isGroupMode) {
            // === לוגיקה למסך חברי הקבוצה (MembersListActivity) ===
            holder.btnToggleAdmin.setVisibility(View.GONE); // מוסתר תמיד במצב קבוצה

            if (isCurrentUserGroupAdmin && !isSelf) {
                // מנהל הקבוצה רואה פח אשפה ליד משתמשים אחרים
                holder.btnDeleteUser.setVisibility(View.VISIBLE);
            } else {
                // משתמשים רגילים (או מנהל על עצמו) לא רואים פח אשפה
                holder.btnDeleteUser.setVisibility(View.GONE);
            }

        } else {
            // === לוגיקה למסך הניהול הראשי (UsersListActivity) ===
            if (isSelf) {
                holder.btnToggleAdmin.setVisibility(View.GONE);
                if (user.getIsAdmin()) {
                    holder.chipRole.setVisibility(View.VISIBLE);
                    holder.chipRole.setText("Admin (Me)");
                } else {
                    holder.chipRole.setVisibility(View.GONE);
                }
            } else {
                holder.btnToggleAdmin.setVisibility(View.VISIBLE);
                if (user.getIsAdmin()) {
                    holder.chipRole.setVisibility(View.VISIBLE);
                    holder.chipRole.setText("Admin");
                    holder.btnToggleAdmin.setImageResource(R.drawable.ic_remove_admin);
                } else {
                    holder.chipRole.setVisibility(View.INVISIBLE);
                    holder.btnToggleAdmin.setImageResource(R.drawable.ic_add_admin);
                }

                holder.btnToggleAdmin.setOnClickListener(v -> {
                    if (onUserClickListener != null) onUserClickListener.onToggleAdmin(user);
                });
            }

            // כפתור מחיקה גלוי למנהל הראשי
            holder.btnDeleteUser.setVisibility(View.VISIBLE);
        }

        // הגדרת לחיצה על כפתור המחיקה (משמש גם למחיקת משתמש וגם להסרה מקבוצה)
        holder.btnDeleteUser.setOnClickListener(v -> {
            if (onUserClickListener != null) onUserClickListener.onDeleteUser(user);
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
        final Chip chipRole;
        final ImageButton btnDeleteUser;
        final ImageButton btnToggleAdmin;
        final ImageView imgProfile;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_item_user_name);
            tvEmail = itemView.findViewById(R.id.tv_item_user_email);
            tvPhone = itemView.findViewById(R.id.tv_item_user_phone);
            tvPassword = itemView.findViewById(R.id.tv_item_user_password);
            chipRole = itemView.findViewById(R.id.chip_user_role);
            btnDeleteUser = itemView.findViewById(R.id.btn_item_user_delete);
            btnToggleAdmin = itemView.findViewById(R.id.btn_item_user_toggleAdmin);
            imgProfile = itemView.findViewById(R.id.img_item_user_profile);
        }
    }
}