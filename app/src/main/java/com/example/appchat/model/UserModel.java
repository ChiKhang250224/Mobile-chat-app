package com.example.appchat.model;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.firebase.Timestamp;

import java.util.Date; // Cần import Date cho việc chuyển đổi Timestamp

public class UserModel implements Parcelable { // <-- Thay Serializable bằng Parcelable
    private String phone;
    private String username;
    private Timestamp createdTimestamp;
    private String userId;
    private String fcmToken;
    private boolean online;
    private Timestamp lastSeen;
    private String profilePicUrl;

    public UserModel() {
        // Constructor không đối số, cần thiết cho Firestore
    }

    public UserModel(String phone, String username, Timestamp createdTimestamp, String userId) {
        this.phone = phone;
        this.username = username;
        this.createdTimestamp = createdTimestamp;
        this.userId = userId;
        this.profilePicUrl = null; // Khởi tạo null nếu không có
    }

    // Constructor đầy đủ (có thể có, tùy thuộc vào cách bạn khởi tạo đối tượng)
    public UserModel(String phone, String username, Timestamp createdTimestamp, String userId, String fcmToken, boolean online, Timestamp lastSeen, String profilePicUrl) {
        this.phone = phone;
        this.username = username;
        this.createdTimestamp = createdTimestamp;
        this.userId = userId;
        this.fcmToken = fcmToken;
        this.online = online;
        this.lastSeen = lastSeen;
        this.profilePicUrl = profilePicUrl;
    }

    public UserModel(String userId, String username, String s, long l) {
    }

    // --- Các phương thức Getter và Setter ---

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Timestamp getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(Timestamp createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public Timestamp getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Timestamp lastSeen) {
        this.lastSeen = lastSeen;
    }

    public String getProfilePicUrl() {
        return profilePicUrl;
    }

    public void setProfilePicUrl(String profilePicUrl) {
        this.profilePicUrl = profilePicUrl;
    }


    // --- Bắt đầu triển khai Parcelable ---

    // Constructor để đọc dữ liệu từ Parcel
    protected UserModel(Parcel in) {
        phone = in.readString();
        username = in.readString();

        // Đọc Timestamp: đọc thời gian dưới dạng long (milliseconds), sau đó chuyển đổi lại thành Timestamp
        // Sử dụng -1L để biểu thị giá trị null
        long createdMillis = in.readLong();
        createdTimestamp = createdMillis == -1L ? null : new Timestamp(new Date(createdMillis));

        userId = in.readString();
        fcmToken = in.readString();
        online = in.readByte() != 0; // Đọc boolean (byte 0 = false, byte 1 = true)

        long lastSeenMillis = in.readLong();
        lastSeen = lastSeenMillis == -1L ? null : new Timestamp(new Date(lastSeenMillis));

        profilePicUrl = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(phone);
        dest.writeString(username);

        // Ghi Timestamp: chuyển đổi thành milliseconds và ghi dưới dạng long.
        // Ghi -1L nếu Timestamp là null.
        dest.writeLong(createdTimestamp != null ? createdTimestamp.toDate().getTime() : -1L);

        dest.writeString(userId);
        dest.writeString(fcmToken);
        dest.writeByte((byte) (online ? 1 : 0)); // Ghi boolean

        dest.writeLong(lastSeen != null ? lastSeen.toDate().getTime() : -1L);

        dest.writeString(profilePicUrl);
    }

    @Override
    public int describeContents() {
        return 0; // Hầu hết các trường hợp đều trả về 0
    }

    // Đối tượng CREATOR là bắt buộc cho Parcelable
    public static final Creator<UserModel> CREATOR = new Creator<UserModel>() {
        @Override
        public UserModel createFromParcel(Parcel in) {
            return new UserModel(in);
        }

        @Override
        public UserModel[] newArray(int size) {
            return new UserModel[size];
        }
    };
}