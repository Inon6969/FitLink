package com.example.fitlink.adapters;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.fitlink.R;
import com.example.fitlink.models.Comment;
import com.example.fitlink.models.User;
import com.example.fitlink.services.DatabaseService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private List<Comment> commentList;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());

    public CommentAdapter(List<Comment> commentList) {
        this.commentList = commentList;
    }

    public void updateList(List<Comment> newList) {
        this.commentList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);

        holder.tvText.setText(comment.getText());
        holder.tvTime.setText(timeFormat.format(new Date(comment.getTimestamp())));

        // --- טיפול בבעיית מיחזור (Recycling) של שורות ---
        // איפוס תמונת הפרופיל למצב "אייקון כחול" לפני שאנחנו בודקים אם יש תמונה אמיתית
        holder.imgUserProfile.setImageResource(R.drawable.ic_user);
        holder.imgUserProfile.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.fitlinkPrimary)));

        int paddingDp = (int) (8 * holder.itemView.getContext().getResources().getDisplayMetrics().density);
        holder.imgUserProfile.setPadding(paddingDp, paddingDp, paddingDp, paddingDp);

        holder.tvUserName.setText("Loading...");

        // משיכת פרטי המשתמש שכתב את התגובה
        if (comment.getUserId() != null) {
            DatabaseService.getInstance().getUser(comment.getUserId(), new DatabaseService.DatabaseCallback<User>() {
                @Override
                public void onCompleted(User user) {
                    if (user != null) {
                        holder.tvUserName.setText(user.getFirstName() + " " + user.getLastName());

                        // שימוש בפונקציה הנכונה מהמודל User שלך (getProfileImage)
                        if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {

                            // ביטול הצבע והריווח כדי שהתמונה האמיתית תוצג על כל העיגול בצורה מושלמת
                            holder.imgUserProfile.setImageTintList(null);
                            holder.imgUserProfile.setPadding(0, 0, 0, 0);

                            // טעינת התמונה מהענן באמצעות Glide
                            Glide.with(holder.itemView.getContext())
                                    .load(user.getProfileImage())
                                    .centerCrop()
                                    .into(holder.imgUserProfile);
                        }
                    } else {
                        holder.tvUserName.setText("Unknown User");
                    }
                }

                @Override
                public void onFailed(Exception e) {
                    holder.tvUserName.setText("Unknown User");
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        final TextView tvUserName, tvTime, tvText;
        final ImageView imgUserProfile; // הוספנו חיבור לתמונה

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tv_comment_user_name);
            tvTime = itemView.findViewById(R.id.tv_comment_time);
            tvText = itemView.findViewById(R.id.tv_comment_text);
            imgUserProfile = itemView.findViewById(R.id.img_item_user_profile);
        }
    }
}