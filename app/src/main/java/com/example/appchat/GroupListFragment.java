package com.example.appchat;

import android.content.Intent;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appchat.adapter.GroupAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class GroupListFragment extends ChatFragment {

    private RecyclerView recyclerViewGroups;
    private GroupAdapter adapter;
    private List<DocumentSnapshot> groupList = new ArrayList<>();
    private String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();



    public View onCreateView( LayoutInflater inflater,
                              ViewGroup container,
                              Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_group_list, container, false);
        recyclerViewGroups = view.findViewById(R.id.recyclerViewGroups);
        recyclerViewGroups.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new GroupAdapter(groupList, groupId -> {
            Intent i = new Intent(getContext(), GroupChatActivity.class);
            i.putExtra("groupId", groupId);
            startActivity(i);
        });

        recyclerViewGroups.setAdapter(adapter);
        loadUserGroups();
        return view;
    }

    private void loadUserGroups() {
        FirebaseFirestore.getInstance()
                .collection("groups")
                .whereArrayContains("members", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    groupList.clear();
                    groupList.addAll(queryDocumentSnapshots.getDocuments());
                    adapter.notifyDataSetChanged();
                });
    }
}

