package com.example.appchat;

// Nhập các thư viện cần thiết cho Activity, RecyclerView, Firebase, và thông báo

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;

import java.io.IOException;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Callback;
import okhttp3.Call;




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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONObject;
import java.util.Arrays;
import okhttp3.MediaType;


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
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private Uri selectedImageUri;
    private static final int REQUEST_IMAGE_PICK = 101;
    private static final int REQUEST_PERMISSION = 102;






    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Liên kết Activity với file layout activity_chat.xml
        setContentView(R.layout.activity_chat);

        // Gán các thành phần giao diện từ layout
        messageInput = findViewById(R.id.chat_message_input);
        sendMessageBtn = findViewById(R.id.message_send_btn);
        backBtn = findViewById(R.id.back_btn);
        otherUsername = findViewById(R.id.other_username); // 👈 Cần gọi findViewById trước khi setText
        recyclerView = findViewById(R.id.chat_recycler_view);
        imageView = findViewById(R.id.profile_pic_image_view);
        TextView onlineStatus = findViewById(R.id.online_status);
        ImageButton sendImageBtn = findViewById(R.id.send_image_btn); // gán view
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        // Gọi hàm upload ảnh
                        uploadImageToImgBB(selectedImageUri);

                        // TODO: xử lý ảnh khác nếu cần
                    }
                }
        );



        sendImageBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        // Lấy thông tin người dùng khác từ Intent
        otherUser = AndroidUtils.getUserModelFromIntent(getIntent());

        //
        if (otherUser != null && otherUser.getUsername() != null) {
            otherUsername.setText(otherUser.getUsername());
        } else {
            otherUsername.setText("Unknown");
        }
        //

        // Tạo ID phòng chat dựa trên ID của người dùng hiện tại và người dùng khác
        chatroomId = FirebaseUtil.getChatroomId(FirebaseUtil.currentUserId(), otherUser.getUserId());

        // Tải ảnh đại diện của người kia
        FirebaseUtil.getOtherProfilePicStorageRef(otherUser.getUserId()).getDownloadUrl()
                .addOnCompleteListener(t -> {
                    if (t.isSuccessful()) {
                        Uri uri = t.getResult();
                        AndroidUtils.setProfilePic(this, uri, imageView);
                    }
                });

        // Xử lý sự kiện nhấn nút quay lại
        backBtn.setOnClickListener((v) -> {
            getOnBackPressedDispatcher().onBackPressed();
        });


        // Hiển thị trạng thái online hoặc last seen
        FirebaseUtil.allUserCollectionReference()
                .document(otherUser.getUserId())
                .addSnapshotListener((snapshot, error) -> {
                    if (snapshot != null && snapshot.exists()) {
                        boolean isOnline = snapshot.getBoolean("online") != null && snapshot.getBoolean("online");
                        if (isOnline) {
                            onlineStatus.setText("Online");
                            onlineStatus.setTextColor(getResources().getColor(R.color.light_green));
                            onlineStatus.setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_dot_green, 0, 0, 0);
                            onlineStatus.setVisibility(View.VISIBLE);
                        } else {
                            Timestamp lastSeen = snapshot.getTimestamp("lastSeen");
                            if (lastSeen != null) {
                                String time = FirebaseUtil.timestampToString(lastSeen);
                                onlineStatus.setText("Last seen: " + time);
                                onlineStatus.setTextColor(getResources().getColor(R.color.gray));
                                onlineStatus.setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_clock_gray, 0, 0, 0);
                                onlineStatus.setVisibility(View.VISIBLE);
                            } else {
                                onlineStatus.setVisibility(View.GONE);
                            }
                        }
                    }
                });





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
    private void uploadImageToImgBB(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            byte[] imageBytes = new byte[inputStream.available()];
            inputStream.read(imageBytes);
            String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            OkHttpClient client = new OkHttpClient();
            RequestBody requestBody = new FormBody.Builder()
                    .add("key", "f8c48e870cff1f6cb6c43a1b9633ac9a") // 👉 thay bằng API Key của bạn
                    .add("image", encodedImage)
                    .build();

            Request request = new Request.Builder()
                    .url("https://api.imgbb.com/1/upload")
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Lỗi upload ảnh", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull okhttp3.Response response) throws IOException {
                    try {
                        String responseBody = response.body().string();
                        JSONObject jsonObject = new JSONObject(responseBody);
                        String imageUrl = jsonObject.getJSONObject("data").getString("url");
                        runOnUiThread(() -> sendImageMessage(imageUrl));
                    } catch (Exception e) {
                        runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Lỗi phản hồi ImgBB", Toast.LENGTH_SHORT).show());
                    }
                }
            });

        } catch (IOException e) {
            Toast.makeText(this, "Không đọc được ảnh", Toast.LENGTH_SHORT).show();
        }
    }


    private void sendImageMessage(String imageUrl) {
        // ✅ Cập nhật thông tin chatroom trước khi gửi ảnh
        chatroomModel.setLastMessageTimestamp(Timestamp.now());
        chatroomModel.setLastMessageSenderId(FirebaseUtil.currentUserId());
        chatroomModel.setLastMessage("📷 Sent a photo");

        // ✅ Gửi ảnh như cũ
        ChatMessageModel chatMessage = new ChatMessageModel("photo", FirebaseUtil.currentUserId(), Timestamp.now());
        chatMessage.setImageUrl(imageUrl);
        FirebaseUtil.getChatroomMessageReference(chatroomId)
                .add(chatMessage)
                .addOnSuccessListener(docRef -> {
                    sendNotification("📷 Sent a photo");
                })
                .addOnFailureListener(e -> {
                    Log.e("SEND_IMG", "Lỗi khi gửi ảnh", e);
                    Toast.makeText(this, "Không gửi được ảnh", Toast.LENGTH_SHORT).show();
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
    // chọn ảnh từ thu viện
    private void openImagePicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES}, REQUEST_PERMISSION);
                return;
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
                return;
            }
        }

        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);  // ✅ dùng launcher thay vì startActivityForResult
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                uploadImageToImgBB(selectedImageUri);

            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(this, "Bạn cần cấp quyền để chọn ảnh", Toast.LENGTH_SHORT).show();
            }
        }
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