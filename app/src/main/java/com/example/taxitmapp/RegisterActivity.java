package com.example.taxitmapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputLayout;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {

    private SharedPreferences.Editor editor;
    private Button registerButton;
    private TextInputLayout textInputPhone, textInputCod;
    private TextView phoneTextView, codTextView;
    private String server;
    private String apiKey;
    private int randomCod;
    private boolean sendCod = true;

    @SuppressLint("CommitPrefEdits")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        registerButton = findViewById(R.id.registerButton);
        textInputPhone = findViewById(R.id.textInputPhone);
        textInputCod = findViewById(R.id.textInputCod);
        phoneTextView = findViewById(R.id.phoneTextView);
        codTextView = findViewById(R.id.codTextView);
        // Создаем файл для сохранения телефона в приложении
        SharedPreferences sharedPreferences = this.getSharedPreferences("mySharedPrefereces", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        // Получаем адрес сервера и АПИ ключ
        SettingsServer settingsServer = new SettingsServer();
        server = settingsServer.getServer();
        apiKey = settingsServer.getApiKey();
        // Генерируем рандомный код из 3 цифр от 100 до 999
        randomCod = (int)(100 + Math.random()*900);
        // Отменяем проверку сертификата

    }

    public void inputCod(View view) throws UnsupportedEncodingException {
        // Отправляем код на номер телефона
        if(sendCod){
            String params = "message=" + URLEncoder.encode("Ваш код: ", "UTF-8") + randomCod + "&phone=" + Objects.requireNonNull(textInputPhone.getEditText()).getText().toString().trim();
            String url = server + "send_sms?" + params;
            // Хэшируем ключ и параметры
            Md5Hash md5Hash = new Md5Hash();
            apiKey = md5Hash.md5(params + apiKey);
            // Делаем запрос в апи на создание смс
            PostRequest getPay = new PostRequest(this, url, params, apiKey);
            getPay.getString(new PostRequest.VolleyCallback() {
                @Override
                public void onSuccess(String req, String jsonArray) {
                    if(req.equals("OK")){
                        textInputPhone.setVisibility(View.GONE);
                        phoneTextView.setVisibility(View.GONE);
                        textInputCod.setVisibility(View.VISIBLE);
                        codTextView.setVisibility(View.VISIBLE);
                        registerButton.setText("Проверить код");
                        sendCod = false;
                    } else {
                        Toast.makeText(RegisterActivity.this, "Номер введен не верно", Toast.LENGTH_LONG).show();
                    }

                }
            });
        } else {
            // Если ввели правильный код из смс от сохраняем данные телефона в файл и переходим на активити заказа иначе тост что код введен не верно
            if(Objects.requireNonNull(textInputCod.getEditText()).getText().toString().trim().equals(Integer.toString(randomCod))){
                editor.putString("phone", Objects.requireNonNull(textInputPhone.getEditText()).getText().toString().trim());
                editor.apply();
                startActivity(new Intent(RegisterActivity.this, OrderActivity.class));
            } else {
                Toast.makeText(this, "Код введен не верно", Toast.LENGTH_LONG).show();
            }
        }
    }
}