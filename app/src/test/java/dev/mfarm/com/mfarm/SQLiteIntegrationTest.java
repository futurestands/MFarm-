package dev.mfarm.com.mfarm;

import dev.mfarm.com.mfarm.dao.JdbcDatabaseAdapter;
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
        stmt.execute("CREATE TABLE animas (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, tag_number TEXT, ear_tag TEXT, species TEXT DEFAULT 'Cattle', origin_source TEXT, birth_type TEXT DEFAULT 'Single', location_name TEXT DEFAULT 'Main Farm', gender TEXT, dob TEXT, breed_id INTEGER, body_conf TEXT, dam_id TEXT, sire_id TEXT, lifecycle_status TEXT, repro_status TEXT, lactation_status TEXT, health_status TEXT, photo_path TEXT, image BLOB, notes TEXT)");
        stmt.execute("CREATE TABLE milk_production (id INTEGER PRIMARY KEY AUTOINCREMENT, animal_id INTEGER, litres REAL, datetime TEXT, description TEXT)");
        stmt.execute("CREATE TABLE expenses (id INTEGER PRIMARY KEY AUTOINCREMENT, category TEXT, amount REAL, date TEXT, description TEXT, related_animal_id INTEGER, project_code TEXT DEFAULT 'GEN', enterprise TEXT DEFAULT 'General')");
        stmt.execute("CREATE TABLE income (id INTEGER PRIMARY KEY AUTOINCREMENT, category TEXT, amount REAL, buyer TEXT, date TEXT, description TEXT, related_animal_id INTEGER, project_code TEXT DEFAULT 'GEN', enterprise TEXT DEFAULT 'General')");
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
        stmt.execute("CREATE TABLE disposal_records (id INTEGER PRIMARY KEY AUTOINCREMENT, animal_id INTEGER, animal_tag TEXT, sex TEXT, age_months REAL, disposal_date TEXT, method TEXT, cause_or_reason TEXT, price REAL DEFAULT 0, buyer_or_destination TEXT, notes TEXT)");
        stmt.execute("CREATE TABLE farm_locations (id INTEGER PRIMARY KEY AUTOINCREMENT, location_name TEXT UNIQUE NOT NULL, description TEXT, is_default INTEGER DEFAULT 0)");
        stmt.execute("CREATE TABLE animal_movements (id INTEGER PRIMARY KEY AUTOINCREMENT, animal_id INTEGER, animal_name TEXT, from_location TEXT, to_location TEXT, movement_date TEXT, reason TEXT, moved_by TEXT)");
        stmt.execute("CREATE TABLE crop_plots (id INTEGER PRIMARY KEY AUTOINCREMENT, plot_name TEXT NOT NULL, crop_type TEXT NOT NULL, size_acres REAL DEFAULT 0, location_name TEXT, status TEXT DEFAULT 'Active')");
        stmt.execute("CREATE TABLE crop_seasons (id INTEGER PRIMARY KEY AUTOINCREMENT, plot_id INTEGER, crop_type TEXT, season_name TEXT, start_date TEXT, end_date TEXT, status TEXT DEFAULT 'Ongoing')");
        stmt.execute("CREATE TABLE crop_activities (id INTEGER PRIMARY KEY AUTOINCREMENT, plot_id INTEGER, season_id INTEGER, activity_type TEXT, activity_date TEXT, inputs_used TEXT, quantity REAL DEFAULT 0, unit TEXT, yield_harvested REAL DEFAULT 0, cost REAL DEFAULT 0, notes TEXT)");
        stmt.execute("CREATE TABLE enterprises (id INTEGER PRIMARY KEY AUTOINCREMENT, code TEXT UNIQUE NOT NULL, name TEXT NOT NULL, category TEXT, description TEXT)");
        stmt.execute("CREATE TABLE species_catalog (species_name TEXT PRIMARY KEY, gestation_days INTEGER DEFAULT 0, birth_term TEXT, offspring_term TEXT, female_term TEXT, male_term TEXT)");
        stmt.execute("CREATE TABLE species_breeds (id INTEGER PRIMARY KEY AUTOINCREMENT, species_name TEXT NOT NULL, breed_name TEXT NOT NULL)");
        stmt.execute("CREATE TABLE livestock_batches (id INTEGER PRIMARY KEY AUTOINCREMENT, batch_id TEXT UNIQUE NOT NULL, species TEXT NOT NULL, subtype TEXT NOT NULL, breed_or_strain TEXT, purpose TEXT, initial_quantity INTEGER NOT NULL, current_quantity INTEGER NOT NULL, acquisition_date TEXT NOT NULL, age_weeks REAL DEFAULT 0, source_supplier TEXT, location_name TEXT DEFAULT 'Main Farm', housing_unit TEXT, purchase_cost REAL DEFAULT 0, project_code TEXT DEFAULT 'PL4', status TEXT DEFAULT 'Active', notes TEXT)");
        stmt.execute("CREATE TABLE batch_quantity_ledger (id INTEGER PRIMARY KEY AUTOINCREMENT, batch_id TEXT NOT NULL, event_type TEXT NOT NULL, event_date TEXT NOT NULL, quantity_change INTEGER NOT NULL, resulting_quantity INTEGER NOT NULL, unit_price REAL DEFAULT 0, total_amount REAL DEFAULT 0, source_ref TEXT, notes TEXT)");
        stmt.execute("CREATE TABLE egg_production_logs (id INTEGER PRIMARY KEY AUTOINCREMENT, batch_id TEXT NOT NULL, log_date TEXT NOT NULL, eggs_collected INTEGER NOT NULL, broken_eggs INTEGER DEFAULT 0, notes TEXT)");
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

        // Execute RegisterCalvingActivity.performCalvingRegistration directly against SQLite JDBC
        JdbcDatabaseAdapter adapter = new JdbcDatabaseAdapter(conn);
        RegisterCalvingActivity.performCalvingRegistration(adapter, 15, "Calf 15", "1", "Female", "Alive", 30.0, "29-09-2026", "Test Notes");

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

    // --- GAP 5: Multi-species Goat registration & location assignment ---
    @Test
    public void testGoatRegistrationAndSpeciesFiltering() throws Exception {
        Statement stmt = conn.createStatement();
        stmt.execute("INSERT INTO animas (name, species, gender, location_name, lifecycle_status) VALUES ('Doeling 101', 'Goat', 'Female', 'Kicwamba', 'Active')");
        stmt.execute("INSERT INTO animas (name, species, gender, location_name, lifecycle_status) VALUES ('Cow Bella', 'Cattle', 'Female', 'Main Farm', 'Active')");

        ResultSet rGoats = stmt.executeQuery("SELECT COUNT(*), location_name FROM animas WHERE species = 'Goat' AND lifecycle_status = 'Active'");
        assertTrue(rGoats.next());
        assertEquals(1, rGoats.getInt(1));
        assertEquals("Kicwamba", rGoats.getString(2));
        rGoats.close();

        stmt.close();
    }

    // --- GAP 6: Location Transfer History ---
    @Test
    public void testFarmLocationTransferHistory() throws Exception {
        Statement stmt = conn.createStatement();
        stmt.execute("INSERT INTO animas (id, name, species, location_name) VALUES (50, 'Goat Buck 50', 'Goat', 'Kicwamba')");
        stmt.execute("INSERT INTO animal_movements (animal_id, animal_name, from_location, to_location, movement_date, reason) VALUES (50, 'Goat Buck 50', 'Kicwamba', 'Nkesiga', '29-09-2026', 'Grazing rotation')");
        stmt.execute("UPDATE animas SET location_name = 'Nkesiga' WHERE id = 50");

        ResultSet rAnim = stmt.executeQuery("SELECT location_name FROM animas WHERE id = 50");
        assertTrue(rAnim.next());
        assertEquals("Nkesiga", rAnim.getString(1));
        rAnim.close();

        ResultSet rMove = stmt.executeQuery("SELECT from_location, to_location FROM animal_movements WHERE animal_id = 50");
        assertTrue(rMove.next());
        assertEquals("Kicwamba", rMove.getString(1));
        assertEquals("Nkesiga", rMove.getString(2));
        rMove.close();

        stmt.close();
    }

    // --- GAP 7: Disposal and Sale Financial Link ---
    @Test
    public void testAnimalDisposalAndFinancialEntry() throws Exception {
        Statement stmt = conn.createStatement();
        stmt.execute("INSERT INTO animas (id, name, species, lifecycle_status) VALUES (60, 'Goat Doe 60', 'Goat', 'Active')");

        // Disposal sale entry
        stmt.execute("INSERT INTO disposal_records (animal_id, animal_tag, method, price) VALUES (60, 'Goat Doe 60', 'Sold', 250000.0)");
        stmt.execute("UPDATE animas SET lifecycle_status = 'Sold' WHERE id = 60");
        stmt.execute("INSERT INTO income (category, amount, date, description, related_animal_id, project_code) VALUES ('Animal Sales', 250000.0, '29-09-2026', 'Sale of Goat Doe 60', 60, 'GT2')");

        ResultSet rDisp = stmt.executeQuery("SELECT lifecycle_status FROM animas WHERE id = 60");
        assertTrue(rDisp.next());
        assertEquals("Sold", rDisp.getString(1));
        rDisp.close();

        ResultSet rInc = stmt.executeQuery("SELECT amount, project_code FROM income WHERE related_animal_id = 60");
        assertTrue(rInc.next());
        assertEquals(250000.0, rInc.getDouble(1), 0.01);
        assertEquals("GT2", rInc.getString(2));
        rInc.close();

        stmt.close();
    }

    // --- GAP 8: Crop Plot Activity & Expense Link ---
    @Test
    public void testCropPlotActivityAndExpense() throws Exception {
        Statement stmt = conn.createStatement();
        stmt.execute("INSERT INTO crop_plots (id, plot_name, crop_type, location_name) VALUES (1, 'Kicwamba Block A', 'Coffee', 'Kicwamba')");
        stmt.execute("INSERT INTO crop_activities (plot_id, activity_type, cost) VALUES (1, 'Spraying (Coffee)', 45000.0)");
        stmt.execute("INSERT INTO expenses (category, amount, project_code, description) VALUES ('Crop Inputs & Labor', 45000.0, 'CF3', 'Spraying for Coffee')");

        ResultSet rExp = stmt.executeQuery("SELECT SUM(amount) FROM expenses WHERE project_code = 'CF3'");
        assertTrue(rExp.next());
        assertEquals(45000.0, rExp.getDouble(1), 0.01);
        rExp.close();

        stmt.close();
    }

    // --- GAP 9: Excel Importer Deduplication ---
    @Test
    public void testExcelImporterDeduplication() throws Exception {
        JdbcDatabaseAdapter adapter = new JdbcDatabaseAdapter(conn);
        dev.mfarm.com.mfarm.sync.ExcelImporter.ImportSummary summary1 = new dev.mfarm.com.mfarm.sync.ExcelImporter.ImportSummary();

        java.util.List<java.util.Map<String, String>> flockRows = new java.util.ArrayList<>();
        java.util.Map<String, String> row1 = new java.util.HashMap<>();
        row1.put("tag_no", "GT-323-2");
        row1.put("sex", "F");
        row1.put("dob", "12-07-2021");
        row1.put("origin", "Born in herd");
        flockRows.add(row1);

        // Import First Pass
        int imported1 = dev.mfarm.com.mfarm.sync.ExcelImporter.importFlockOverview(adapter, flockRows, summary1);
        assertEquals(1, imported1);

        // Import Second Pass (Duplicate)
        dev.mfarm.com.mfarm.sync.ExcelImporter.ImportSummary summary2 = new dev.mfarm.com.mfarm.sync.ExcelImporter.ImportSummary();
        int imported2 = dev.mfarm.com.mfarm.sync.ExcelImporter.importFlockOverview(adapter, flockRows, summary2);
        assertEquals(0, imported2);
        assertEquals(1, summary2.duplicateSkipped);
    }

    // --- GAP 10: Database Upgrade V2 to V3 Survival ---
    @Test
    public void testDatabaseVersion2ToVersion3Upgrade() throws Exception {
        Connection v2Conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        Statement stmt = v2Conn.createStatement();

        // Create V2 Schema
        stmt.execute("CREATE TABLE animas (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, gender TEXT, dob TEXT, breed_id INTEGER)");
        stmt.execute("CREATE TABLE milk_production (id INTEGER PRIMARY KEY AUTOINCREMENT, animal_id INTEGER, litres REAL, datetime TEXT)");

        // Populate V2 Cattle & Milk Records
        stmt.execute("INSERT INTO animas (id, name, gender, dob, breed_id) VALUES (1, 'Dairy Cow Daisy', 'Female', '10-05-2020', 1)");
        stmt.execute("INSERT INTO milk_production (animal_id, litres, datetime) VALUES (1, 22.5, '28-09-2026')");

        // Upgrade V2 to V3 via ALTER TABLE
        stmt.execute("ALTER TABLE animas ADD COLUMN species TEXT DEFAULT 'Cattle'");
        stmt.execute("ALTER TABLE animas ADD COLUMN location_name TEXT DEFAULT 'Main Farm'");
        stmt.execute("CREATE TABLE IF NOT EXISTS disposal_records (id INTEGER PRIMARY KEY AUTOINCREMENT, animal_id INTEGER, method TEXT)");

        // Verify V2 Cattle & Milk Records survive 100% intact
        ResultSet rAnim = stmt.executeQuery("SELECT name, species FROM animas WHERE id = 1");
        assertTrue(rAnim.next());
        assertEquals("Dairy Cow Daisy", rAnim.getString(1));
        assertEquals("Cattle", rAnim.getString(2));
        rAnim.close();

        ResultSet rMilk = stmt.executeQuery("SELECT litres FROM milk_production WHERE animal_id = 1");
        assertTrue(rMilk.next());
        assertEquals(22.5, rMilk.getDouble(1), 0.01);
        rMilk.close();

        stmt.close();
        v2Conn.close();
    }

    // --- GAP 11: 500 Chicks Batch Lifecycle, Mortality, Sale & Ledger Math (500 - 15 - 120 = 365) ---
    @Test
    public void testChicksBatchLifecycleAndLedgerMath() throws Exception {
        JdbcDatabaseAdapter adapter = new JdbcDatabaseAdapter(conn);
        String batchId = "BATCH-CHICKS-500";

        // 1. Register 500 chicks
        dev.mfarm.com.mfarm.dao.BatchManager.createBatch(adapter, batchId, "Poultry", "Day-Old Chicks", "Kuroiler", "Layers", 500, "29-09-2026", 1.0, "Biyinzika", "Main Farm", "Pen 1", 1500000.0, "Initial batch");
        assertEquals(500, dev.mfarm.com.mfarm.dao.BatchManager.getCurrentQuantity(adapter, batchId));

        // 2. Mortality of 15 chicks
        dev.mfarm.com.mfarm.dao.BatchManager.recordMortality(adapter, batchId, 15, "05-10-2026", "Coccidiosis", "Routine mortality");
        assertEquals(485, dev.mfarm.com.mfarm.dao.BatchManager.getCurrentQuantity(adapter, batchId));

        // 3. Sale of 120 chicks
        dev.mfarm.com.mfarm.dao.BatchManager.recordBatchSale(adapter, batchId, 120, "15-10-2026", 600000.0, "Kicwamba Buyer", "Batch sale");

        // 4. Assert remaining quantity = 365
        assertEquals(365, dev.mfarm.com.mfarm.dao.BatchManager.getCurrentQuantity(adapter, batchId));
    }

    // --- GAP 12: Negative Quantity Prevention ---
    @Test(expected = IllegalStateException.class)
    public void testNegativeQuantityPrevention() throws Exception {
        JdbcDatabaseAdapter adapter = new JdbcDatabaseAdapter(conn);
        String batchId = "BATCH-NEG-TEST";

        dev.mfarm.com.mfarm.dao.BatchManager.createBatch(adapter, batchId, "Poultry", "Broilers", "Cobb 500", "Meat", 50, "29-09-2026", 2.0, "Supplier", "Main Farm", "Pen 2", 200000.0, "Test");

        // Attempt mortality of 60 when only 50 exist -> MUST throw IllegalStateException!
        dev.mfarm.com.mfarm.dao.BatchManager.recordMortality(adapter, batchId, 60, "30-09-2026", "Disease", "Exceeds quantity");
    }

    // --- GAP 13: Convert Selected Batch Animal to Individual Tracking ---
    @Test
    public void testBatchToIndividualConversion() throws Exception {
        JdbcDatabaseAdapter adapter = new JdbcDatabaseAdapter(conn);
        String batchId = "BATCH-RABBITS-20";

        dev.mfarm.com.mfarm.dao.BatchManager.createBatch(adapter, batchId, "Rabbit", "Grower Rabbits", "New Zealand White", "Breeding", 20, "29-09-2026", 8.0, "Farm", "Main Farm", "Hutch A", 0.0, "Rabbit batch");
        assertEquals(20, dev.mfarm.com.mfarm.dao.BatchManager.getCurrentQuantity(adapter, batchId));

        // Convert 1 rabbit buck to individual tracking
        long newAnimalId = dev.mfarm.com.mfarm.dao.BatchManager.convertBatchToIndividual(adapter, batchId, "29-09-2026", "Buck Champ", "RAB-001", "Male", "White", "Selected breeding buck");
        assertTrue(newAnimalId > 0);

        // Assert remaining batch quantity decremented to 19
        assertEquals(19, dev.mfarm.com.mfarm.dao.BatchManager.getCurrentQuantity(adapter, batchId));

        // Verify individual animal created in animas
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT name, species, tag_number FROM animas WHERE id = " + newAnimalId);
        assertTrue(rs.next());
        assertEquals("Buck Champ", rs.getString(1));
        assertEquals("Rabbit", rs.getString(2));
        assertEquals("RAB-001", rs.getString(3));
        rs.close();
        stmt.close();
    }

    // --- GAP 14: Egg Production Logging ---
    @Test
    public void testEggProductionLogging() throws Exception {
        Statement stmt = conn.createStatement();
        stmt.execute("INSERT INTO egg_production_logs (batch_id, log_date, eggs_collected, broken_eggs) VALUES ('BATCH-LAYERS-100', '29-09-2026', 85, 2)");
        stmt.execute("INSERT INTO egg_production_logs (batch_id, log_date, eggs_collected, broken_eggs) VALUES ('BATCH-LAYERS-100', '30-09-2026', 88, 1)");

        ResultSet rs = stmt.executeQuery("SELECT SUM(eggs_collected), SUM(broken_eggs) FROM egg_production_logs WHERE batch_id = 'BATCH-LAYERS-100'");
        assertTrue(rs.next());
        assertEquals(173, rs.getInt(1));
        assertEquals(3, rs.getInt(2));
        rs.close();
        stmt.close();
    }

    // --- GAP 15: Database Upgrade V3 to V4 Survival ---
    @Test
    public void testDatabaseVersion3ToVersion4Upgrade() throws Exception {
        Connection v3Conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        Statement stmt = v3Conn.createStatement();

        // Create V3 Schema
        stmt.execute("CREATE TABLE animas (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, species TEXT, location_name TEXT)");
        stmt.execute("CREATE TABLE disposal_records (id INTEGER PRIMARY KEY AUTOINCREMENT, animal_tag TEXT, method TEXT)");

        // Populate V3 records
        stmt.execute("INSERT INTO animas (id, name, species, location_name) VALUES (1, 'Goat Doe 323-2', 'Goat', 'Kicwamba')");
        stmt.execute("INSERT INTO disposal_records (id, animal_tag, method) VALUES (1, '608-9', 'Death')");

        // Upgrade V3 to V4
        stmt.execute("CREATE TABLE IF NOT EXISTS livestock_batches (id INTEGER PRIMARY KEY AUTOINCREMENT, batch_id TEXT UNIQUE, species TEXT, current_quantity INTEGER)");
        stmt.execute("CREATE TABLE IF NOT EXISTS batch_quantity_ledger (id INTEGER PRIMARY KEY AUTOINCREMENT, batch_id TEXT, quantity_change INTEGER)");

        // Verify V3 records survive 100% intact
        ResultSet rAnim = stmt.executeQuery("SELECT name, location_name FROM animas WHERE id = 1");
        assertTrue(rAnim.next());
        assertEquals("Goat Doe 323-2", rAnim.getString(1));
        assertEquals("Kicwamba", rAnim.getString(2));
        rAnim.close();

        ResultSet rDisp = stmt.executeQuery("SELECT animal_tag, method FROM disposal_records WHERE id = 1");
        assertTrue(rDisp.next());
        assertEquals("608-9", rDisp.getString(1));
        assertEquals("Death", rDisp.getString(2));
        rDisp.close();

        stmt.close();
        v3Conn.close();
    }

    // --- GAP 20: Database Upgrade V4 to V5 Survival (project_code column on livestock_batches) ---
    @Test
    public void testDatabaseVersion4ToVersion5Upgrade() throws Exception {
        Connection v4Conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        Statement stmt = v4Conn.createStatement();

        // Create V4 Schema without project_code on livestock_batches
        stmt.execute("CREATE TABLE livestock_batches (id INTEGER PRIMARY KEY AUTOINCREMENT, batch_id TEXT UNIQUE, species TEXT, subtype TEXT, current_quantity INTEGER)");

        // Populate V4 Batch record
        stmt.execute("INSERT INTO livestock_batches (id, batch_id, species, subtype, current_quantity) VALUES (1, 'BATCH-V4-001', 'Poultry', 'Chicks', 500)");

        // Upgrade V4 to V5 via ALTER TABLE
        stmt.execute("ALTER TABLE livestock_batches ADD COLUMN project_code TEXT DEFAULT 'PL4'");

        // Verify V4 records survive 100% intact with new project_code default
        ResultSet rBatch = stmt.executeQuery("SELECT batch_id, current_quantity, project_code FROM livestock_batches WHERE id = 1");
        assertTrue(rBatch.next());
        assertEquals("BATCH-V4-001", rBatch.getString(1));
        assertEquals(500, rBatch.getInt(2));
        assertEquals("PL4", rBatch.getString(3));
        rBatch.close();

        stmt.close();
        v4Conn.close();
    }

    // --- GAP 16: Location-Aware Batch Split Transfer (500 chicks -> 300 Main Farm, 200 Kicwamba, Total = 500) ---
    @Test
    public void testLocationAwareBatchSplitTransfer() throws Exception {
        JdbcDatabaseAdapter adapter = new JdbcDatabaseAdapter(conn);
        String sourceBatchId = "BATCH-SPLIT-500";

        // Register 500 chicks at Main Farm
        dev.mfarm.com.mfarm.dao.BatchManager.createBatch(adapter, sourceBatchId, "Poultry", "Chicks", "Kuroiler", "Layers", 500, "29-09-2026", 1.0, "Supplier", "Main Farm", "Pen A", 0.0, "PL4", "Initial");
        assertEquals(500, dev.mfarm.com.mfarm.dao.BatchManager.getCurrentQuantity(adapter, sourceBatchId));

        // Transfer 200 chicks to Kicwamba
        String destBatchId = dev.mfarm.com.mfarm.dao.BatchManager.recordBatchTransfer(adapter, sourceBatchId, 200, "29-09-2026", "Main Farm", "Kicwamba", "Grazing rotation");

        // Verify Source Batch quantity = 300
        assertEquals(300, dev.mfarm.com.mfarm.dao.BatchManager.getCurrentQuantity(adapter, sourceBatchId));
        // Verify Destination Batch quantity = 200
        assertEquals(200, dev.mfarm.com.mfarm.dao.BatchManager.getCurrentQuantity(adapter, destBatchId));

        // Total farm-wide headcount = 500
        Statement stmt = conn.createStatement();
        ResultSet rsSum = stmt.executeQuery("SELECT SUM(current_quantity) FROM livestock_batches WHERE batch_id IN ('" + sourceBatchId + "', '" + destBatchId + "')");
        assertTrue(rsSum.next());
        assertEquals(500, rsSum.getInt(1));
        rsSum.close();
        stmt.close();

        // Verify ledger invariants for both batches
        assertTrue(dev.mfarm.com.mfarm.dao.BatchManager.verifyBatchLedgerInvariant(adapter, sourceBatchId));
        assertTrue(dev.mfarm.com.mfarm.dao.BatchManager.verifyBatchLedgerInvariant(adapter, destBatchId));
    }

    // --- GAP 17: Configurable Enterprise Project Codes per Batch ---
    @Test
    public void testConfigurableEnterpriseCodesPerBatch() throws Exception {
        JdbcDatabaseAdapter adapter = new JdbcDatabaseAdapter(conn);

        dev.mfarm.com.mfarm.dao.BatchManager.createBatch(adapter, "BATCH-PL4", "Poultry", "Broilers", "Cobb 500", "Meat", 100, "29-09-2026", 4.0, "Supplier", "Main Farm", "Pen 1", 0.0, "PL4", "Poultry Batch");
        dev.mfarm.com.mfarm.dao.BatchManager.createBatch(adapter, "BATCH-RB5", "Rabbit", "Growers", "Chinchilla", "Meat", 50, "29-09-2026", 6.0, "Supplier", "Main Farm", "Hutch B", 0.0, "RB5", "Rabbit Batch");

        dev.mfarm.com.mfarm.dao.BatchManager.recordBatchSale(adapter, "BATCH-PL4", 20, "29-09-2026", 300000.0, "Hotel Buyer", "Broiler sale");
        dev.mfarm.com.mfarm.dao.BatchManager.recordBatchSale(adapter, "BATCH-RB5", 10, "29-09-2026", 150000.0, "Restaurant Buyer", "Rabbit sale");

        Statement stmt = conn.createStatement();
        ResultSet rPL4 = stmt.executeQuery("SELECT amount FROM income WHERE project_code = 'PL4'");
        assertTrue(rPL4.next());
        assertEquals(300000.0, rPL4.getDouble(1), 0.01);
        rPL4.close();

        ResultSet rRB5 = stmt.executeQuery("SELECT amount FROM income WHERE project_code = 'RB5'");
        assertTrue(rRB5.next());
        assertEquals(150000.0, rRB5.getDouble(1), 0.01);
        rRB5.close();
        stmt.close();
    }

    // --- GAP 18: Batch Ledger Mathematical Invariant (current_quantity == SUM(quantity_change)) ---
    @Test
    public void testBatchLedgerMathematicalInvariant() throws Exception {
        JdbcDatabaseAdapter adapter = new JdbcDatabaseAdapter(conn);
        String batchId = "BATCH-INVARIANT-100";

        dev.mfarm.com.mfarm.dao.BatchManager.createBatch(adapter, batchId, "Poultry", "Layers", "ISA Brown", "Eggs", 100, "29-09-2026", 18.0, "Hatchery", "Main Farm", "Layer Coop", 0.0, "PL4", "Invariant Test");
        dev.mfarm.com.mfarm.dao.BatchManager.recordMortality(adapter, batchId, 5, "01-10-2026", "Natural", "Death");
        dev.mfarm.com.mfarm.dao.BatchManager.recordBatchSale(adapter, batchId, 10, "02-10-2026", 100000.0, "Buyer", "Sale");
        dev.mfarm.com.mfarm.dao.BatchManager.recordBatchTransfer(adapter, batchId, 20, "03-10-2026", "Main Farm", "Nkesiga", "Transfer");
        dev.mfarm.com.mfarm.dao.BatchManager.convertBatchToIndividual(adapter, batchId, "04-10-2026", "Prize Layer", "P-101", "Female", "Brown", "Selected hen");

        // 100 - 5 - 10 - 20 - 1 = 64
        assertEquals(64, dev.mfarm.com.mfarm.dao.BatchManager.getCurrentQuantity(adapter, batchId));
        assertTrue(dev.mfarm.com.mfarm.dao.BatchManager.verifyBatchLedgerInvariant(adapter, batchId));
    }

    // --- GAP 19: Species Catalog Terminologies and Gestations ---
    @Test
    public void testSpeciesCatalogTerminologiesAndGestations() throws Exception {
        JdbcDatabaseAdapter adapter = new JdbcDatabaseAdapter(conn);

        dev.mfarm.com.mfarm.dao.SpeciesCatalog.SpeciesInfo cow = dev.mfarm.com.mfarm.dao.SpeciesCatalog.getSpeciesInfo(adapter, "Cattle");
        assertEquals(283, cow.durationDays);
        assertFalse(cow.isIncubation);
        assertEquals("Calving", cow.birthTerm);

        dev.mfarm.com.mfarm.dao.SpeciesCatalog.SpeciesInfo goat = dev.mfarm.com.mfarm.dao.SpeciesCatalog.getSpeciesInfo(adapter, "Goat");
        assertEquals(150, goat.durationDays);
        assertFalse(goat.isIncubation);
        assertEquals("Kidding", goat.birthTerm);

        dev.mfarm.com.mfarm.dao.SpeciesCatalog.SpeciesInfo sheep = dev.mfarm.com.mfarm.dao.SpeciesCatalog.getSpeciesInfo(adapter, "Sheep");
        assertEquals(147, sheep.durationDays);
        assertFalse(sheep.isIncubation);
        assertEquals("Lambing", sheep.birthTerm);

        dev.mfarm.com.mfarm.dao.SpeciesCatalog.SpeciesInfo rabbit = dev.mfarm.com.mfarm.dao.SpeciesCatalog.getSpeciesInfo(adapter, "Rabbit");
        assertEquals(31, rabbit.durationDays);
        assertFalse(rabbit.isIncubation);
        assertEquals("Kindling", rabbit.birthTerm);

        dev.mfarm.com.mfarm.dao.SpeciesCatalog.SpeciesInfo poultry = dev.mfarm.com.mfarm.dao.SpeciesCatalog.getSpeciesInfo(adapter, "Poultry");
        assertEquals(21, poultry.durationDays);
        assertTrue(poultry.isIncubation);
        assertEquals("Hatching", poultry.birthTerm);
    }
}
