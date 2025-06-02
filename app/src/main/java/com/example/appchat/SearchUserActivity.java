package com.example.appchat;

// Nhập các thư viện cần thiết cho Activity, RecyclerView và Firebase

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;

import com.example.appchat.adapter.SearchUserRecyclerAdapter;
import com.example.appchat.model.UserModel;
import com.example.appchat.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.Query;

public class SearchUserActivity extends AppCompatActivity {

    // Khai báo các thành phần giao diện
    EditText searchInput; // Ô nhập liệu để người dùng nhập tên cần tìm
    ImageButton searchButton; // Nút tìm kiếm
    ImageButton backButton; // Nút quay lại
    RecyclerView recyclerView; // RecyclerView để hiển thị danh sách người dùng

    // Adapter để quản lý dữ liệu hiển thị trong RecyclerView
    SearchUserRecyclerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Liên kết Activity với file layout activity_search_user.xml
        setContentView(R.layout.activity_search_user);

        // Gán các thành phần giao diện từ layout
        searchInput = findViewById(R.id.search_username_input); // Ô nhập liệu tìm kiếm
        searchButton = findViewById(R.id.search_user_btn); // Nút tìm kiếm
        backButton = findViewById(R.id.back_btn); // Nút quay lại
        recyclerView = findViewById(R.id.search_user_recycler_view); // Danh sách người dùng

        // Đặt con trỏ vào ô tìm kiếm ngay khi Activity mở
        searchInput.requestFocus();

        // Xử lý sự kiện khi nhấn nút quay lại
        backButton.setOnClickListener(v -> {
            // Gọi phương thức quay lại Activity trước đó
            onBackPressed();
        });

        // Xử lý sự kiện khi nhấn nút tìm kiếm
        searchButton.setOnClickListener(v -> {
            // Lấy nội dung người dùng nhập
            String searchTerm = searchInput.getText().toString();
            // Kiểm tra nếu nội dung rỗng hoặc ngắn hơn 3 ký tự
            if (searchTerm.isEmpty() || searchTerm.length() < 3) {
                searchInput.setError("Tên người dùng không hợp lệ");
                return;
            }
            // Gọi hàm để thiết lập RecyclerView với từ khóa tìm kiếm
            setupSearchRecyclerView(searchTerm);
        });
    }

    // Hàm thiết lập RecyclerView để hiển thị kết quả tìm kiếm
    void setupSearchRecyclerView(String searchTerm) {
        // Tạo truy vấn Firebase để tìm kiếm người dùng theo tên
        Query query = FirebaseUtil.allUserCollectionReference()
                // Tìm username >= searchTerm
                .whereGreaterThanOrEqualTo("username", searchTerm)
                // Tìm username <= searchTerm + ký tự Unicode lớn nhất để lấy toàn bộ kết quả phù hợp
                .whereLessThanOrEqualTo("username", searchTerm + '\uf8ff');

        // Tạo FirestoreRecyclerOptions để liên kết truy vấn với UserModel
        FirestoreRecyclerOptions<UserModel> options = new FirestoreRecyclerOptions.Builder<UserModel>()
                .setQuery(query, UserModel.class).build();

        // Khởi tạo adapter với dữ liệu từ Firestore
        adapter = new SearchUserRecyclerAdapter(options, getApplicationContext());
        // Thiết lập LinearLayoutManager để hiển thị danh sách dạng dọc
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        // Gán adapter cho RecyclerView
        recyclerView.setAdapter(adapter);
        // Bắt đầu lắng nghe dữ liệu từ Firestore
        adapter.startListening();
    }

    // Ghi đè phương thức onStart để đảm bảo adapter bắt đầu lắng nghe khi Activity khởi động
    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null)
            adapter.startListening(); // Kích hoạt lắng nghe dữ liệu từ Firestore
    }

    // Ghi đè phương thức onStop để dừng lắng nghe khi Activity không còn hiển thị
    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null)
            adapter.stopListening(); // Dừng lắng nghe để tiết kiệm tài nguyên
    }

    // Ghi đè phương thức onResume để đảm bảo adapter tiếp tục lắng nghe khi Activity trở lại
    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null)
            adapter.startListening(); // Kích hoạt lại lắng nghe dữ liệu
    }
}
