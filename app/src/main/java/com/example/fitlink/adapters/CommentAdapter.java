package com.example.fitlink.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
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

        // משיכת שם המשתמש שכתב את התגובה
        holder.tvUserName.setText("Loading...");
        if (comment.getUserId() != null) {
            DatabaseService.getInstance().getUser(comment.getUserId(), new DatabaseService.DatabaseCallback<User>() {
                @Override
                public void onCompleted(User user) {
                    if (user != null) {
                        holder.tvUserName.setText(user.getFirstName() + " " + user.getLastName());
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

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tv_comment_user_name);
            tvTime = itemView.findViewById(R.id.tv_comment_time);
            tvText = itemView.findViewById(R.id.tv_comment_text);
        }
    }
}