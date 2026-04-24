package com.example.fitlink.adapters;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitlink.R;
import com.example.fitlink.models.User;
import com.example.fitlink.utils.ImageUtil;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class JoinRequestAdapter extends RecyclerView.Adapter<JoinRequestAdapter.ViewHolder> {

    private final List<User> requestList;
    private final OnRequestClickListener listener;

    public JoinRequestAdapter(List<User> requestList, OnRequestClickListener listener) {
        this.requestList = requestList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_join_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = requestList.get(position);

        holder.tvName.setText(user.getFullName());
        holder.tvEmail.setText(user.getEmail());

        // הוספת הצגת מספר הטלפון
        if (user.getPhone() != null && !user.getPhone().isEmpty()) {
            holder.tvPhone.setText(user.getPhone());
        } else {
            holder.tvPhone.setText("No phone");
        }

        String base64Image = user.getProfileImage();
        if (base64Image != null && !base64Image.isEmpty()) {
            Bitmap bmp = ImageUtil.convertFrom64base(base64Image);
            if (bmp != null) holder.imgProfile.setImageBitmap(bmp);
            else holder.imgProfile.setImageResource(R.drawable.ic_user);
        } else {
            holder.imgProfile.setImageResource(R.drawable.ic_user);
        }

        // --- הוספת מאזין הלחיצה על הפריט עצמו למעבר לפרופיל ---
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUserClick(user);
            }
        });
        // ------------------------------------------------------

        holder.btnApprove.setOnClickListener(v -> listener.onApproveClick(user));
        holder.btnDecline.setOnClickListener(v -> listener.onDeclineClick(user));
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    public void updateList(List<User> newList) {
        requestList.clear();
        requestList.addAll(newList);
        notifyDataSetChanged();
    }

    public interface OnRequestClickListener {
        void onUserClick(User user); // <-- הוספנו את מתודת הלחיצה על המשתמש

        void onApproveClick(User user);

        void onDeclineClick(User user);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName, tvEmail, tvPhone;
        final ImageView imgProfile;
        final MaterialButton btnApprove, btnDecline;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_request_name);
            tvEmail = itemView.findViewById(R.id.tv_request_email);
            tvPhone = itemView.findViewById(R.id.tv_request_phone);
            imgProfile = itemView.findViewById(R.id.img_request_profile);
            btnApprove = itemView.findViewById(R.id.btn_request_approve);
            btnDecline = itemView.findViewById(R.id.btn_request_decline);
        }
    }
}