package com.example.appchat;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appchat.adapter.FriendAdapter;
import com.example.appchat.model.UserModel;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SelectFriendsFragment extends ChatFragment {

    private RecyclerView recyclerView;
    private List<UserModel> userList = new ArrayList<>();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private Set<String> selectedUserIds = new HashSet<>();


    private FriendAdapter adapter;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_select_friends, container, false);
        recyclerView = view.findViewById(R.id.recyclerViewFriends);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        loadUsers();

        view.findViewById(R.id.btnCreateGroup).setOnClickListener(v -> {
            if (!selectedUserIds.isEmpty()) {
                createGroup("Nhóm Chat Mới", selectedUserIds);
            }
        });

        return view;
    }

    private void loadUsers() {
        db.collection("users").get().addOnSuccessListener(query -> {
            for (DocumentSnapshot doc : query.getDocuments()) {
                UserModel user = doc.toObject(UserModel.class);
                if (user != null) userList.add(user);
            }
            adapter = new FriendAdapter(userList, selectedUserIds, () -> {

            });
            recyclerView.setAdapter(adapter);
        });
    }

    private void createGroup(String groupName, Set<String> members) {
        Map<String, Object> groupData = new HashMap<>();
        groupData.put("groupName", groupName);
        groupData.put("members", members);

        db.collection("groups").add(groupData)
                .addOnSuccessListener(groupRef -> {
                    // Mở màn hình chat nhóm
                    Intent intent = new Intent(getContext(), GroupChatActivity.class);
                    intent.putExtra("GROUP_ID", groupRef.getId());
                    startActivity(intent);
                });
    }
}

