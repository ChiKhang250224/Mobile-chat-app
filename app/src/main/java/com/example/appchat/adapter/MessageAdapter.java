package com.example.appchat.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appchat.R;
import com.example.appchat.model.ChatMessageModel;


import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    private List<ChatMessageModel> messages;
    private String currentUserId;

    public MessageAdapter(List<ChatMessageModel> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtSender, textMessage;
        ImageView imageMessage;

        public ViewHolder(View view) {
            super(view);
            txtSender = view.findViewById(R.id.txtSender);
            textMessage = view.findViewById(R.id.textMessage);
            imageMessage = view.findViewById(R.id.imageMessage);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ChatMessageModel m = messages.get(position);

        // Hiển thị người gửi
        holder.txtSender.setText(m.getSenderId().equals(currentUserId) ? "Bạn" : m.getSenderId());

    }

    @Override
    public int getItemCount() {
        return messages.size();
    }
}
