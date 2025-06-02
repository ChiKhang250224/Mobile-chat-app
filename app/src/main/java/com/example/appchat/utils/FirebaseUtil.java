package com.example.appchat.utils;

// Nhập các thư viện Firebase và Java cần thiết

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.List;

// Lớp tiện ích để xử lý các thao tác liên quan đến Firebase
public class FirebaseUtil {

    // Lấy ID của người dùng hiện tại
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
        // Sắp xếp để đảm bảo ID phòng chat luôn nhất quán (ID nhỏ hơn đứng trước)
        if (userId1.hashCode() < userId2.hashCode()) {
            return userId1 + "_" + userId2;
        } else {
            return userId2 + "_" + userId1;
        }
    }

    // Lấy tham chiếu đến bộ sưu tập tất cả phòng chat trong Firestore
    public static CollectionReference allChatroomCollectionReference() {
        return FirebaseFirestore.getInstance().collection("chatrooms");
    }

    // Lấy thông tin người dùng khác từ danh sách userIds của phòng chat
    public static DocumentReference getOtherUserFromChatroom(List<String> userIds) {
        // Nếu userId đầu tiên là người dùng hiện tại, trả về người dùng thứ hai
        if (userIds.get(0).equals(FirebaseUtil.currentUserId())) {
            return allUserCollectionReference().document(userIds.get(1));
        } else {
            // Ngược lại, trả về người dùng đầu tiên
            return allUserCollectionReference().document(userIds.get(0));
        }
    }

    // Chuyển đổi Timestamp thành chuỗi thời gian (giờ:phút)
    public static String timestampToString(Timestamp timestamp) {
        return new SimpleDateFormat("HH:MM").format(timestamp.toDate());
    }

    // Đăng xuất người dùng khỏi Firebase Authentication
    public static void logout() {
        FirebaseAuth.getInstance().signOut();
    }

    // Lấy tham chiếu đến ảnh đại diện của người dùng hiện tại trong Firebase Storage
    public static StorageReference getCurrentProfilePicStorageRef() {
        return FirebaseStorage.getInstance().getReference().child("profile_pic")
                .child(FirebaseUtil.currentUserId());
    }

    // Lấy tham chiếu đến ảnh đại diện của người dùng khác trong Firebase Storage
    public static StorageReference getOtherProfilePicStorageRef(String otherUserId) {
        return FirebaseStorage.getInstance().getReference().child("profile_pic")
                .child(otherUserId);
    }
}