package com.example.fitlink.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitlink.R;
import com.example.fitlink.models.ContactMessage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ContactMessageAdapter extends RecyclerView.Adapter<ContactMessageAdapter.ViewHolder> {

    private final List<ContactMessage> messagesList = new ArrayList<>();
    private final OnMessageClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public interface OnMessageClickListener {
        void onReplyClick(ContactMessage message);
        void onDeleteClick(ContactMessage message);
    }

    public ContactMessageAdapter(OnMessageClickListener listener) {
        this.listener = listener;
    }

    public void updateList(List<ContactMessage> newMessages) {
        messagesList.clear();
        messagesList.addAll(newMessages);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // שימוש בשם העיצוב החדש והספציפי
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ContactMessage msg = messagesList.get(position);

        holder.tvName.setText(msg.getName());
        holder.tvEmail.setText(msg.getEmail());

        // הגדרת מספר הטלפון (עם הגנה למקרה שאין מספר)
        if (msg.getPhone() != null && !msg.getPhone().isEmpty()) {
            holder.tvPhone.setText(msg.getPhone());
        } else {
            holder.tvPhone.setText("No phone provided");
        }

        holder.tvContent.setText(msg.getMessage());
        holder.tvDate.setText(dateFormat.format(new Date(msg.getTimestamp())));

        holder.btnReply.setOnClickListener(v -> listener.onReplyClick(msg));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(msg));
    }

    @Override
    public int getItemCount() {
        return messagesList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvPhone, tvDate, tvContent;
        ImageButton btnDelete, btnReply;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // קישור לשמות ה-IDs החדשים מהעיצוב המעודכן
            tvName = itemView.findViewById(R.id.tv_item_msg_sender_name);
            tvEmail = itemView.findViewById(R.id.tv_item_msg_sender_email);
            tvPhone = itemView.findViewById(R.id.tv_item_msg_sender_phone);
            tvDate = itemView.findViewById(R.id.tv_item_msg_date);
            tvContent = itemView.findViewById(R.id.tv_item_msg_content);
            btnDelete = itemView.findViewById(R.id.btn_item_msg_delete);
            btnReply = itemView.findViewById(R.id.btn_item_msg_reply);
        }
    }
}