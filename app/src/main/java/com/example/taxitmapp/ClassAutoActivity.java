package com.example.taxitmapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class ClassAutoActivity extends AppCompatActivity {
    private String [] titles = {"ЭКОНОМ", "СТАНДАРТ", "БИЗНЕС"}; // массив названия класса авто
    private String [] descriptions = {"Машины эконом класса", "Машины стандарт класса", "Машины бизнесс класса"}; // массив описания класса авто
    private ArrayList<String> sum; // массив суммы класса авто
    private int [] images = {R.drawable.econom, R.drawable.standart, R.drawable.buisness}; // массив картинок
    private ListView lv;
    private SettingsServer settingsServer;
    private String url, server, hashApiKey, apiKey, phone, sourceZoneId, destZoneId, cityDist, countryDist, sourceCountryDist;
    private PostRequestParams postRequest;
    private SharedPreferences sharedPreferences;
    private String[] crewGroupId;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_auto);
        lv = findViewById(R.id.classAutoListView);
        progressBar = findViewById(R.id.progressBar);

        sum = new ArrayList<>(); // заполняем массив сумм пустыми значениями
        sum.add("");
        sum.add("");
        sum.add("");

        final Intent recivedIntent = getIntent(); // собираем из интента ид зон и расстояния
        sourceZoneId = recivedIntent.getStringExtra("sourceZoneId");
        destZoneId = recivedIntent.getStringExtra("destZoneId");
        cityDist = recivedIntent.getStringExtra("cityDist");
        countryDist = recivedIntent.getStringExtra("countryDist");
        sourceCountryDist = recivedIntent.getStringExtra("sourceCountryDist");

        sharedPreferences = this.getSharedPreferences("mySharedPrefereces", Context.MODE_PRIVATE);
        phone = sharedPreferences.getString("phone", ""); // получаем номер телефона

        settingsServer = new SettingsServer();
        server = settingsServer.getServer();
        apiKey = settingsServer.getApiKey();
        crewGroupId = settingsServer.getCrewGroupId(); // получаем массив ид групп экипажей из настроек
        calcOrderCost(sourceZoneId, destZoneId, cityDist, countryDist, sourceCountryDist, crewGroupId); // расчитываем суммы по группам экипажей

        Thread thread = new Thread(){ // делаем задержку 2 сек для расчета стоиммости по всем группам экипажей
            @Override
            public void run() {
                try {
                    synchronized (this) {
                        wait(2000);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setVisibility(View.GONE);
                                MyAdapter adapter = new MyAdapter(ClassAutoActivity.this, titles, descriptions, sum, images);
                                lv.setAdapter(adapter);
                            }
                        });
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() { // выбранную группу экипажей возвращаем в окно заказа
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent();
                intent.putExtra("crewId", position);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    @Override // отключаем кнопку назад
    public void onBackPressed() {
    }
        // расчитываем сумму заказа для каждой группы экипажей
    private void calcOrderCost(String sourceZoneId, String destZoneId, String cityDist, String countryDist, String sourceCountryDist, String[] crewGroupId) {
        for (int i = 0; i < crewGroupId.length; i++) {
            @SuppressLint("SimpleDateFormat") SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
            String timeNow = format.format(new Date());
            HashMap<String, Serializable> params = new HashMap<>();
            params.put("source_time", timeNow);
            params.put("phone", phone);
            params.put("source_zone_id", Integer.valueOf(sourceZoneId));
            params.put("dest_zone_id", Integer.valueOf(destZoneId));
            params.put("distance_city", Double.valueOf(cityDist));
            params.put("distance_country", Double.valueOf(countryDist));
            params.put("source_distance_country", Double.valueOf(sourceCountryDist));
            params.put("is_country", true);
            params.put("crew_group_id", Integer.valueOf(crewGroupId[i]));
            JSONObject parameters = new JSONObject(params);
            url = server + "calc_order_cost2";
            Md5Hash md5Hash = new Md5Hash();
            hashApiKey = md5Hash.md5(parameters + apiKey);
            postRequest = new PostRequestParams(this, url, params, hashApiKey);
            final int finalI = i;
            postRequest.getString(new PostRequestParams.VolleyCallback() {
                @SuppressLint("SetTextI18n")
                @Override
                public void onSuccess(String req, String jsonArray) throws JSONException {
                    if (req.equals("OK")){
                        JSONObject mainObject = new JSONObject(jsonArray);
                        sum.set(finalI, mainObject.getString("sum") + "р.");
                    } else {
                        sum.set(finalI, "СУММЫ НЕТ");
                    }
                }
            });
        }
    }
}


class MyAdapter extends ArrayAdapter {
    private int[] imageArray;
    private String[] titleArray;
    private String[] descriptoinArray;
    private ArrayList<String> sumArray;
    MyAdapter(Context context, String[] titles1, String[] descriptions1, ArrayList<String> sum1, int[] img1){
        super(context, R.layout.class_auto_row, R.id.classAutoTextView, titles1);
        this.imageArray = img1;
        this.titleArray = titles1;
        this.descriptoinArray = descriptions1;
        this.sumArray = sum1;
    }

    @SuppressLint("SetTextI18n")
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        @SuppressLint("ViewHolder") View row = inflater.inflate(R.layout.class_auto_row, parent, false);

        ImageView myImage = row.findViewById(R.id.classAutoImageView);
        TextView myTitle = row.findViewById(R.id.classAutoTextView);
        TextView myDescription = row.findViewById(R.id.classAutoDescriptionTextView);
        TextView mySum = row.findViewById(R.id.classAutoSumTextView);

        myImage.setImageResource(imageArray[position]);
        myTitle.setText(titleArray[position]);
        myDescription.setText(descriptoinArray[position]);
        mySum.setText(sumArray.get(position));
        return row;
    }
}
