package com.example.appchat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.appchat.adapter.ChatRecyclerAdapter;
import com.example.appchat.model.ChatMessageModel;
import com.example.appchat.model.ChatroomModel;
import com.example.appchat.model.UserModel;
import com.example.appchat.utils.AndroidUtils;
import com.example.appchat.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;


import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Request;
import okhttp3.Response;

public class ChatActivity extends AppCompatActivity {

    UserModel otherUser;
    String currentChatroomId;
    ChatroomModel currentChatroomModel;
    ChatRecyclerAdapter adapter;

    boolean isGroupChat;

    EditText messageInput;
    ImageButton sendMessageBtn, backBtn, sendImageBtn, addMemberBtn;
    TextView otherUsername, onlineStatus;
    ImageView profilePicImageView;
    RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        messageInput = findViewById(R.id.chat_message_input);
        sendMessageBtn = findViewById(R.id.message_send_btn);
        backBtn = findViewById(R.id.back_btn);
        otherUsername = findViewById(R.id.other_username);
        recyclerView = findViewById(R.id.chat_recycler_view);
        profilePicImageView = findViewById(R.id.profile_pic_image_view);
        onlineStatus = findViewById(R.id.online_status);
        addMemberBtn = findViewById(R.id.add_group_btn);


        isGroupChat = getIntent().getBooleanExtra("IS_GROUP_CHAT", false);

        if (isGroupChat) {
            currentChatroomId = getIntent().getStringExtra("GROUP_ID");
            if (currentChatroomId == null || currentChatroomId.isEmpty()) {
                Toast.makeText(this, "Lỗi: Không tìm thấy ID nhóm.", Toast.LENGTH_LONG).show();
                Log.e("ChatActivity", "Group ID was null or empty for group chat.");
                finish();
                return;
            }
            Log.d("ChatActivity", "Opening Group Chat with ID: " + currentChatroomId);
            loadGroupInfoAndSetupUI(currentChatroomId);
            addMemberBtn.setVisibility(View.VISIBLE);
            addMemberBtn.setImageResource(R.drawable.ic_add_member);
        } else {
            otherUser = AndroidUtils.getUserModelFromIntent(getIntent());
            if (otherUser == null) {
                Toast.makeText(this, "Lỗi: Không tìm thấy thông tin người dùng.", Toast.LENGTH_LONG).show();
                Log.e("ChatActivity", "Other user was null for 1-1 chat.");
                finish();
                return;
            }
            currentChatroomId = FirebaseUtil.getChatroomId(FirebaseUtil.currentUserId(), otherUser.getUserId());
            Log.d("ChatActivity", "Opening 1-1 Chat with ID: " + currentChatroomId + " and user: " + otherUser.getUsername());
            setupOneToOneUI(otherUser);
            addMemberBtn.setVisibility(View.VISIBLE);
            addMemberBtn.setImageResource(R.drawable.ic_add_member);
        }

        backBtn.setOnClickListener(v -> onBackPressed());

        if (!isGroupChat && otherUser != null) {
            listenForOnlineStatus();
        } else {
            onlineStatus.setVisibility(View.GONE);
        }

