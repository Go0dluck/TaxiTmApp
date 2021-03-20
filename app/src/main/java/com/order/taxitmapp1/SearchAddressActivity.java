package com.order.taxitmapp1;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Objects;

public class SearchAddressActivity extends AppCompatActivity {
    private TextInputLayout searchAddressTextInput;
    private ProgressBar progressBar;
    private ListView addressListView;
    private String server, apiKey, address, params, url;
    long latest = 0;
    long delay = 2000;
    private ArrayList<String> addresses;
    private ArrayList<String> cites;
    private ArrayList<String> kindAddress;
    private ArrayList<Integer> imgs;
    private ArrayList<String> latAddress;
    private ArrayList<String> lonAddress;

    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_address);
        searchAddressTextInput = findViewById(R.id.searchAddressTextInput);
        addressListView = findViewById(R.id.addressListView);
        progressBar = findViewById(R.id.progressBar);
        // Получаем адрес сервера и АПИ ключ
        SettingsServer settingsServer = new SettingsServer();
        server = settingsServer.getServer();
        apiKey = settingsServer.getApiKey();
        //массивы для адресов городов и картинок и типов адресов
        addresses = new ArrayList<>();
        cites = new ArrayList<>();
        imgs = new ArrayList<>();
        kindAddress = new ArrayList<>();
        latAddress = new ArrayList<>();
        lonAddress = new ArrayList<>();
        final Intent recivedIntent = getIntent();

        SharedPreferences sharedPreferences = this.getSharedPreferences("mySharedPrefereces", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        //отслеживаем ввод текста
        Objects.requireNonNull(searchAddressTextInput.getEditText()).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                latest = System.currentTimeMillis(); //обновляем время последнего изменения текста
                Handler h = new Handler(Looper.getMainLooper());
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.VISIBLE);

                        if(System.currentTimeMillis() - delay > latest) //если в последние 2 секунды текст не менялся
                            if(searchAddressTextInput.getEditText().length() >= 3){
                                try {
                                    address = URLEncoder.encode(searchAddressTextInput.getEditText().getText().toString().trim(), "UTF-8");
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                                //заполняем параметры и урл
                                params = "max_addresses_count=20&get_streets=true&get_points=true&get_houses=true&address=" + address;
                                url = server + "get_addresses_like2?" + params;
                                //получаем апи ключ
                                SettingsServer settingsServer = new SettingsServer();
                                apiKey = settingsServer.getApiKey();
                                //хэшируем ключ и параметры
                                Md5Hash md5Hash = new Md5Hash();
                                apiKey = md5Hash.md5(params + apiKey);
                                //гет запрос на адреса
                                GetRequest getPay = new GetRequest(SearchAddressActivity.this,url,params, apiKey);
                                getPay.getString(new GetRequest.VolleyCallback() {
                                    @Override
                                    public void onSuccess(String req, String jsonArray) throws JSONException {
                                        if(req.equals("OK")){
                                            //очищаем все массивы
                                            addresses.clear();
                                            cites.clear();
                                            imgs.clear();
                                            kindAddress.clear();
                                            latAddress.clear();
                                            lonAddress.clear();
                                            //ищем адреса и заполняем массивы
                                            JSONObject mainObject = new JSONObject(jsonArray);
                                            JSONArray arrayAddress = mainObject.getJSONArray("addresses");
                                            final int numberOfItemsInResp = arrayAddress.length();
                                            for (int i = 0; i < numberOfItemsInResp; i++) {
                                                if(arrayAddress.getJSONObject(i).getString("kind").equals("street")){
                                                    addresses.add(arrayAddress.getJSONObject(i).getString("street"));
                                                    cites.add(arrayAddress.getJSONObject(i).getString("city"));
                                                    latAddress.add(arrayAddress.getJSONObject(i).getJSONObject("coords").getString("lat"));
                                                    lonAddress.add(arrayAddress.getJSONObject(i).getJSONObject("coords").getString("lon"));
                                                    imgs.add(R.drawable.ic_import_export_black_24dp);
                                                    kindAddress.add(arrayAddress.getJSONObject(i).getString("kind"));
                                                } else if (arrayAddress.getJSONObject(i).getString("kind").equals("point")){
                                                    addresses.add(arrayAddress.getJSONObject(i).getString("point") + " * " + arrayAddress.getJSONObject(i).getString("street") + ", " +
                                                            arrayAddress.getJSONObject(i).getString("house"));
                                                    cites.add(arrayAddress.getJSONObject(i).getString("city"));
                                                    latAddress.add(arrayAddress.getJSONObject(i).getJSONObject("coords").getString("lat"));
                                                    lonAddress.add(arrayAddress.getJSONObject(i).getJSONObject("coords").getString("lon"));
                                                    imgs.add(R.drawable.ic_location_city_black_24dp);
                                                    kindAddress.add(arrayAddress.getJSONObject(i).getString("kind"));
                                                } else if (arrayAddress.getJSONObject(i).getString("kind").equals("house")){
                                                    addresses.add(arrayAddress.getJSONObject(i).getString("street") + ", " + arrayAddress.getJSONObject(i).getString("house"));
                                                    cites.add(arrayAddress.getJSONObject(i).getString("city"));
                                                    latAddress.add(arrayAddress.getJSONObject(i).getJSONObject("coords").getString("lat"));
                                                    lonAddress.add(arrayAddress.getJSONObject(i).getJSONObject("coords").getString("lon"));
                                                    imgs.add(R.drawable.ic_home_black_24dp);
                                                    kindAddress.add(arrayAddress.getJSONObject(i).getString("kind"));
                                                }
                                            }
                                            //кастомизируем адаптер
                                            MyAdapter adapter = new MyAdapter(SearchAddressActivity.this, addresses, cites, imgs);
                                            addressListView.setAdapter(adapter);
                                            progressBar.setVisibility(View.GONE);
                                        } else {
                                            //тут надо подумать как это лучше сделать
                                            Toast.makeText(SearchAddressActivity.this, "Адресов нет", Toast.LENGTH_LONG).show();
                                        }
                                    }
                                });
                            }
                    }
                };
                h.postDelayed(r, delay + 50);//в главный поток с задержкой delay + 50 миллисекунд
            }
        });
        //отслеживаем выбор из списка если выбрали улицу заносим ее в едиттекст для поиска дома
        //заносим выбор из списка в файл что бы передать на окно заказа
        addressListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(kindAddress.get(position).equals("street")){
                    searchAddressTextInput.getEditText().setText(addresses.get(position) + ", ");
                    searchAddressTextInput.getEditText().setSelection(searchAddressTextInput.getEditText().length());
                } else if(Objects.equals(recivedIntent.getStringExtra("getEditText"), "source")){
                    editor.putString("source_address", addresses.get(position));
                    editor.putString("source_address_lat", latAddress.get(position));
                    editor.putString("source_address_lon", lonAddress.get(position));
                    editor.apply();
                    finish();
                } else {
                    editor.putString("dest_address", addresses.get(position));
                    editor.putString("dest_address_lat", latAddress.get(position));
                    editor.putString("dest_address_lon", lonAddress.get(position));
                    editor.apply();
                    finish();
                }

            }
        });

    }

    /**
     * Кастомный адаптер для отображения списка адресов
     */

    static class MyAdapter extends ArrayAdapter {
        ArrayList<String> addresses;
        ArrayList<String> cites;
        ArrayList<Integer> imgs;
        MyAdapter(Context context, ArrayList<String> addresses1, ArrayList<String> cites1, ArrayList<Integer> imgs1){
            super(context, R.layout.search_adress_row,R.id.adressTextView, addresses1);
            this.addresses = addresses1;
            this.cites = cites1;
            this.imgs = imgs1;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            assert inflater != null;
            @SuppressLint("ViewHolder") View row = inflater.inflate(R.layout.search_adress_row, parent, false);
            ImageView myImage = row.findViewById(R.id.picImageView);
            TextView myAddress = row.findViewById(R.id.adressTextView);
            TextView myCity = row.findViewById(R.id.cityTextView);


            myImage.setImageResource(imgs.get(position));
            myAddress.setText(addresses.get(position));
            myCity.setText(cites.get(position));
            return row;
        }
    }

}
