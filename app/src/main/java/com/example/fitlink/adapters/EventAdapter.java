package com.example.fitlink.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitlink.R;
import com.example.fitlink.models.Event;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private List<Event> eventList;
    private final String currentUserId;
    private final OnEventClickListener listener;

    // אובייקט לעיצוב תאריך ושעה מתוך מספר (Timestamp)
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault());

    public EventAdapter(List<Event> eventList, String currentUserId, OnEventClickListener listener) {
        this.eventList = eventList;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    public void updateList(List<Event> newList) {
        this.eventList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = eventList.get(position);
        Context context = holder.itemView.getContext();

        holder.tvTitle.setText(event.getTitle());

        // המרת הזמן ממילישניות לטקסט קריא
        if (event.getStartTimestamp() > 0) {
            String formattedDate = dateFormat.format(new Date(event.getStartTimestamp()));
            holder.tvDateTime.setText(formattedDate);
        } else {
            holder.tvDateTime.setText("Time not set");
        }

        // הצגת מיקום
        if (event.getLocation() != null && event.getLocation().getAddress() != null) {
            holder.tvLocation.setText(event.getLocation().getAddress());
        } else {
            holder.tvLocation.setText("No location");
        }

        // --- התיקון כאן: שימוש ב-getFormattedDuration() במקום getDurationMinutes() ---
        int participants = event.getParticipantsCount();
        String limit = event.getMaxParticipants() > 0 ? String.valueOf(event.getMaxParticipants()) : "Unlimited";
        String details = event.getFormattedDuration() + " • " + participants + "/" + limit + " Participants";
        holder.tvDetails.setText(details);

        // בדיקה האם המשתמש כבר משתתף באירוע
        boolean isJoined = event.getParticipants() != null && event.getParticipants().containsKey(currentUserId);

        if (isJoined) {
            holder.chipStatus.setVisibility(View.VISIBLE);
            holder.chipStatus.setText("JOINED");
            holder.btnAction.setText("LEAVE");
            holder.btnAction.setTextColor(ContextCompat.getColor(context, R.color.fitlinkTextSecondary)); // אפור
        } else {
            // שימוש ב-INVISIBLE כדי לשמור על העיצוב יציב ללא קפיצות
            holder.chipStatus.setVisibility(View.INVISIBLE);
            holder.btnAction.setText("JOIN");
            holder.btnAction.setTextColor(ContextCompat.getColor(context, R.color.fitlinkPrimary)); // צבע ראשי
        }

        // לחיצה על כפתור הפעולה (Join / Leave)
        holder.btnAction.setOnClickListener(v -> {
            if (listener != null) {
                listener.onActionClick(event, isJoined);
            }
        });

        // לחיצה על כל השורה (לצפייה בפרטים המלאים)
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEventClick(event);
            }
        });
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    // ממשק להאזנה ללחיצות
    public interface OnEventClickListener {
        void onEventClick(Event event);
        void onActionClick(Event event, boolean isCurrentlyJoined);
    }

    // מחלקת ViewHolder לחיבור רכיבי ה-UI
    public static class EventViewHolder extends RecyclerView.ViewHolder {
        final TextView tvTitle, tvDateTime, tvLocation, tvDetails;
        final ImageView imgIcon;
        final Chip chipStatus;
        final Button btnAction;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_item_event_title);
            tvDateTime = itemView.findViewById(R.id.tv_item_event_datetime);
            tvLocation = itemView.findViewById(R.id.tv_item_event_location);
            tvDetails = itemView.findViewById(R.id.tv_item_event_details);
            imgIcon = itemView.findViewById(R.id.img_item_event_icon);
            chipStatus = itemView.findViewById(R.id.chip_event_status);
            btnAction = itemView.findViewById(R.id.btn_item_event_action);
        }
    }
}