        sendMessageBtn.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessage(message, null);
            }
        });

        sendImageBtn = findViewById(R.id.send_image_btn);
        sendImageBtn.setOnClickListener(v -> selectImage());

        addMemberBtn.setOnClickListener(v -> {
            if (isGroupChat) {
                Intent intent = new Intent(ChatActivity.this, AddMemberActivity.class);
                intent.putExtra("chatroomId", currentChatroomId);
                startActivity(intent);
            } else {
                showCreateGroupDialog();
            }
        });
        getOrCreateChatroomModel();
        setupChatRecyclerView();
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
                    if (groupName != null && !groupName.isEmpty()) {
                        otherUsername.setText(groupName);
                    } else {
                        otherUsername.setText("Group Chat");
                    }
                    // TODO: Load group image if available and display it on profilePicImageView
                    profilePicImageView.setImageResource(R.drawable.chat_icon); // Default icon for group
                    onlineStatus.setVisibility(View.GONE); // Hide online status for groups
                }
            } else {
                Log.d("ChatActivity", "Group document not found for ID: " + groupId);
                otherUsername.setText("Nhóm không tồn tại"); // Group does not exist
                profilePicImageView.setImageResource(R.drawable.ic_avatar_placeholder);
            }
        });
    }

    private void setupOneToOneUI(UserModel user) {
        otherUsername.setText(user.getUsername());
        // Load the other user's profile picture
        FirebaseUtil.getOtherProfilePicStorageRef(user.getUserId()).getDownloadUrl()
                .addOnCompleteListener(t -> {
                    if (t.isSuccessful()) {
                        Uri uri = t.getResult();
                        AndroidUtils.setProfilePic(this, uri, profilePicImageView);
                    } else {
                        profilePicImageView.setImageResource(R.drawable.ic_avatar_placeholder);
                    }
                });
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
                    Log.d("ChatActivity", "Creating new chatroom/group model: " + currentChatroomId);
                    currentChatroomModel = new ChatroomModel();
                    currentChatroomModel.setChatroomId(currentChatroomId);
                    currentChatroomModel.setLastMessage("");
                    currentChatroomModel.setLastMessageTimestamp(null);
                    if (isGroupChat) {
                        currentChatroomModel.setMemberIds(new ArrayList<>(Arrays.asList(FirebaseUtil.currentUserId())));
                        currentChatroomModel.setGroupName("New Group Name");
                        currentChatroomModel.setIsGroupChat(true);
                    } else {
                        currentChatroomModel.setMemberIds(Arrays.asList(
                                FirebaseUtil.currentUserId(),
                                otherUser.getUserId()
                        ));
                        currentChatroomModel.setGroupName(null);
                        currentChatroomModel.setIsGroupChat(false);
                    }
                    chatroomRef.set(currentChatroomModel)
                            .addOnSuccessListener(aVoid -> Log.d("ChatActivity", "Chatroom/Group model created successfully."))
                            .addOnFailureListener(e -> Log.e("ChatActivity", "Error creating chatroom/group model: " + e.getMessage()));
                } else {
                    Log.d("ChatActivity", "Chatroom/Group model already exists: " + currentChatroomId);
                    if (isGroupChat && currentChatroomModel.getGroupName() != null && !currentChatroomModel.getGroupName().isEmpty()) {
                        otherUsername.setText(currentChatroomModel.getGroupName());
                    }
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

        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                recyclerView.smoothScrollToPosition(0);
            }
        });
    }

    private void sendMessage(String message, String imageUrl) {
        if (currentChatroomModel == null) {
            Log.e("ChatActivity", "ChatroomModel is null, cannot send message.");
            Toast.makeText(this, "Lỗi: Không thể gửi tin nhắn. Chatroom chưa được tải.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (message == null || (message.isEmpty() && imageUrl == null)) {
            Log.d("ChatActivity", "Message and imageUrl are both empty/null. Not sending.");
            return;
        }


        DocumentReference chatroomDocRef;
        CollectionReference messagesCollectionRef;

        if (isGroupChat) {
            chatroomDocRef = FirebaseUtil.getGroupChatroomReference(currentChatroomId);
            messagesCollectionRef = FirebaseUtil.getGroupChatroomMessageReference(currentChatroomId);
        } else {
            chatroomDocRef = FirebaseUtil.getChatroomReference(currentChatroomId);
            messagesCollectionRef = FirebaseUtil.getChatroomMessageReference(currentChatroomId);
        }


        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessageTimestamp", FieldValue.serverTimestamp());
        updates.put("lastMessageSenderId", FirebaseUtil.currentUserId());
        updates.put("lastMessage", message != null ? message : "[Hình ảnh]");

        chatroomDocRef.update(updates)
                .addOnFailureListener(e -> Log.e("FIRESTORE", "Lỗi cập nhật chatroom model: " + e.getMessage(), e));


        ChatMessageModel chatMessageModel = new ChatMessageModel(
                message, FirebaseUtil.currentUserId(), Timestamp.now(), imageUrl);

        messagesCollectionRef.add(chatMessageModel)
                .addOnSuccessListener(docRef -> {
                    messageInput.setText("");
                    Log.d("SEND_MSG", "Gửi thành công: " + (isGroupChat ? "nhóm" : "1-1")); // Successfully sent: group/1-1
                    // Only send notifications for 1-1 chats (FCM for groups is more complex)
                    if (!isGroupChat) {
                        sendNotification(message != null ? message : "[Hình ảnh]"); // Send notification content
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("SEND_MSG", "Lỗi khi gửi tin nhắn: " + e.getMessage(), e); // Error sending message
                    Toast.makeText(ChatActivity.this, "Không gửi được tin nhắn", Toast.LENGTH_SHORT).show(); // Could not send message
                });
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, 101);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            uploadImage(imageUri);
        } else {
            Log.d("ChatActivity", "Image selection cancelled or failed. RequestCode: " + requestCode + ", ResultCode: " + resultCode);
        }
    }

    private void uploadImage(Uri uri) {
        if (currentChatroomId == null) {
            Toast.makeText(this, "Lỗi: Không thể tải ảnh. Chatroom chưa được tải.", Toast.LENGTH_SHORT).show(); // Error: Cannot upload image. Chatroom not loaded.
            return;
        }

        // Define the storage path for the image in Firebase Storage
        String imagePath;
        if (isGroupChat) {
            imagePath = "group_chat_images/" + currentChatroomId + "/" + UUID.randomUUID().toString();
        } else {
            imagePath = "one_to_one_chat_images/" + currentChatroomId + "/" + UUID.randomUUID().toString();
        }

        FirebaseUtil.getStorageReference(imagePath).putFile(uri)
                .addOnSuccessListener(taskSnapshot -> {
                    taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        sendMessage(null, downloadUri.toString()); // Send image URL, message is null
                    }).addOnFailureListener(e -> Log.e("UPLOAD_IMAGE", "Failed to get download URL: " + e.getMessage()));
                })
                .addOnFailureListener(e -> {
                    Log.e("UPLOAD_IMAGE", "Image upload failed: " + e.getMessage());
                    Toast.makeText(ChatActivity.this, "Tải ảnh thất bại", Toast.LENGTH_SHORT).show(); // Image upload failed
                });
    }

    //endregion

    //region --- FCM Notification Handling ---

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
                    finish(); // Finish the current ChatActivity

                    // OR
                    // 2. Navigate directly to the ChatActivity of the newly created group.
                    /*
                    Intent intent = new Intent(this, ChatActivity.class);
                    intent.putExtra("GROUP_ID", newChatroomId);
                    intent.putExtra("IS_GROUP_CHAT", true);
                    startActivity(intent);
                    finish();
                    */
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