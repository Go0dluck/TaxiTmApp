package com.example.taxitmapp;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.yandex.mapkit.Animation;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.BoundingBox;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.runtime.image.ImageProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class CurrentOrderActivity extends AppCompatActivity {
    private MapView mapView;
    private String server, apiKey, order_id, params, url, hashApiKey, abortedStateId, callKey, callServer;
    private SharedPreferences.Editor editor;

    private ProgressBar progressBar;
    private TextView infoTextView;

    private BoundingBox boundingBox;
    private CameraPosition cameraPosition;
    private PlacemarkMapObject markMe, markDriver;
    private Point pointMe, pointDriver;

    private GetRequest getRequest;
    private PostRequest postRequest;
    private PostRequestXML postRequestXML;

    private Boolean getDriverOrder = false, notificationGetDriver = false, notificationMoveDriver = false;
    private ImageProvider driverImageProvider;

    @SuppressLint("CommitPrefEdits")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String MAPKIT_API_KEY = "331a7ae7-9f07-4053-8010-41cfb9f289c0";
        MapKitFactory.setApiKey(MAPKIT_API_KEY);
        MapKitFactory.initialize(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_current_order);

        SharedPreferences sharedPreferences = this.getSharedPreferences("mySharedPrefereces", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        order_id = sharedPreferences.getString("ORDER_ID", "");

        progressBar = findViewById(R.id.progressBar);
        infoTextView = findViewById(R.id.infoTextView);
        ImageButton cancelButton = findViewById(R.id.cancelButton);

        pointMe = new Point(55.768351, 49.153199);

        mapView = findViewById(R.id.mapview);
        mapView.getMap().setScrollGesturesEnabled(false);
        mapView.getMap().setZoomGesturesEnabled(false);
        mapView.getMap().setTiltGesturesEnabled(false);
        mapView.getMap().setRotateGesturesEnabled(false);

        mapView.getMap().move(new CameraPosition(pointMe, 14.0f, 0.0f, 0.0f),new Animation(Animation.Type.SMOOTH, 1),null);
        markMe = mapView.getMap().getMapObjects().addPlacemark(pointMe, ImageProvider.fromResource(this, R.drawable.marker));

        driverImageProvider = ImageProvider.fromResource(this, R.drawable.driver);

        SettingsServer settingsServer = new SettingsServer();
        server = settingsServer.getServer();
        apiKey = settingsServer.getApiKey();
        abortedStateId = settingsServer.getAbortedStateId();
        callKey = settingsServer.getCallKey();
        callServer = settingsServer.getCallServer();



        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                getInfoOrderId(server, apiKey, order_id);
            }
        }, 1, 5, TimeUnit.SECONDS);

    }

    @Override
    public void onBackPressed() { }

    private void getInfoOrderId(String server, String apiKey, String order_id) {
        params = "order_id=" + order_id;
        url = server + "get_order_state?" + params;
        Md5Hash md5Hash = new Md5Hash();
        hashApiKey = md5Hash.md5(params + apiKey);
        getRequest = new GetRequest(this,url,params, hashApiKey);
        getRequest.getString(new GetRequest.VolleyCallback() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onSuccess(String req, String jsonArray) throws JSONException {
                if (req.equals("OK")) {
                    JSONObject mainObject = new JSONObject(jsonArray);
                        //ТУТ НАДО ПРЕДУМАТЬ КАК УВЕДОМЛЯТЬ О УСПЕШНОМ ИЛИ ОТМЕНЕНОМ ЗАКАЗЕ
                    if (mainObject.getString("state_kind").equals("finished")){
                        editor.remove("ORDER_ID");
                        editor.apply();
                        finish();
                        //ТУТ НАДО ПРЕДУМАТЬ КАК УВЕДОМЛЯТЬ О УСПЕШНОМ ИЛИ ОТМЕНЕНОМ ЗАКАЗЕ
                    } else if (mainObject.getString("state_kind").equals("aborted")){
                        editor.remove("ORDER_ID");
                        editor.apply();
                        finish();
                    } else if (mainObject.getString("state_kind").equals("new_order")
                            || (mainObject.getString("state_kind").equals("driver_assigned") & mainObject.getString("confirmed").equals("not_confirmed"))){
                        progressBar.setVisibility(View.VISIBLE);
                        infoTextView.setText("Идет поиск автомобиля...");
                        if (notificationGetDriver){
                            notificationGetDriver = false;
                        }
                        if (notificationMoveDriver){
                            notificationMoveDriver = false;
                        }
                        if (getDriverOrder){
                            mapView.getMap().move(new CameraPosition(pointMe, 14.0f, 0.0f, 0.0f),new Animation(Animation.Type.SMOOTH, 1),null);
                            try {
                                mapView.getMap().getMapObjects().remove(markDriver);
                            } catch (Exception ignored){

                            }
                            getDriverOrder = false;
                        }

                    } else if (mainObject.getString("state_kind").equals("driver_assigned") & !mainObject.getString("confirmed").equals("not_confirmed")){
                        progressBar.setVisibility(View.GONE);
                        infoTextView.setText("К вам подъедет: \n" + "Автомобиль: " + mainObject.getString("car_mark")  + " " + mainObject.getString("car_model") + "\n" +
                                "Цвет: " + mainObject.getString("car_color") + "\n" + "Гос номер: " + mainObject.getString("car_number"));
                        if(mainObject.has("crew_coords")){
                            setMarkerDriver(mainObject.getJSONObject("crew_coords").getDouble("lat"), mainObject.getJSONObject("crew_coords").getDouble("lon"));
                        }
                        callNotification("Водитель назначен", "Автомобиль: " + mainObject.getString("car_mark")  + " " + mainObject.getString("car_model") + "\n" +
                                "Цвет: " + mainObject.getString("car_color") + "\n" + "Гос номер: " + mainObject.getString("car_number"));

                    } else if (mainObject.getString("state_kind").equals("car_at_place") || mainObject.getString("state_kind").equals("client_inside")){
                        progressBar.setVisibility(View.GONE);
                        infoTextView.setText("Вас ожидает: \n" + "Автомобиль: " + mainObject.getString("car_mark")  + " " + mainObject.getString("car_model") + "\n" +
                                "Цвет: " + mainObject.getString("car_color") + "\n" + "Гос номер: " + mainObject.getString("car_number"));
                        if(mainObject.has("crew_coords")){
                            setMarkerDriver(mainObject.getJSONObject("crew_coords").getDouble("lat"), mainObject.getJSONObject("crew_coords").getDouble("lon"));
                        }
                        callNotificationMove("Водитель ожидает", "Автомобиль: " + mainObject.getString("car_mark")  + " " + mainObject.getString("car_model") + "\n" +
                                "Цвет: " + mainObject.getString("car_color") + "\n" + "Гос номер: " + mainObject.getString("car_number"));
                    }
                } else {
                    editor.remove("ORDER_ID");
                    editor.apply();
                    startActivity(new Intent(CurrentOrderActivity.this, OrderActivity.class));
                }
            }
        });
        }

    private void setMarkerDriver(double lat, double lon) {
        pointDriver = new Point(lat, lon);

        ///////////это надо переделать/////////
        if(getDriverOrder){
            try {
                mapView.getMap().getMapObjects().remove(markDriver);
            } catch (Exception ignored){

            }
        }

        markDriver = mapView.getMap().getMapObjects().addPlacemark(pointDriver, driverImageProvider);
        boundingBox = new BoundingBox(pointMe, pointDriver);
        cameraPosition = mapView.getMap().cameraPosition(boundingBox);
        mapView.getMap().move(new CameraPosition(cameraPosition.getTarget(), cameraPosition.getZoom() - 0.8f, cameraPosition.getAzimuth(), cameraPosition.getTilt()),
                new Animation(Animation.Type.SMOOTH, 0f), null);
        getDriverOrder = true;

    }

    @Override
    protected void onStop() {
        // Вызов onStop нужно передавать инстансам MapView и MapKit.
        mapView.onStop();
        MapKitFactory.getInstance().onStop();
        super.onStop();
    }

    @Override
    protected void onStart() {
        // Вызов onStart нужно передавать инстансам MapView и MapKit.
        super.onStart();
        MapKitFactory.getInstance().onStart();
        mapView.onStart();
    }

    public void abortedOrder(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle("Предупреждение !");
        builder.setMessage("Вы действительно хотите отказаться от заказа ?");
        builder.setPositiveButton("Да !", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                params = "order_id=" + order_id + "&new_state=" + abortedStateId;
                url = server + "change_order_state?" + params;
                Md5Hash md5Hash = new Md5Hash();
                hashApiKey = md5Hash.md5(params + apiKey);
                postRequest = new PostRequest(CurrentOrderActivity.this,url,params, hashApiKey);
                postRequest.getString(new PostRequest.VolleyCallback() {
                    @Override
                    public void onSuccess(String req, String jsonArray){
                        Log.d("HELLO", req);
                        if (req.equals("OK")){
                            editor.remove("ORDER_ID");
                            editor.apply();
                            startActivity(new Intent(CurrentOrderActivity.this, OrderActivity.class));
                        } else {
                            Toast.makeText(CurrentOrderActivity.this, "Не удалось отменить заказ", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

            }
        }).setNegativeButton("НЕТ !", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.create().show();
    }


    public void callDriver(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle("Предупреждение !");
        builder.setMessage("Соединить Вас с водителем ?");
        builder.setPositiveButton("Да !", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                params = "order_id=" + order_id;
                Md5Hash md5Hash = new Md5Hash();
                hashApiKey = md5Hash.md5(params + callKey);
                params = "order_id=" + order_id + "&signature=" + hashApiKey;
                url = callServer + "connect_client_and_driver?" + params;
                postRequestXML = new PostRequestXML(CurrentOrderActivity.this,url,params, hashApiKey);
                postRequestXML.getString(new PostRequestXML.VolleyCallback() {
                    @Override
                    public void onSuccess(String req){
                        Log.d("RESPONSE", req);
                    }
                });

            }
        }).setNegativeButton("НЕТ !", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.create().show();

    }

    public void callNotification(String title, String text){
        if(!notificationGetDriver){
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setShowWhen(true)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(text));
            Notification notification = builder.build();
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            assert notificationManager != null;
            notificationManager.notify(1, notification);
            notificationGetDriver = true;
        }
    }

    public void callNotificationMove(String title, String text){
        if(!notificationMoveDriver){
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setShowWhen(true)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(text));
            Notification notification = builder.build();
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            assert notificationManager != null;
            notificationManager.notify(2, notification);
            notificationMoveDriver = true;
        }
    }
}
