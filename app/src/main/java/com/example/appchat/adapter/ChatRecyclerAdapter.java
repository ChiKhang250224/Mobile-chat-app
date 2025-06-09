package com.example.appchat.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.appchat.R;
import com.example.appchat.model.ChatMessageModel;
import com.example.appchat.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

public class ChatRecyclerAdapter extends FirestoreRecyclerAdapter<ChatMessageModel, ChatRecyclerAdapter.ChatModelViewHolder> {

    Context context;

    public ChatRecyclerAdapter(@NonNull FirestoreRecyclerOptions<ChatMessageModel> options, Context context) {
        super(options);
        this.context = context;
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatModelViewHolder holder, int position, @NonNull ChatMessageModel model) {
        if (model.getSenderId().equals(FirebaseUtil.currentUserId())) {
            // Tin nhắn của người dùng hiện tại (bên phải)
            holder.leftChatLayout.setVisibility(View.GONE);
            holder.rightChatLayout.setVisibility(View.VISIBLE);

            if (model.getImageUrl() != null && !model.getImageUrl().isEmpty()) {
                holder.rightChatTextview.setVisibility(View.GONE);
                holder.rightImageView.setVisibility(View.VISIBLE);
                Glide.with(context).load(model.getImageUrl()).into(holder.rightImageView);
            } else {
                holder.rightImageView.setVisibility(View.GONE);
                holder.rightChatTextview.setVisibility(View.VISIBLE);
                holder.rightChatTextview.setText(model.getMessage());
            }

        } else {
            // Tin nhắn từ người khác (bên trái)
            holder.rightChatLayout.setVisibility(View.GONE);
            holder.leftChatLayout.setVisibility(View.VISIBLE);

            if (model.getImageUrl() != null && !model.getImageUrl().isEmpty()) {
                holder.leftChatTextview.setVisibility(View.GONE);
                holder.leftImageView.setVisibility(View.VISIBLE);
                Glide.with(context).load(model.getImageUrl()).into(holder.leftImageView);
            } else {
                holder.leftImageView.setVisibility(View.GONE);
                holder.leftChatTextview.setVisibility(View.VISIBLE);
                holder.leftChatTextview.setText(model.getMessage());
            }
        }
    }

    @NonNull
    @Override
    public ChatModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.chat_message_recycler_row, parent, false);
        return new ChatModelViewHolder(view);
    }

    class ChatModelViewHolder extends RecyclerView.ViewHolder {
        LinearLayout leftChatLayout, rightChatLayout;
        TextView leftChatTextview, rightChatTextview;
        ImageView leftImageView, rightImageView;

        public ChatModelViewHolder(@NonNull View itemView) {
            super(itemView);
            leftChatLayout = itemView.findViewById(R.id.left_chat_layout);
            rightChatLayout = itemView.findViewById(R.id.right_chat_layout);
            leftChatTextview = itemView.findViewById(R.id.left_chat_textview);
            rightChatTextview = itemView.findViewById(R.id.right_chat_textview);
            leftImageView = itemView.findViewById(R.id.left_chat_image_view);
            rightImageView = itemView.findViewById(R.id.right_chat_image_view);
        }
    }
}
