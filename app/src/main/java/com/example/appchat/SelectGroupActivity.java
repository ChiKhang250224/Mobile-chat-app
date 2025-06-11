package com.example.appchat;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appchat.adapter.SelectGroupAdapter;
import com.example.appchat.model.ChatroomModel;
import com.example.appchat.utils.FirebaseUtil;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SelectGroupActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    List<ChatroomModel> groupList = new ArrayList<>();
    SelectGroupAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_group);

        recyclerView = findViewById(R.id.recycler_view_groups);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new SelectGroupAdapter(groupList, group -> {
            // Khi chọn nhóm thì mở AddMemberActivity với chatroomId
            Intent intent = new Intent(SelectGroupActivity.this, AddMemberActivity.class);
            intent.putExtra("chatroomId", group.getChatroomId());
            startActivity(intent);
            finish(); // Đóng màn hình chọn nhóm
        });
        recyclerView.setAdapter(adapter);

        loadUserGroups();
    }

    private void loadUserGroups() {
        String currentUserId = FirebaseUtil.currentUserId();
        FirebaseUtil.getChatroomsCollection()
                .whereArrayContains("memberIds", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    groupList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        ChatroomModel group = doc.toObject(ChatroomModel.class);
                        if (group != null) {
                            group.setChatroomId(doc.getId()); // set id để dùng truyền intent
                            groupList.add(group);
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải nhóm", Toast.LENGTH_SHORT).show();
                });
    }
}

