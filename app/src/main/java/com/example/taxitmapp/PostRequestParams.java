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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PostRequestParams {
    private Context context;
    private RequestQueue mQueue;
    private String url;
    private String apiKey;
    private HashMap params;
    public  String req, jsonArray;
    private String timeNow;
    /*  Формируем POST запросы и отправляем данные в активити которое сформировала эти запросы с парметрами*/

    PostRequestParams(Context context, String url, HashMap params, String apiKey) {
        this.params = params;
        this.url = url;
        this.apiKey = apiKey;
        this.context = context;
        mQueue = Volley.newRequestQueue(context);
    }

    public void getString(final VolleyCallback callback) {
        //Map<String, String> params1 = new HashMap();
        //params1.put("source_time", timeNow);
        JSONObject parameters = new JSONObject(params);

        JsonObjectRequest stringRequest = new JsonObjectRequest(Request.Method.POST, url , parameters,
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
                //headers.put("Content-Type", "application/x-www-form-urlencoded");
                return headers;
            }
        };
        mQueue.add(stringRequest);
    }

    public interface VolleyCallback{
        void onSuccess(String req, String jsonArray) throws JSONException;
    }
}
