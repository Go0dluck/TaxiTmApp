package com.example.taxitmapp;

import android.content.Context;
import android.util.Log;

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

public class PostRequestParams {
    private Context context;
    private RequestQueue mQueue;
    private String url;
    private String apiKey;
    private HashMap params;
    public  String req, jsonArray;
    private String timeNow;
    /*  Формируем POST запросы и отправляем данные в активити которое сформировала эти запросы с парметрами*/

    public PostRequestParams(Context context, String url, HashMap params, String apiKey) {
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
                Log.d("VolleyError: " , error.getMessage());
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
