package com.example.appchat;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appchat.adapter.MessageAdapter;
import com.example.appchat.model.ChatMessageModel;
import com.example.appchat.utils.ImgBBUploader;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GroupChatActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText edtMessage;
    private FirebaseFirestore db;
    private String groupId;
    private String currentUserId;
    private List<ChatMessageModel> messageList = new ArrayList<>();
    private MessageAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat);


        groupId = getIntent().getStringExtra("GROUP_ID");
        if (groupId == null || groupId.isEmpty()) {
            Toast.makeText(this, "Lỗi: Không tìm thấy thông tin nhóm.", Toast.LENGTH_LONG).show();
            Log.e("GroupChatActivity", "Group ID was not passed in the intent. Finishing activity.");
            finish();

            return;
        }
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();

        recyclerView = findViewById(R.id.recyclerViewMessages);
        edtMessage = findViewById(R.id.edtMessage);

        adapter = new MessageAdapter(messageList, currentUserId);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        findViewById(R.id.btnSend).setOnClickListener(v -> sendMessage(edtMessage.getText().toString(), null));
        findViewById(R.id.btnSendImage).setOnClickListener(v -> selectImage());

        // Bắt đầu lắng nghe tin nhắn từ Firestore
        listenForMessages();
    }

    private void sendMessage(String message, String imageUrl) {
        if (message.isEmpty() && imageUrl == null) return;

        Map<String, Object> msg = new HashMap<>();
        msg.put("senderId", currentUserId);
        msg.put("message", message);
        msg.put("imageUrl", imageUrl);
        msg.put("timestamp", FieldValue.serverTimestamp());

        db.collection("groups").document(groupId)
                .collection("messages").add(msg);
        edtMessage.setText("");
    }

    private void listenForMessages() {
        db.collection("groups").document(groupId)
                .collection("messages").orderBy("timestamp")
                .addSnapshotListener((snapshots, e) -> {
                    if (snapshots == null || snapshots.isEmpty()) return;

                    messageList.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        ChatMessageModel m = doc.toObject(ChatMessageModel.class);
                        messageList.add(m);
                    }
                    adapter.notifyDataSetChanged();
                    recyclerView.scrollToPosition(messageList.size() - 1);
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
        if (requestCode == 101 && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            uploadImage(imageUri);
        }
    }

    private void uploadImage(Uri uri) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang tải ảnh lên..."); // "Uploading image..."
        progressDialog.show();

        ImgBBUploader.uploadImage(this, uri, new ImgBBUploader.UploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                progressDialog.dismiss();
                // Tin nhắn ảnh đã được tải lên ImgBB thành công, bây giờ gửi URL vào chat
                sendMessage(null, imageUrl); // Gửi tin nhắn chứa URL ảnh ImgBB
                Toast.makeText(GroupChatActivity.this, "Tải ảnh thành công!", Toast.LENGTH_SHORT).show(); // "Image uploaded successfully!"
            }

            @Override
            public void onFailure(String errorMessage) {
                progressDialog.dismiss();
                // Xử lý lỗi khi tải ảnh lên ImgBB
                Toast.makeText(GroupChatActivity.this, "Tải ảnh thất bại: " + errorMessage, Toast.LENGTH_LONG).show(); // "Image upload failed:"
                Log.e("GroupChatActivity", "Lỗi tải ảnh lên ImgBB: " + errorMessage); // "ImgBB upload error:"
            }
        });
    }
}
