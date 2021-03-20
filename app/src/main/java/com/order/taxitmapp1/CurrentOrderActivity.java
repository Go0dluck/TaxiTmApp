package com.order.taxitmapp1;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class CurrentOrderActivity extends AppCompatActivity {
    private ScheduledFuture<?> result;
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    public static final String CHANNEL_ID = "TMTAXICHANNELID"; // ид канала уведомлений

    private MapView mapView;
    private String server, apiKey, order_id, params, url, hashApiKey, abortedStateId, callKey, callServer;
    private SharedPreferences.Editor editor;

    private ProgressBar progressBar;
    private TextView infoTextView, sumTextView;
    private FloatingActionButton cancelButton, callButton;

    private BoundingBox boundingBox;
    private CameraPosition cameraPosition;
    private PlacemarkMapObject markMe, markDriver;
    private Point pointMe, pointDriver;

    private GetRequest getRequest;
    private PostRequest postRequest;
    private PostRequestXML postRequestXML;
    private GetRequestXML getRequestXML;

    private Boolean getDriverOrder = false, notificationPassangerGetDriver = false, notificationPassangerMoveDriver = false;

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
        cancelButton = findViewById(R.id.cancelButton);
        callButton = findViewById(R.id.callDriverButton);
        sumTextView = findViewById(R.id.sumTextView);

        // создаем маркер с точкой подачи отключаем все действия с картой
        pointMe = new Point(55.371157, 52.735562);
        mapView = findViewById(R.id.mapview);
        mapView.getMap().setScrollGesturesEnabled(false);
        mapView.getMap().setZoomGesturesEnabled(false);
        mapView.getMap().setTiltGesturesEnabled(false);
        mapView.getMap().setRotateGesturesEnabled(false);
        mapView.getMap().move(new CameraPosition(pointMe, 14.0f, 0.0f, 0.0f),new Animation(Animation.Type.SMOOTH, 1),null);
        markMe = mapView.getMap().getMapObjects().addPlacemark(pointMe, ImageProvider.fromResource(this, R.drawable.marker)); // иконка маркера подачи

        driverImageProvider = ImageProvider.fromResource(this, R.drawable.driver); // иконка маркера водителя

        SettingsServer settingsServer = new SettingsServer();
        server = settingsServer.getServer();
        apiKey = settingsServer.getApiKey();
        abortedStateId = settingsServer.getAbortedStateId();
        callKey = settingsServer.getCallKey();
        callServer = settingsServer.getCallServer();

        createNotificationChannel();

        result = executorService.scheduleAtFixedRate(new Runnable() { // каждые 5 сек обновляем инфу о заказе и автомобиле
            @Override
            public void run() {
                getInfoOrderId(server, apiKey, order_id);
            }
        }, 1, 5, TimeUnit.SECONDS);


    }
    // создаем канал уведомлений для android 8 и выше
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "TaxiTM";
            String description = "TaxiTMdescription";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(channel);
        }
    }
    // отключаем кнопку назад
    @Override
    public void onBackPressed() { }

    private void getInfoOrderId(String server, String apiKey, String order_id) {
        getInfoSum();
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
                        callNotification("Заказ успешно завершен", "Благодарим Вас за заказ, ждём Вас снова!");
                        result.cancel(true);
                        finish();
                        //ТУТ НАДО ПРЕДУМАТЬ КАК УВЕДОМЛЯТЬ О УСПЕШНОМ ИЛИ ОТМЕНЕНОМ ЗАКАЗЕ
                    } else if (mainObject.getString("state_kind").equals("aborted")){
                        editor.remove("ORDER_ID");
                        editor.apply();
                        callNotification("Заказ отменен", "К сожалению Ваш заказ отменен, ждём Вас снова!");
                        result.cancel(true);
                        finish();
                    } else if (mainObject.getString("state_kind").equals("new_order")
                            || (mainObject.getString("state_kind").equals("driver_assigned") & mainObject.getString("confirmed").equals("not_confirmed"))){
                        progressBar.setVisibility(View.VISIBLE);
                        infoTextView.setText("Идет поиск автомобиля...");
                        callButton.setVisibility(View.GONE);
                        notificationPassangerGetDriver = false;
                        notificationPassangerMoveDriver = false;
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
                        callButton.setVisibility(View.VISIBLE);
                        infoTextView.setText("К вам подъедет: \n" + "Автомобиль: " + mainObject.getString("car_mark")  + " " + mainObject.getString("car_model") + "\n" +
                                "Цвет: " + mainObject.getString("car_color") + "\n" + "Гос номер: " + mainObject.getString("car_number"));
                        if(mainObject.has("crew_coords")){
                            setMarkerDriver(mainObject.getJSONObject("crew_coords").getDouble("lat"), mainObject.getJSONObject("crew_coords").getDouble("lon"));
                        }
                        callNotificationGetDriver("Вам назначен автомобиль", "К вам подъедет: \n" + "Автомобиль: " + mainObject.getString("car_mark")  + " " + mainObject.getString("car_model") + "\n" +
                                "Цвет: " + mainObject.getString("car_color") + "\n" + "Гос номер: " + mainObject.getString("car_number"));
                        notificationPassangerGetDriver = true;

                    } else if (mainObject.getString("state_kind").equals("car_at_place") || mainObject.getString("state_kind").equals("client_inside")){
                        progressBar.setVisibility(View.GONE);
                        callButton.setVisibility(View.VISIBLE);
                        infoTextView.setText("Вас ожидает: \n" + "Автомобиль: " + mainObject.getString("car_mark")  + " " + mainObject.getString("car_model") + "\n" +
                                "Цвет: " + mainObject.getString("car_color") + "\n" + "Гос номер: " + mainObject.getString("car_number"));
                        if(mainObject.has("crew_coords")){
                            setMarkerDriver(mainObject.getJSONObject("crew_coords").getDouble("lat"), mainObject.getJSONObject("crew_coords").getDouble("lon"));
                        }
                        callNotificationMoveDriver("Автомобиль на месте", "Вас ожидает: \n" + "Автомобиль: " + mainObject.getString("car_mark")  + " " + mainObject.getString("car_model") + "\n" +
                                "Цвет: " + mainObject.getString("car_color") + "\n" + "Гос номер: " + mainObject.getString("car_number"));
                        notificationPassangerMoveDriver = true;
                    }
                } else {
                    editor.remove("ORDER_ID");
                    editor.apply();
                    startActivity(new Intent(CurrentOrderActivity.this, OrderActivity.class));
                }
            }
        });
        }
    // получаем инфу о сумме заказа и вносим ее в текстовое поле
    private void getInfoSum() {
        params = "order_id=" + order_id + "&fields=DISCOUNTEDSUMM";
        Md5Hash md5Hash = new Md5Hash();
        hashApiKey = md5Hash.md5(params + callKey);
        params = "order_id=" + order_id + "&fields=DISCOUNTEDSUMM&signature=" + hashApiKey;
        url = callServer + "get_info_by_order_id?" + params;
        getRequestXML = new GetRequestXML(CurrentOrderActivity.this,url,params, hashApiKey);
        getRequestXML.getString(new GetRequestXML.VolleyCallback() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onSuccess(String req, String data){
                if (req.equals("OK")){
                    sumTextView.setText("Сумма заказа " + data + "р.");
                }
            }
        });
    }
    // тут рисуем маркер по координатам водителя и зумим карту в зависиммости от его росстаяния
    private void setMarkerDriver(double lat, double lon) {
        pointDriver = new Point(lat, lon);
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
        mapView.onStop();
        MapKitFactory.getInstance().onStop();
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        MapKitFactory.getInstance().onStart();
        mapView.onStart();
    }
    // отмена заказа
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

    // звонок водителю
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
                        if(req.equals("OK")){
                            Toast.makeText(CurrentOrderActivity.this, "Ожидайте соединение с водителем", Toast.LENGTH_SHORT).show();
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
    // уведомление о назначении водителя
    public void callNotificationGetDriver(String title, String text){
        if (!notificationPassangerGetDriver){
            Intent intent = new Intent(this, CurrentOrderActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setShowWhen(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setVibrate(new long[] { 1000, 1000, 1000, 1000, 1000 })
                    .setAutoCancel(true)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(text));
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            notificationManager.notify(1, builder.build());
        }
    }
    // уведомление о том что водитель подъехал
    public void callNotificationMoveDriver(String title, String text){
        if (!notificationPassangerMoveDriver){
            Intent intent = new Intent(this, CurrentOrderActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setShowWhen(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setVibrate(new long[] { 1000, 1000, 1000, 1000, 1000 })
                    .setAutoCancel(true)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(text));
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            notificationManager.notify(2, builder.build());
        }
    }

    // уведомление о отмене заказа или успешном его завершении
    public void callNotification(String title, String text){
        Intent intent = new Intent(this, OrderActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(text)
                .setShowWhen(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[] { 1000, 1000, 1000, 1000, 1000 })
                .setAutoCancel(true)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text));
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(2, builder.build());
    }

}
