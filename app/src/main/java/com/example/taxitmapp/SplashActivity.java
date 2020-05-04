package com.example.taxitmapp;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        handleSSLHandshake();
        /* Открываем стартовое окно приложения через 4 сек проверяем регистрировался челоке в приложении или нет
        если нет отправляем на окно регистрации если да отправляем на окно создания заказа
        * */
        final SharedPreferences sharedPreferences = this.getSharedPreferences("mySharedPrefereces", Context.MODE_PRIVATE);
        Thread thread = new Thread(){
            @Override
            public void run() {
                try {
                    sleep(4000);
                } catch (Exception e){
                    e.printStackTrace();
                }finally {
                    if(!sharedPreferences.getString("phone", "").isEmpty() & !sharedPreferences.getString("ORDER_ID", "").isEmpty()){
                        startActivity(new Intent(SplashActivity.this, CurrentOrderActivity.class));
                    } else if (!sharedPreferences.getString("phone", "").isEmpty()){
                        startActivity(new Intent(SplashActivity.this, OrderActivity.class));
                    } else {
                        startActivity(new Intent(SplashActivity.this, RegisterActivity.class));
                    }
                }
            }
        };
        thread.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }

    /**
     * Отключаем проверку сертефиката как это работает я без понятия просто копипаст со стаковерфлоу
     */
    @SuppressLint("TrulyRandom")
    public static void handleSSLHandshake() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                @SuppressLint("TrustAllX509TrustManager")
                @Override
                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                @SuppressLint("TrustAllX509TrustManager")
                @Override
                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }};

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                @SuppressLint("BadHostnameVerifier")
                @Override
                public boolean verify(String arg0, SSLSession arg1) {
                    return true;
                }
            });
        } catch (Exception ignored) {
        }
    }
}
