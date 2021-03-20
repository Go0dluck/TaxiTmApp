package com.order.taxitmapp1;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputLayout;
import com.yandex.mapkit.Animation;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.BoundingBox;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.geometry.Polyline;
import com.yandex.mapkit.location.FilteringMode;
import com.yandex.mapkit.location.Location;
import com.yandex.mapkit.location.LocationListener;
import com.yandex.mapkit.location.LocationManager;
import com.yandex.mapkit.location.LocationStatus;
import com.yandex.mapkit.map.CameraListener;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.CameraUpdateSource;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.PolylineMapObject;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.mapkit.search.Response;
import com.yandex.mapkit.search.SearchFactory;
import com.yandex.mapkit.search.SearchManager;
import com.yandex.mapkit.search.SearchManagerType;
import com.yandex.mapkit.search.SearchOptions;
import com.yandex.mapkit.search.SearchType;
import com.yandex.mapkit.search.Session;
import com.yandex.runtime.Error;
import com.yandex.runtime.image.ImageProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;

public class OrderActivity extends AppCompatActivity implements CameraListener {
    private static final int REQUEST_CODE_PERMISSION_FINE_LOCATION = 777;
    private MapView mapView;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private ProgressBar searchProgressBar;
    private TextInputLayout sourceTextLayout;
    private TextInputLayout destTextLayout, commentTextLayout;
    private String server;
    private String apiKey;
    private String url;
    private String phone;
    private String hashApiKey;
    private String sourceZoneId;
    private String destZoneId;
    private String cityDist;
    private String countryDist;
    private String sourceCountryDist, params, nameParametr, idParametr;
    private String crewGroupId;
    private Double my_lat, my_lon;
    private TextView summTextView;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private GetRequest getRequest;
    private GetRequestXML getRequestXML;
    private PostRequestParams postRequest;
    private SettingsServer settingsServer;
    private Button createOrderButton;
    private String[] paramsOrder;
    private Double sumParametr, percentParametr;
    private ArrayList<String> parametrs;
    private ArrayList<Integer> wishes;
    private int summOrder;
    private String callKey, callServer;
    private int bonusBalanse = 0;
    private int summBonuse = 0;
    private ArrayList<Point> polylinePoints;
    private MapObjectCollection mapObjects;
    private PolylineMapObject polyline;
    private Boolean routingToMap = false;
    private BoundingBox boundingBox;
    private CameraPosition cameraSourceDestPoint;
    private ImageView markerImageView;

    long latest = 0;
    long delay = 2000; // задержка запросов в яндекс при перемещении камеры

    private SearchManager searchManager;

    private static final double DESIRED_ACCURACY = 0;
    private static final long MINIMAL_TIME = 0;
    private static final double MINIMAL_DISTANCE = 50;
    private static final boolean USE_IN_BACKGROUND = false;
    public static final int COMFORTABLE_ZOOM_LEVEL = 18;
    private int checkedItem = 0;

