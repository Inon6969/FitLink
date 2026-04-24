package com.example.fitlink.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
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
import com.example.fitlink.models.Group;
import com.example.fitlink.models.SportType;
import com.example.fitlink.models.User;
import com.example.fitlink.services.DatabaseService;
import com.example.fitlink.utils.ImageUtil;
import com.google.android.material.chip.Chip;

import java.util.List;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupViewHolder> {

    private final boolean showJoinButton;
    private final String currentUserId;
    private final OnGroupClickListener listener;
    private List<Group> groupList;

    public GroupAdapter(List<Group> groupList, boolean showJoinButton, String currentUserId, OnGroupClickListener listener) {
        this.groupList = groupList;
        this.showJoinButton = showJoinButton;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    public void updateList(List<Group> newList) {
        this.groupList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_group, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        Group group = groupList.get(position);

        holder.tvName.setText(group.getName());
        if (group.getLocation() != null) {
            holder.tvLocation.setText(group.getLocation().getAddress());
        } else {
            holder.tvLocation.setText("No location");
        }

        if (group.getLevel() != null) {
            holder.chipLevel.setText(group.getLevel().getDisplayName());
        } else {
            holder.chipLevel.setText("Unknown");
        }

        holder.tvSport.setText(group.getSportType().getDisplayName());

        int sportIconRes = getSportIconResource(group.getSportType());
        holder.imgSportMini.setImageResource(sportIconRes);

        // --- הלוגיקה של התמונה (כולל ביטול ה-Tint) ---
        String base64Image = group.getGroupImage();
        Context context = holder.itemView.getContext();

        if (base64Image != null && !base64Image.isEmpty()) {
            Bitmap bitmap = ImageUtil.convertFrom64base(base64Image);
            if (bitmap != null) {
                holder.imgIcon.setImageBitmap(bitmap);
                holder.imgIcon.setPadding(0, 0, 0, 0);
                // ביטול ה-Tint כדי שהתמונה תוצג בצבעים המקוריים שלה!
                holder.imgIcon.setImageTintList(null);
            } else {
                setFallbackIcon(holder, sportIconRes, context);
            }
        } else {
            setFallbackIcon(holder, sportIconRes, context);
        }

        int memberCount = (group.getMembers() != null) ? group.getMembers().size() : 0;
        holder.tvMembers.setText(memberCount + (memberCount == 1 ? " Member" : " Members"));

        String creatorId = group.getCreatorId();
        boolean isCreator = creatorId != null && creatorId.equals(currentUserId);
        boolean isManager = group.getManagers() != null && group.getManagers().containsKey(currentUserId);

        if (isCreator) {
            holder.chipCreator.setText("Created by you");
            holder.chipCreator.setVisibility(View.VISIBLE);
        } else if (isManager) {
            holder.chipCreator.setText("Managed by you");
            holder.chipCreator.setVisibility(View.VISIBLE);
        } else {
            holder.chipCreator.setVisibility(View.GONE);
        }

        holder.tvCreator.setText("Loading...");
        if (creatorId != null && !creatorId.isEmpty()) {
            DatabaseService.getInstance().getUser(creatorId, new DatabaseService.DatabaseCallback<>() {
                @Override
                public void onCompleted(User user) {
                    if (user != null) {
                        holder.tvCreator.setText(String.format("By %s %s", user.getFirstName(), user.getLastName()));
                    } else {
                        holder.tvCreator.setText("By Unknown");
                    }
                }

                @Override
                public void onFailed(Exception e) {
                    holder.tvCreator.setText("By Unknown");
                }
            });
        } else {
            holder.tvCreator.setText("By Unknown");
        }

        if (showJoinButton && currentUserId != null) {
            holder.btnJoin.setVisibility(View.VISIBLE);

            boolean isMember = group.getMembers() != null && group.getMembers().containsKey(currentUserId);
            boolean isPending = group.getPendingRequests() != null && group.getPendingRequests().containsKey(currentUserId);

            if (isMember) {
                holder.btnJoin.setText("LEAVE");
                holder.btnJoin.setTextColor(ContextCompat.getColor(context, R.color.fitlinkTextSecondary));
                holder.btnJoin.setOnClickListener(v -> {
                    if (listener != null) listener.onLeaveClick(group);
                });
            } else if (isPending) {
                // התיקון: שינוי הטקסט והפעלת אירוע הלחיצה במקום Toast
                holder.btnJoin.setText("CANCEL REQ");
                holder.btnJoin.setTextColor(ContextCompat.getColor(context, R.color.fitlinkTextSecondary));
                holder.btnJoin.setOnClickListener(v -> {
                    if (listener != null) listener.onJoinClick(group);
                });
            } else {
                holder.btnJoin.setText("JOIN");
                holder.btnJoin.setTextColor(ContextCompat.getColor(context, R.color.fitlinkPrimary));
                holder.btnJoin.setOnClickListener(v -> {
                    if (listener != null) listener.onJoinClick(group);
                });
            }
        } else {
            holder.btnJoin.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onGroupClick(group);
        });
    }

    private void setFallbackIcon(GroupViewHolder holder, int sportIconRes, Context context) {
        holder.imgIcon.setImageResource(sportIconRes);
        int padding = (int) (12 * context.getResources().getDisplayMetrics().density);
        holder.imgIcon.setPadding(padding, padding, padding, padding);

        // החזרת צבע הפרימרי של האפליקציה (כחול) לאייקון
        holder.imgIcon.setImageTintList(ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.fitlinkPrimary)
        ));
    }

    private int getSportIconResource(SportType type) {
        if (type == SportType.RUNNING) return R.drawable.ic_running;
        else if (type == SportType.SWIMMING) return R.drawable.ic_swimming;
        else if (type == SportType.CYCLING) return R.drawable.ic_cycling;
        return R.drawable.ic_sport;
    }

    @Override
    public int getItemCount() {
        return groupList.size();
    }

    public interface OnGroupClickListener {
        void onJoinClick(Group group);

        void onLeaveClick(Group group);

        void onGroupClick(Group group);
    }

    public static class GroupViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName, tvSport, tvLocation, tvMembers, tvCreator;
        final ImageView imgIcon, imgSportMini;
        final Chip chipLevel;
        final Button btnJoin;
        final Chip chipCreator;

        public GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_item_group_name);
            tvSport = itemView.findViewById(R.id.tv_item_group_sport);
            tvLocation = itemView.findViewById(R.id.tv_item_group_location);
            tvMembers = itemView.findViewById(R.id.tv_item_group_members);
            tvCreator = itemView.findViewById(R.id.tv_item_group_creator);
            imgIcon = itemView.findViewById(R.id.img_item_group_icon);
            imgSportMini = itemView.findViewById(R.id.img_item_group_sport_mini);
            chipLevel = itemView.findViewById(R.id.chip_group_level);
            btnJoin = itemView.findViewById(R.id.btn_item_group_join);
            chipCreator = itemView.findViewById(R.id.chip_group_creator);
        }
    }
}