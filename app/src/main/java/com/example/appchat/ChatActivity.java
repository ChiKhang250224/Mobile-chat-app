package com.example.appchat;

// Nhập các thư viện cần thiết cho Activity, RecyclerView, Firebase, và thông báo

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.appchat.adapter.ChatRecyclerAdapter;
import com.example.appchat.adapter.SearchUserRecyclerAdapter;
import com.example.appchat.model.ChatMessageModel;
import com.example.appchat.model.ChatroomModel;
import com.example.appchat.model.UserModel;
import com.example.appchat.utils.AndroidUtils;
import com.example.appchat.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.common.api.Response;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;


import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Request;
import okhttp3.Callback;

public class ChatActivity extends AppCompatActivity {

    // Khai báo biến để lưu thông tin người dùng khác
    UserModel otherUser;
    // ID của phòng chat
    String chatroomId;
    // Model cho phòng chat
    ChatroomModel chatroomModel;
    // Adapter để hiển thị danh sách tin nhắn trong RecyclerView
    ChatRecyclerAdapter adapter;

    // Các thành phần giao diện
    EditText messageInput; // Ô nhập tin nhắn
    ImageButton sendMessageBtn; // Nút gửi tin nhắn
    ImageButton backBtn; // Nút quay lại
    TextView otherUsername; // TextView hiển thị tên người dùng khác
    RecyclerView recyclerView; // RecyclerView hiển thị danh sách tin nhắn
    ImageView imageView; // ImageView hiển thị ảnh đại diện

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Liên kết Activity với file layout activity_chat.xml
        setContentView(R.layout.activity_chat);

        // Lấy thông tin người dùng khác từ Intent
        otherUser = AndroidUtils.getUserModelFromIntent(getIntent());
        // Tạo ID phòng chat dựa trên ID của người dùng hiện tại và người dùng khác
        chatroomId = FirebaseUtil.getChatroomId(FirebaseUtil.currentUserId(), otherUser.getUserId());

        // Gán các thành phần giao diện từ layout
        messageInput = findViewById(R.id.chat_message_input);
        sendMessageBtn = findViewById(R.id.message_send_btn);
        backBtn = findViewById(R.id.back_btn);
        otherUsername = findViewById(R.id.other_username);
        recyclerView = findViewById(R.id.chat_recycler_view);
        imageView = findViewById(R.id.profile_pic_image_view);

        FirebaseUtil.getOtherProfilePicStorageRef(otherUser.getUserId()).getDownloadUrl()
                .addOnCompleteListener(t -> {
                    if(t.isSuccessful()){
                        Uri uri  = t.getResult();
                        AndroidUtils.setProfilePic(this,uri,imageView);
                    }
                });

        // Xử lý sự kiện nhấn nút quay lại
        backBtn.setOnClickListener((v) -> {
            // Quay lại Activity trước đó
            onBackPressed();
        });

        // Hiển thị tên người dùng khác
        otherUsername.setText(otherUser.getUsername());

        // Xử lý sự kiện nhấn nút gửi tin nhắn
        sendMessageBtn.setOnClickListener((v -> {
            // Lấy nội dung tin nhắn và xóa khoảng trắng thừa
            String message = messageInput.getText().toString().trim();
            // Nếu tin nhắn rỗng, không làm gì
            if (message.isEmpty())
                return;
            // Gửi tin nhắn
            sendMessageToUser(message);
        }));

