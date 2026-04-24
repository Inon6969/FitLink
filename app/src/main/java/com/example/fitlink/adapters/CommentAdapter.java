package com.example.fitlink.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitlink.R;
import com.example.fitlink.models.Comment;
import com.example.fitlink.models.User;
import com.example.fitlink.services.DatabaseService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
    // התיקון: ממשק חדש להאזנה ללחיצות
    private final OnCommentClickListener listener;
    private List<Comment> commentList;

    // התיקון: הוספנו את ה-listener לבנאי
    public CommentAdapter(List<Comment> commentList, OnCommentClickListener listener) {
        this.commentList = commentList;
        this.listener = listener;
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

        // הגדרת משתנים לצבע ול-padding
        int primaryColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.fitlinkPrimary);
        float density = holder.itemView.getContext().getResources().getDisplayMetrics().density;
        int paddingPx = (int) (8 * density);

        holder.imgUserProfile.setImageResource(R.drawable.ic_user);
        holder.imgUserProfile.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
        holder.imgUserProfile.setColorFilter(primaryColor);

        holder.tvUserName.setText("Loading...");

        if (comment.getUserId() != null) {

            // התיקון: הפעלת הלחיצות בעזרת ה-listener
            holder.tvUserName.setOnClickListener(v -> {
                if (listener != null) listener.onNameClick(comment.getUserId());
            });

            holder.imgUserProfile.setOnClickListener(v -> {
                if (listener != null) listener.onImageClick(comment.getUserId());
            });

            DatabaseService.getInstance().getUser(comment.getUserId(), new DatabaseService.DatabaseCallback<User>() {
                @Override
                public void onCompleted(User user) {
                    if (user != null) {
                        holder.tvUserName.setText(user.getFirstName() + " " + user.getLastName());

                        if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
                            try {
                                byte[] decodedString = Base64.decode(user.getProfileImage(), Base64.DEFAULT);
                                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                                holder.imgUserProfile.setImageBitmap(decodedByte);
                                holder.imgUserProfile.clearColorFilter();
                                holder.imgUserProfile.setPadding(0, 0, 0, 0);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
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

    public interface OnCommentClickListener {
        void onNameClick(String userId);

        void onImageClick(String userId);
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        final TextView tvUserName, tvTime, tvText;
        final ImageView imgUserProfile;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tv_comment_user_name);
            tvTime = itemView.findViewById(R.id.tv_comment_time);
            tvText = itemView.findViewById(R.id.tv_comment_text);
            imgUserProfile = itemView.findViewById(R.id.img_item_user_profile);
        }
    }
}