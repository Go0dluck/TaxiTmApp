package com.example.taxitmapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.material.textfield.TextInputLayout;
import com.yandex.mapkit.Animation;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.location.FilteringMode;
import com.yandex.mapkit.location.Location;
import com.yandex.mapkit.location.LocationListener;
import com.yandex.mapkit.location.LocationManager;
import com.yandex.mapkit.location.LocationStatus;
import com.yandex.mapkit.map.CameraListener;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.CameraUpdateSource;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.mapkit.search.Response;
import com.yandex.mapkit.search.SearchManager;
import com.yandex.mapkit.search.SearchManagerType;
import com.yandex.mapkit.search.SearchOptions;
import com.yandex.mapkit.search.SearchType;
import com.yandex.mapkit.search.Session;
import com.yandex.runtime.Error;
import com.yandex.mapkit.search.SearchFactory;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;

public class OrderActivity extends AppCompatActivity implements CameraListener {
    private MapView mapView;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private ProgressBar searchProgressBar;
    private TextInputLayout sourceTextLayout;
    private TextInputLayout destTextLayout, podTextLayout, commentTextLayout;
    private String server;
    private String apiKey;
    private String url;
    private String phone;
    private String hashApiKey;
    private String sourceZoneId;
    private String destZoneId;
    private String cityDist;
    private String countryDist;
    private String sourceCountryDist, params, timeNow;
    private String crewGroupId;
    private Double my_lat, my_lon;
    private TextView summTextView;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private GetRequest getRequest;
    private PostRequestParams postRequest;
    private PostRequest postPay;

    long latest = 0;
    long delay = 2000;

    private SearchManager searchManager;

    private static final double DESIRED_ACCURACY = 0;
    private static final long MINIMAL_TIME = 0;
    private static final double MINIMAL_DISTANCE = 50;
    private static final boolean USE_IN_BACKGROUND = false;
    public static final int COMFORTABLE_ZOOM_LEVEL = 18;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String MAPKIT_API_KEY = "331a7ae7-9f07-4053-8010-41cfb9f289c0";
        MapKitFactory.setApiKey(MAPKIT_API_KEY);
        MapKitFactory.initialize(this);
        SearchFactory.initialize(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);

        searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED);

        mapView = findViewById(R.id.mapview);
        mapView.getMap().setTiltGesturesEnabled(false);
        mapView.getMap().setRotateGesturesEnabled(false);
        mapView.getMap().move(new CameraPosition(new Point(55.75370903771494, 37.61981338262558), 18, 0, 0));
        //mapView.getMap().move(new CameraPosition(new Point(55.751574, 37.573856), 18.0f, 0.0f, 0.0f), new Animation(Animation.Type.SMOOTH, 5), null);
        mapView.getMap().addCameraListener(this);

        locationManager = MapKitFactory.getInstance().createLocationManager();
        locationListener = new LocationListener() {
            @Override
            public void onLocationUpdated(@NonNull Location location) {
                my_lat = location.getPosition().getLatitude();
                my_lon = location.getPosition().getLongitude();
                mapView.getMap().move(
                        new CameraPosition(new Point(my_lat, my_lon), 18.0f, 0.0f, 0.0f),
                        new Animation(Animation.Type.SMOOTH, 1),
                        null);
            }

            @Override
            public void onLocationStatusUpdated(@NonNull LocationStatus locationStatus) {
            }
        };
        subscribeToLocationUpdate();

        sourceTextLayout = findViewById(R.id.sourceTextLayout);
        podTextLayout = findViewById(R.id.podTextLayout);
        destTextLayout = findViewById(R.id.destTextLayout);
        commentTextLayout = findViewById(R.id.commentTextLayout);
        Button createOrderButton = findViewById(R.id.createOrderButton);
        summTextView = findViewById(R.id.summTextView);
        searchProgressBar = findViewById(R.id.searchProgressBar);

        // Получаем адрес сервера и АПИ ключ и группу экипажей
        SettingsServer settingsServer = new SettingsServer();
        server = settingsServer.getServer();
        apiKey = settingsServer.getApiKey();
        crewGroupId = settingsServer.getCrewGroupId();

        final Intent sourceIntent = new Intent(OrderActivity.this, SearchAddressActivity.class);

        sharedPreferences = this.getSharedPreferences("mySharedPrefereces", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        editor.remove("dest_address");
        editor.remove("source_address");
        editor.remove("source_address_lat");
        editor.remove("source_address_lon");
        editor.remove("dest_address_lat");
        editor.remove("dest_address_lon");
        editor.apply();

        phone = sharedPreferences.getString("phone", "");


        Objects.requireNonNull(sourceTextLayout.getEditText()).setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    sourceIntent.putExtra("getEditText", "source");
                    startActivity(sourceIntent);
                }
            }
        });

        Objects.requireNonNull(destTextLayout.getEditText()).setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    sourceIntent.putExtra("getEditText", "dest");
                    startActivity(sourceIntent);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Objects.requireNonNull(sourceTextLayout.getEditText()).setText(sharedPreferences.getString("source_address", ""));
        Objects.requireNonNull(destTextLayout.getEditText()).setText(sharedPreferences.getString("dest_address", ""));
        sourceTextLayout.getEditText().clearFocus();
        destTextLayout.getEditText().clearFocus();
        try {
            analyzeRoute();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }

    //Анализируем маршрут
    private void analyzeRoute() throws UnsupportedEncodingException {
        if(!sharedPreferences.getString("source_address", "").isEmpty() & !sharedPreferences.getString("dest_address", "").isEmpty()) {
            String params = "source=" + URLEncoder.encode(sharedPreferences.getString("source_address", ""), "UTF-8") +
                    "&dest=" + URLEncoder.encode(sharedPreferences.getString("dest_address", ""), "UTF-8") +
                    "&source_lon=" + sharedPreferences.getString("source_address_lon", "") +
                    "&source_lat=" + sharedPreferences.getString("source_address_lat", "") +
                    "&dest_lon=" + sharedPreferences.getString("dest_address_lon", "") +
                    "&dest_lat=" + sharedPreferences.getString("dest_address_lat", "");
            url = server + "analyze_route?" + params;
            Md5Hash md5Hash = new Md5Hash();
            hashApiKey = md5Hash.md5(params + apiKey);
            getRequest = new GetRequest(this, url, params, hashApiKey);
            getRequest.getString(new GetRequest.VolleyCallback() {
                @Override
                public void onSuccess(String req, String jsonArray) throws JSONException {
                    if (req.equals("OK")) {
                        JSONObject mainObject = new JSONObject(jsonArray);
                        sourceZoneId = mainObject.getString("source_zone_id");
                        destZoneId = mainObject.getString("dest_zone_id");
                        cityDist = mainObject.getString("city_dist");
                        countryDist = mainObject.getString("country_dist");
                        sourceCountryDist = mainObject.getString("source_country_dist");
                        calcOrderCost(sourceZoneId, destZoneId, cityDist, countryDist, sourceCountryDist);
                    } else {
                        Toast.makeText(OrderActivity.this, "Анализ маршрута не получен", Toast.LENGTH_SHORT).show();
                        summTextView.setText("Предварительная стоимость: 0р");
                    }
                }
            });
        }
    }
    //Расчитываем стоиммость заказа
    private void calcOrderCost(String sourceZoneId, String destZoneId, String cityDist, String countryDist, String sourceCountryDist) {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        String timeNow = format.format(new Date());
        HashMap<String, java.io.Serializable> params = new HashMap<>();
        params.put("source_time", timeNow);
        params.put("phone", phone);
        params.put("source_zone_id", Integer.valueOf(sourceZoneId));
        params.put("dest_zone_id", Integer.valueOf(destZoneId));
        params.put("distance_city", Double.valueOf(cityDist));
        params.put("distance_country", Double.valueOf(countryDist));
        params.put("source_distance_country", Double.valueOf(sourceCountryDist));
        params.put("is_country", true);
        params.put("crew_group_id", Integer.valueOf(crewGroupId));

        JSONObject parameters = new JSONObject(params);

        url = server + "calc_order_cost2";
        Md5Hash md5Hash = new Md5Hash();
        hashApiKey = md5Hash.md5(parameters + apiKey);
        postRequest = new PostRequestParams(this, url, params, hashApiKey);
        postRequest.getString(new PostRequestParams.VolleyCallback() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onSuccess(String req, String jsonArray) throws JSONException {
                if (req.equals("OK")){
                    JSONObject mainObject = new JSONObject(jsonArray);
                    summTextView.setText("Предварительная стоимость: " + mainObject.getString("sum") + "р");
                } else {
                    Toast.makeText(OrderActivity.this, "Не удалось расчитать стоимость", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onStop() {
        // Вызов onStop нужно передавать инстансам MapView и MapKit.
        mapView.onStop();
        MapKitFactory.getInstance().onStop();
        locationManager.unsubscribe(locationListener);
        super.onStop();
    }

    @Override
    protected void onStart() {
        // Вызов onStart нужно передавать инстансам MapView и MapKit.
        super.onStart();
        MapKitFactory.getInstance().onStart();
        mapView.onStart();

    }

    @Override
    public void onBackPressed() {
    }

    private void subscribeToLocationUpdate() {
        if (locationManager != null && locationListener != null) {
            locationManager.subscribeForLocationUpdates(DESIRED_ACCURACY, MINIMAL_TIME, MINIMAL_DISTANCE, USE_IN_BACKGROUND, FilteringMode.OFF, locationListener);
        }
    }

    ///Создаем заказ
    public void createOrder(View view) throws UnsupportedEncodingException {

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("ORDER_ID", "11083370");
        editor.apply();
        startActivity(new Intent(OrderActivity.this, CurrentOrderActivity.class));
        /*@SuppressLint("SimpleDateFormat") SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        timeNow = format.format(new Date());
        String source = URLEncoder.encode(Objects.requireNonNull(sourceTextLayout.getEditText()).getText().toString().trim(), "UTF-8") + "* " + URLEncoder.encode(Objects.requireNonNull(podTextLayout.getEditText()).getText().toString().trim(), "UTF-8");
        String dest = URLEncoder.encode(Objects.requireNonNull(destTextLayout.getEditText()).getText().toString().trim(), "UTF-8");
        String comment = URLEncoder.encode(Objects.requireNonNull(commentTextLayout.getEditText()).getText().toString().trim(), "UTF-8");
        if(source.isEmpty()){
            Toast.makeText(this, "Введите адрес подачи", Toast.LENGTH_SHORT).show();
        } else if(dest.isEmpty()){
            Toast.makeText(this, "Введите адрес назначения", Toast.LENGTH_SHORT).show();
        } else {
            params = "phone=" + phone + "&source=" + source + "&dest=" + dest + "&source_time=" + timeNow + "&comment=" + comment;
            url = server + "create_order?" + params;
            Md5Hash md5Hash = new Md5Hash();
            hashApiKey = md5Hash.md5(params + apiKey);
            postPay = new PostRequest(this,url,params, hashApiKey);
            postPay.getString(new PostRequest.VolleyCallback() {
                @Override
                public void onSuccess(String req, String jsonArray) throws JSONException {
                    if(req.equals("OK")){
                        JSONObject mainObject = new JSONObject(jsonArray);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("ORDER_ID", mainObject.getString("order_id"));
                        editor.apply();
                        Log.d("ORDER_ID", mainObject.getString("order_id"));
                        startActivity(new Intent(OrderActivity.this, CurrentOrderActivity.class));
                    } else {
                        Toast.makeText(OrderActivity.this, "Заказ не создан", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }*/
    }


    @Override
    public void onCameraPositionChanged(@NonNull Map map, @NonNull CameraPosition cameraPosition, @NonNull CameraUpdateSource cameraUpdateSource, boolean b) {
        if(b){
            searchProgressBar.setVisibility(View.VISIBLE);
            latest = System.currentTimeMillis(); //обновляем время последнего изменения текста
            Handler h = new Handler(Looper.getMainLooper());
            Runnable r = new Runnable() {
                @Override
                public void run() {

                    if(System.currentTimeMillis() - delay > latest){
                        searchManager.submit(
                                new Point(mapView.getMap().getCameraPosition().getTarget().getLatitude(), mapView.getMap().getCameraPosition().getTarget().getLongitude()),
                                18,
                                new SearchOptions().setSearchTypes(SearchType.GEO.value),
                                new Session.SearchListener() {
                                    @Override
                                    public void onSearchResponse(@NonNull Response response) {
                                        Objects.requireNonNull(sourceTextLayout.getEditText()).setText(Objects.requireNonNull(response.getCollection().getChildren().get(0).getObj()).getName());
                                        editor.putString("source_address", Objects.requireNonNull(sourceTextLayout.getEditText()).getText().toString().trim());
                                        editor.putString("source_address_lat", String.valueOf(mapView.getMap().getCameraPosition().getTarget().getLatitude()));
                                        editor.putString("source_address_lon", String.valueOf(mapView.getMap().getCameraPosition().getTarget().getLongitude()));
                                        editor.apply();
                                        searchProgressBar.setVisibility(View.GONE);
                                        try {
                                            analyzeRoute();
                                        } catch (UnsupportedEncodingException e) {
                                            e.printStackTrace();
                                        }
                                    }

                                    @Override
                                    public void onSearchError(@NonNull Error error) {
                                        Toast.makeText(OrderActivity.this, "Нет данных", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }

                }
            };
            h.postDelayed(r, delay + 50);//в главный поток с задержкой delay + 50 миллисекунд
        }
    }

    public void searchMe(View view) {
        mapView.getMap().move(
                new CameraPosition(new Point(my_lat, my_lon), 18.0f, 0.0f, 0.0f),
                new Animation(Animation.Type.SMOOTH, 1),
                null);
    }
}