        // Lấy hoặc tạo mới phòng chat
        getOrCreateChatroomModel();
        // Thiết lập RecyclerView để hiển thị tin nhắn
        setupChatRecyclerView();
    }

    // Hàm thiết lập RecyclerView để hiển thị danh sách tin nhắn
    void setupChatRecyclerView() {
        // Tạo truy vấn Firebase để lấy tin nhắn theo thời gian, sắp xếp giảm dần
        Query query = FirebaseUtil.getChatroomMessageReference(chatroomId)
                .orderBy("timestamp", Query.Direction.DESCENDING);

        // Tạo FirestoreRecyclerOptions để liên kết truy vấn với ChatMessageModel
        FirestoreRecyclerOptions<ChatMessageModel> options = new FirestoreRecyclerOptions.Builder<ChatMessageModel>()
                .setQuery(query, ChatMessageModel.class).build();

        // Khởi tạo adapter với dữ liệu từ Firestore
        adapter = new ChatRecyclerAdapter(options, getApplicationContext());
        // Thiết lập LinearLayoutManager với bố cục đảo ngược (tin nhắn mới nhất ở dưới)
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setReverseLayout(true);
        recyclerView.setLayoutManager(manager);
        // Gán adapter cho RecyclerView
        recyclerView.setAdapter(adapter);
        // Bắt đầu lắng nghe dữ liệu từ Firestore
        adapter.startListening();
        // Tự động cuộn đến tin nhắn mới nhất khi có tin nhắn mới
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                recyclerView.smoothScrollToPosition(0);
            }
        });
    }

    void sendMessageToUser(String message) {
        // Cập nhật thông tin phòng chat
        chatroomModel.setLastMessageTimestamp(Timestamp.now());
        chatroomModel.setLastMessageSenderId(FirebaseUtil.currentUserId());
        chatroomModel.setLastMessage(message);

        FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel)
                .addOnFailureListener(e -> Log.e("FIRESTORE", "Lỗi cập nhật chatroom", e));

        // Tạo tin nhắn mới
        ChatMessageModel chatMessageModel = new ChatMessageModel(message, FirebaseUtil.currentUserId(), Timestamp.now());

        FirebaseUtil.getChatroomMessageReference(chatroomId)
                .add(chatMessageModel)
                .addOnSuccessListener(docRef -> {
                    messageInput.setText("");
                    sendNotification(message);
                    Log.d("SEND_MSG", "Gửi thành công");
                })
                .addOnFailureListener(e -> {
                    Log.e("SEND_MSG", "Lỗi khi gửi tin nhắn", e);
                    Toast.makeText(ChatActivity.this, "Không gửi được tin nhắn", Toast.LENGTH_SHORT).show();
                });
    }


    // Hàm lấy hoặc tạo mới phòng chat trong Firestore
    void getOrCreateChatroomModel() {
        // Lấy thông tin phòng chat từ Firestore
        FirebaseUtil.getChatroomReference(chatroomId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Chuyển dữ liệu Firestore thành ChatroomModel
                chatroomModel = task.getResult().toObject(ChatroomModel.class);
                if (chatroomModel == null) {
                    // Nếu phòng chat chưa tồn tại, tạo mới
                    chatroomModel = new ChatroomModel(
                            chatroomId,
                            Arrays.asList(FirebaseUtil.currentUserId(), otherUser.getUserId()),
                            Timestamp.now(),
                            ""
                    );
                    // Lưu phòng chat mới vào Firestore
                    FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel);
                }
            }
        });
    }
    void sendNotification(String message){

        FirebaseUtil.currentUserDetails().get().addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                UserModel currentUser = task.getResult().toObject(UserModel.class);
                try{
                    JSONObject jsonObject  = new JSONObject();

                    JSONObject notificationObj = new JSONObject();
                    notificationObj.put("title",currentUser.getUsername());
                    notificationObj.put("body",message);

                    JSONObject dataObj = new JSONObject();
                    dataObj.put("userId",currentUser.getUserId());

                    jsonObject.put("notification",notificationObj);
                    jsonObject.put("data",dataObj);
                    jsonObject.put("to",otherUser.getFcmToken());

                    callApi(jsonObject);


                }catch (Exception e){

                }

            }
        });

    }

    void callApi(JSONObject jsonObject){
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        OkHttpClient client = new OkHttpClient();
        String url = "https://fcm.googleapis.com/fcm/send";
        RequestBody body = RequestBody.create(jsonObject.toString(),JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("Authorization","Bearer YOUR_API_KEY")
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("NOTIFY_API", "Gửi thông báo thất bại", e);
            }
            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                Log.d("NOTIFY_API", "Gửi thông báo thành công");

            }
        });

    }





}