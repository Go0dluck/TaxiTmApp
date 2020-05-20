package com.order.taxitmapp;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Objects;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class WishesActivity extends AppCompatActivity {
    private ListView wishesListView;
    private Button okButton;
    private SettingsServer settingsServer;
    private String[] paramsOrder;
    private String server, url, apiKey, nameParametr, idParametr;
    private ArrayList<Integer> parametrs;
    private ArrayAdapter arrayAdapter;
    private Double sumParametr, percentParametr;
    private SparseBooleanArray chosen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wishes);
        wishesListView = findViewById(R.id.wishesListView);
        okButton = findViewById(R.id.okButton);
        wishesListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        parametrs = new ArrayList<>();
        final Intent recivedIntent = getIntent();
        arrayAdapter = new ArrayAdapter<>(WishesActivity.this, android.R.layout.simple_list_item_checked, Objects.requireNonNull(recivedIntent.getStringArrayListExtra("parametrs")));
        wishesListView.setAdapter(arrayAdapter);

        wishesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                parametrs.clear();
                chosen = ((ListView) parent).getCheckedItemPositions();
                for (int i = 0; i < chosen.size(); i++) {
                    if (chosen.valueAt(i)) {
                        parametrs.add(chosen.keyAt(i));
                    }
                }
            }
        });

    }

    public void closeParametrs(View view) {
        Intent intent = new Intent();
        intent.putExtra("parametrs", parametrs);
        setResult(RESULT_OK, intent);
        finish();
    }

}
