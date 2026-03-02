package com.example.fitlink.adapters;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitlink.R;
import com.example.fitlink.models.ChatMessage;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private final List<ChatMessage> messages;
    private final String currentUserId;
    private final String creatorId; // שונה מ-adminId ל-creatorId
    private final Map<String, Boolean> managers; // הוספנו את רשימת המנהלים
    private final OnMessageLongClickListener longClickListener;

    public interface OnMessageLongClickListener {
        void onMessageLongClick(ChatMessage message);
    }

    // הקונסטרקטור המעודכן שמקבל גם את המנהלים
    public ChatAdapter(List<ChatMessage> messages, String currentUserId, String creatorId, Map<String, Boolean> managers, OnMessageLongClickListener longClickListener) {
        this.messages = messages;
        this.currentUserId = currentUserId;
        this.creatorId = creatorId;
        this.managers = managers;
        this.longClickListener = longClickListener;
    }

    @Override
    public int getItemViewType(int position) {
        if (messages.get(position).getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

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
                if(longClickListener != null) longClickListener.onMessageLongClick(message);
                return true;
            });

        } else {
            ReceivedMessageHolder receivedHolder = (ReceivedMessageHolder) holder;
            receivedHolder.tvMessage.setText(message.getText());
            receivedHolder.tvTime.setText(timeText);

            // --- לוגיקת ההדגשה לפי תפקיד (Creator או Manager) ---
            if (creatorId != null && message.getSenderId().equals(creatorId)) {
                // יוצר הקבוצה
                receivedHolder.tvName.setText(message.getSenderName() + " (Creator)");
                receivedHolder.tvName.setTextColor(Color.parseColor("#D32F2F")); // כתום בולט
                receivedHolder.tvName.setTypeface(null, Typeface.BOLD);
            } else if (managers != null && managers.containsKey(message.getSenderId())) {
                // מנהל משנה
                receivedHolder.tvName.setText(message.getSenderName() + " (Manager)");
                receivedHolder.tvName.setTextColor(Color.parseColor("#FF9800")); // ירוק בולט
                receivedHolder.tvName.setTypeface(null, Typeface.BOLD);
            } else {
                // חבר רגיל
                receivedHolder.tvName.setText(message.getSenderName());
                receivedHolder.tvName.setTextColor(Color.parseColor("#424242")); // אפור רגיל
                receivedHolder.tvName.setTypeface(null, Typeface.NORMAL);
            }

            receivedHolder.itemView.setOnLongClickListener(v -> {
                if(longClickListener != null) longClickListener.onMessageLongClick(message);
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

    static class SentMessageHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;

        SentMessageHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_chat_sent_message);
            tvTime = itemView.findViewById(R.id.tv_chat_sent_time);
        }
    }

    static class ReceivedMessageHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvMessage, tvTime;

        ReceivedMessageHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_chat_received_name);
            tvMessage = itemView.findViewById(R.id.tv_chat_received_message);
            tvTime = itemView.findViewById(R.id.tv_chat_received_time);
        }
    }
}