package dev.mfarm.com.mfarm.sync;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FarmMergerTest {

    @Test
    public void newerRecordWins() throws Exception {
        JSONObject local = snapshot("farm-1", record("a", "animas", 10, "Bella"));
        JSONObject remote = snapshot("farm-1", record("a", "animas", 20, "Bella-updated"));
        JSONObject merged = FarmMerger.merge(local, remote);
        JSONObject rec = merged.getJSONArray("records").getJSONObject(0);
        assertEquals(20, rec.getLong("updatedAt"));
        assertEquals("Bella-updated", rec.getJSONObject("fields").getString("name"));
    }

    @Test
    public void unionKeepsUniqueAnimals() throws Exception {
        JSONObject local = snapshot("farm-1", record("a", "animas", 10, "Bella"));
        JSONObject remote = snapshot("farm-1", record("b", "animas", 11, "Daisy"));
        JSONObject merged = FarmMerger.merge(local, remote);
        assertEquals(2, merged.getJSONArray("records").length());
    }

    @Test(expected = Exception.class)
    public void differentFarmsRejected() throws Exception {
        JSONObject local = snapshot("farm-1", record("a", "animas", 10, "Bella"));
        JSONObject remote = snapshot("farm-2", record("b", "animas", 11, "Daisy"));
        FarmMerger.merge(local, remote);
    }

    private static JSONObject snapshot(String farmId, JSONObject record) throws Exception {
        JSONObject root = new JSONObject();
        root.put("format", 1);
        root.put("farmId", farmId);
        root.put("farmName", "Test");
        root.put("exportedAt", 1);
        JSONArray records = new JSONArray();
        records.put(record);
        root.put("records", records);
        return root;
    }

    private static JSONObject record(String uuid, String table, long updatedAt, String name) throws Exception {
        JSONObject rec = new JSONObject();
        rec.put("uuid", uuid);
        rec.put("table", table);
        rec.put("updatedAt", updatedAt);
        rec.put("deleted", false);
        JSONObject fields = new JSONObject();
        fields.put("name", name);
        rec.put("fields", fields);
        rec.put("links", new JSONObject());
        return rec;
    }

    @Test
    public void joinCodeRoundTrip() {
        String code = FarmIdentity.generateJoinCode();
        assertTrue(FarmIdentity.isValidJoinCode(code));
        assertEquals(9, FarmIdentity.formatJoinCode(code).length());
        assertEquals(8, FarmIdentity.normalizeJoinCode(code).length());
    }
}
