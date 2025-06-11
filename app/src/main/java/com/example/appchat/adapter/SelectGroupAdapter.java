package com.example.appchat.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.appchat.model.ChatroomModel;

import java.util.List;

public class SelectGroupAdapter extends RecyclerView.Adapter<SelectGroupAdapter.GroupViewHolder> {

    public interface OnGroupClickListener {
        void onGroupClick(ChatroomModel group);
    }

    private List<ChatroomModel> groups;
    private OnGroupClickListener listener;

    public SelectGroupAdapter(List<ChatroomModel> groups, OnGroupClickListener listener) {
        this.groups = groups;
        this.listener = listener;
    }

    public GroupViewHolder onCreateViewHolder( ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder( GroupViewHolder holder, int position) {
        ChatroomModel group = groups.get(position);
        holder.textView.setText(group.getGroupName());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onGroupClick(group);
            }
        });
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    static class GroupViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        public GroupViewHolder( View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
    }
}

