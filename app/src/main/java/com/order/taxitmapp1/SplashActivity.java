package com.order.taxitmapp1;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE); // удаляем все уведомления которые висят на телефоне
        assert notificationManager != null;
        notificationManager.cancelAll();
        final SharedPreferences sharedPreferences = this.getSharedPreferences("mySharedPrefereces", Context.MODE_PRIVATE);
        SettingsServer settingsServer = new SettingsServer();
        String server = settingsServer.getServer();

        String url = server + "ping";
        GetRequest getRequest = new GetRequest(SplashActivity.this, url, null, null);
        getRequest.getString(new GetRequest.VolleyCallback() {
            @Override
            public void onSuccess(String req, String jsonArray) {
                /* Открываем стартовое окно приложения через 4 сек проверяем регистрировался челоке в приложении или нет
        если нет отправляем на окно регистрации если да отправляем на окно создания заказа
        * */
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
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }

}
