package com.example.appchat.utils;

// Nhập các thư viện Firebase và Java cần thiết

import android.view.Menu;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// Lớp tiện ích để xử lý các thao tác liên quan đến Firebase
public class FirebaseUtil {

    // Lấy ID của người dùng hiện tạiC
    public static String currentUserId() {
        return FirebaseAuth.getInstance().getUid(); // Trả về UID từ Firebase Authentication
    }

    // Kiểm tra xem người dùng đã đăng nhập hay chưa
    public static boolean isLoggedIn() {
        if (currentUserId() != null) { // Nếu có UID, người dùng đã đăng nhập
            return true;
        }
        return false; // Nếu không có UID, người dùng chưa đăng nhập
    }

    // Lấy tham chiếu đến tài liệu của người dùng hiện tại trong Firestore
    public static DocumentReference currentUserDetails() {
        return FirebaseFirestore.getInstance().collection("users").document(currentUserId());
    }

    // Lấy tham chiếu đến bộ sưu tập tất cả người dùng trong Firestore
    public static CollectionReference allUserCollectionReference() {
        return FirebaseFirestore.getInstance().collection("users");
    }

    // Lấy tham chiếu đến tài liệu phòng chat trong Firestore dựa trên chatroomId
    public static DocumentReference getChatroomReference(String chatroomId) {
        return FirebaseFirestore.getInstance().collection("chatrooms").document(chatroomId);
    }

    // Lấy tham chiếu đến bộ sưu tập tin nhắn của một phòng chat
    public static CollectionReference getChatroomMessageReference(String chatroomId) {
        return getChatroomReference(chatroomId).collection("chats");
    }

    // Tạo ID phòng chat dựa trên hai ID người dùng
    public static String getChatroomId(String userId1, String userId2) {
        // Sắp xếp ID để đảm bảo chatroomId luôn giống nhau cho cùng một cặp người dùng
        List<String> userIds = Arrays.asList(userId1, userId2);
        Collections.sort(userIds); // Sắp xếp theo bảng chữ cái

        // Ghép các ID lại với nhau
        return userIds.get(0) + "_" + userIds.get(1);
    }

    // Lấy tham chiếu đến bộ sưu tập tất cả phòng chat trong Firestore
    public static CollectionReference allChatroomCollectionReference() {
        return FirebaseFirestore.getInstance().collection("chatrooms");
    }

    // Lấy thông tin người dùng khác từ danh sách userIds của phòng chat
    public static List<DocumentReference> getOtherUserFromChatroom(List<String> userIds) {
        List<DocumentReference> otherRefs = new ArrayList<>();
        String current = currentUserId();
        if (userIds == null) return otherRefs;
        for (String uid : userIds) {
            if (uid != null && !uid.equals(current)) {
                otherRefs.add(allUserCollectionReference().document(uid));
            }
        }
        return otherRefs;
    }


    // Chuyển đổi Timestamp thành chuỗi thời gian (giờ:phút)
    public static String timestampToString(Timestamp timestamp) {
        return new SimpleDateFormat("HH:MM").format(timestamp.toDate());
    }

    // Đăng xuất người dùng khỏi Firebase Authentication
    public static void logout() {
        FirebaseAuth.getInstance().signOut();
    }


    public static CollectionReference getChatroomsCollection() {
        return FirebaseFirestore.getInstance().collection("chatrooms");
    }
    // Trong FirebaseUtil.java
    public static DocumentReference getGroupChatroomReference(String chatroomId) {
        return getChatroomsCollection().document(chatroomId);
    }

    public static CollectionReference getGroupChatroomMessageReference(String chatroomId) {
        return getGroupChatroomReference(chatroomId).collection("messages");
    }


}