package dev.mfarm.com.mfarm.sync;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;

import dev.mfarm.com.mfarm.MainActivity;
import dev.mfarm.com.mfarm.dao.DatabaseHelper;

public final class FarmSyncManager {
    public static final String BACKUP_CACHE_NAME = "mfarm-shared.mfarm";
    private static final String TAG = "FarmSyncManager";

    private FarmSyncManager() {}

    public static synchronized String autoSync(Context context) {
        if (!FarmIdentity.isSet(context)) {
            return "Solo phone — records stay on this device until you set up Farm Sync.";
        }
        String folder = FarmIdentity.folderUri(context);
        if (folder == null || folder.isEmpty()) {
            return bannerText(context, "Farm is set up. Send a backup or pick a shared folder when you have data.");
        }
        try {
            SyncResult result = syncFolder(context, Uri.parse(folder));
            FarmIdentity.setLastSync(context, System.currentTimeMillis(), result.message);
            return bannerText(context, result.message);
        } catch (Exception e) {
            Log.w(TAG, "auto sync failed", e);
            String msg = FarmNetwork.isOnline(context)
                    ? "Could not reach the shared folder. Entries are still saved on this phone."
                    : "Offline — entries saved on this phone. Sync when you have data.";
            FarmIdentity.setLastSync(context, FarmIdentity.lastSync(context), msg);
            return bannerText(context, msg);
        }
    }

    public static String bannerText(Context context) {
        return bannerText(context, FarmIdentity.lastStatus(context));
    }

    public static String bannerText(Context context, String detail) {
        FarmIdentity identity = FarmIdentity.get(context);
        String net = FarmNetwork.statusLabel(context);
        if (identity == null) {
            return net + " · Solo mode. Tap to share this farm with someone.";
        }
        String extra = detail == null || detail.isEmpty() ? "Tap to sync or invite." : detail;
        return net + " · " + identity.farmName + " · code " + FarmIdentity.formatJoinCode(identity.joinCode)
                + "\n" + extra;
    }

    public static SyncResult syncFolder(Context context, Uri treeUri) throws Exception {
        FarmIdentity identity = requireFarm(context);
        SQLiteDatabase db = openDb(context);
        JSONObject local = FarmSnapshot.capture(db, identity);
        DocumentFile file = FarmBackupStore.backupFile(context, treeUri, true);
        if (file == null) {
            throw new Exception("Shared folder is not writable");
        }
        JSONObject remote = null;
        if (file.length() > 0) {
            byte[] raw = FarmBackupStore.read(context, file.getUri());
            if (raw != null && raw.length > 0) {
                try {
                    remote = new JSONObject(FarmCrypto.decrypt(raw, identity.joinCode));
                } catch (Exception e) {
                    throw new Exception("Wrong join code or damaged backup in the folder");
                }
            }
        }
        JSONObject merged = FarmMerger.merge(local, remote);
        merged.put("farmId", identity.farmId);
        merged.put("farmName", identity.farmName);
        merged.put("exportedAt", System.currentTimeMillis());
        int applied = FarmSnapshot.apply(db, merged);
        afterApply(context);
        byte[] encrypted = FarmCrypto.encrypt(merged.toString(), identity.joinCode);
        FarmBackupStore.write(context, file.getUri(), encrypted);
        String msg = FarmNetwork.isOnline(context)
                ? "Synced with the shared folder. Partner will see updates when they open MFarm."
                : "Saved into the shared folder. Drive/WhatsApp can upload it later when there is data.";
        if (applied > 0) {
            msg = "Merged " + applied + " updates. " + msg;
        }
        return new SyncResult(true, msg, applied);
    }

