package com.example.appchat.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.io.InputStream;

public class MediaStoreUtils {

    private static final String TAG = "MediaStoreUtils";

    /**
     * Lấy Bitmap từ Uri hình ảnh.
     * Phương pháp này sẽ thử tải ảnh với kích thước nhỏ hơn nếu cần để tránh OutOfMemoryError.
     * @param context Context của ứng dụng.
     * @param imageUri Uri của ảnh.
     * @return Bitmap của ảnh hoặc null nếu không thể tải.
     */
    public static Bitmap getBitmapFromUri(Context context, Uri imageUri) {
        InputStream inputStream = null;
        try {
            inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                Log.e(TAG, "Could not open input stream for URI: " + imageUri);
                return null;
            }

            // Lấy kích thước ảnh mà không tải vào bộ nhớ
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);

            // Đóng stream và mở lại để decode lại
            inputStream.close();
            inputStream = context.getContentResolver().openInputStream(imageUri);

            // Tính toán inSampleSize để scale ảnh xuống nếu cần
            final int REQUIRED_SIZE = 1024; // Kích thước mong muốn (pixel)
            int width = options.outWidth;
            int height = options.outHeight;
            int inSampleSize = 1;

            if (height > REQUIRED_SIZE || width > REQUIRED_SIZE) {
                final int halfHeight = height / 2;
                final int halfWidth = width / 2;

                // Tính toán inSampleSize là lũy thừa của 2
                while ((halfHeight / inSampleSize) >= REQUIRED_SIZE && (halfWidth / inSampleSize) >= REQUIRED_SIZE) {
                    inSampleSize *= 2;
                }
            }

            options.inJustDecodeBounds = false;
            options.inSampleSize = inSampleSize;

            // Decode bitmap với inSampleSize đã tính toán
            return BitmapFactory.decodeStream(inputStream, null, options);

        } catch (Exception e) {
            Log.e(TAG, "Error loading bitmap from URI: " + imageUri, e);
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error closing input stream", e);
                }
            }
        }
    }
}
