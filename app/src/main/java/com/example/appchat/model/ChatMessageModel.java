package com.example.appchat.model;

import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ChatMessageModel {
    private String message;
    private String senderId;
    private Timestamp timestamp;
    private String imageUrl;
    private boolean isRecalled; // Trường mới: kiểm tra tin nhắn có bị thu hồi hay không
    private List<String> deletedBy; // Trường mới: danh sách userId đã xóa tin nhắn này

    public ChatMessageModel() {
        this.isRecalled = false; // Mặc định tin nhắn không bị thu hồi
        this.deletedBy = new ArrayList<>(); // Khởi tạo danh sách rỗng
    }

    // Constructor cho tin nhắn văn bản
    public ChatMessageModel(String message, String senderId, Timestamp timestamp) {
        this.message = message;
        this.senderId = senderId;
        this.timestamp = timestamp;
        this.imageUrl = null;
        this.isRecalled = false;
        this.deletedBy = new ArrayList<>();
    }

    // Constructor cho tin nhắn ảnh
    public ChatMessageModel(String message, String senderId, Timestamp timestamp, String imageUrl) {
        this.message = message;
        this.senderId = senderId;
        this.timestamp = timestamp;
        this.imageUrl = imageUrl;
        this.isRecalled = false;
        this.deletedBy = new ArrayList<>();
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    // Getter và Setter cho isRecalled
    public boolean isRecalled() {
        return isRecalled;
    }

    public void setRecalled(boolean recalled) {
        isRecalled = recalled;
    }

    // Getter và Setter cho deletedBy
    public List<String> getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(List<String> deletedBy) {
        this.deletedBy = deletedBy;
    }
}