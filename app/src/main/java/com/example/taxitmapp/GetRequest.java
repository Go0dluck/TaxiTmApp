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

import java.util.HashMap;
import java.util.Map;

/*  Формируем GET запросы и отправляем данные в активити которое сформировала эти запросы*/

public class GetRequest {
    private Context context;
    private RequestQueue mQueue;
    private String url, apiKey, params;
    public  String req, jsonArray;

    public GetRequest(Context context, String url, String params, String apiKey) {
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
