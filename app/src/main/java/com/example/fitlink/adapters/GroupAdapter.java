package com.example.fitlink.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitlink.R;
import com.example.fitlink.models.Group;
import com.example.fitlink.models.SportType;
import com.example.fitlink.models.User;
import com.example.fitlink.services.DatabaseService;
import com.google.android.material.chip.Chip;

import java.util.List;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupViewHolder> {

    private List<Group> groupList;
    private OnGroupClickListener listener;

    public GroupAdapter(List<Group> groupList, OnGroupClickListener listener) {
        this.groupList = groupList;
        this.listener = listener;
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

        // הגדרת טקסטים בסיסיים
        holder.tvName.setText(group.getName());
        holder.tvLocation.setText(group.getLocation());
        holder.chipLevel.setText(group.getLevel());
        holder.tvSport.setText(group.getSportType().name());

        // עדכון אייקונים דינמיים (גם הראשי וגם המיני)
        int sportIconRes = getSportIconResource(group.getSportType());
        holder.imgIcon.setImageResource(sportIconRes);
        holder.imgSportMini.setImageResource(sportIconRes);

        // חישוב כמות חברים מתוך ה-Map (שימוש ב-size)
        int memberCount = (group.getMembers() != null) ? group.getMembers().size() : 0;
        holder.tvMembers.setText(memberCount + (memberCount == 1 ? " Member" : " Members"));

        // שליפת שם היוצר מה-Database
        holder.tvCreator.setText("Loading...");
        DatabaseService.getInstance().getUser(group.getAdminId(), new DatabaseService.DatabaseCallback<User>() {
            @Override
            public void onCompleted(User user) {
                if (user != null) {
                    holder.tvCreator.setText("By " + user.getFirstName() + " " + user.getLastName());
                }
            }

            @Override
            public void onFailed(Exception e) {
                holder.tvCreator.setText("By Unknown");
            }
        });

        holder.btnJoin.setOnClickListener(v -> {
            if (listener != null) {
                listener.onJoinClick(group);
            }
        });
    }

    private int getSportIconResource(SportType type) {
        if (type == SportType.RUNNING) {
            return R.drawable.ic_running;
        } else if (type == SportType.SWIMMING) {
            return R.drawable.ic_swimming;
        } else if (type == SportType.CYCLING) {
            return R.drawable.ic_cycling;
        }
        return R.drawable.ic_sport;
    }

    @Override
    public int getItemCount() {
        return groupList.size();
    }

    public interface OnGroupClickListener {
        void onJoinClick(Group group);
    }

    public static class GroupViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvSport, tvLocation, tvMembers, tvCreator;
        ImageView imgIcon, imgSportMini;
        Chip chipLevel;
        Button btnJoin;

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
        }
    }
}