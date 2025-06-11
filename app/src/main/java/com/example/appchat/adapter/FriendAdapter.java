package com.example.appchat.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appchat.R;
import com.example.appchat.model.UserModel;

import java.util.List;
import java.util.Set;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.FriendViewHolder> {

    private final List<UserModel> userList;
    private final Set<String> selectedUserIds;
    private final OnUserCheckedChangeListener listener;

    // ✅ Sửa: Không truyền userId nữa
    public interface OnUserCheckedChangeListener {
        void onUserCheckedChanged();
    }

    public FriendAdapter(List<UserModel> userList, Set<String> selectedUserIds, OnUserCheckedChangeListener listener) {
        this.userList = userList;
        this.selectedUserIds = selectedUserIds;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.friend_item, parent, false);
        return new FriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        UserModel user = userList.get(position);
        holder.usernameTextView.setText(user.getUsername());

        // ✅ Cập nhật checkbox chính xác
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(selectedUserIds.contains(user.getUserId()));

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedUserIds.add(user.getUserId());
            } else {
                selectedUserIds.remove(user.getUserId());
            }
            listener.onUserCheckedChanged(); // ✅ Callback đơn giản
            notifyItemChanged(holder.getAdapterPosition()); // cập nhật màu nền
        });

        // ✅ Click vào item thì toggle checkbox
        holder.itemView.setOnClickListener(v -> holder.checkBox.toggle());

        // ✅ Thay đổi màu nền nếu được chọn
        if (selectedUserIds.contains(user.getUserId())) {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.light_gray));
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class FriendViewHolder extends RecyclerView.ViewHolder {
        TextView usernameTextView;
        CheckBox checkBox;

        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            // ✅ Đảm bảo đúng ID XML
            usernameTextView = itemView.findViewById(R.id.friend_username);
            checkBox = itemView.findViewById(R.id.friend_checkbox);
        }
    }
}
