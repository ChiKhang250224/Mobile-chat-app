package com.example.appchat.model;

// Nhập thư viện Firebase để sử dụng kiểu Timestamp

import com.google.firebase.Timestamp;

// Lớp mô hình (Model) để đại diện cho một tin nhắn trong ứng dụng chat
public class ChatMessageModel {
    // Nội dung của tin nhắn
    private String message;
    // ID của người gửi tin nhắn
    private String senderId;
    // Thời gian gửi tin nhắn
    private Timestamp timestamp;

    // Constructor mặc định (yêu cầu bởi Firestore để chuyển đổi dữ liệu)
    public ChatMessageModel() {
    }

    // Constructor đầy đủ để khởi tạo tin nhắn với nội dung, người gửi và thời gian
    public ChatMessageModel(String message, String senderId, Timestamp timestamp) {
        this.message = message; // Gán nội dung tin nhắn
        this.senderId = senderId; // Gán ID người gửi
        this.timestamp = timestamp; // Gán thời gian gửi
    }

    // Lấy nội dung tin nhắn
    public String getMessage() {
        return message;
    }

    // Đặt nội dung cho tin nhắn
    public void setMessage(String message) {
        this.message = message;
    }

    // Lấy ID của người gửi tin nhắn
    public String getSenderId() {
        return senderId;
    }

    // Đặt ID cho người gửi tin nhắn
    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    // Lấy thời gian gửi tin nhắn
    public Timestamp getTimestamp() {
        return timestamp;
    }

    // Đặt thời gian gửi tin nhắn
    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
    private String imageUrl;

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

}
