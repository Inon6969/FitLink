package com.example.fitlink.adapters;

import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitlink.R;
import com.example.fitlink.enums.SportType;
import com.example.fitlink.models.Event;
import com.example.fitlink.models.Group;
import com.example.fitlink.models.User;
import com.example.fitlink.services.DatabaseService;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private final String currentUserId;
    private final OnEventClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault());
    private List<Event> eventList;
    private Map<String, String> groupNamesMap = new HashMap<>();
    private boolean showGroupContext = false;

    public EventAdapter(List<Event> eventList, String currentUserId, OnEventClickListener listener) {
        this.eventList = eventList;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    public void updateList(List<Event> newList) {
        this.eventList = newList;
        notifyDataSetChanged();
    }

    public void setGroupNamesMap(Map<String, String> groupNamesMap) {
        this.groupNamesMap = groupNamesMap;
        notifyDataSetChanged();
    }

    public void setShowGroupContext(boolean showGroupContext) {
        this.showGroupContext = showGroupContext;
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

        holder.tvTitle.setText(event.getTitle());

        if (event.getSportType() != null) {
            holder.imgIcon.setImageResource(getSportIconResource(event.getSportType()));
        } else {
            holder.imgIcon.setImageResource(R.drawable.ic_calendar);
        }

        if (event.isIndependent()) {
            holder.layoutSport.setVisibility(View.VISIBLE);
            if (event.getSportType() != null) {
                holder.tvSport.setText(event.getSportType().getDisplayName());
                holder.imgSport.setImageResource(getSportIconResource(event.getSportType()));
            } else {
                holder.tvSport.setText("General");
                holder.imgSport.setImageResource(R.drawable.ic_sport);
            }
        } else {
            holder.layoutSport.setVisibility(View.GONE);
        }

        holder.tvCreator.setText("Loading...");

        if (showGroupContext) {
            if (event.isIndependent()) {
                loadCreatorNameAndSetSubtitle(holder, event.getCreatorId(), "Independent");
            } else {
                String groupId = event.getGroupId();
                String cachedGroupName = groupNamesMap.get(groupId);

                if (cachedGroupName != null) {
                    loadCreatorNameAndSetSubtitle(holder, event.getCreatorId(), cachedGroupName);
                } else {
                    if (groupId != null && !groupId.isEmpty()) {
                        DatabaseService.getInstance().getGroup(groupId, new DatabaseService.DatabaseCallback<>() {
                            @Override
                            public void onCompleted(Group group) {
                                String fetchedName = (group != null) ? group.getName() : "Group Event";
                                groupNamesMap.put(groupId, fetchedName);
                                loadCreatorNameAndSetSubtitle(holder, event.getCreatorId(), fetchedName);
                            }

                            @Override
                            public void onFailed(Exception e) {
                                loadCreatorNameAndSetSubtitle(holder, event.getCreatorId(), "Group Event");
                            }
                        });
                    } else {
                        loadCreatorNameAndSetSubtitle(holder, event.getCreatorId(), "Group Event");
                    }
                }
            }
        } else {
            String creatorId = event.getCreatorId();
            if (creatorId != null && !creatorId.isEmpty()) {
                DatabaseService.getInstance().getUser(creatorId, new DatabaseService.DatabaseCallback<>() {
                    @Override
                    public void onCompleted(User user) {
                        String creatorName = (user != null) ? (user.getFirstName() + " " + user.getLastName()) : "Unknown";
                        holder.tvCreator.setText("By " + creatorName);
                    }

                    @Override
                    public void onFailed(Exception e) {
                        holder.tvCreator.setText("By Unknown");
                    }
                });
            } else {
                holder.tvCreator.setText("By Unknown");
            }
        }

        boolean isCreator = event.getCreatorId() != null && event.getCreatorId().equals(currentUserId);
        holder.chipCreator.setVisibility(isCreator ? View.VISIBLE : View.GONE);

        if (event.getStartTimestamp() > 0) {
            String formattedDate = dateFormat.format(new Date(event.getStartTimestamp()));
            holder.tvDateTime.setText(formattedDate);
        } else {
            holder.tvDateTime.setText("Time not set");
        }

        if (event.getLocation() != null && event.getLocation().getAddress() != null) {
            holder.tvLocation.setText(event.getLocation().getAddress());
        } else {
            holder.tvLocation.setText("No location");
        }

        holder.tvDuration.setText(event.getFormattedDuration());

        int participants = event.getParticipantsCount();
        String limit = event.getMaxParticipants() > 0 ? String.valueOf(event.getMaxParticipants()) : "Unlimited";
        holder.tvParticipants.setText(participants + "/" + limit + " Participants");

        boolean isPastEvent = event.getEndTimestamp() < System.currentTimeMillis();
        boolean isJoined = event.getParticipants() != null && event.getParticipants().containsKey(currentUserId);

        if (isPastEvent) {
            holder.chipStatus.setVisibility(View.VISIBLE);
            holder.chipStatus.setText("COMPLETED");
            holder.itemView.setAlpha(0.6f);
        } else {
            holder.itemView.setAlpha(1.0f);
            if (isJoined) {
                holder.chipStatus.setVisibility(View.VISIBLE);
                holder.chipStatus.setText("JOINED");
            } else {
                holder.chipStatus.setVisibility(View.INVISIBLE);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEventClick(event);
            }
        });
    }

    private void loadCreatorNameAndSetSubtitle(EventViewHolder holder, String creatorId, String contextPrefix) {
        if (creatorId != null && !creatorId.isEmpty()) {
            DatabaseService.getInstance().getUser(creatorId, new DatabaseService.DatabaseCallback<>() {
                @Override
                public void onCompleted(User user) {
                    String creatorName = (user != null) ? (user.getFirstName() + " " + user.getLastName()) : "Unknown";
                    holder.tvCreator.setText(formatSubtitle(contextPrefix, creatorName));
                }

                @Override
                public void onFailed(Exception e) {
                    holder.tvCreator.setText(formatSubtitle(contextPrefix, "Unknown"));
                }
            });
        } else {
            holder.tvCreator.setText(formatSubtitle(contextPrefix, "Unknown"));
        }
    }

    // פונקציה שמייצרת עיצוב טקסטואלי נקי ומודרני (ללא אימוג'ים)
    private Spanned formatSubtitle(String groupName, String creatorName) {
        String formatted;
        if (groupName.equals("Independent")) {
            formatted = "<b>Independent Event</b> &nbsp;&nbsp;&#8226;&nbsp;&nbsp; By " + creatorName;
        } else {
            formatted = "<b>" + groupName + "</b> &nbsp;&nbsp;&#8226;&nbsp;&nbsp; By " + creatorName;
        }

        return Html.fromHtml(formatted, Html.FROM_HTML_MODE_COMPACT);
    }

    private int getSportIconResource(SportType type) {
        if (type == SportType.RUNNING) return R.drawable.ic_running;
        else if (type == SportType.SWIMMING) return R.drawable.ic_swimming;
        else if (type == SportType.CYCLING) return R.drawable.ic_cycling;
        return R.drawable.ic_sport;
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    public static class EventViewHolder extends RecyclerView.ViewHolder {
        final TextView tvTitle, tvCreator, tvDateTime, tvLocation, tvDuration, tvParticipants, tvSport;
        final ImageView imgIcon, imgSport;
        final LinearLayout layoutSport;
        final Chip chipStatus, chipCreator;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_item_event_title);
            tvCreator = itemView.findViewById(R.id.tv_item_event_creator);
            tvDateTime = itemView.findViewById(R.id.tv_item_event_datetime);
            tvLocation = itemView.findViewById(R.id.tv_item_event_location);
            tvDuration = itemView.findViewById(R.id.tv_item_event_duration);
            tvParticipants = itemView.findViewById(R.id.tv_item_event_participants);

            layoutSport = itemView.findViewById(R.id.layout_item_event_sport);
            tvSport = itemView.findViewById(R.id.tv_item_event_sport);
            imgSport = itemView.findViewById(R.id.img_item_event_sport);

            imgIcon = itemView.findViewById(R.id.img_item_event_icon);
            chipStatus = itemView.findViewById(R.id.chip_event_status);
            chipCreator = itemView.findViewById(R.id.chip_event_creator);
        }
    }
}