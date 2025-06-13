package com.example.appchat.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast; // Chỉ để debugging, không nên dùng trong lớp tiện ích thực tế

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImgBBUploader {

    // THAY THẾ 'YOUR_IMGBB_API_KEY' BẰNG API KEY THỰC TẾ CỦA BẠN TẠI IMGBB
    // ĐẢM BẢO KEY NÀY GIỐNG VỚI KEY BẠN DÙNG TRONG ChatActivity.java
    private static final String IMGBB_API_KEY = "154971ca0e3415a3155bab02d7160f3b"; // Dùng key bạn đã cung cấp
    private static final String IMGBB_UPLOAD_URL = "https://api.imgbb.com/1/upload";
    private static final String TAG = "ImgBBUploader";

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler handler = new Handler(Looper.getMainLooper());

    public interface UploadCallback {
        void onSuccess(String imageUrl);
        void onFailure(String errorMessage);
    }

    public static void uploadImage(Context context, Uri imageUri, UploadCallback callback) {
        Log.d(TAG, "Bắt đầu tải ảnh lên ImgBB cho URI: " + imageUri); // Start upload
        if (IMGBB_API_KEY.equals("YOUR_IMGBB_API_KEY")) {
            Log.e(TAG, "ImgBB API Key chưa được đặt!"); // API Key not set
            handler.post(() -> {
                Toast.makeText(context, "Lỗi: API Key ImgBB chưa được thiết lập.", Toast.LENGTH_LONG).show(); // Error: ImgBB API Key not set
                callback.onFailure("ImgBB API Key is not set. Please replace 'YOUR_IMGBB_API_KEY'.");
            });
            return;
        }

        executor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                Log.d(TAG, "Đang xử lý ảnh từ URI."); // Processing image from URI
                Bitmap bitmap = MediaStoreUtils.getBitmapFromUri(context, imageUri);
                if (bitmap == null) {
                    Log.e(TAG, "Không thể tải bitmap từ URI."); // Failed to load bitmap
                    handler.post(() -> callback.onFailure("Không thể tải ảnh từ URI."));
                    return;
                }
                Log.d(TAG, "Bitmap đã tải thành công. Kích thước: " + bitmap.getWidth() + "x" + bitmap.getHeight()); // Bitmap loaded. Size:

                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                // Nén ảnh để giảm kích thước và tối ưu việc tải lên
                // Điều chỉnh chất lượng nén (thường là 70-90)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
                byte[] byteArray = byteArrayOutputStream.toByteArray();
                String encodedImage = Base64.encodeToString(byteArray, Base64.DEFAULT);
                Log.d(TAG, "Ảnh đã được nén và mã hóa Base64. Kích thước byte: " + byteArray.length); // Image compressed and Base64 encoded. Byte size:

                // Chuẩn bị dữ liệu cho yêu cầu POST
                String postData = "key=" + IMGBB_API_KEY + "&image=" + URLEncoder.encode(encodedImage, "UTF-8");
                byte[] postDataBytes = postData.getBytes("UTF-8");
                Log.d(TAG, "Dữ liệu POST đã chuẩn bị. Kích thước: " + postDataBytes.length + " bytes."); // POST data prepared. Size:

                // Thiết lập kết nối HTTP
                URL url = new URL(IMGBB_UPLOAD_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setUseCaches(false);
                connection.setFixedLengthStreamingMode(postDataBytes.length);
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                // Thiết lập timeout
                connection.setConnectTimeout(15000); // 15 giây
                connection.setReadTimeout(15000); // 15 giây
                Log.d(TAG, "Đang kết nối đến ImgBB API."); // Connecting to ImgBB API

                // Gửi dữ liệu
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(postDataBytes);
                    Log.d(TAG, "Dữ liệu ảnh đã gửi."); // Image data sent.
                }

                // Nhận phản hồi
                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Mã phản hồi từ ImgBB: " + responseCode); // Response code from ImgBB:
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (InputStream is = connection.getInputStream()) {
                        ByteArrayOutputStream result = new ByteArrayOutputStream();
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = is.read(buffer)) != -1) {
                            result.write(buffer, 0, length);
                        }
                        String response = result.toString("UTF-8");
                        Log.d(TAG, "Phản hồi thành công từ ImgBB: " + response); // Successful response from ImgBB:

                        // Phân tích cú pháp phản hồi JSON
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.has("data") && jsonResponse.getJSONObject("data").has("url")) {
                            String imageUrl = jsonResponse.getJSONObject("data").getString("url");
                            handler.post(() -> {
                                Log.d(TAG, "Tải ảnh lên ImgBB thành công. URL: " + imageUrl); // ImgBB upload success. URL:
                                callback.onSuccess(imageUrl);
                            });
                        } else {
                            String errorMsg = "Phản hồi ImgBB không chứa URL ảnh: " + response; // ImgBB response does not contain image URL:
                            Log.e(TAG, errorMsg);
                            handler.post(() -> callback.onFailure(errorMsg));
                        }
                    }
                } else {
                    // Xử lý lỗi HTTP khác
                    try (InputStream is = connection.getErrorStream()) {
                        ByteArrayOutputStream result = new ByteArrayOutputStream();
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = is.read(buffer)) != -1) {
                            result.write(buffer, 0, length);
                        }
                        String errorResponse = result.toString("UTF-8");
                        String errorMsg = "Lỗi HTTP từ ImgBB: " + responseCode + " - " + errorResponse; // HTTP error from ImgBB:
                        Log.e(TAG, errorMsg);
                        handler.post(() -> callback.onFailure(errorMsg));
                    } catch (Exception streamEx) {
                        String errorMsg = "Không thể đọc stream lỗi từ ImgBB: " + streamEx.getMessage(); // Cannot read error stream from ImgBB:
                        Log.e(TAG, errorMsg, streamEx);
                        handler.post(() -> callback.onFailure(errorMsg));
                    }
                }

            } catch (Exception e) {
                // Xử lý các ngoại lệ chung (mạng, IO, JSON parsing)
                String errorMsg = "Lỗi tải ảnh lên ImgBB: " + e.getMessage(); // ImgBB upload exception:
                Log.e(TAG, errorMsg, e);
                handler.post(() -> callback.onFailure(errorMsg));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                    Log.d(TAG, "Kết nối ImgBB đã ngắt."); // ImgBB connection disconnected.
                }
            }
        });
    }
}
