package com.example.appchat.adapter;

// Nhập các thư viện cần thiết cho RecyclerView và Firebase

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appchat.ChatActivity;
import com.example.appchat.R;
import com.example.appchat.model.ChatMessageModel;
import com.example.appchat.utils.AndroidUtils;
import com.example.appchat.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

// Lớp adapter để hiển thị danh sách tin nhắn trong RecyclerView
public class ChatRecyclerAdapter extends FirestoreRecyclerAdapter<ChatMessageModel, ChatRecyclerAdapter.ChatModelViewHolder> {

    // Biến lưu trữ Context của ứng dụng
    Context context;

    // Constructor nhận FirestoreRecyclerOptions và Context
    public ChatRecyclerAdapter(@NonNull FirestoreRecyclerOptions<ChatMessageModel> options, Context context) {
        super(options); // Gọi constructor của lớp cha
        this.context = context; // Lưu Context để sử dụng
    }

    // Gắn dữ liệu của ChatMessageModel vào ViewHolder
    @Override
    protected void onBindViewHolder(@NonNull ChatModelViewHolder holder, int position, @NonNull ChatMessageModel model) {
        // Ghi log để debug (có thể xóa khi không cần)
        Log.i("haushd", "asjd");
        // Kiểm tra xem tin nhắn có phải của người dùng hiện tại hay không
        if (model.getSenderId().equals(FirebaseUtil.currentUserId())) {
            // Nếu là tin nhắn của người dùng hiện tại
            holder.leftChatLayout.setVisibility(View.GONE); // Ẩn layout tin nhắn bên trái
            holder.rightChatLayout.setVisibility(View.VISIBLE); // Hiển thị layout tin nhắn bên phải
            holder.rightChatTextview.setText(model.getMessage()); // Đặt nội dung tin nhắn vào TextView bên phải
        } else {
            // Nếu là tin nhắn của người khác
            holder.rightChatLayout.setVisibility(View.GONE); // Ẩn layout tin nhắn bên phải
            holder.leftChatLayout.setVisibility(View.VISIBLE); // Hiển thị layout tin nhắn bên trái
            holder.leftChatTextview.setText(model.getMessage()); // Đặt nội dung tin nhắn vào TextView bên trái
        }
    }

    // Tạo ViewHolder mới cho mỗi mục trong RecyclerView
    @NonNull
    @Override
    public ChatModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate layout cho mỗi hàng trong RecyclerView từ file chat_message_recycler_row.xml
        View view = LayoutInflater.from(context).inflate(R.layout.chat_message_recycler_row, parent, false);
        return new ChatModelViewHolder(view);
    }

    // Lớp ViewHolder để lưu trữ các thành phần giao diện của mỗi hàng tin nhắn
    class ChatModelViewHolder extends RecyclerView.ViewHolder {
        // Layout cho tin nhắn bên trái (người khác gửi)
        LinearLayout leftChatLayout;
        // Layout cho tin nhắn bên phải (người dùng hiện tại gửi)
        LinearLayout rightChatLayout;
        // TextView hiển thị nội dung tin nhắn bên trái
        TextView leftChatTextview;
        // TextView hiển thị nội dung tin nhắn bên phải
        TextView rightChatTextview;

        // Constructor của ViewHolder
        public ChatModelViewHolder(@NonNull View itemView) {
            super(itemView);
            // Gán các thành phần giao diện từ layout
            leftChatLayout = itemView.findViewById(R.id.left_chat_layout);
            rightChatLayout = itemView.findViewById(R.id.right_chat_layout);
            leftChatTextview = itemView.findViewById(R.id.left_chat_textview);
            rightChatTextview = itemView.findViewById(R.id.right_chat_textview);
        }
    }
}
