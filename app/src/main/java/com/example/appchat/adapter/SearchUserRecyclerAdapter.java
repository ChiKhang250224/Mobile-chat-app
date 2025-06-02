package com.example.appchat.adapter;

// Nhập các thư viện cần thiết cho RecyclerView, Firebase, và các thành phần giao diện

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appchat.ChatActivity;
import com.example.appchat.R;
import com.example.appchat.model.UserModel;
import com.example.appchat.utils.AndroidUtils;
import com.example.appchat.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

// Lớp adapter để hiển thị danh sách người dùng trong RecyclerView
public class SearchUserRecyclerAdapter extends FirestoreRecyclerAdapter<UserModel, SearchUserRecyclerAdapter.UserModelViewHolder> {

    // Biến lưu trữ Context của ứng dụng
    Context context;

    // Constructor nhận FirestoreRecyclerOptions và Context
    public SearchUserRecyclerAdapter(@NonNull FirestoreRecyclerOptions<UserModel> options, Context context) {
        super(options); // Gọi constructor của lớp cha
        this.context = context; // Lưu Context để sử dụng
    }

    // Gắn dữ liệu của UserModel vào ViewHolder
    @Override
    protected void onBindViewHolder(@NonNull UserModelViewHolder holder, int position, @NonNull UserModel model) {
        // Hiển thị tên người dùng
        holder.usernameText.setText(model.getUsername());
        // Hiển thị số điện thoại
        holder.phoneText.setText(model.getPhone());
        // Nếu người dùng là chính mình, thêm "(Me)" vào tên
        if (model.getUserId().equals(FirebaseUtil.currentUserId())) {
            holder.usernameText.setText(model.getUsername() + " (Me)");
        }

        // Xử lý sự kiện khi nhấn vào một mục trong danh sách
        holder.itemView.setOnClickListener(v -> {
            // Chuyển sang ChatActivity
            Intent intent = new Intent(context, ChatActivity.class);
            // Truyền dữ liệu UserModel qua Intent
            AndroidUtils.passUserModelAsIntent(intent, model);
            // Đặt cờ để tạo Activity mới
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            // Khởi động ChatActivity
            context.startActivity(intent);
        });
    }

    // Tạo ViewHolder mới cho mỗi mục trong RecyclerView
    @NonNull
    @Override
    public UserModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate layout cho mỗi hàng trong RecyclerView từ file search_user_recycler_row.xml
        View view = LayoutInflater.from(context).inflate(R.layout.search_user_recycler_row, parent, false);
        return new UserModelViewHolder(view);
    }

    // Lớp ViewHolder để lưu trữ các thành phần giao diện của mỗi hàng
    class UserModelViewHolder extends RecyclerView.ViewHolder {
        TextView usernameText; // TextView hiển thị tên người dùng
        TextView phoneText; // TextView hiển thị số điện thoại
        ImageView profilePic; // ImageView hiển thị ảnh đại diện

        // Constructor của ViewHolder
        public UserModelViewHolder(@NonNull View itemView) {
            super(itemView);
            // Gán các thành phần giao diện từ layout
            usernameText = itemView.findViewById(R.id.user_name_text);
            phoneText = itemView.findViewById(R.id.phone_text);
            profilePic = itemView.findViewById(R.id.profile_pic_image_view);
        }
    }
}