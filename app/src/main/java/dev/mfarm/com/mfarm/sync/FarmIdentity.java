package dev.mfarm.com.mfarm.sync;

import android.content.Context;
import android.content.SharedPreferences;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.UUID;

/**
 * A farm is the account. No email/password — the join code is how a second
 * phone attaches to the same herd and decrypts backups.
 */
public final class FarmIdentity {
    public static final String PREFS = "farm_sync";
    public static final String KEY_FARM_ID = "farm_id";
    public static final String KEY_FARM_NAME = "farm_name";
    public static final String KEY_JOIN_CODE = "join_code";
    public static final String KEY_ROLE = "role";
    public static final String KEY_DEVICE_ID = "device_id";
    public static final String KEY_FOLDER_URI = "folder_uri";
    public static final String KEY_LAST_SYNC = "last_sync";
    public static final String KEY_LAST_STATUS = "last_status";

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public final String farmId;
    public final String farmName;
    public final String joinCode;
    public final String role;
    public final String deviceId;

    public FarmIdentity(String farmId, String farmName, String joinCode, String role, String deviceId) {
        this.farmId = farmId;
        this.farmName = farmName;
        this.joinCode = joinCode;
        this.role = role;
        this.deviceId = deviceId;
    }

    public static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String deviceId(Context context) {
        SharedPreferences p = prefs(context);
        String id = p.getString(KEY_DEVICE_ID, null);
        if (id == null || id.isEmpty()) {
            id = UUID.randomUUID().toString();
            p.edit().putString(KEY_DEVICE_ID, id).apply();
        }
        return id;
    }

    public static FarmIdentity get(Context context) {
        SharedPreferences p = prefs(context);
        String farmId = p.getString(KEY_FARM_ID, null);
        if (farmId == null || farmId.isEmpty()) {
            return null;
        }
        return new FarmIdentity(
                farmId,
                p.getString(KEY_FARM_NAME, "My farm"),
                p.getString(KEY_JOIN_CODE, ""),
                p.getString(KEY_ROLE, "owner"),
                deviceId(context)
        );
    }

    public static boolean isSet(Context context) {
        return get(context) != null;
    }

    public static FarmIdentity create(Context context, String farmName) {
        String name = farmName == null || farmName.trim().isEmpty() ? "My farm" : farmName.trim();
        FarmIdentity identity = new FarmIdentity(
                UUID.randomUUID().toString(),
                name,
                generateJoinCode(),
                "owner",
                deviceId(context)
        );
        save(context, identity);
        return identity;
    }

    public static FarmIdentity join(Context context, String farmId, String farmName, String joinCode) {
        FarmIdentity identity = new FarmIdentity(
                farmId,
                farmName == null || farmName.isEmpty() ? "Shared farm" : farmName,
                normalizeJoinCode(joinCode),
                "member",
                deviceId(context)
        );
        save(context, identity);
        return identity;
    }

    public static void save(Context context, FarmIdentity identity) {
        prefs(context).edit()
                .putString(KEY_FARM_ID, identity.farmId)
                .putString(KEY_FARM_NAME, identity.farmName)
                .putString(KEY_JOIN_CODE, identity.joinCode)
                .putString(KEY_ROLE, identity.role)
                .putString(KEY_DEVICE_ID, identity.deviceId)
                .apply();
    }

    public static void setFolderUri(Context context, String uri) {
        prefs(context).edit().putString(KEY_FOLDER_URI, uri).apply();
    }

    public static String folderUri(Context context) {
        return prefs(context).getString(KEY_FOLDER_URI, null);
    }

    public static void setLastSync(Context context, long when, String status) {
        prefs(context).edit()
                .putLong(KEY_LAST_SYNC, when)
                .putString(KEY_LAST_STATUS, status)
                .apply();
    }

    public static long lastSync(Context context) {
        return prefs(context).getLong(KEY_LAST_SYNC, 0L);
    }

    public static String lastStatus(Context context) {
        return prefs(context).getString(KEY_LAST_STATUS, "");
    }

    public static String generateJoinCode() {
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return formatJoinCode(sb.toString());
    }

    public static String formatJoinCode(String raw) {
        String compact = normalizeJoinCode(raw);
        if (compact.length() != 8) {
            return compact;
        }
        return compact.substring(0, 4) + "-" + compact.substring(4);
    }

    public static String normalizeJoinCode(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("-", "").replace(" ", "").toUpperCase(Locale.US);
    }

    public static boolean isValidJoinCode(String raw) {
        String compact = normalizeJoinCode(raw);
        if (compact.length() != 8) {
            return false;
        }
        for (int i = 0; i < compact.length(); i++) {
            if (ALPHABET.indexOf(compact.charAt(i)) < 0) {
                return false;
            }
        }
        return true;
    }

    public String shareMessage() {
        return "Join my farm on MFarm\n\n"
                + "Farm: " + farmName + "\n"
                + "Code: " + formatJoinCode(joinCode) + "\n\n"
                + "On your phone: Farm Sync → Join this farm → enter the code, "
                + "then import the backup I send or open the same shared Drive folder.\n"
                + "You can keep entering records even with no data; sync when you have Wi‑Fi.";
    }
}
