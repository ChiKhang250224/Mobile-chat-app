package com.example.appchat.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.appchat.model.UserModel;

import org.json.JSONException;
import org.json.JSONObject;

public class AndroidUtils {

    public static  void showToast(Context context,String message){
        Toast.makeText(context,message,Toast.LENGTH_LONG).show();
    }

    public static void passUserModelAsIntent(Intent intent, UserModel model){
        intent.putExtra("userModel", model);
    }

    public static UserModel getUserModelFromIntent(Intent intent){
        return intent.getParcelableExtra("userModel");
    }
    public static void setProfilePic(Context context, Uri imageUri, ImageView imageView){
        Glide.with(context).load(imageUri).apply(RequestOptions.circleCropTransform()).into(imageView);
    }
    public static void passUserModelAsJsonObject(JSONObject jsonObject, UserModel model) throws JSONException {
        if (model != null) {
            jsonObject.put("username", model.getUsername());
            jsonObject.put("phone", model.getPhone());
            jsonObject.put("userId", model.getUserId());
            jsonObject.put("fcmToken", model.getFcmToken());
            // Thêm profilePicUrl nếu bạn muốn gửi qua JSON
            if (model.getProfilePicUrl() != null) {
                jsonObject.put("profilePicUrl", model.getProfilePicUrl());
            }
        }
    }

    // Phương thức để lấy UserModel từ JSONObject (cho khi nhận notification)
    public static UserModel getUserModelFromJsonObject(JSONObject jsonObject) {
        UserModel userModel = new UserModel();
        try {
            if (jsonObject.has("username")) userModel.setUsername(jsonObject.getString("username"));
            if (jsonObject.has("phone")) userModel.setPhone(jsonObject.getString("phone"));
            if (jsonObject.has("userId")) userModel.setUserId(jsonObject.getString("userId"));
            if (jsonObject.has("fcmToken")) userModel.setFcmToken(jsonObject.getString("fcmToken"));
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return userModel;
    }
}
