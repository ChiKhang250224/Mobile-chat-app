package com.example.appchat.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appchat.R;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.ViewHolder> {

    public interface OnGroupClickListener {
        void onGroupClick(String groupId);
    }

    private List<DocumentSnapshot> groupList;
    private OnGroupClickListener listener;

    public GroupAdapter(List<DocumentSnapshot> groupList, OnGroupClickListener listener) {
        this.groupList = groupList;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtGroupName;

        public ViewHolder(View view) {
            super(view);
            txtGroupName = view.findViewById(R.id.txtGroupName);
        }
    }

    @NonNull
    @Override
    public GroupAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_group, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupAdapter.ViewHolder holder, int position) {
        DocumentSnapshot group = groupList.get(position);
        String groupName = group.getString("groupName");
        String groupId = group.getId();

        holder.txtGroupName.setText(groupName);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onGroupClick(groupId);
        });
    }

    @Override
    public int getItemCount() {
        return groupList.size();
    }
}

