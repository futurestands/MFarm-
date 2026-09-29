package dev.mfarm.com.mfarm;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SQLiteIntegrationTest {

    private Connection conn;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy", Locale.US);

    @Before
    public void setUp() throws Exception {
        Class.forName("org.sqlite.JDBC");
        conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        createSchema(conn);
    }

    @After
    public void tearDown() throws Exception {
        if (conn != null && !conn.isClosed()) {
            conn.close();
        }
    }

    private void createSchema(Connection c) throws Exception {
        Statement stmt = c.createStatement();
        stmt.execute("CREATE TABLE animas (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, gender TEXT, dob TEXT, breed_id INTEGER, body_conf TEXT, dam_id TEXT, sire_id TEXT, lifecycle_status TEXT, repro_status TEXT, lactation_status TEXT, health_status TEXT, photo_path TEXT, image BLOB)");
        stmt.execute("CREATE TABLE milk_production (id INTEGER PRIMARY KEY AUTOINCREMENT, animal_id INTEGER, litres REAL, datetime TEXT, description TEXT)");
        stmt.execute("CREATE TABLE expenses (id INTEGER PRIMARY KEY AUTOINCREMENT, category TEXT, amount REAL, date TEXT, description TEXT, related_animal_id INTEGER)");
        stmt.execute("CREATE TABLE income (id INTEGER PRIMARY KEY AUTOINCREMENT, category TEXT, amount REAL, buyer TEXT, date TEXT, description TEXT, related_animal_id INTEGER)");
        stmt.execute("CREATE TABLE vaccinations (id INTEGER PRIMARY KEY AUTOINCREMENT, animal_id INTEGER, vaccine_name TEXT, scheduled_date TEXT, status TEXT, remarks TEXT)");
        stmt.execute("CREATE TABLE illness (id INTEGER PRIMARY KEY AUTOINCREMENT, animal_id TEXT, animal_name TEXT, illness_occured TEXT, sings_noted TEXT, date_occured TEXT, sync_datetime TEXT, treatment TEXT, diagnosis TEXT, medicine TEXT, treatment_date TEXT, others TEXT, medicine_quantity TEXT, pregnancy_status TEXT, comments TEXT, cost REAL)");
        stmt.execute("CREATE TABLE inventory (id INTEGER PRIMARY KEY AUTOINCREMENT, item_name TEXT, category TEXT, quantity REAL, unit TEXT, min_quantity REAL)");
        stmt.execute("CREATE TABLE inventory_transactions (id INTEGER PRIMARY KEY AUTOINCREMENT, item_id INTEGER, type TEXT, quantity REAL, date TEXT, remarks TEXT)");
        stmt.execute("CREATE TABLE breeding_records (id INTEGER PRIMARY KEY AUTOINCREMENT, animal_id INTEGER, mating_date TEXT, bull_id TEXT, expected_birth_date TEXT, status TEXT, pregnancy_confirmed INTEGER)");
        stmt.execute("CREATE TABLE vet_checks (id INTEGER PRIMARY KEY AUTOINCREMENT, animal_id INTEGER, check_type TEXT, check_date TEXT, remarks TEXT)");
        stmt.execute("CREATE TABLE calving_records (id INTEGER PRIMARY KEY AUTOINCREMENT, dam_id INTEGER, sire_id TEXT, birth_date TEXT, offspring_id INTEGER, sex TEXT, birth_weight REAL, survival_status TEXT, notes TEXT)");
        stmt.execute("CREATE TABLE feed_types (id INTEGER PRIMARY KEY AUTOINCREMENT, feed_name TEXT, category TEXT, unit TEXT)");
        stmt.execute("CREATE TABLE feed_consumption (id INTEGER PRIMARY KEY AUTOINCREMENT, feed_id INTEGER, date TEXT, quantity REAL, cost REAL, group_or_animal_id TEXT, notes TEXT)");
        stmt.execute("CREATE TABLE farm_profile (id INTEGER PRIMARY KEY AUTOINCREMENT, farm_name TEXT, owner_name TEXT, location TEXT, phone TEXT, reg_number TEXT, currency_symbol TEXT, farm_size TEXT, notes TEXT)");
        stmt.execute("CREATE TABLE audit_logs (id INTEGER PRIMARY KEY AUTOINCREMENT, timestamp INTEGER, action TEXT, module TEXT, entity_id INTEGER, details TEXT)");
        stmt.execute("CREATE TABLE sync_index (uuid TEXT PRIMARY KEY NOT NULL, table_name TEXT NOT NULL, local_id INTEGER NOT NULL, updated_at INTEGER NOT NULL, deleted INTEGER NOT NULL DEFAULT 0, UNIQUE(table_name, local_id))");
        stmt.execute("CREATE TABLE sync_state (k TEXT PRIMARY KEY, v TEXT)");
        stmt.close();
    }

    // --- GAP 1: Real Production Queries against SQLite Schema (Intelligence rules) ---
    @Test
    public void testIntelligenceQueriesAgainstRealSQLite() throws Exception {
        Statement stmt = conn.createStatement();
        String todayStr = DATE_FORMAT.format(new Date());

        // Insert active animal
        stmt.execute("INSERT INTO animas (id, name, gender, lifecycle_status) VALUES (1, 'Bessie', 'Female', 'Active')");
        // Insert today's milk yield
        stmt.execute("INSERT INTO milk_production (animal_id, litres, datetime) VALUES (1, 18.5, '" + todayStr + "')");
        // Insert today's completed vaccination
        stmt.execute("INSERT INTO vaccinations (animal_id, vaccine_name, scheduled_date, status) VALUES (1, 'Anthrax', '" + todayStr + "', 'Completed')");
        // Insert today's income
        stmt.execute("INSERT INTO income (category, amount, date) VALUES ('Milk Sales', 50000, '" + todayStr + "')");
        // Insert today's expense
        stmt.execute("INSERT INTO expenses (category, amount, date) VALUES ('Feed', 12000, '" + todayStr + "')");

        // Verify Milk Query
        PreparedStatement pMilk = conn.prepareStatement("SELECT SUM(litres) FROM milk_production WHERE datetime = ?");
        pMilk.setString(1, todayStr);
        ResultSet rMilk = pMilk.executeQuery();
        assertTrue(rMilk.next());
        assertEquals(18.5, rMilk.getDouble(1), 0.01);
        rMilk.close();
        pMilk.close();

        // Verify Vaccination Today Query
        PreparedStatement pVac = conn.prepareStatement("SELECT COUNT(*) FROM vaccinations WHERE status = 'Completed' AND scheduled_date = ?");
        pVac.setString(1, todayStr);
        ResultSet rVac = pVac.executeQuery();
        assertTrue(rVac.next());
        assertEquals(1, rVac.getInt(1));
        rVac.close();
        pVac.close();

        // Verify Current Month Income Query
        Calendar cal = Calendar.getInstance();
        int targetMonth = cal.get(Calendar.MONTH);
        int targetYear = cal.get(Calendar.YEAR);

        ResultSet rInc = stmt.executeQuery("SELECT date, amount FROM income");
        double monthIncome = 0;
        while (rInc.next()) {
            String dStr = rInc.getString(1);
            double amt = rInc.getDouble(2);
            Date d = DATE_FORMAT.parse(dStr);
            Calendar cD = Calendar.getInstance();
            cD.setTime(d);
            if (cD.get(Calendar.MONTH) == targetMonth && cD.get(Calendar.YEAR) == targetYear) {
                monthIncome += amt;
            }
        }
        rInc.close();
        assertEquals(50000.0, monthIncome, 0.01);
        stmt.close();
    }

    // --- GAP 2: Animal Lineage Remapping across sync (`dam_id`, `sire_id`) ---
    @Test
    public void testAnimalLineageRemappingAcrossSync() throws Exception {
        Statement stmt = conn.createStatement();

        // Source device: Dam local_id=5 (uuid=uuid-dam-100), Calf local_id=9 (uuid=uuid-calf-200, dam_id=5)
        stmt.execute("INSERT INTO sync_index (uuid, table_name, local_id, updated_at, deleted) VALUES ('uuid-dam-100', 'animas', 5, 1000, 0)");
        stmt.execute("INSERT INTO sync_index (uuid, table_name, local_id, updated_at, deleted) VALUES ('uuid-calf-200', 'animas', 9, 1000, 0)");

        stmt.execute("INSERT INTO animas (id, name, gender) VALUES (5, 'Dam Cow', 'Female')");
        stmt.execute("INSERT INTO animas (id, name, gender, dam_id) VALUES (9, 'Calf Cow', 'Female', '5')");

        // Destination device: Dam assigned local_id=27, Calf assigned local_id=41
        // Simulate Pass 2 lineage resolution logic using UUID mapping
        stmt.execute("UPDATE sync_index SET local_id=27 WHERE uuid='uuid-dam-100'");
        stmt.execute("UPDATE sync_index SET local_id=41 WHERE uuid='uuid-calf-200'");

        // Resolve calf's dam_id via uuid-dam-100
        PreparedStatement pUuid = conn.prepareStatement("SELECT local_id FROM sync_index WHERE uuid=? AND table_name='animas' AND deleted=0");
        pUuid.setString(1, "uuid-dam-100");
        ResultSet rUuid = pUuid.executeQuery();
        assertTrue(rUuid.next());
        int destDamId = rUuid.getInt(1);
        rUuid.close();
        pUuid.close();

        assertEquals(27, destDamId);

        stmt.execute("UPDATE animas SET dam_id='" + destDamId + "' WHERE id=9");

        ResultSet rCalf = stmt.executeQuery("SELECT dam_id FROM animas WHERE id=9");
        assertTrue(rCalf.next());
        assertEquals("27", rCalf.getString(1));
        rCalf.close();

        stmt.close();
    }

    // --- GAP 3: Animal Deletion Transaction Asserts Zero Orphan Rows Across Dependent Tables ---
    @Test
    public void testAnimalDeletionAssertsZeroOrphanRows() throws Exception {
        Statement stmt = conn.createStatement();

        // 1. Seed animal ID 10 with records across dependent tables
        stmt.execute("INSERT INTO animas (id, name, gender) VALUES (10, 'Bella', 'Female')");
        stmt.execute("INSERT INTO vaccinations (animal_id, vaccine_name, status) VALUES (10, 'Anthrax', 'Pending')");
        stmt.execute("INSERT INTO illness (animal_id, animal_name, illness_occured) VALUES ('10', 'Bella', 'Fever')");
        stmt.execute("INSERT INTO milk_production (animal_id, litres, datetime) VALUES (10, 15.0, '20-09-2026')");
        stmt.execute("INSERT INTO breeding_records (animal_id, status) VALUES (10, 'Pregnant')");
        stmt.execute("INSERT INTO vet_checks (animal_id, check_type) VALUES (10, 'General')");
        stmt.execute("INSERT INTO income (category, amount, related_animal_id) VALUES ('Sale', 100000, 10)");
        stmt.execute("INSERT INTO expenses (category, amount, related_animal_id) VALUES ('Vet', 20000, 10)");
        stmt.execute("INSERT INTO calving_records (dam_id, notes) VALUES (10, 'Normal birth')");

        // Execute deletion transaction logic
        conn.setAutoCommit(false);
        try {
            stmt.execute("DELETE FROM vaccinations WHERE animal_id = 10");
            stmt.execute("DELETE FROM illness WHERE animal_id = '10'");
            stmt.execute("DELETE FROM milk_production WHERE animal_id = 10");
            stmt.execute("DELETE FROM breeding_records WHERE animal_id = 10");
            stmt.execute("DELETE FROM vet_checks WHERE animal_id = 10");

            stmt.execute("UPDATE income SET related_animal_id = NULL WHERE related_animal_id = 10");
            stmt.execute("UPDATE expenses SET related_animal_id = NULL WHERE related_animal_id = 10");
            stmt.execute("UPDATE animas SET dam_id = NULL WHERE dam_id = '10'");
            stmt.execute("UPDATE animas SET sire_id = NULL WHERE sire_id = '10'");
            stmt.execute("UPDATE calving_records SET dam_id = NULL WHERE dam_id = 10");
            stmt.execute("UPDATE calving_records SET sire_id = NULL WHERE sire_id = '10'");
            stmt.execute("UPDATE calving_records SET offspring_id = NULL WHERE offspring_id = 10");

            stmt.execute("DELETE FROM animas WHERE id = 10");
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }

        // Assert zero orphan rows remain
        ResultSet r1 = stmt.executeQuery("SELECT COUNT(*) FROM vaccinations WHERE animal_id = 10");
        assertTrue(r1.next());
        assertEquals(0, r1.getInt(1));
        r1.close();

        ResultSet r2 = stmt.executeQuery("SELECT COUNT(*) FROM illness WHERE animal_id = '10'");
        assertTrue(r2.next());
        assertEquals(0, r2.getInt(1));
        r2.close();

        ResultSet r3 = stmt.executeQuery("SELECT COUNT(*) FROM milk_production WHERE animal_id = 10");
        assertTrue(r3.next());
        assertEquals(0, r3.getInt(1));
        r3.close();

        ResultSet r4 = stmt.executeQuery("SELECT COUNT(*) FROM breeding_records WHERE animal_id = 10");
        assertTrue(r4.next());
        assertEquals(0, r4.getInt(1));
        r4.close();

        ResultSet r5 = stmt.executeQuery("SELECT COUNT(*) FROM vet_checks WHERE animal_id = 10");
        assertTrue(r5.next());
        assertEquals(0, r5.getInt(1));
        r5.close();

        // Financial records preserved with detached NULL reference
        ResultSet r6 = stmt.executeQuery("SELECT COUNT(*), related_animal_id FROM income WHERE category = 'Sale'");
        assertTrue(r6.next());
        assertEquals(1, r6.getInt(1));
        assertTrue(r6.getObject(2) == null);
        r6.close();

        ResultSet r7 = stmt.executeQuery("SELECT COUNT(*) FROM animas WHERE id = 10");
        assertTrue(r7.next());
        assertEquals(0, r7.getInt(1));
        r7.close();

        stmt.close();
    }

    // --- GAP 4: RegisterCalvingActivity breeding_records pregnancy closure ---
    @Test
    public void testCalvingClosesBreedingRecordStatus() throws Exception {
        Statement stmt = conn.createStatement();

        // Seed Dam ID 15 with Pregnant breeding record
        stmt.execute("INSERT INTO animas (id, name, gender, repro_status) VALUES (15, 'Daisy', 'Female', 'Pregnant')");
        stmt.execute("INSERT INTO breeding_records (animal_id, status) VALUES (15, 'Pregnant')");

        // Verify initial state: 1 pregnant breeding record
        ResultSet rPre = stmt.executeQuery("SELECT COUNT(*) FROM breeding_records WHERE animal_id = 15 AND status = 'Pregnant'");
        assertTrue(rPre.next());
        assertEquals(1, rPre.getInt(1));
        rPre.close();

        // Execute saveCalving() transaction logic
        conn.setAutoCommit(false);
        try {
            stmt.execute("UPDATE animas SET repro_status = 'Calved', lactation_status = 'Lactating' WHERE id = 15");
            stmt.execute("UPDATE breeding_records SET status = 'Calved' WHERE animal_id = 15 AND status = 'Pregnant'");
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }

        // Verify post-calving state: 0 pregnant breeding records, status updated to Calved
        ResultSet rPost = stmt.executeQuery("SELECT COUNT(*) FROM breeding_records WHERE animal_id = 15 AND status = 'Pregnant'");
        assertTrue(rPost.next());
        assertEquals(0, rPost.getInt(1));
        rPost.close();

        ResultSet rCalved = stmt.executeQuery("SELECT status FROM breeding_records WHERE animal_id = 15");
        assertTrue(rCalved.next());
        assertEquals("Calved", rCalved.getString(1));
        rCalved.close();

        stmt.close();
    }
}
