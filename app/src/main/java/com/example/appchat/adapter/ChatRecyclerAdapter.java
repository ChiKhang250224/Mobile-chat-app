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
    private OnMessageLongClickListener longClickListener; // Listener cho sự kiện nhấn giữ

    // Interface để xử lý sự kiện nhấn giữ
    public interface OnMessageLongClickListener {
        void onMessageLongClick(ChatMessageModel message, String documentId, int position);
    }

    public ChatRecyclerAdapter(@NonNull FirestoreRecyclerOptions<ChatMessageModel> options, Context context) {
        super(options);
        this.context = context;
    }

    // Phương thức để set listener
    public void setOnMessageLongClickListener(OnMessageLongClickListener listener) {
        this.longClickListener = listener;
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatModelViewHolder holder, int position, @NonNull ChatMessageModel model) {
        // Lấy document ID của tin nhắn
        String documentId = getSnapshots().getSnapshot(position).getId();

        // Kiểm tra nếu tin nhắn đã bị xóa bởi người dùng hiện tại
        if (model.getDeletedBy() != null && model.getDeletedBy().contains(FirebaseUtil.currentUserId())) {
            holder.itemView.setVisibility(View.GONE);
            holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0)); // Ẩn hoàn toàn
            return;
        } else {
            holder.itemView.setVisibility(View.VISIBLE);
            holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        // Kiểm tra có tin nhắn nào đã bị thu hồi chưa
        if (model.isRecalled()) {
            holder.leftChatLayout.setVisibility(View.GONE);
            holder.rightChatLayout.setVisibility(View.GONE);
            if (model.getSenderId().equals(FirebaseUtil.currentUserId())) {
                holder.rightChatLayout.setVisibility(View.VISIBLE);
                holder.rightChatTextview.setVisibility(View.VISIBLE);
                holder.rightImageView.setVisibility(View.GONE);
                holder.rightChatTextview.setText("Tin nhắn đã được thu hồi");
                holder.rightChatLayout.setBackgroundResource(R.drawable.bg_right_recalled_bubble);

            } else {
                holder.leftChatLayout.setVisibility(View.VISIBLE);
                holder.leftChatTextview.setVisibility(View.VISIBLE);
                holder.leftImageView.setVisibility(View.GONE);
                holder.leftChatTextview.setText("Tin nhắn đã được thu hồi");
                holder.leftChatLayout.setBackgroundResource(R.drawable.bg_left_recalled_bubble);
            }
            return;
        } else {
            // Thiết lập nền mặc định
            holder.leftChatLayout.setBackgroundResource(R.drawable.bg_left_bubble);
            holder.rightChatLayout.setBackgroundResource(R.drawable.bg_right_bubble);

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

        // Thiết lập sự kiện nhấn giữ
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onMessageLongClick(model, documentId, position);
            }
            return true;
        });
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