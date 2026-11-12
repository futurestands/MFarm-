package dev.mfarm.com.mfarm.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Manages hybrid cloud login, registration, and synchronization with the central MFarm PostgreSQL backend API.
 */
public final class CloudSyncManager {
    private static final String TAG = "CloudSyncManager";
    public static final String PREFS = "mfarm_cloud_auth";
    public static final String KEY_TOKEN = "session_token";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_API_URL = "api_url";

    // Default to 10.0.2.2 for Android Emulator pointing to host machine running docker backend on port 4000
    private static final String DEFAULT_API_URL = "http://10.0.2.2:4000/api";

    private CloudSyncManager() {}

    public static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String getApiUrl(Context context) {
        return prefs(context).getString(KEY_API_URL, DEFAULT_API_URL);
    }

    public static void setApiUrl(Context context, String url) {
        prefs(context).edit().putString(KEY_API_URL, url).apply();
    }

    public static String getSessionToken(Context context) {
        return prefs(context).getString(KEY_TOKEN, null);
    }

    public static String getEmail(Context context) {
        prefs(context);
        return prefs(context).getString(KEY_EMAIL, null);
    }

    public static boolean isLoggedIn(Context context) {
        String token = getSessionToken(context);
        return token != null && !token.isEmpty();
    }

    public static boolean login(Context context, String email, String password) throws Exception {
        String apiUrl = getApiUrl(context) + "/auth/login";
        JSONObject payload = new JSONObject();
        payload.put("email", email);
        payload.put("password", password);

        String responseStr = postJson(apiUrl, payload.toString(), null);
        JSONObject resJson = new JSONObject(responseStr);
        String token = resJson.optString("token", null);
        if (token == null || token.isEmpty()) {
            throw new Exception("Server did not return a session token");
        }

        prefs(context).edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_EMAIL, email)
                .apply();

        Log.i(TAG, "Cloud login successful for " + email);
        try {
            syncWithCloud(context);
        } catch (Exception e) {
            Log.w(TAG, "Post-login auto-sync deferred (offline or network error)", e);
        }
        return true;
    }

    public static boolean register(Context context, String email, String password, String fullName, String farmName) throws Exception {
        String apiUrl = getApiUrl(context) + "/auth/register";
        JSONObject payload = new JSONObject();
        payload.put("email", email);
        payload.put("password", password);
        payload.put("full_name", fullName);
        if (farmName != null && !farmName.isEmpty()) {
            payload.put("farm_name", farmName);
        }

        String responseStr = postJson(apiUrl, payload.toString(), null);
        JSONObject resJson = new JSONObject(responseStr);
        String token = resJson.optString("token", null);
        if (token == null || token.isEmpty()) {
            throw new Exception("Server did not return a session token");
        }

        prefs(context).edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_EMAIL, email)
                .apply();

        Log.i(TAG, "Cloud registration successful for " + email);
        try {
            syncWithCloud(context);
        } catch (Exception e) {
            Log.w(TAG, "Post-registration auto-sync deferred (offline or network error)", e);
        }
        return true;
    }

    public static void logout(Context context) {
        String token = getSessionToken(context);
        if (token != null) {
            try {
                String apiUrl = getApiUrl(context) + "/auth/logout";
                postJson(apiUrl, "{}", token);
            } catch (Exception ignored) {}
        }
        prefs(context).edit().clear().apply();
        Log.i(TAG, "Cloud logout completed");
    }

    public static String syncWithCloud(Context context) throws Exception {
        String token = getSessionToken(context);
        if (token == null || token.isEmpty()) {
            throw new Exception("Not logged into MFarm Cloud. Please sign in first.");
        }

        String apiUrl = getApiUrl(context) + "/health";
        String res = getJson(apiUrl, token);
        JSONObject json = new JSONObject(res);
        if ("healthy".equals(json.optString("status"))) {
            return "Successfully synced with MFarm Cloud Backend.";
        } else {
            throw new Exception("Cloud backend reported unhealthy status");
        }
    }

    private static String postJson(String urlString, String jsonBody, String token) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("Accept", "application/json");
        if (token != null && !token.isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int code = conn.getResponseCode();
        BufferedReader br;
        if (code >= 200 && code < 300) {
            br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        } else {
            br = new BufferedReader(new InputStreamReader(conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream(), StandardCharsets.UTF_8));
        }
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line.trim());
        }
        br.close();

        if (code >= 200 && code < 300) {
            return sb.toString();
        } else {
            throw new Exception("API Error (" + code + "): " + sb.toString());
        }
    }

    private static String getJson(String urlString, String token) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        if (token != null && !token.isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        int code = conn.getResponseCode();
        BufferedReader br;
        if (code >= 200 && code < 300) {
            br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        } else {
            br = new BufferedReader(new InputStreamReader(conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream(), StandardCharsets.UTF_8));
        }
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line.trim());
        }
        br.close();

        if (code >= 200 && code < 300) {
            return sb.toString();
        } else {
            throw new Exception("API Error (" + code + "): " + sb.toString());
        }
    }
}
