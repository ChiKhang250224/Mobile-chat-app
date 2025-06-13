package com.example.appchat;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.appchat.adapter.ChatRecyclerAdapter;
import com.example.appchat.model.ChatMessageModel;
import com.example.appchat.model.ChatroomModel;
import com.example.appchat.model.UserModel;
import com.example.appchat.utils.AndroidUtils;
import com.example.appchat.utils.FirebaseUtil;
import com.example.appchat.utils.ImgBBUploader;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Query;

import android.os.Build; // Thêm dòng này để dùng Build.VERSION.SDK_INT
import androidx.core.content.ContextCompat; // Thêm dòng này
import androidx.core.app.ActivityCompat; // Thêm dòng này

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Request;
import okhttp3.Response;

public class ChatActivity extends AppCompatActivity {

    // Models and related variables
    UserModel otherUser; // Used only for 1-1 chats
    String currentChatroomId; // ID of the chatroom (common for both 1-1 and group)
    ChatroomModel currentChatroomModel; // Model of the chatroom (common for both 1-1 and group)
    ChatRecyclerAdapter adapter;

    boolean isGroupChat; // Flag to distinguish between 1-1 and group chats

    // UI elements
    EditText messageInput;
    ImageButton sendMessageBtn, backBtn, sendImageBtn, addMemberBtn, messageSendBtn;
    TextView otherUsername, onlineStatus;
    ImageView profilePicImageView;
    RecyclerView recyclerView;


    private EditText chatMessageInput;
    private ActivityResultLauncher<Intent> imagePickerLauncher; // Cho cách mới để chọn ảnh

    // Hằng số cho quyền (tùy chọn, nhưng là một thực hành tốt)
    private static final int PERMISSION_REQUEST_CODE = 1001;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Initialize UI views
        messageInput = findViewById(R.id.chat_message_input);
        sendMessageBtn = findViewById(R.id.message_send_btn);
        backBtn = findViewById(R.id.back_btn);
        otherUsername = findViewById(R.id.other_username);
        recyclerView = findViewById(R.id.chat_recycler_view);
        profilePicImageView = findViewById(R.id.profile_pic_image_view);
        onlineStatus = findViewById(R.id.online_status);
        addMemberBtn = findViewById(R.id.add_group_btn);
        sendImageBtn = findViewById(R.id.send_image_btn); // Dòng này

        // Retrieve IS_GROUP_CHAT flag and IDs/Models from Intent
        isGroupChat = getIntent().getBooleanExtra("IS_GROUP_CHAT", false); // Default to false (1-1)

        // --- Initialization logic based on chat type ---
        if (isGroupChat) {
            currentChatroomId = getIntent().getStringExtra("GROUP_ID");
            if (currentChatroomId == null || currentChatroomId.isEmpty()) {
                Toast.makeText(this, "Lỗi: Không tìm thấy ID nhóm.", Toast.LENGTH_LONG).show();
                Log.e("ChatActivity", "Group ID was null or empty for group chat.");
                finish();
                return;
            }
            Log.d("ChatActivity", "Opening Group Chat with ID: " + currentChatroomId);

            // Set a temporary placeholder while loading group info
            otherUsername.setText("Đang tải..."); // Loading...
            profilePicImageView.setImageResource(R.drawable.chat_icon); // Default icon for group

            // Load group info and set up UI header
            loadGroupInfoAndSetupUI(currentChatroomId);

            // Always show the addMemberBtn
            addMemberBtn.setVisibility(View.VISIBLE);
            // Set the icon for adding a member in a group chat
            addMemberBtn.setImageResource(R.drawable.ic_add_member); // Or R.drawable.ic_add_user if you prefer
        } else {
            otherUser = AndroidUtils.getUserModelFromIntent(getIntent());
            if (otherUser == null) {
                Toast.makeText(this, "Lỗi: Không tìm thấy thông tin người dùng.", Toast.LENGTH_LONG).show();
                Log.e("ChatActivity", "Other user was null for 1-1 chat.");
                finish();
                return;
            }else {
                // For 1-1 chat, create the chatroomId based on the IDs of the two users
                currentChatroomId = FirebaseUtil.getChatroomId(FirebaseUtil.currentUserId(), otherUser.getUserId());
                Log.d("ChatActivity", "Opening 1-1 Chat with ID: " + currentChatroomId + " and user: " + otherUser.getUsername());
                setupOneToOneUI(otherUser);

                // Always show the addMemberBtn
                addMemberBtn.setVisibility(View.VISIBLE);
                // Set the icon for creating a new group when in a 1-1 chat
                addMemberBtn.setImageResource(R.drawable.ic_add_member);
            }
        }