    public static File exportToCache(Context context) throws Exception {
        FarmIdentity identity = requireFarm(context);
        SQLiteDatabase db = openDb(context);
        JSONObject local = FarmSnapshot.capture(db, identity);
        File dir = new File(context.getCacheDir(), "backups");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new Exception("Could not create backup folder");
        }
        File out = new File(dir, BACKUP_CACHE_NAME);
        byte[] encrypted = FarmCrypto.encrypt(local.toString(), identity.joinCode);
        FileOutputStream fos = new FileOutputStream(out);
        try {
            fos.write(encrypted);
        } finally {
            fos.close();
        }
        return out;
    }

    public static SyncResult joinFromFolder(Context context, String joinCode, Uri treeUri) throws Exception {
        DocumentFile file = FarmBackupStore.backupFile(context, treeUri, false);
        if (file == null) {
            throw new Exception("No MFarm backup in that folder yet. Ask the owner to tap Sync now first.");
        }
        SyncResult result = adoptFromBackup(context, joinCode, file.getUri());
        FarmIdentity.setFolderUri(context, treeUri.toString());
        return result;
    }

    public static SyncResult importBackup(Context context, Uri uri) throws Exception {
        FarmIdentity identity = FarmIdentity.get(context);
        byte[] raw = FarmBackupStore.read(context, uri);
        if (raw == null || raw.length == 0) {
            throw new Exception("Backup is empty");
        }
        if (identity == null) {
            throw new Exception("Enter the farm join code first");
        }
        JSONObject remote;
        try {
            remote = new JSONObject(FarmCrypto.decrypt(raw, identity.joinCode));
        } catch (Exception e) {
            throw new Exception("Wrong join code for this backup");
        }
        String remoteFarm = remote.optString("farmId", "");
        if (!remoteFarm.isEmpty() && !remoteFarm.equals(identity.farmId)) {
            FarmIdentity.join(context, remoteFarm, remote.optString("farmName", identity.farmName),
                    identity.joinCode);
            identity = FarmIdentity.get(context);
        }
        SQLiteDatabase db = openDb(context);
        JSONObject local = FarmSnapshot.capture(db, identity);
        JSONObject merged = FarmMerger.merge(local, remote);
        int applied = FarmSnapshot.apply(db, merged);
        afterApply(context);
        FarmIdentity.setLastSync(context, System.currentTimeMillis(), "Imported partner backup");
        return new SyncResult(true, "Imported. Merged " + applied + " records onto this phone.", applied);
    }

    public static SyncResult adoptFromBackup(Context context, String joinCode, Uri uri) throws Exception {
        if (!FarmIdentity.isValidJoinCode(joinCode)) {
            throw new Exception("Enter the 8-character farm code");
        }
        byte[] raw = FarmBackupStore.read(context, uri);
        JSONObject remote = new JSONObject(FarmCrypto.decrypt(raw, joinCode));
        String farmId = remote.optString("farmId", "");
        if (farmId.isEmpty()) {
            throw new Exception("Backup has no farm id");
        }
        FarmIdentity.join(context, farmId, remote.optString("farmName", "Shared farm"), joinCode);
        SQLiteDatabase db = openDb(context);
        int applied = FarmSnapshot.apply(db, remote);
        afterApply(context);
        FarmIdentity.setLastSync(context, System.currentTimeMillis(), "Joined farm from backup");
        return new SyncResult(true, "Joined " + remote.optString("farmName") + ". Loaded " + applied + " records.", applied);
    }

    private static void afterApply(Context context) {
        try {
            dev.mfarm.com.mfarm.AlarmScheduler.reschedulePending(context);
        } catch (Exception ignored) {
        }
    }

    private static FarmIdentity requireFarm(Context context) throws Exception {
        FarmIdentity identity = FarmIdentity.get(context);
        if (identity == null) {
            throw new Exception("Create or join a farm first");
        }
        return identity;
    }

    private static SQLiteDatabase openDb(Context context) throws Exception {
        if (MainActivity.database != null && MainActivity.database.isOpen()) {
            return MainActivity.database;
        }
        DatabaseHelper helper = DatabaseHelper.getHelper(context.getApplicationContext());
        SQLiteDatabase db = helper.openDataBase();
        MainActivity.database = db;
        return db;
    }

    public static final class SyncResult {
        public final boolean ok;
        public final String message;
        public final int applied;

        public SyncResult(boolean ok, String message, int applied) {
            this.ok = ok;
            this.message = message;
            this.applied = applied;
        }
    }
}
