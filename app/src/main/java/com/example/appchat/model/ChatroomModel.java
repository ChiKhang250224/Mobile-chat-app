package com.example.appchat.model;

import com.google.firebase.Timestamp;

import java.util.List;

public class ChatroomModel {
    private String chatroomId;
    private List<String> memberIds;

    private String lastMessage;
    private String lastMessageSenderId;
    private Timestamp lastMessageTimestamp;
    private String groupName;
    private boolean isGroupChat;

    private Timestamp createdAt;

    public ChatroomModel() { }


    public String getChatroomId() { return chatroomId; }
    public void setChatroomId(String chatroomId) { this.chatroomId = chatroomId; }

    public List<String> getMemberIds() { return memberIds; }
    public void setMemberIds(List<String> memberIds) { this.memberIds = memberIds; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public String getLastMessageSenderId() { return lastMessageSenderId; }
    public void setLastMessageSenderId(String lastMessageSenderId) { this.lastMessageSenderId = lastMessageSenderId; }

    public Timestamp getLastMessageTimestamp() { return lastMessageTimestamp; }
    public void setLastMessageTimestamp(Timestamp lastMessageTimestamp) { this.lastMessageTimestamp = lastMessageTimestamp; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public boolean getIsGroupChat() { return isGroupChat; }
    public void setIsGroupChat(boolean groupChat) { isGroupChat = groupChat; }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
