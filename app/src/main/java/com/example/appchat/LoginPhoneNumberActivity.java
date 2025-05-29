package com.example.appchat;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.hbb20.CountryCodePicker;

public class LoginPhoneNumberActivity extends AppCompatActivity {
    private CountryCodePicker countryCodePicker;
    private EditText phoneInput;
    private Button sendOtpBtn;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_phone_number);

        // Ánh xạ view từ layout
        countryCodePicker = findViewById(R.id.login_countrycode);
        phoneInput = findViewById(R.id.login_mobile_number);
        sendOtpBtn = findViewById(R.id.send_otp_btn);
        progressBar = findViewById(R.id.login_progress_bar);

        progressBar.setVisibility(View.GONE);

        // Gắn EditText với CCP
        countryCodePicker.registerCarrierNumberEditText(phoneInput);

        sendOtpBtn.setOnClickListener(view -> {
            if (!countryCodePicker.isValidFullNumber()) {
                phoneInput.setError("Phone number not valid");
                return;
            }

            String fullPhoneNumber = countryCodePicker.getFullNumberWithPlus();

            // Hiển thị progress và vô hiệu hóa nút
            progressBar.setVisibility(View.VISIBLE);
            sendOtpBtn.setEnabled(false);

            // Chuyển sang màn hình OTP
            Intent intent = new Intent(LoginPhoneNumberActivity.this, LoginOTPActivity.class);
            intent.putExtra("phone", fullPhoneNumber);
            startActivity(intent);

            // Ẩn progress sau khi chuyển màn hình (tuỳ theo logic bạn có thể để bên LoginOtp xử lý lại sau)
            progressBar.setVisibility(View.GONE);
            sendOtpBtn.setEnabled(true);
        });
    }
}
