package com.example.fitlink.adapters;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitlink.R;
import com.example.fitlink.models.ContactMessage;
import com.example.fitlink.models.User;
import com.example.fitlink.services.DatabaseService;
import com.example.fitlink.utils.ImageUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ContactMessageAdapter extends RecyclerView.Adapter<ContactMessageAdapter.ViewHolder> {

    private final List<ContactMessage> messagesList = new ArrayList<>();
    private final OnMessageClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ContactMessage msg = messagesList.get(position);

        holder.tvName.setText(msg.getName());
        holder.tvEmail.setText(msg.getEmail());

        if (msg.getPhone() != null && !msg.getPhone().isEmpty()) {
            holder.tvPhone.setText(msg.getPhone());
        } else {
            holder.tvPhone.setText("No phone provided");
        }

        holder.tvContent.setText(msg.getMessage());
        holder.tvDate.setText(dateFormat.format(new Date(msg.getTimestamp())));

        holder.imgProfile.setImageResource(R.drawable.ic_user);

        if (msg.getUserId() != null && !msg.getUserId().isEmpty()) {
            DatabaseService.getInstance().getUser(msg.getUserId(), new DatabaseService.DatabaseCallback<>() {
                @Override
                public void onCompleted(User user) {
                    if (user != null && user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
                        Bitmap bmp = ImageUtil.convertFrom64base(user.getProfileImage());
                        if (bmp != null) {
                            holder.imgProfile.setImageBitmap(bmp);
                        }
                    }
                }

                @Override
                public void onFailed(Exception e) {
                }
            });
        }

        // האזנה ללחיצה על הכרטיסייה כולה (מעבר לפרופיל)
        holder.itemView.setOnClickListener(v -> listener.onMessageClick(msg));

        // האזנה לכפתורים
        holder.btnReply.setOnClickListener(v -> listener.onReplyClick(msg));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(msg));
    }

    @Override
    public int getItemCount() {
        return messagesList.size();
    }

    // הוספנו את onMessageClick כדי להאזין ללחיצה על כל ההודעה
    public interface OnMessageClickListener {
        void onMessageClick(ContactMessage message);

        void onReplyClick(ContactMessage message);

        void onDeleteClick(ContactMessage message);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName, tvEmail, tvPhone, tvDate, tvContent;
        final ImageView imgProfile;
        final Button btnDelete, btnReply; // מעודכן לכפתורים הרחבים

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_item_msg_sender_name);
            tvEmail = itemView.findViewById(R.id.tv_item_msg_sender_email);
            tvPhone = itemView.findViewById(R.id.tv_item_msg_sender_phone);
            tvDate = itemView.findViewById(R.id.tv_item_msg_date);
            tvContent = itemView.findViewById(R.id.tv_item_msg_content);
            imgProfile = itemView.findViewById(R.id.img_item_msg_sender_profile);
            btnDelete = itemView.findViewById(R.id.btn_item_msg_delete);
            btnReply = itemView.findViewById(R.id.btn_item_msg_reply);
        }
    }
}