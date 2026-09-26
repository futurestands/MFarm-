package dev.mfarm.com.mfarm.sync;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Last-write-wins per record UUID so two phones can edit offline and combine
 * when they next share a backup. Different farms are rejected.
 */
public final class FarmMerger {
    private FarmMerger() {}

    public static JSONObject merge(JSONObject local, JSONObject remote) throws JSONException {
        if (local == null || !local.has("records")) {
            return remote;
        }
        if (remote == null || !remote.has("records")) {
            return local;
        }
        String localFarm = local.optString("farmId", "");
        String remoteFarm = remote.optString("farmId", "");
        if (!localFarm.isEmpty() && !remoteFarm.isEmpty() && !localFarm.equals(remoteFarm)) {
            throw new JSONException("This backup belongs to a different farm");
        }
        Map<String, JSONObject> byUuid = new LinkedHashMap<>();
        ingest(byUuid, local.optJSONArray("records"));
        ingest(byUuid, remote.optJSONArray("records"));

        JSONObject out = new JSONObject();
        out.put("format", 1);
        out.put("farmId", localFarm.isEmpty() ? remoteFarm : localFarm);
        String localName = local.optString("farmName", "");
        String remoteName = remote.optString("farmName", "");
        out.put("farmName", localName.isEmpty() ? remoteName : localName);
        out.put("exportedAt", Math.max(local.optLong("exportedAt"), remote.optLong("exportedAt")));
        JSONArray records = new JSONArray();
        Iterator<JSONObject> values = byUuid.values().iterator();
        while (values.hasNext()) {
            records.put(values.next());
        }
        out.put("records", records);
        return out;
    }

    private static void ingest(Map<String, JSONObject> map, JSONArray array) throws JSONException {
        if (array == null) {
            return;
        }
        for (int i = 0; i < array.length(); i++) {
            JSONObject rec = array.getJSONObject(i);
            String uuid = rec.optString("uuid", "");
            if (uuid.isEmpty()) {
                continue;
            }
            JSONObject existing = map.get(uuid);
            if (existing == null || rec.optLong("updatedAt") >= existing.optLong("updatedAt")) {
                map.put(uuid, rec);
            }
        }
    }
}
