package com.example.taxitmapp;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.util.Objects;

public class PostRequestXML {
    private Context context;
    private RequestQueue mQueue;
    private String url, apiKey, params;
    public  String req, jsonArray;

    /*  Формируем POST запросы и отправляем данные в активити которое сформировала эти запросы*/

    PostRequestXML(Context context, String url, String params, String apiKey) {
        this.params = params;
        this.url = url;
        this.apiKey = apiKey;
        this.context = context;
        mQueue = Volley.newRequestQueue(context);
    }

    void getString(final VolleyCallback callback) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Serializer serializer = new Persister();
                try {
                    ResponseXML dest = serializer.read(ResponseXML.class, response);
                    callback.onSuccess(dest.descr);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("VolleyError: " , Objects.requireNonNull(error.getMessage()));
            }
        })
        {
        };
        mQueue.add(stringRequest);
    }

    public interface VolleyCallback{
        void onSuccess(String req) throws JSONException;
    }
}
