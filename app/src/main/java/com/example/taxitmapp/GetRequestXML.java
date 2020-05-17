package com.example.taxitmapp;

import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

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

public class GetRequestXML {
    private Context context;
    private RequestQueue mQueue;
    private String url, apiKey, params;
    public  String req, jsonArray;

    /*  Формируем POST XML запросы и отправляем данные в активити которое сформировала эти запросы*/

    GetRequestXML(Context context, String url, String params, String apiKey) {
        this.params = params;
        this.url = url;
        this.apiKey = apiKey;
        this.context = context;
        mQueue = Volley.newRequestQueue(context);
    }

    void getString(final VolleyCallback callback) {
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Serializer serializer = new Persister();
                try {
                    ResponseXML dest = serializer.read(ResponseXML.class, response);
                    ResponseXML data = serializer.read(ResponseXML.class, response);
                    callback.onSuccess(dest.descr , data.data);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
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
        };
        mQueue.add(stringRequest);
    }

    public interface VolleyCallback{
        void onSuccess(String req, String data) throws JSONException;
    }
}
