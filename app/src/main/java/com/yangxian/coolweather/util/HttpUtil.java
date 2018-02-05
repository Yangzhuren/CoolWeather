package com.yangxian.coolweather.util;

import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * Created by MZyx on 2018/2/5.
 */

public class HttpUtil {

    public static void sendOkHttpRequest(String addreess, Callback callback) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(addreess).build();
        client.newCall(request).enqueue(callback);
    }
}
