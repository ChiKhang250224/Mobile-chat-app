package com.example.appchat;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.appchat.R;
import com.example.appchat.SplashActivity;
import com.example.appchat.model.UserModel;
import com.example.appchat.utils.AndroidUtils;
import com.example.appchat.utils.FirebaseUtil;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.UploadTask;
import com.example.appchat.utils.ImgBBUploader;
import android.app.ProgressDialog; // Import ProgressDialog
import com.bumptech.glide.Glide; // Import Glide

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class ProfileFragment extends Fragment {

    ImageView profilePic;
    EditText usernameInput;
    EditText phoneInput;
    Button updateProfileBtn;
    ProgressBar progressBar;
    TextView logoutBtn;

    UserModel currentUserModel;
    ActivityResultLauncher<Intent> imagePickLauncher;
    Uri selectedImageUri;
    ProgressDialog progressDialog;

    public ProfileFragment() {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        imagePickLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if(result.getResultCode() == Activity.RESULT_OK){
                        Intent data = result.getData();
                        if(data!=null && data.getData()!=null){
                            selectedImageUri = data.getData();
                            AndroidUtils.setProfilePic(getContext(),selectedImageUri,profilePic);
                        }
                    }
                }
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_profile, container, false);
        profilePic = view.findViewById(R.id.profile_image_view);
        usernameInput = view.findViewById(R.id.profile_username);
        phoneInput = view.findViewById(R.id.profile_phone);
        updateProfileBtn = view.findViewById(R.id.profle_update_btn);
        progressBar = view.findViewById(R.id.profile_progress_bar);
        logoutBtn = view.findViewById(R.id.logout_btn);

        String avatarUrl = "https://your-image-url.com/avatar.jpg"; // hoặc lấy từ SharedPreferences
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            Glide.with(this)
                    .load(avatarUrl)
                    .placeholder(R.drawable.ic_avatar_placeholder) // ảnh mặc định khi tải

                    .circleCrop()
                    .into(profilePic);
        }

        getUserData();

        updateProfileBtn.setOnClickListener((v -> {
            updateBtnClick();
        }));

        logoutBtn.setOnClickListener((v)->{
            FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful()){
                        FirebaseUtil.logout();
                        Intent intent = new Intent(getContext(), SplashActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
                }
            });



        });

        profilePic.setOnClickListener((v)->{
            ImagePicker.with(this).cropSquare().compress(512).maxResultSize(512,512)
                    .createIntent(new Function1<Intent, Unit>() {
                        @Override
                        public Unit invoke(Intent intent) {
                            imagePickLauncher.launch(intent);
                            return null;
                        }
                    });
        });

        return view;
    }

    void updateBtnClick(){
        String newUsername = usernameInput.getText().toString();
        if(newUsername.isEmpty() || newUsername.length()<3){
            usernameInput.setError("Username length should be at least 3 chars");
            return;
        }

        currentUserModel.setUsername(newUsername);
        setInProgress(true); // Đặt trạng thái đang xử lý

        // Khởi tạo ProgressDialog ở đây để nó luôn có sẵn khi cần
        // Đảm bảo getContext() không null hoặc Fragment đã attach
        if (progressDialog == null) { // Tránh khởi tạo lại nếu đã có
            progressDialog = new ProgressDialog(getContext());
            progressDialog.setCancelable(false); // Không cho phép hủy bằng cách chạm ra ngoài
            progressDialog.setMessage("Đang cập nhật...");
        }
        progressDialog.show(); // Hiển thị ProgressDialog ngay khi bắt đầu quá trình cập nhật

        if(selectedImageUri!=null){
            ImgBBUploader.uploadImage(getContext(), selectedImageUri, new ImgBBUploader.UploadCallback() {
                @Override
                public void onSuccess(String imageUrl) {
                    currentUserModel.setProfilePicUrl(imageUrl); // Lưu URL ảnh từ ImgBB
                    updateToFirestore(); // Gọi updateToFirestore sau khi có URL
                    // progressDialog.dismiss() sẽ được gọi trong updateToFirestore() hoặc sau khi update xong
                }

                @Override
                public void onFailure(String errorMessage) {
                    progressDialog.dismiss(); // Ẩn ProgressDialog nếu lỗi upload ảnh
                    setInProgress(false); // Kết thúc trạng thái đang xử lý
                    AndroidUtils.showToast(getContext(), "Tải ảnh đại diện thất bại: " + errorMessage);
                    Log.e("ProfileFragment", "Lỗi tải ảnh đại diện lên ImgBB: " + errorMessage);
                    // Vẫn có thể cập nhật thông tin khác nếu ảnh đại diện thất bại
                    updateToFirestore(); // Vẫn gọi updateToFirestore dù upload ảnh thất bại
                }
            });
        }else{
            updateToFirestore(); // Nếu không có ảnh mới, chỉ cập nhật thông tin
        }
    }

    void updateToFirestore(){
        FirebaseUtil.currentUserDetails().set(currentUserModel)
                .addOnCompleteListener(task -> {
                    // Luôn dismiss ProgressDialog khi quá trình hoàn tất
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    setInProgress(false); // Đặt lại trạng thái đang xử lý

                    if(task.isSuccessful()){
                        AndroidUtils.showToast(getContext(),"Cập nhật thành công");
                    }else{
                        String errorMessage = "Cập nhật thất bại.";
                        if (task.getException() != null) {
                            errorMessage += " Lỗi: " + task.getException().getLocalizedMessage(); // Sử dụng getLocalizedMessage() hoặc getMessage() tùy ý
                            Log.e("ProfileFragment", "Lỗi cập nhật Firestore: " + task.getException().getMessage(), task.getException());
                        }
                        AndroidUtils.showToast(getContext(), errorMessage);
                    }
                });
    }



    void getUserData(){
        setInProgress(true);

        // KHÔNG CÒN GỌI getDownloadUrl() TỪ FIRESTORE STORAGE NỮA
        // Thay vào đó, tải dữ liệu UserModel trước, rồi mới lấy URL ảnh đại diện từ UserModel
        FirebaseUtil.currentUserDetails().get().addOnCompleteListener(task -> {
            setInProgress(false);
            if(task.isSuccessful()){
                currentUserModel = task.getResult().toObject(UserModel.class);
                if (currentUserModel != null) {
                    usernameInput.setText(currentUserModel.getUsername());
                    phoneInput.setText(currentUserModel.getPhone());

                    // Tải ảnh đại diện từ URL đã lưu trong Firestore
                    String profileImageUrl = currentUserModel.getProfilePicUrl();
                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        Glide.with(getContext())
                                .load(profileImageUrl)
                                .apply(RequestOptions.circleCropTransform())
                                .placeholder(R.drawable.default_profile_pic) // Ảnh tạm thời khi đang tải
                                .error(R.drawable.default_profile_pic) // Ảnh khi tải lỗi hoặc không tìm thấy
                                .into(profilePic); // Sử dụng ImageView profilePic
                    } else {
                        // Nếu không có URL ảnh đại diện, hiển thị ảnh mặc định
                        profilePic.setImageResource(R.drawable.default_profile_pic);
                    }
                } else {
                    Log.e("ProfileFragment", "UserModel is null after fetching.");
                    AndroidUtils.showToast(getContext(), "Lỗi tải dữ liệu người dùng.");
                }
            } else {
                Log.e("ProfileFragment", "Failed to get user data: " + task.getException().getMessage());
                AndroidUtils.showToast(getContext(), "Lỗi tải dữ liệu người dùng.");
            }
        });
    }



    void setInProgress(boolean inProgress){
        if(inProgress){
            progressBar.setVisibility(View.VISIBLE);
            updateProfileBtn.setVisibility(View.GONE);
        }else{
            progressBar.setVisibility(View.GONE);
            updateProfileBtn.setVisibility(View.VISIBLE);
        }
    }
}