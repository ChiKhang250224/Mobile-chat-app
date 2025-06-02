package com.example.appchat.model;

// Nhập thư viện Firebase để sử dụng kiểu Timestamp

import com.google.firebase.Timestamp;

// Nhập thư viện List để lưu danh sách ID người dùng
import java.util.List;

// Lớp mô hình (Model) để đại diện cho một phòng chat trong ứng dụng
public class ChatroomModel {
    // ID duy nhất của phòng chat
    String chatroomId;
    // Danh sách ID của các người dùng tham gia phòng chat
    List<String> userIds;
    // Thời gian gửi tin nhắn cuối cùng trong phòng chat
    Timestamp lastMessageTimestamp;
    // ID của người gửi tin nhắn cuối cùng
    String lastMessageSenderId;
    // Nội dung tin nhắn cuối cùng
    String lastMessage;

    // Constructor mặc định (yêu cầu bởi Firestore để chuyển đổi dữ liệu)
    public ChatroomModel() {
    }

    // Constructor đầy đủ để khởi tạo phòng chat với các thông tin cần thiết
    public ChatroomModel(String chatroomId, List<String> userIds, Timestamp lastMessageTimestamp, String lastMessageSenderId) {
        this.chatroomId = chatroomId; // Gán ID phòng chat
        this.userIds = userIds; // Gán danh sách người dùng
        this.lastMessageTimestamp = lastMessageTimestamp; // Gán thời gian tin nhắn cuối
        this.lastMessageSenderId = lastMessageSenderId; // Gán ID người gửi tin nhắn cuối
    }

    // Lấy ID của phòng chat
    public String getChatroomId() {
        return chatroomId;
    }

    // Đặt ID cho phòng chat
    public void setChatroomId(String chatroomId) {
        this.chatroomId = chatroomId;
    }

    // Lấy danh sách ID của người dùng trong phòng chat
    public List<String> getUserIds() {
        return userIds;
    }

    // Đặt danh sách ID của người dùng trong phòng chat
    public void setUserIds(List<String> userIds) {
        this.userIds = userIds;
    }

    // Lấy thời gian của tin nhắn cuối cùng
    public Timestamp getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    // Đặt thời gian cho tin nhắn cuối cùng
    public void setLastMessageTimestamp(Timestamp lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    // Lấy ID của người gửi tin nhắn cuối cùng
    public String getLastMessageSenderId() {
        return lastMessageSenderId;
    }

    // Đặt ID của người gửi tin nhắn cuối cùng
    public void setLastMessageSenderId(String lastMessageSenderId) {
        this.lastMessageSenderId = lastMessageSenderId;
    }

    // Lấy nội dung của tin nhắn cuối cùng
    public String getLastMessage() {
        return lastMessage;
    }

    // Đặt nội dung cho tin nhắn cuối cùng
    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }
}