package com.example.appchat;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appchat.adapter.FriendAdapter;
import com.example.appchat.model.UserModel;
import com.example.appchat.utils.FirebaseUtil;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AddMemberActivity extends AppCompatActivity {

    RecyclerView friendsRecyclerView;
    Button btnAddSelected;
    String chatroomId;

    List<UserModel> friendList = new ArrayList<>();
    Set<String> selectedUserIds = new HashSet<>();
    FriendAdapter friendsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_member);

        friendsRecyclerView = findViewById(R.id.friends_recycler_view);
        btnAddSelected = findViewById(R.id.btn_add_selected);
        btnAddSelected.setEnabled(false);

        chatroomId = getIntent().getStringExtra("chatroomId");

        setupRecyclerView();
        loadFriends();

        btnAddSelected.setOnClickListener(v -> {
            if (selectedUserIds.isEmpty()) {
                Toast.makeText(this, "Chọn ít nhất 1 bạn bè để thêm", Toast.LENGTH_SHORT).show();
                return;
            }
            addMembersToGroup();
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    void setupRecyclerView() {
        friendsAdapter = new FriendAdapter(friendList, selectedUserIds, () -> {
            btnAddSelected.setEnabled(!selectedUserIds.isEmpty());
        });
        friendsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        friendsRecyclerView.setAdapter(friendsAdapter);
    }

    @SuppressLint("NotifyDataSetChanged")
    void loadFriends() {
        FirebaseUtil.allUserCollectionReference()
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        friendList.clear();
                        String currentUserId = FirebaseUtil.currentUserId();
                        for (DocumentSnapshot doc : task.getResult()) {
                            UserModel user = doc.toObject(UserModel.class);
                            if (user != null && !user.getUserId().equals(currentUserId)) {
                                friendList.add(user);
                            }
                        }
                        friendsAdapter.notifyDataSetChanged();
                    } else {
                        Log.e("AddMemberActivity", "Lỗi tải danh sách bạn bè", task.getException());
                    }
                });
    }

    void addMembersToGroup() {
        // Chuyển Set -> Array
        String[] userIdsArray = selectedUserIds.toArray(new String[0]);

        // Cập nhật Firestore với arrayUnion đúng cách
        FirebaseUtil.getChatroomReference(chatroomId)
                .update("memberIds", FieldValue.arrayUnion(userIdsArray))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã thêm bạn vào nhóm", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Thêm bạn vào nhóm thất bại", Toast.LENGTH_SHORT).show();
                    Log.e("AddMemberActivity", "Lỗi cập nhật nhóm", e);
                });
    }
}
