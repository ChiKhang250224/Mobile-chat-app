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
        final UserModel[] otherUser = {null};
        // Biến để xác định loại chat
        boolean isGroupChat; // Mặc định là không phải nhóm

        // --- Bắt đầu logic phân loại và cập nhật UI ---

        // Ưu tiên kiểm tra chat nhóm trước nếu bạn có trường `isGroupChat` trong ChatroomModel
        // if (model.getIsGroupChat()) { // Nếu bạn đã thêm trường isGroupChat vào model và Firestore
        //     isGroupChat = true;
        //     // Hiển thị tên nhóm và ảnh nhóm
        //     String groupName = model.getGroupName();
        //     holder.usernameText.setText((groupName != null && !groupName.trim().isEmpty()) ? groupName : "Group Chat");
        //     holder.profilePic.setImageResource(R.drawable.chat_icon); // Hoặc tải ảnh nhóm
        //     // Ẩn trạng thái online nếu có
        //     // if (holder.onlineStatus != null) holder.onlineStatus.setVisibility(View.GONE);
        // } else
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

        } else if (memberIds.size() == 2) {
            // ===== Xử lý Chat 1-1 =====
            isGroupChat = false; // Đảm bảo cờ là false
            List<DocumentReference> otherRefs =
                    FirebaseUtil.getOtherUserFromChatroom(memberIds);

            // Fetch user info in a listener
            // (You might want to refactor this to avoid nested listeners if performance is critical)
            if (otherRefs.size() == 1) {
                otherRefs.get(0).get().addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    otherUser[0] = doc.toObject(UserModel.class);
                    if (otherUser[0] == null) return;

                    // Cập nhật UI cho 1-1
                    // Avatar
                    FirebaseUtil.getOtherProfilePicStorageRef(otherUser[0].getUserId())
                            .getDownloadUrl()
                            .addOnSuccessListener(uri ->
                                    AndroidUtils.setProfilePic(
                                            holder.profilePic.getContext(),
                                            uri,
                                            holder.profilePic
                                    )
                            )
                            .addOnFailureListener(e ->
                                    holder.profilePic.setImageResource(
                                            R.drawable.ic_avatar_placeholder
                                    )
                            );
                    // Tên người dùng
                    holder.usernameText.setText(otherUser[0].getUsername());
                    // Hiển thị trạng thái online (nếu có TextView cho nó)
                    // if (holder.onlineStatus != null) holder.onlineStatus.setVisibility(View.VISIBLE);
                    // TODO: Cập nhật trạng thái online từ UserModel nếu có
                }).addOnFailureListener(e -> {
                    Log.e("ChatAdapter", "Error fetching other user info: " + e.getMessage());
                    holder.usernameText.setText("Lỗi tải user");
                    holder.profilePic.setImageResource(R.drawable.ic_avatar_placeholder);
                });
            }
        } else {
            isGroupChat = false;
            // Fallback: trường hợp không xác định được loại chat (rất hiếm)
            holder.usernameText.setText("Unknown Chat");
            holder.profilePic.setImageResource(R.drawable.ic_avatar_placeholder);
            // if (holder.onlineStatus != null) holder.onlineStatus.setVisibility(View.GONE);
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

        // Thiết lập OnClickListener cho itemView (chung cho cả 1-1 và nhóm)
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ChatActivity.class);

            if (isGroupChat) {
                // Đối với chat nhóm, truyền GROUP_ID
                intent.putExtra("GROUP_ID", model.getChatroomId());
            } else {
                // Đối với chat 1-1, truyền UserModel của người kia
                if (otherUser[0] != null) {
                    AndroidUtils.passUserModelAsIntent(intent, otherUser[0]);
                } else {
                    // Xử lý trường hợp otherUser[0] chưa được tải kịp hoặc bị lỗi
                    Log.e("ChatAdapter", "Error: otherUser is null for 1-1 chat. Cannot open ChatActivity.");
                    Toast.makeText(v.getContext(), "Lỗi: Không thể mở chat 1-1. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                    return; // Ngăn không cho mở Intent nếu thiếu dữ liệu
                }
            }
            // Thêm cờ để ChatActivity biết đây là nhóm hay 1-1
            intent.putExtra("IS_GROUP_CHAT", isGroupChat);

            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            v.getContext().startActivity(intent);
        });
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