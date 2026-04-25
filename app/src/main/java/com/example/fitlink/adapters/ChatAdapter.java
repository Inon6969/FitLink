package com.example.fitlink.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.format.DateFormat;
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
import com.example.fitlink.models.ChatMessage;
import com.example.fitlink.models.User;
import com.example.fitlink.services.DatabaseService;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private final List<ChatMessage> messages;
    private final String currentUserId;
    private final String creatorId;
    // התיקון: הרחבנו את הממשק כדי שיתמוך בלחיצה על הודעה, שם ותמונה
    private final OnMessageClickListener clickListener;
    // הוסר ה-final כדי שנוכל לעדכן את מנהלי הקבוצה בזמן אמת
    private Map<String, Boolean> managers;

    public ChatAdapter(List<ChatMessage> messages, String currentUserId, String creatorId, Map<String, Boolean> managers, OnMessageClickListener clickListener) {
        this.messages = messages;
        this.currentUserId = currentUserId;
        this.creatorId = creatorId;
        this.managers = managers;
        this.clickListener = clickListener;
    }

    // --- הפונקציה החדשה שנוספה ---
    public void updateGroupManagers(Map<String, Boolean> newManagers) {
        this.managers = newManagers;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (messages.get(position).getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }
    // ------------------------------

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message_sent, parent, false);
            return new SentMessageHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message_received, parent, false);
            return new ReceivedMessageHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);

        String timeText = formatTime(message.getTimestamp());

        if (holder.getItemViewType() == VIEW_TYPE_SENT) {
            SentMessageHolder sentHolder = (SentMessageHolder) holder;
            sentHolder.tvMessage.setText(message.getText());
            sentHolder.tvTime.setText(timeText);

            sentHolder.itemView.setOnLongClickListener(v -> {
                if (clickListener != null) clickListener.onMessageLongClick(message);
                return true;
            });

        } else {
            ReceivedMessageHolder receivedHolder = (ReceivedMessageHolder) holder;
            receivedHolder.tvMessage.setText(message.getText());
            receivedHolder.tvTime.setText(timeText);

            if (message.getSenderId().equals(creatorId)) {
                receivedHolder.tvName.setText(message.getSenderName() + " (Creator)");
                receivedHolder.tvName.setTextColor(Color.parseColor("#D32F2F"));
                receivedHolder.tvName.setTypeface(null, Typeface.BOLD);
            } else if (managers != null && managers.containsKey(message.getSenderId())) {
                receivedHolder.tvName.setText(message.getSenderName() + " (Manager)");
                receivedHolder.tvName.setTextColor(Color.parseColor("#FF9800"));
                receivedHolder.tvName.setTypeface(null, Typeface.BOLD);
            } else {
                receivedHolder.tvName.setText(message.getSenderName());
                receivedHolder.tvName.setTextColor(Color.parseColor("#424242"));
                receivedHolder.tvName.setTypeface(null, Typeface.NORMAL);
            }

            // --- הגדרת מאזיני הלחיצות החדשים ---
            receivedHolder.tvName.setOnClickListener(v -> {
                if (clickListener != null) clickListener.onNameClick(message);
            });

            receivedHolder.imgUserProfile.setOnClickListener(v -> {
                if (clickListener != null) clickListener.onImageClick(message);
            });
            // -----------------------------------

            int primaryColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.fitlinkPrimary);
            float density = holder.itemView.getContext().getResources().getDisplayMetrics().density;
            int paddingPx = (int) (6 * density);

            receivedHolder.imgUserProfile.setImageResource(R.drawable.ic_user);
            receivedHolder.imgUserProfile.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
            receivedHolder.imgUserProfile.setColorFilter(primaryColor);

            if (message.getSenderId() != null) {
                DatabaseService.getInstance().getUser(message.getSenderId(), new DatabaseService.DatabaseCallback<>() {
                    @Override
                    public void onCompleted(User user) {
                        if (user != null && user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
                            try {
                                byte[] decodedString = Base64.decode(user.getProfileImage(), Base64.DEFAULT);
                                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                                receivedHolder.imgUserProfile.setImageBitmap(decodedByte);
                                receivedHolder.imgUserProfile.clearColorFilter();
                                receivedHolder.imgUserProfile.setPadding(0, 0, 0, 0);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void onFailed(Exception e) {
                    }
                });
            }

            receivedHolder.itemView.setOnLongClickListener(v -> {
                if (clickListener != null) clickListener.onMessageLongClick(message);
                return true;
            });
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    private String formatTime(long timestamp) {
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTimeInMillis(timestamp);
        return DateFormat.format("HH:mm", cal).toString();
    }

    public interface OnMessageClickListener {
        void onMessageLongClick(ChatMessage message);

        void onNameClick(ChatMessage message);

        void onImageClick(ChatMessage message);
    }

    static class SentMessageHolder extends RecyclerView.ViewHolder {
        final TextView tvMessage, tvTime;

        SentMessageHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_chat_sent_message);
            tvTime = itemView.findViewById(R.id.tv_chat_sent_time);
        }
    }

    static class ReceivedMessageHolder extends RecyclerView.ViewHolder {
        final TextView tvName, tvMessage, tvTime;
        final ImageView imgUserProfile;

        ReceivedMessageHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_chat_received_name);
            tvMessage = itemView.findViewById(R.id.tv_chat_received_message);
            tvTime = itemView.findViewById(R.id.tv_chat_received_time);
            imgUserProfile = itemView.findViewById(R.id.img_chat_user_profile);
        }
    }
}