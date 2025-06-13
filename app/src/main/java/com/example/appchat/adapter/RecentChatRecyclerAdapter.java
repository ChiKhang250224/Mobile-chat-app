package com.example.appchat.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.appchat.ChatActivity; // Chỉ sử dụng ChatActivity
// import com.example.appchat.GroupChatActivity; // Xóa bỏ dòng này
import com.example.appchat.R;
import com.example.appchat.model.ChatroomModel;
import com.example.appchat.model.UserModel;
import com.example.appchat.utils.AndroidUtils;
import com.example.appchat.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Collections;
import java.util.List;

public class RecentChatRecyclerAdapter
        extends FirestoreRecyclerAdapter<ChatroomModel, RecentChatRecyclerAdapter.ChatroomModelViewHolder> {

    private final Context context;

    public RecentChatRecyclerAdapter(FirestoreRecyclerOptions<ChatroomModel> options,
                                     Context context) {
        super(options);
        this.context = context;
    }

    @Override
    public ChatroomModel getItem(int position) {
        DocumentSnapshot snapshot = getSnapshots().getSnapshot(position);
        ChatroomModel model = snapshot.toObject(ChatroomModel.class);
        if (model != null) {
            model.setChatroomId(snapshot.getId());
            // TODO: (Nếu bạn đã thêm trường isGroupChat vào ChatroomModel và Firestore)
            // model.setIsGroupChat(snapshot.getBoolean("isGroupChat")); // Đảm bảo field "isGroupChat" tồn tại trong Firestore
        }
        return model;
    }

    @Override
    protected void onBindViewHolder(
            ChatroomModelViewHolder holder,
            int position,
            ChatroomModel model) {

        // Debug: Kiểm tra chatroomId đã được gán đúng chưa
        Log.d("ChatAdapter", "Binding Chatroom ID: " + model.getChatroomId());
        Log.d("ChatAdapter", "Member IDs: " + model.getMemberIds());

        List<String> memberIds = model.getMemberIds() != null
                ? model.getMemberIds()
                : Collections.emptyList();

        // Biến để lưu trữ UserModel của người dùng 1-1 (nếu có)
        // **QUAN TRỌNG:** Biến này cần là final để có thể được truy cập trong lambda expression của listener
        final UserModel[] otherUserContainer = {null}; // Sử dụng mảng để có thể gán giá trị bên trong callback

        boolean isGroupChat;

        // --- Bắt đầu logic phân loại và cập nhật UI ---

        // Kiểm tra chat nhóm trước
        if (memberIds.size() > 2 || FirebaseUtil.getOtherUserFromChatroom(memberIds).size() > 1) {
            // ===== Xử lý Chat nhóm dựa trên số lượng thành viên =====
            isGroupChat = true;
            String groupName = model.getGroupName();
            holder.usernameText.setText(
                    (groupName != null && !groupName.trim().isEmpty())
                            ? groupName
                            : "Group Chat"
            );
            // Thiết lập ảnh đại diện nhóm (ví dụ: một icon nhóm mặc định)
            holder.profilePic.setImageResource(R.drawable.chat_icon); // Đảm bảo bạn có icon_group_chat

            // Nếu có TextView cho trạng thái online, hãy ẩn nó đi cho nhóm
            // if (holder.onlineStatus != null) holder.onlineStatus.setVisibility(View.GONE);

            // Thiết lập OnClickListener cho chat nhóm
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), ChatActivity.class);
                intent.putExtra("GROUP_ID", model.getChatroomId());
                intent.putExtra("IS_GROUP_CHAT", true); // Luôn là true cho nhóm
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                v.getContext().startActivity(intent);
            });

        } else if (memberIds.size() == 2) {
            // ===== Xử lý Chat 1-1 =====
            isGroupChat = false;
            List<DocumentReference> otherRefs =
                    FirebaseUtil.getOtherUserFromChatroom(memberIds);

            if (otherRefs.size() == 1) {
                otherRefs.get(0).get().addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Log.w("ChatAdapter", "Other user document not found for 1-1 chat.");
                        holder.usernameText.setText("Người dùng không tồn tại");
                        holder.profilePic.setImageResource(R.drawable.ic_avatar_placeholder);
                        // Vẫn đặt listener để tránh crash nếu người dùng click
                        holder.itemView.setOnClickListener(v -> {
                            Toast.makeText(v.getContext(), "Lỗi: Không thể mở chat 1-1. Dữ liệu người dùng không tồn tại.", Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }
                    // Lấy UserModel của người dùng khác
                    UserModel fetchedOtherUser = doc.toObject(UserModel.class);
                    if (fetchedOtherUser == null) {
                        Log.w("ChatAdapter", "Fetched UserModel is null for 1-1 chat.");
                        holder.usernameText.setText("Lỗi tải user");
                        holder.profilePic.setImageResource(R.drawable.ic_avatar_placeholder);
                        // Vẫn đặt listener để tránh crash nếu người dùng click
                        holder.itemView.setOnClickListener(v -> {
                            Toast.makeText(v.getContext(), "Lỗi: Không thể mở chat 1-1. Dữ liệu người dùng rỗng.", Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }
                    otherUserContainer[0] = fetchedOtherUser; // Gán fetchedOtherUser vào container

                    // Cập nhật UI cho 1-1
                    // Avatar - TẢI TỪ URL TRONG USERMODEL (sử dụng otherUserContainer[0])
                    String profileImageUrl = otherUserContainer[0].getProfilePicUrl(); // Đã sửa từ fetchedOtherUser
                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        Glide.with(holder.profilePic.getContext())
                                .load(profileImageUrl)
                                .apply(RequestOptions.circleCropTransform())
                                .placeholder(R.drawable.default_profile_pic)
                                .error(R.drawable.default_profile_pic)
                                .into(holder.profilePic);
                    } else {
                        holder.profilePic.setImageResource(R.drawable.default_profile_pic);
                    }
                    // Tên người dùng
                    holder.usernameText.setText(otherUserContainer[0].getUsername()); // Đã sửa từ otherUser[0]

                    // Hiển thị trạng thái online (nếu có TextView cho nó)
                    // if (holder.onlineStatus != null) holder.onlineStatus.setVisibility(View.VISIBLE);
                    // TODO: Cập nhật trạng thái online từ UserModel nếu có

                    // ĐẶT LISTENER CHO ITEM VIEW TRONG PHẠM VI NÀY ĐỂ TRUY CẬP otherUserContainer[0] ĐÃ ĐƯỢC TẢI
                    holder.itemView.setOnClickListener(v -> {
                        Intent intent = new Intent(v.getContext(), ChatActivity.class);
                        AndroidUtils.passUserModelAsIntent(intent, otherUserContainer[0]); // Sử dụng biến từ container
                        intent.putExtra("IS_GROUP_CHAT", false); // Đảm bảo cờ là false cho 1-1
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        v.getContext().startActivity(intent);
                    });

                }).addOnFailureListener(e -> {
                    Log.e("ChatAdapter", "Error fetching other user info: " + e.getMessage());
                    holder.usernameText.setText("Lỗi tải user");
                    holder.profilePic.setImageResource(R.drawable.ic_avatar_placeholder);
                    // Dù lỗi, vẫn set click listener cơ bản để tránh crash nếu user click
                    holder.itemView.setOnClickListener(v -> {
                        Toast.makeText(v.getContext(), "Lỗi: Không thể mở chat 1-1 do tải dữ liệu lỗi.", Toast.LENGTH_SHORT).show();
                    });
                });
            } else {
                Log.w("ChatAdapter", "Could not find other user for 1-1 chat in memberIds: " + memberIds);
                isGroupChat = false;
                holder.usernameText.setText("Lỗi chat 1-1");
                holder.profilePic.setImageResource(R.drawable.ic_avatar_placeholder);
                holder.itemView.setOnClickListener(v -> {
                    Toast.makeText(v.getContext(), "Lỗi: Không thể mở chat 1-1. Dữ liệu thành viên không hợp lệ.", Toast.LENGTH_SHORT).show();
                });
            }
        } else {
            // Trường hợp không xác định được loại chat (rất hiếm hoặc lỗi logic)
            isGroupChat = false; // Mặc định là không phải nhóm
            holder.usernameText.setText("Unknown Chat");
            holder.profilePic.setImageResource(R.drawable.ic_avatar_placeholder);
            holder.itemView.setOnClickListener(v -> {
                Toast.makeText(v.getContext(), "Lỗi: Không thể mở chat. Loại chat không xác định.", Toast.LENGTH_SHORT).show();
            });
        }

        // --- Logic chung cho cả 1-1 và nhóm ---
        // Tin nhắn cuối cùng
        boolean byMe = model.getLastMessageSenderId() != null
                && model.getLastMessageSenderId()
                .equals(FirebaseUtil.currentUserId());
        String lastMsg = model.getLastMessage() != null
                ? model.getLastMessage()
                : "";
        holder.lastMessageText.setText(
                byMe ? "Bạn: " + lastMsg : lastMsg
        );

        // Thời gian tin nhắn cuối cùng
        holder.lastMessageTime.setText(
                FirebaseUtil.timestampToString(
                        model.getLastMessageTimestamp()
                )
        );

        // **LƯU Ý QUAN TRỌNG:**
        // HÃY ĐẢM BẢO CHỈ CÓ MỘT holder.itemView.setOnClickListener ĐƯỢC THIẾT LẬP
        // trong onBindViewHolder. Logic đã được điều chỉnh để mỗi khối if/else if/else
        // thiết lập listener riêng của nó. Điều này ngăn chặn việc listener bị ghi đè
        // và đảm bảo rằng listener được thiết lập với dữ liệu chính xác.
        // Bạn không cần một holder.itemView.setOnClickListener chung ở cuối nữa.
    }


    @Override
    public ChatroomModelViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.recent_chat_recycler_row,
                        parent, false);
        return new ChatroomModelViewHolder(view);
    }

    static class ChatroomModelViewHolder extends RecyclerView.ViewHolder {
        TextView usernameText;
        TextView lastMessageText;
        TextView lastMessageTime;
        ImageView profilePic;
        // TextView onlineStatus; // Thêm nếu bạn có trạng thái online/offline trong recent_chat_recycler_row

        ChatroomModelViewHolder(View itemView) {
            super(itemView);
            usernameText = itemView.findViewById(R.id.user_name_text);
            lastMessageText = itemView.findViewById(R.id.last_message_text);
            lastMessageTime = itemView.findViewById(R.id.last_message_time_text);
            profilePic = itemView.findViewById(R.id.profile_pic_image_view);
            // onlineStatus = itemView.findViewById(R.id.online_status); // Ánh xạ nếu có
        }
    }
}