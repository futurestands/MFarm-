package dev.mfarm.com.mfarm.sync;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public final class FarmNetwork {
    private FarmNetwork() {}

    public static boolean isOnline(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) {
                return false;
            }
            NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    public static String statusLabel(Context context) {
        return isOnline(context) ? "Online" : "Offline";
    }
}