        // --- Common logic for both chat types ---

        // Back button
        backBtn.setOnClickListener(v -> onBackPressed());

        // Listen for online/offline status only for 1-1 chats
        if (!isGroupChat && otherUser != null) {
            listenForOnlineStatus();
        } else {
            onlineStatus.setVisibility(View.GONE); // Hide online status for groups
        }

        // Send message button
        sendMessageBtn.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessage(message, null); // Gửi tin nhắn văn bản
                messageInput.setText(""); // Xóa input sau khi gửi
            } else {
                AndroidUtils.showToast(this, "Vui lòng nhập tin nhắn."); // Đã bỏ phần "hoặc chọn ảnh"
            }
        });

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        uploadImageToImgBB(imageUri); // Tải lên ảnh đã chọn
                    } else {
                        AndroidUtils.showToast(this, "Không có ảnh nào được chọn.");
                    }
                }
        );
        sendImageBtn.setOnClickListener(v -> {
            checkAndRequestPermissions(); // Kiểm tra quyền trước khi mở trình chọn ảnh
        });

        // Set Listener for the "Add Member" / "Create Group" button (addMemberBtn)
        addMemberBtn.setOnClickListener(v -> {
            if (isGroupChat) {
                // If in a group chat, the function is "Add Member"
                Intent intent = new Intent(ChatActivity.this, AddMemberActivity.class);
                intent.putExtra("chatroomId", currentChatroomId); // Pass the ID of the current group
                startActivity(intent);
            } else {
                // If in a 1-1 chat, the function is "Create New Group"
                showCreateGroupDialog(); // This function will handle creating the new group
            }
        });

        // Create or get chatroom model (using currentChatroomId)
        getOrCreateChatroomModel();
        setupChatRecyclerView();
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, PERMISSION_REQUEST_CODE);
            } else {
                openImagePicker();
            }
        } else { // Các phiên bản Android cũ hơn
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            } else {
                openImagePicker();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                AndroidUtils.showToast(this, "Quyền truy cập ảnh đã bị từ chối.");
            }
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }
    // Trong ChatActivity.java
    private void uploadImageToImgBB(Uri imageUri) {
        if (imageUri == null) {
            AndroidUtils.showToast(this, "Không có ảnh để tải lên.");
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang tải ảnh lên...");
        progressDialog.show();

        // Sử dụng ImgBBUploader class để xử lý logic tải lên
        ImgBBUploader.uploadImage(this, imageUri, new ImgBBUploader.UploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                progressDialog.dismiss();
                runOnUiThread(() -> { // Đảm bảo chạy trên UI thread
                    AndroidUtils.showToast(ChatActivity.this, "Tải ảnh lên thành công!");
                    Log.d(TAG, "ImgBB Image URL: " + imageUrl);
                    sendMessage(null, imageUrl); // Gửi tin nhắn sau khi tải lên thành công
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                progressDialog.dismiss();
                runOnUiThread(() -> { // Đảm bảo chạy trên UI thread
                    AndroidUtils.showToast(ChatActivity.this, "Tải ảnh thất bại: " + errorMessage);
                    Log.e(TAG, "Lỗi tải ảnh lên ImgBB: " + errorMessage);
                });
            }
        });
    }

    private void loadGroupInfoAndSetupUI(String groupId) {
        FirebaseUtil.getGroupChatroomReference(groupId).addSnapshotListener((documentSnapshot, e) -> {
            if (e != null) {
                Log.e("ChatActivity", "Error listening for group info: " + e.getMessage());
                return;
            }
            if (documentSnapshot != null && documentSnapshot.exists()) {
                currentChatroomModel = documentSnapshot.toObject(ChatroomModel.class);
                if (currentChatroomModel != null) {
                    String groupName = currentChatroomModel.getGroupName();
                    Log.d("ChatActivity", "Fetched groupName from Firestore: " + groupName); // THÊM DÒNG NÀY
                    if (groupName != null && !groupName.isEmpty()) {
                        otherUsername.setText(groupName); // Cập nhật tên nhóm TỪ FIRESTORE
                    } else {
                        otherUsername.setText("Group Chat"); // Fallback nếu tên nhóm rỗng
                        Log.d("ChatActivity", "groupName is null or empty, setting to 'Group Chat'"); // THÊM DÒNG NÀY
                    }
                    profilePicImageView.setImageResource(R.drawable.chat_icon);
                    onlineStatus.setVisibility(View.GONE);
                } else {
                    Log.d("ChatActivity", "currentChatroomModel is null after toObject()"); // THÊM DÒNG NÀY
                }
            } else {
                Log.d("ChatActivity", "Group document not found or does not exist for ID: " + groupId); // THÊM DÒNG NÀY
                otherUsername.setText("Nhóm không tồn tại");
                profilePicImageView.setImageResource(R.drawable.ic_avatar_placeholder);
            }
        });
    }

    private void setupOneToOneUI(UserModel userModel) {
        otherUsername.setText(userModel.getUsername());

        // Tải ảnh đại diện trực tiếp từ UserModel (Firestore)
        String profileImageUrl = userModel.getProfilePicUrl();
        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
            Glide.with(this)
                    .load(profileImageUrl)
                    .apply(RequestOptions.circleCropTransform())
                    .placeholder(R.drawable.default_profile_pic) // Ảnh tạm thời khi đang tải
                    .error(R.drawable.default_profile_pic) // Ảnh khi tải lỗi hoặc không tìm thấy
                    .into(profilePicImageView); // Sử dụng profilePicImageView
        } else {
            // Nếu không có URL ảnh đại diện, hiển thị ảnh mặc định
            profilePicImageView.setImageResource(R.drawable.default_profile_pic);
        }
        // onlineStatus.setVisibility(View.VISIBLE); // Hiển thị trạng thái online/offline
    }

    private void listenForOnlineStatus() {
        FirebaseUtil.allUserCollectionReference()
                .document(otherUser.getUserId())
                .addSnapshotListener((snapshot, error) -> {
                    if (snapshot != null && snapshot.exists()) {
                        boolean isOnline = Boolean.TRUE.equals(snapshot.getBoolean("online"));
                        if (isOnline) {
                            onlineStatus.setText("Online");
                            onlineStatus.setTextColor(getResources().getColor(R.color.light_green));
                            onlineStatus.setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_dot_green, 0, 0, 0);
                            onlineStatus.setVisibility(View.VISIBLE);
                        } else {
                            Timestamp lastSeen = snapshot.getTimestamp("lastSeen");
                            if (lastSeen != null) {
                                onlineStatus.setText("Last seen: " + FirebaseUtil.timestampToString(lastSeen));
                                onlineStatus.setTextColor(getResources().getColor(R.color.gray));
                                onlineStatus.setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_clock_gray, 0, 0, 0);
                                onlineStatus.setVisibility(View.VISIBLE);
                            } else {
                                onlineStatus.setVisibility(View.GONE);
                            }
                        }
                    }
                });
    }

    //endregion

    //region --- Chatroom Model and RecyclerView Handling ---

    private void getOrCreateChatroomModel() {
        DocumentReference chatroomRef;
        if (isGroupChat) {
            chatroomRef = FirebaseUtil.getGroupChatroomReference(currentChatroomId);
        } else {
            chatroomRef = FirebaseUtil.getChatroomReference(currentChatroomId);
        }

        chatroomRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                currentChatroomModel = task.getResult().toObject(ChatroomModel.class);
                if (currentChatroomModel == null) {
                    // Chatroom does not exist, create new
                    Log.d("ChatActivity", "Creating new chatroom/group model: " + currentChatroomId);
                    currentChatroomModel = new ChatroomModel();
                    currentChatroomModel.setChatroomId(currentChatroomId);
                    currentChatroomModel.setLastMessage("");
                    currentChatroomModel.setLastMessageTimestamp(null);
                    // Set memberIds and groupName based on chat type
                    if (isGroupChat) {
                        currentChatroomModel.setMemberIds(new ArrayList<>(Arrays.asList(FirebaseUtil.currentUserId())));
                        currentChatroomModel.setGroupName("New Group Name"); // Placeholder
                        currentChatroomModel.setIsGroupChat(true); // Set flag for group
                    } else {
                        currentChatroomModel.setMemberIds(Arrays.asList(
                                FirebaseUtil.currentUserId(),
                                otherUser.getUserId()
                        ));
                        currentChatroomModel.setGroupName(null); // No group name for 1-1 chat
                        currentChatroomModel.setIsGroupChat(false); // Set flag for 1-1
                    }
                    chatroomRef.set(currentChatroomModel)
                            .addOnSuccessListener(aVoid -> Log.d("ChatActivity", "Chatroom/Group model created successfully."))
                            .addOnFailureListener(e -> Log.e("ChatActivity", "Error creating chatroom/group model: " + e.getMessage()));
                } else {
                    Log.d("ChatActivity", "Chatroom/Group model already exists: " + currentChatroomId);
                    // If it's a group, and you want to update the group name here, it's handled by loadGroupInfoAndSetupUI
                    // No need to update otherUsername.setText here as loadGroupInfoAndSetupUI handles real-time updates.
                }
            } else {
                Log.e("ChatActivity", "Error getting chatroom/group model: " + task.getException().getMessage());
                Toast.makeText(this, "Lỗi khi tải dữ liệu chat.", Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void setupChatRecyclerView() {
        Query query;
        if (isGroupChat) {
            query = FirebaseUtil.getGroupChatroomMessageReference(currentChatroomId)
                    .orderBy("timestamp", Query.Direction.DESCENDING);
        } else {
            query = FirebaseUtil.getChatroomMessageReference(currentChatroomId)
                    .orderBy("timestamp", Query.Direction.DESCENDING);
        }

        FirestoreRecyclerOptions<ChatMessageModel> options =
                new FirestoreRecyclerOptions.Builder<ChatMessageModel>()
                        .setQuery(query, ChatMessageModel.class).build();

        adapter = new ChatRecyclerAdapter(options, getApplicationContext());
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setReverseLayout(true); // Display latest messages at the bottom
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);
        adapter.startListening();
    }


    private void sendMessage(String message, String imageUrl) {
        if (currentChatroomModel == null) {
            Log.e(TAG, "ChatroomModel is null, cannot send message.");
            Toast.makeText(this, "Lỗi: Không thể gửi tin nhắn. Chatroom chưa được tải.", Toast.LENGTH_SHORT).show();
            return;
        }
        // Check if both message and image URL are empty/null, then don't send
        if ((message == null || message.isEmpty()) && (imageUrl == null || imageUrl.isEmpty())) {
            Log.d(TAG, "Message and imageUrl are both empty/null. Not sending.");
            return;
        }

        // Select the correct CollectionReference
        DocumentReference chatroomDocRef;
        CollectionReference messagesCollectionRef;

        if (isGroupChat) {
            chatroomDocRef = FirebaseUtil.getGroupChatroomReference(currentChatroomId);
            messagesCollectionRef = FirebaseUtil.getGroupChatroomMessageReference(currentChatroomId);
        } else {
            chatroomDocRef = FirebaseUtil.getChatroomReference(currentChatroomId);
            messagesCollectionRef = FirebaseUtil.getChatroomMessageReference(currentChatroomId);
        }

        // Create HashMap to update chatroom model
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessageTimestamp", FieldValue.serverTimestamp()); // Update timestamp on the server
        updates.put("lastMessageSenderId", FirebaseUtil.currentUserId());
        updates.put("lastMessage", message != null ? message : "[Hình ảnh]"); // Update last message to "[Image]" if only an image

        chatroomDocRef.update(updates) // Use update instead of set to avoid overwriting the entire model
                .addOnFailureListener(e -> Log.e(TAG, "Lỗi cập nhật chatroom model: " + e.getMessage(), e));

        // Create and send message
        ChatMessageModel chatMessageModel = new ChatMessageModel(
                message, FirebaseUtil.currentUserId(), Timestamp.now(), imageUrl); // Ensure constructor has imageUrl

        messagesCollectionRef.add(chatMessageModel)
                .addOnSuccessListener(docRef -> {
                    messageInput.setText(""); // Clear input after sending
                    Log.d(TAG, "Gửi thành công: " + (isGroupChat ? "nhóm" : "1-1")); // Successfully sent: group/1-1
                    // Only send notifications for 1-1 chats (FCM for groups is more complex)
                    if (!isGroupChat) {
                        sendNotification(message != null ? message : "[Hình ảnh]"); // Send notification content
                    }
                    // Di chuyển RecyclerView đến cuối để hiển thị tin nhắn mới
                    // CHỈ CUỘN KHI ADAPTER ĐÃ CÓ DỮ LIỆU
                    if (adapter != null && adapter.getItemCount() > 0) {
                        // Delay cuộn một chút để FirestoreRecyclerAdapter kịp cập nhật
                        recyclerView.postDelayed(() -> {
                            recyclerView.smoothScrollToPosition(adapter.getItemCount() - 1);
                        }, 300); // Khoảng 300ms
                    } else {
                        Log.d(TAG, "Adapter is null or empty, cannot scroll to position.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi gửi tin nhắn: " + e.getMessage(), e); // Error sending message
                    Toast.makeText(ChatActivity.this, "Không gửi được tin nhắn", Toast.LENGTH_SHORT).show(); // Could not send message
                });
    }

    private void sendNotification(String message) {
        // Only send notifications for 1-1 chats in this case (FCM for groups is more complex)
        if (isGroupChat) {
            Log.d("NOTIFY_FCM", "Skipping FCM notification for group chat.");
            return;
        }

        FirebaseUtil.currentUserDetails().get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                UserModel currentUser = task.getResult().toObject(UserModel.class);
                if (currentUser == null || otherUser == null || otherUser.getFcmToken() == null) {
                    Log.e("NOTIFY_FCM", "Missing current user, other user, or FCM token for notification.");
                    return;
                }
                try {
                    JSONObject jsonObject = new JSONObject();
                    JSONObject notificationObj = new JSONObject();
                    notificationObj.put("title", currentUser.getUsername());
                    notificationObj.put("body", message);

                    JSONObject dataObj = new JSONObject();
                    dataObj.put("userId", currentUser.getUserId());
                    dataObj.put("chatroomId", currentChatroomId); // Pass chatroomId to open the correct chat
                    dataObj.put("isGroupChat", isGroupChat); // Pass isGroupChat flag
                    AndroidUtils.passUserModelAsJsonObject(dataObj, otherUser); // Pass otherUser info (for opening chat)

                    jsonObject.put("notification", notificationObj);
                    jsonObject.put("data", dataObj);
                    jsonObject.put("to", otherUser.getFcmToken());

                    callApi(jsonObject);
                } catch (Exception e) {
                    Log.e("NOTIFY_FCM", "Error creating JSON for notification: " + e.getMessage(), e);
                }
            } else {
                Log.e("NOTIFY_FCM", "Failed to get current user details: " + task.getException().getMessage());
            }
        });
    }

    private void callApi(JSONObject jsonObject) {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        OkHttpClient client = new OkHttpClient();
        String url = "https://fcm.googleapis.com/fcm/send";
        RequestBody body = RequestBody.create(jsonObject.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("Authorization", "Bearer YOUR_SERVER_KEY") // <<< REPLACE WITH YOUR SERVER KEY >>>
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("NOTIFY_API", "Gửi thông báo thất bại: " + e.getMessage(), e); // Notification sending failed
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d("NOTIFY_API", "Gửi thông báo thành công: " + response.body().string()); // Notification sent successfully
                } else {
                    Log.e("NOTIFY_API", "Gửi thông báo thất bại: " + response.code() + " " + response.message() + " " + response.body().string()); // Notification sending failed
                }
            }
        });
    }

    //endregion

    //region --- New Group Creation and Member Addition ---
    private void showCreateGroupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Tạo nhóm mới"); // Create New Group

        final EditText input = new EditText(this);
        input.setHint("Nhập tên nhóm"); // Enter group name
        builder.setView(input);

        builder.setPositiveButton("Tạo", (dialog, which) -> { // Create
            String groupName = input.getText().toString().trim();
            if (!groupName.isEmpty()) {
                createNewGroup(groupName);
            } else {
                Toast.makeText(this, "Tên nhóm không được để trống", Toast.LENGTH_SHORT).show(); // Group name cannot be empty
            }
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss()); // Cancel
        builder.show();
    }

    private void createNewGroup(String groupName) {
        Log.d("ChatActivity", "Creating new group with name: " + groupName);
        // Create an initial list containing only the current user
        List<String> members = new ArrayList<>();
        members.add(FirebaseUtil.currentUserId());

        // If you are in a 1-1 chat and want to create a group with that person, add them
        // `isGroupChat` here will still be `false` because we are creating a group FROM a 1-1 chat.
        if (!isGroupChat && otherUser != null) {
            members.add(otherUser.getUserId());
        }

        Map<String, Object> groupData = new HashMap<>();
        groupData.put("groupName", groupName);
        groupData.put("memberIds", members);
        groupData.put("createdAt", FieldValue.serverTimestamp()); // Use FieldValue.serverTimestamp()
        groupData.put("lastMessage", "Nhóm \"" + groupName + "\" đã được tạo."); // Group "[groupName]" has been created.
        groupData.put("lastMessageSenderId", FirebaseUtil.currentUserId());
        groupData.put("lastMessageTimestamp", FieldValue.serverTimestamp()); // Use FieldValue.serverTimestamp()
        groupData.put("isGroupChat", true); // Always set isGroupChat = true when creating a new group


        FirebaseUtil.getChatroomsCollection() // Use getChatroomsCollection() for both 1-1 and groups if you store them together
                .add(groupData)
                .addOnSuccessListener(documentReference -> {
                    String newChatroomId = documentReference.getId();
                    Log.d("ChatActivity", "New group created with ID: " + newChatroomId);

                    // Send the first message announcing the group creation
                    FirebaseUtil.getGroupChatroomMessageReference(newChatroomId) // Use a separate function for group messages
                            .add(new ChatMessageModel(
                                    "Nhóm \"" + groupName + "\" đã được tạo.", // Group "[groupName]" has been created.
                                    FirebaseUtil.currentUserId(),
                                    Timestamp.now() // Use Timestamp.now() for immediate message
                            ));

                    // After creating the group, you can decide:
                    // 1. Navigate to AddMemberActivity to continue adding members.
                    Intent intent = new Intent(this, AddMemberActivity.class);
                    intent.putExtra("chatroomId", newChatroomId); // Pass the ID of the new group
                    startActivity(intent);
                    finish();

                })
                .addOnFailureListener(e -> {
                    Log.e("ChatActivity", "Failed to create new group: " + e.getMessage());
                    Toast.makeText(this, "Tạo nhóm thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show(); // Group creation failed
                });
    }

    private void openSelectGroupActivity() {
        // This function doesn't seem to be used directly from ChatActivity in the current logic.
        // If it's meant to navigate to another group from a list of groups, you would need to pass that group's ID.
        Intent intent = new Intent(this, SelectGroupActivity.class); // Assuming SelectGroupActivity has a list of groups to choose from
        startActivity(intent);
    }

    //endregion

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) {
            adapter.startListening();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update online status
        if (FirebaseUtil.currentUserId() != null) {
            FirebaseUtil.currentUserDetails().update("online", true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Update offline status and lastSeen
        if (FirebaseUtil.currentUserId() != null) {
            FirebaseUtil.currentUserDetails().update("online", false, "lastSeen", Timestamp.now());
        }
    }
}