    @Override // запрашивает доступ к GPS
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSION_FINE_LOCATION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // если получили разрешение к GPS ищем местоположение телефона переводим туда камеру и делаем запрос в яндекс что бы получить адрес
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
            }  // permission denied
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String MAPKIT_API_KEY = "331a7ae7-9f07-4053-8010-41cfb9f289c0"; // ключ от яндекс мапкит
        MapKitFactory.setApiKey(MAPKIT_API_KEY);
        MapKitFactory.initialize(this);
        SearchFactory.initialize(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);

        parametrs = new ArrayList<>(); // массив всех параметров заказа
        wishes = new ArrayList<>(); // массив выбранных параметров заказа

        searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED); // инициализаруем поиск яндекс

        mapView = findViewById(R.id.mapview);
        mapView.getMap().setTiltGesturesEnabled(false); // запрещаем что то
        mapView.getMap().setRotateGesturesEnabled(false); // запрещаем крутить карту
        mapView.getMap().move(new CameraPosition(new Point(55.75370903771494, 37.61981338262558), 18, 0, 0)); // изначально координаты мавзолея в Москве
        mapView.getMap().addCameraListener(this);
        mapObjects = mapView.getMap().getMapObjects().addCollection();

        int permissionStatus = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION); // проверяем разрешение на работу с GPS
        // если получили разрешение к GPS ищем местоположение телефона переводим туда камеру и делаем запрос в яндекс что бы получить адрес
        if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
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
        } else {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_PERMISSION_FINE_LOCATION);
        }

        sourceTextLayout = findViewById(R.id.sourceTextLayout);
        destTextLayout = findViewById(R.id.destTextLayout);
        commentTextLayout = findViewById(R.id.commentTextLayout);
        createOrderButton = findViewById(R.id.createOrderButton);
        summTextView = findViewById(R.id.summTextView);
        searchProgressBar = findViewById(R.id.searchProgressBar);
        markerImageView = findViewById(R.id.markerImageView);

        // Получаем адрес сервера и АПИ ключ и группу экипажей
        settingsServer = new SettingsServer();
        server = settingsServer.getServer(); // адрес сервера
        apiKey = settingsServer.getApiKey(); // апи ключ
        crewGroupId = settingsServer.getCrewGroupId()[0]; // группа экипажей на которую создаем заказ
        paramsOrder = settingsServer.getParamsOrder(); // массив параметров заказа которые будут работать
        callKey = settingsServer.getCallKey();
        callServer = settingsServer.getCallServer();

        getOrderParamsList(); // запрос на все параметры заказа

        final Intent sourceIntent = new Intent(OrderActivity.this, SearchAddressActivity.class);

        sharedPreferences = this.getSharedPreferences("mySharedPrefereces", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        // очищаем адреса и их координаты
        editor.remove("dest_address");
        editor.remove("source_address");
        editor.remove("source_address_lat");
        editor.remove("source_address_lon");
        editor.remove("dest_address_lat");
        editor.remove("dest_address_lon");
        editor.apply();
        phone = sharedPreferences.getString("phone", ""); // получаем номер телефона

        getBalance(); // запрос на бонусный баланс клиента

        polylinePoints = new ArrayList<>();

        // очищаем все зоны и расстояния заказа
        sourceZoneId = "0";
        destZoneId = "0";
        cityDist = "0";
        countryDist = "0";
        sourceCountryDist = "0";

        // если нажимаем на адрес подачи переходим на окно поиска
        Objects.requireNonNull(sourceTextLayout.getEditText()).setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    sourceIntent.putExtra("getEditText", "source"); // передаем то что перешли из адреса подачи
                    startActivity(sourceIntent);
                }
            }
        });
        // если нажимаем на адрес назначения переходим на окно поиска
        Objects.requireNonNull(destTextLayout.getEditText()).setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    sourceIntent.putExtra("getEditText", "dest");// передаем то что перешли из адреса назначения
                    startActivity(sourceIntent);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // при возврате из окна поиска адреса заносим данные в поля адрес подачи и адрес назначения
        Objects.requireNonNull(sourceTextLayout.getEditText()).setText(sharedPreferences.getString("source_address", ""));
        Objects.requireNonNull(destTextLayout.getEditText()).setText(sharedPreferences.getString("dest_address", ""));
        // если адрес подачи внесли вручную переводим камеру на эти координаты
        if(sharedPreferences.getString("source_address_lon", "").length() > 0){
            mapView.getMap().move(new CameraPosition(new Point(Double.parseDouble(sharedPreferences.getString("source_address_lat", "")),
                    Double.parseDouble(sharedPreferences.getString("source_address_lon", ""))), 18, 0, 0));
        }

        sourceTextLayout.getEditText().clearFocus(); // убираем фокус с полей
        destTextLayout.getEditText().clearFocus(); // убираем фокус с полей

        try {
            analyzeRoute();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }

    //Анализируем маршрут
    private void analyzeRoute() throws UnsupportedEncodingException {
        if(!sharedPreferences.getString("source_address", "").isEmpty() & !sharedPreferences.getString("dest_address", "").isEmpty()) {

            HashMap<String, java.io.Serializable> sourceHashMap = new HashMap<>();
            sourceHashMap.put("address", sharedPreferences.getString("source_address", ""));
            sourceHashMap.put("lat", Double.valueOf(sharedPreferences.getString("source_address_lat", "")));
            sourceHashMap.put("lon", Double.valueOf(sharedPreferences.getString("source_address_lon", "")));

            HashMap<String, java.io.Serializable> destHashMap = new HashMap<>();
            destHashMap.put("address", sharedPreferences.getString("dest_address", ""));
            destHashMap.put("lat", Double.valueOf(sharedPreferences.getString("dest_address_lat", "")));
            destHashMap.put("lon", Double.valueOf(sharedPreferences.getString("dest_address_lon", "")));

            final ArrayList<HashMap<String, Serializable>> adresses = new ArrayList<>();
            adresses.add(sourceHashMap);
            adresses.add(destHashMap);

            HashMap<String, java.io.Serializable> params = new HashMap<>();
            params.put("addresses", adresses);
            params.put("get_full_route_coords", true);

            JSONObject parameters = new JSONObject(params);

            url = server + "analyze_route2";
            Md5Hash md5Hash = new Md5Hash();
            hashApiKey = md5Hash.md5(parameters + apiKey);
            postRequest = new PostRequestParams(this, url, params, hashApiKey);
            postRequest.getString(new PostRequestParams.VolleyCallback() {
                @SuppressLint("SetTextI18n")
                @Override
                public void onSuccess(String req, String jsonArray) throws JSONException {
                    if (req.equals("OK")) {
                        JSONObject mainObject = new JSONObject(jsonArray);
                        cityDist = mainObject.getString("city_dist");
                        countryDist = mainObject.getString("country_dist");
                        sourceCountryDist = mainObject.getString("source_country_dist");

                        polylinePoints.clear();
                        JSONArray routeCoords = mainObject.getJSONArray("full_route_coords");
                        for (int i = 0; i < routeCoords.length(); i++) {
                            polylinePoints.add(new Point(Double.parseDouble(routeCoords.getJSONObject(i).getString("lat")), Double.parseDouble(routeCoords.getJSONObject(i).getString("lon"))));
                        }
                        routingToMap = true;
                        createObject();
                        JSONArray adressesAnswer = mainObject.getJSONArray("addresses");
                        sourceZoneId = adressesAnswer.getJSONObject(0).getString("zone_id");
                        destZoneId = adressesAnswer.getJSONObject(1).getString("zone_id");


                        calcOrderCost(sourceZoneId, destZoneId, cityDist, countryDist, sourceCountryDist);
                    } else {
                        sourceZoneId = "0";
                        destZoneId = "0";
                        cityDist = "0";
                        countryDist = "0";
                        sourceCountryDist = "0";
                        Toast.makeText(OrderActivity.this, "Анализ маршрута не получен", Toast.LENGTH_SHORT).show();
                        createOrderButton.setText("Заказать     |        0р.");
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
        params.put("order_params", wishes);
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
                    summOrder = Integer.parseInt(mainObject.getString("sum"));
                    createOrderButton.setText("ЗАКАЗАТЬ     |     ~" + summOrder + "р.");
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

    @Override // отключаем кнопку назад
    public void onBackPressed() {
    }
    // переводим камеру на местоположение телефона
    private void subscribeToLocationUpdate() {
        if (locationManager != null && locationListener != null) {
            locationManager.subscribeForLocationUpdates(DESIRED_ACCURACY, MINIMAL_TIME, MINIMAL_DISTANCE, USE_IN_BACKGROUND, FilteringMode.OFF, locationListener);
        }
    }

    ///Создаем заказ
    public void createOrder(View view){
        @SuppressLint("SimpleDateFormat") SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        String source = Objects.requireNonNull(sourceTextLayout.getEditText()).getText().toString().trim();
        String dest = Objects.requireNonNull(destTextLayout.getEditText()).getText().toString().trim();
        String comment = Objects.requireNonNull(commentTextLayout.getEditText()).getText().toString().trim();
        String timeNow = format.format(new Date());
        if(source.isEmpty()){
            Toast.makeText(this, "Введите адрес подачи", Toast.LENGTH_SHORT).show();
        } else if(dest.isEmpty()){
            Toast.makeText(this, "Введите адрес назначения", Toast.LENGTH_SHORT).show();
        } else {
            HashMap<String, java.io.Serializable> sourceHashMap = new HashMap<>();
            sourceHashMap.put("address", source);
            sourceHashMap.put("lat", Double.valueOf(sharedPreferences.getString("source_address_lat", "")));
            sourceHashMap.put("lon", Double.valueOf(sharedPreferences.getString("source_address_lon", "")));
            sourceHashMap.put("zone_id", Integer.valueOf(sourceZoneId));

            HashMap<String, java.io.Serializable> destHashMap = new HashMap<>();
            destHashMap.put("address", dest);
            destHashMap.put("lat", Double.valueOf(sharedPreferences.getString("dest_address_lat", "")));
            destHashMap.put("lon", Double.valueOf(sharedPreferences.getString("dest_address_lon", "")));
            destHashMap.put("zone_id", Integer.valueOf(destZoneId));

            ArrayList<HashMap<String, Serializable>> adresses = new ArrayList<>();
            adresses.add(sourceHashMap);
            adresses.add(destHashMap);
            HashMap<String, java.io.Serializable> params = new HashMap<>();
            params.put("source_time", timeNow);
            params.put("phone", phone);
            params.put("comment", comment);
            params.put("order_params", wishes);
            params.put("crew_group_id", Integer.valueOf(crewGroupId));
            params.put("addresses", adresses);
            params.put("server_time_offset", 0);
            params.put("check_duplicate", true);
            params.put("total_cost", (double) summOrder);
            if(summBonuse > 0){
                params.put("use_bonus", true);
            }

            JSONObject parameters = new JSONObject(params);

            url = server + "create_order2";
            Md5Hash md5Hash = new Md5Hash();
            hashApiKey = md5Hash.md5(parameters + apiKey);
            postRequest = new PostRequestParams(this, url, params, hashApiKey);
            postRequest.getString(new PostRequestParams.VolleyCallback() {
                @SuppressLint("SetTextI18n")
                @Override
                public void onSuccess(String req, String jsonArray) throws JSONException {
                    if (req.equals("OK")){
                        JSONObject mainObject = new JSONObject(jsonArray);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("ORDER_ID", mainObject.getString("order_id"));
                        editor.apply();
                        startActivity(new Intent(OrderActivity.this, CurrentOrderActivity.class));
                    } else {
                        Toast.makeText(OrderActivity.this, "Заказ не создан", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }

    // при перетаскивании камеры получаем координаты и адрес из Яндекса
    @Override
    public void onCameraPositionChanged(@NonNull Map map, @NonNull CameraPosition cameraPosition, @NonNull CameraUpdateSource cameraUpdateSource, boolean b) {
        if (b) {
            searchProgressBar.setVisibility(View.VISIBLE); // показываем прогресс бар
            latest = System.currentTimeMillis(); //обновляем время последнего изменения текста
            Handler h = new Handler(Looper.getMainLooper());
            Runnable r = new Runnable() {
                // получаем адрес из Яндекса только если перестали перемещать карту 2 сек
                @Override
                public void run() {
                    if (System.currentTimeMillis() - delay > latest) {
                        if (!routingToMap) {
                            searchManager.submit(
                                    new Point(mapView.getMap().getCameraPosition().getTarget().getLatitude(), mapView.getMap().getCameraPosition().getTarget().getLongitude()),
                                    18,
                                    new SearchOptions().setSearchTypes(SearchType.GEO.value),
                                    new Session.SearchListener() {
                                        @Override
                                        public void onSearchResponse(@NonNull Response response) {
                                            Objects.requireNonNull(sourceTextLayout.getEditText()).setText(Objects.requireNonNull(response.getCollection().getChildren().get(0).getObj()).getName());
                                            // заносим адрес и координаты в файл
                                            editor.putString("source_address", Objects.requireNonNull(sourceTextLayout.getEditText()).getText().toString().trim());
                                            editor.putString("source_address_lat", String.valueOf(mapView.getMap().getCameraPosition().getTarget().getLatitude()));
                                            editor.putString("source_address_lon", String.valueOf(mapView.getMap().getCameraPosition().getTarget().getLongitude()));
                                            editor.apply();
                                            searchProgressBar.setVisibility(View.GONE); // убираем прогресс бар
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

                }
            };
            h.postDelayed(r, delay + 50);//в главный поток с задержкой delay + 50 миллисекунд
        }
    }

    // переводим камеру на местоположение телефона при нажатии на кнопку стрелки
    public void searchMe(View view) {
        mapView.getMap().move(
                new CameraPosition(new Point(my_lat, my_lon), 18.0f, 0.0f, 0.0f),
                new Animation(Animation.Type.SMOOTH, 1),
                null);
    }
    // открываем окно класса авто
    public void openClassAuto(View view) {
        Intent intent = new Intent(this, ClassAutoActivity.class);
        intent.putExtra("sourceZoneId", sourceZoneId);
        intent.putExtra("destZoneId", destZoneId);
        intent.putExtra("cityDist", cityDist);
        intent.putExtra("countryDist", countryDist);
        intent.putExtra("sourceCountryDist", sourceCountryDist);
        startActivityForResult(intent, 1);
    }
    // результаты выбора класса авто или параметров заказа
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case 1:
                if(data != null){
                    crewGroupId = settingsServer.getCrewGroupId()[data.getIntExtra("crewId", 0)];
                }
            break;
            case 2:
                if(data != null){
                    wishes.clear();
                    for(int i = 0; i < Objects.requireNonNull(data.getIntegerArrayListExtra("parametrs")).size(); i++){
                        wishes.add(Integer.valueOf(paramsOrder[Objects.requireNonNull(data.getIntegerArrayListExtra("parametrs")).get(i)]));
                    }

                }
        }
    }

    // открываем окно параметров заказа
    public void openWishes(View view) {
        Intent intent = new Intent(this, WishesActivity.class);
        intent.putExtra("parametrs", parametrs);
        startActivityForResult(intent, 2);
    }
    // запрашиваем все параметры, отбираем параметры которые нам нужны берем их имя и сумму параметра если они есть
    private void getOrderParamsList() {
        url = server + "get_order_params_list";
        Md5Hash md5Hash = new Md5Hash();
        hashApiKey = md5Hash.md5(apiKey);
        GetRequest getRequest = new GetRequest(OrderActivity.this, url, null, hashApiKey);
        getRequest.getString(new GetRequest.VolleyCallback() {
            @Override
            public void onSuccess(String req, String jsonArray) throws JSONException {
                JSONObject mainObject = new JSONObject(jsonArray);
                JSONArray arrayParametrs = mainObject.getJSONArray("order_params");
                final int numberOfItemsInResp = arrayParametrs.length();
                for(int i = 0; i < numberOfItemsInResp; i++){
                    for (String s : paramsOrder) {
                        idParametr = arrayParametrs.getJSONObject(i).getString("id");
                        if (idParametr.equals(s)) {
                            nameParametr = arrayParametrs.getJSONObject(i).getString("name");
                            sumParametr = arrayParametrs.getJSONObject(i).getDouble("sum");
                            percentParametr = arrayParametrs.getJSONObject(i).getDouble("percent");
                            if (sumParametr > 0) {
                                nameParametr = nameParametr + " +" + sumParametr + "р.";
                            } else if (percentParametr > 0) {
                                nameParametr = nameParametr + " +" + percentParametr + "%";
                            }
                            parametrs.add(nameParametr);
                        }
                    }
                }
            }
        });
    }

// открываем диалог с выбором типа оплаты
    public void openBalance(View view) {
        CharSequence[] array = {"Наличными", "Бонусами: " + bonusBalanse};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle("Выберите тип оплаты");
        builder.setSingleChoiceItems(array, checkedItem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(which == 1){

                    summBonuse = bonusBalanse;
                } else {
                    summBonuse = 0;
                }
                checkedItem = which;
            }
        }).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    analyzeRoute();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        });
        builder.create().show();
    }

// Получаем бонусный баланс клиента
    private void getBalance() {
        params = "PHONE=" + phone + "&fields=CLIENT_BONUS_BALANCE";
        Md5Hash md5Hash = new Md5Hash();
        hashApiKey = md5Hash.md5(params + callKey);
        params = "PHONE=" + phone + "&fields=CLIENT_BONUS_BALANCE&signature=" + hashApiKey;
        url = callServer + "get_info_by_phone?" + params;
        getRequestXML = new GetRequestXML(OrderActivity.this,url,params, hashApiKey);
        getRequestXML.getString(new GetRequestXML.VolleyCallback() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onSuccess(String req, String data){
                if (req.equals("OK")){
                    bonusBalanse = (int) Math.floor(Double.parseDouble(data));
                }
            }
        });
    }

    private void createObject() {
        mapObjects.clear();
        polyline = mapObjects.addPolyline(new Polyline(polylinePoints));
        polyline.setOutlineColor(Color.BLACK);
        polyline.setStrokeColor(Color.RED);
        polyline.setZIndex(100.0f);

        boundingBox = new BoundingBox(polylinePoints.get(0), polylinePoints.get(polylinePoints.size() - 1));
        cameraSourceDestPoint = mapView.getMap().cameraPosition(boundingBox);
        mapView.getMap().move(new CameraPosition(cameraSourceDestPoint.getTarget(), cameraSourceDestPoint.getZoom() - 0.5f, cameraSourceDestPoint.getAzimuth(), cameraSourceDestPoint.getTilt()),
                new Animation(Animation.Type.SMOOTH, 0f), null);

        markerImageView.setVisibility(View.GONE);
        mapObjects.addPlacemark(polylinePoints.get(0), ImageProvider.fromResource(this, R.drawable.markera));
        mapObjects.addPlacemark(polylinePoints.get(polylinePoints.size() - 1), ImageProvider.fromResource(this, R.drawable.markerb));
    }
}



