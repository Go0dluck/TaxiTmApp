package com.example.taxitmapp;

import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/*  Формируем GET запросы и отправляем данные в активити которое сформировала эти запросы*/

public class GetRequest {
    private Context context;
    private RequestQueue mQueue;
    private String url, apiKey, params;
    public  String req, jsonArray;

    GetRequest(Context context, String url, String params, String apiKey) {
        this.params = params;
        this.url = url;
        this.apiKey = apiKey;
        this.context = context;
        mQueue = Volley.newRequestQueue(context);
    }

    public void getString(final VolleyCallback callback) {
        JsonObjectRequest stringRequest = new JsonObjectRequest(Request.Method.GET, url , null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            req = response.getString("descr");
                            jsonArray = response.getString("data");
                            callback.onSuccess(req, jsonArray);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
                ,new Response.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError error){
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setCancelable(false);
                builder.setTitle("Предупреждение !");
                builder.setMessage("Связь с сервером не установлена");
                builder.setNegativeButton("Выход", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        System.exit(0);
                    }
                });
                builder.create().show();
                Log.d("VolleyError: " , Objects.requireNonNull(error.getMessage()));
            }
        })
        {
            @Override
            public Map<String, String> getHeaders(){
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Signature", apiKey);
                return headers;
            }
        };
        mQueue.add(stringRequest);
    }

    public interface VolleyCallback{
        void onSuccess(String req, String jsonArray) throws JSONException;
    }
}
