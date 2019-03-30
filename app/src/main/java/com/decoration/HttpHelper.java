package com.decoration;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class HttpHelper {

    // base url
    private static final String BASE_URL = "https://aandh-58727.firebaseapp.com";

    // urls
    public static final String DELETE_USER_URL = BASE_URL + "/delete-user";

    // coordinates
    public static final float LATITUDE = 23.580608f;
    public static final float LONGITUDE = 58.433002f;

    // method to check network state
    public static boolean isOnline(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
