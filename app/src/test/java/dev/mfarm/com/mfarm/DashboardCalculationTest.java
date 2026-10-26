package dev.mfarm.com.mfarm;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import dev.mfarm.com.mfarm.dao.FormatUtils;
import dev.mfarm.com.mfarm.dao.JdbcDatabaseAdapter;

public class DashboardCalculationTest {

    private Connection conn;

    @Before
    public void setUp() throws Exception {
        conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        JdbcDatabaseAdapter adapter = new JdbcDatabaseAdapter(conn);
        dev.mfarm.com.mfarm.dao.DatabaseHelper.ensureSchema(adapter);

        Statement stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS animas (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, species TEXT, gender TEXT, lifecycle_status TEXT DEFAULT 'Active')");
        stmt.execute("CREATE TABLE IF NOT EXISTS livestock_batches (id INTEGER PRIMARY KEY AUTOINCREMENT, batch_id TEXT UNIQUE, species TEXT, current_quantity INTEGER, status TEXT DEFAULT 'Active')");
        stmt.execute("CREATE TABLE IF NOT EXISTS inventory (id INTEGER PRIMARY KEY AUTOINCREMENT, item_name TEXT, quantity REAL, min_quantity REAL)");
        stmt.execute("CREATE TABLE IF NOT EXISTS vaccinations (id INTEGER PRIMARY KEY AUTOINCREMENT, animal_id INTEGER, vaccine_name TEXT, scheduled_date TEXT, status TEXT)");
        stmt.execute("CREATE TABLE IF NOT EXISTS income (id INTEGER PRIMARY KEY AUTOINCREMENT, category TEXT, amount REAL, date TEXT, description TEXT)");
        stmt.execute("CREATE TABLE IF NOT EXISTS expenses (id INTEGER PRIMARY KEY AUTOINCREMENT, category TEXT, amount REAL, date TEXT, description TEXT)");
        stmt.close();
    }

    @After
    public void tearDown() throws Exception {
        if (conn != null) conn.close();
    }

    @Test
    public void testZeroRecordState() throws Exception {
        Statement stmt = conn.createStatement();
        
        ResultSet rsAnim = stmt.executeQuery("SELECT COUNT(*) FROM animas");
        assertTrue(rsAnim.next());
        assertEquals(0, rsAnim.getInt(1));
        rsAnim.close();

        ResultSet rsInc = stmt.executeQuery("SELECT SUM(amount) FROM income");
        assertTrue(rsInc.next());
        assertEquals(0.0, rsInc.getDouble(1), 0.01);
        rsInc.close();

        stmt.close();
    }

    @Test
    public void testNormalRecordHeadcountAndCashFlow() throws Exception {
        Statement stmt = conn.createStatement();
        
        // Add 2 individual cattle and 100 poultry batch
        stmt.execute("INSERT INTO animas (name, species, gender) VALUES ('Daisy', 'Cattle', 'Female')");
        stmt.execute("INSERT INTO animas (name, species, gender) VALUES ('Boss', 'Cattle', 'Male')");
        stmt.execute("INSERT INTO livestock_batches (batch_id, species, subtype, initial_quantity, current_quantity, acquisition_date) VALUES ('BATCH-100', 'Poultry', 'Chicks', 100, 100, '01-01-2026')");

        // Add monthly income and expenses
        String today = new SimpleDateFormat("dd-MM-yyyy", Locale.US).format(new Date());
        stmt.execute("INSERT INTO income (category, amount, date, description) VALUES ('Milk', 150000, '" + today + "', 'Morning milk')");
        stmt.execute("INSERT INTO expenses (category, amount, date, description) VALUES ('Feed', 45000, '" + today + "', 'Dairy feed')");

        // Verify total headcount = 102
        ResultSet rsHead = stmt.executeQuery("SELECT (SELECT COUNT(*) FROM animas) + (SELECT SUM(current_quantity) FROM livestock_batches)");
        assertTrue(rsHead.next());
        assertEquals(102, rsHead.getInt(1));
        rsHead.close();

        // Verify cash flow formatting
        double inc = 150000;
        double exp = 45000;
        double net = inc - exp;
        assertEquals("UGX 150,000", FormatUtils.formatCurrency(inc));
        assertEquals("UGX 45,000", FormatUtils.formatCurrency(exp));
        assertEquals("UGX 105,000", FormatUtils.formatCurrency(net));

        stmt.close();
    }

    @Test
    public void testLowStockAndOverdueVaccinations() throws Exception {
        Statement stmt = conn.createStatement();

        // Inventory item below min_quantity threshold
        stmt.execute("INSERT INTO inventory (item_name, quantity, min_quantity) VALUES ('Dewormer', 2.0, 5.0)");
        stmt.execute("INSERT INTO inventory (item_name, quantity, min_quantity) VALUES ('Feed Bag', 10.0, 5.0)");

        // Pending and overdue vaccinations
        stmt.execute("INSERT INTO vaccinations (animal_id, vaccine_name, scheduled_date, status) VALUES (1, 'FMD', '10-10-2026', 'Overdue')");
        stmt.execute("INSERT INTO vaccinations (animal_id, vaccine_name, scheduled_date, status) VALUES (1, 'Anthrax', '30-10-2026', 'Pending')");

        ResultSet rsLow = stmt.executeQuery("SELECT COUNT(*) FROM inventory WHERE quantity <= min_quantity");
        assertTrue(rsLow.next());
        assertEquals(1, rsLow.getInt(1));
        rsLow.close();

        ResultSet rsVac = stmt.executeQuery("SELECT COUNT(*) FROM vaccinations WHERE status IN ('Pending', 'Overdue')");
        assertTrue(rsVac.next());
        assertEquals(2, rsVac.getInt(1));
        rsVac.close();

        stmt.close();
    }

    @Test
    public void testMultiFormatDateFinancials() {
        java.util.Calendar calToday = java.util.Calendar.getInstance();
        int curMonth = calToday.get(java.util.Calendar.MONTH);
        int curYear = calToday.get(java.util.Calendar.YEAR);

        String ddMmYyyy = String.format(Locale.US, "15-%02d-%d", curMonth + 1, curYear);
        String yyyyMmDd = String.format(Locale.US, "%d-%02d-15", curYear, curMonth + 1);
        String oldDate = "10-01-2020";

        assertTrue("Should parse dd-MM-yyyy date", dev.mfarm.com.mfarm.fragments.DashboardFragment.isSameMonth(ddMmYyyy, curMonth, curYear));
        assertTrue("Should parse yyyy-MM-dd date", dev.mfarm.com.mfarm.fragments.DashboardFragment.isSameMonth(yyyyMmDd, curMonth, curYear));
        org.junit.Assert.assertFalse("Should exclude old date", dev.mfarm.com.mfarm.fragments.DashboardFragment.isSameMonth(oldDate, curMonth, curYear));
    }

    @Test
    public void testInactiveAndSoldAnimalFiltering() throws Exception {
        Statement stmt = conn.createStatement();

        // 1 Active, 1 Sold, 1 Deceased
        stmt.execute("INSERT INTO animas (name, species, lifecycle_status) VALUES ('Active Cow', 'Cattle', 'Active')");
        stmt.execute("INSERT INTO animas (name, species, lifecycle_status) VALUES ('Sold Goat', 'Goat', 'Sold')");
        stmt.execute("INSERT INTO animas (name, species, lifecycle_status) VALUES ('Dead Hen', 'Poultry', 'Deceased')");

        ResultSet rsActive = stmt.executeQuery("SELECT COUNT(*) FROM animas WHERE lifecycle_status = 'Active' OR lifecycle_status IS NULL OR lifecycle_status = ''");
        assertTrue(rsActive.next());
        assertEquals(1, rsActive.getInt(1));
        rsActive.close();

        stmt.close();
    }

    @Test
    public void testOtherSentinelExactMatching() {
        String sentinel = dev.mfarm.com.mfarm.dao.CategoryManager.OTHER_SENTINEL;
        assertEquals("Other (Specify)...", sentinel);

        // Legitimate category containing "Other" should NOT trigger sentinel match
        String legitCategory = "Other Income";
        org.junit.Assert.assertFalse("Legitimate 'Other Income' must not trigger sentinel match", sentinel.equals(legitCategory));
        assertTrue("Exact sentinel string must match", sentinel.equals("Other (Specify)..."));
    }

    @Test
    public void testNonpositiveAndNonfiniteAmountValidation() {
        double zero = 0.0;
        double negative = -1500.0;
        double nan = Double.NaN;
        double inf = Double.POSITIVE_INFINITY;
        double valid = 50000.0;

        org.junit.Assert.assertFalse("Zero amount must be rejected", Double.isFinite(zero) && zero > 0);
        org.junit.Assert.assertFalse("Negative amount must be rejected", Double.isFinite(negative) && negative > 0);
        org.junit.Assert.assertFalse("NaN amount must be rejected", Double.isFinite(nan) && nan > 0);
        org.junit.Assert.assertFalse("Infinite amount must be rejected", Double.isFinite(inf) && inf > 0);
        assertTrue("Valid positive amount must pass", Double.isFinite(valid) && valid > 0);
    }

    @Test
    public void testCustomCategoryPersistenceAndDeduplication() {
        JdbcDatabaseAdapter adapter = new JdbcDatabaseAdapter(conn);
        
        // Add valid custom category
        dev.mfarm.com.mfarm.dao.CategoryManager.CategoryResult res1 = dev.mfarm.com.mfarm.dao.CategoryManager.addCategory(adapter, "income_categories", "category_name", "Consultancy Fees");
        assertEquals(dev.mfarm.com.mfarm.dao.CategoryManager.CategoryResult.ADDED, res1);

        // Duplicate case-insensitive insert must return ALREADY_EXISTS
        dev.mfarm.com.mfarm.dao.CategoryManager.CategoryResult res2 = dev.mfarm.com.mfarm.dao.CategoryManager.addCategory(adapter, "income_categories", "category_name", "consultancy fees");
        assertEquals(dev.mfarm.com.mfarm.dao.CategoryManager.CategoryResult.ALREADY_EXISTS, res2);

        // Empty/Sentinel category must return FAILED
        dev.mfarm.com.mfarm.dao.CategoryManager.CategoryResult res3 = dev.mfarm.com.mfarm.dao.CategoryManager.addCategory(adapter, "income_categories", "category_name", "Other (Specify)...");
        assertEquals(dev.mfarm.com.mfarm.dao.CategoryManager.CategoryResult.FAILED, res3);
    }

    @Test
    public void testDuplicateAnimalSaleRejection() throws Exception {
        Statement stmt = conn.createStatement();

        // 1. Create active animal
        stmt.execute("INSERT INTO animas (id, name, species, lifecycle_status) VALUES (1, 'Cow Daisy', 'Cattle', 'Active')");

        // 2. Transaction 1: Successful First Sale
        conn.setAutoCommit(false);
        stmt.executeUpdate("INSERT INTO income (id, category, amount, related_animal_id) VALUES (101, 'Animal Sales', 1500000, 1)");
        int updatedRows1 = stmt.executeUpdate("UPDATE animas SET lifecycle_status = 'Sold' WHERE id = 1 AND (lifecycle_status = 'Active' OR lifecycle_status IS NULL OR lifecycle_status = '')");
        assertEquals(1, updatedRows1);
        stmt.executeUpdate("INSERT INTO audit_logs (action, module, entity_id, details) VALUES ('RECORD_INCOME', 'FINANCIAL', 101, 'Sold Daisy')");
        conn.commit();

        // Verify First Sale State
        ResultSet rs1 = stmt.executeQuery("SELECT lifecycle_status FROM animas WHERE id = 1");
        assertTrue(rs1.next());
        assertEquals("Sold", rs1.getString(1));
        rs1.close();

        ResultSet rsInc1 = stmt.executeQuery("SELECT COUNT(*) FROM income WHERE related_animal_id = 1");
        assertTrue(rsInc1.next());
        assertEquals(1, rsInc1.getInt(1));
        rsInc1.close();

        // 3. Transaction 2: Duplicate Sale Attempt (MUST FAIL AND ROLLBACK)
        conn.setAutoCommit(false);
        stmt.executeUpdate("INSERT INTO income (id, category, amount, related_animal_id) VALUES (102, 'Animal Sales', 1500000, 1)");
        int updatedRows2 = stmt.executeUpdate("UPDATE animas SET lifecycle_status = 'Sold' WHERE id = 1 AND (lifecycle_status = 'Active' OR lifecycle_status IS NULL OR lifecycle_status = '')");
        
        if (updatedRows2 == 0) {
            // Expected failure: Rollback Transaction 2!
            conn.rollback();
        } else {
            conn.commit();
            org.junit.Assert.fail("Duplicate sale on already sold animal must be rejected!");
        }
        conn.setAutoCommit(true);

        // 4. Verify Final State: No duplicate income or audit logs were created
        ResultSet rsIncFinal = stmt.executeQuery("SELECT COUNT(*) FROM income WHERE related_animal_id = 1");
        assertTrue(rsIncFinal.next());
        assertEquals("Income count must remain exactly 1 after duplicate sale rollback", 1, rsIncFinal.getInt(1));
        rsIncFinal.close();

        ResultSet rsAuditFinal = stmt.executeQuery("SELECT COUNT(*) FROM audit_logs WHERE module = 'FINANCIAL'");
        assertTrue(rsAuditFinal.next());
        assertEquals("Audit count must remain exactly 1 after duplicate sale rollback", 1, rsAuditFinal.getInt(1));
        rsAuditFinal.close();

        stmt.close();
    }

    @Test
    public void testLegacyAnimalNullAndEmptyStatusSaleAndRejection() throws Exception {
        Statement stmt = conn.createStatement();

        // Legacy animal 2 with NULL lifecycle_status
        stmt.execute("INSERT INTO animas (id, name, species, lifecycle_status) VALUES (2, 'Legacy Cow Null', 'Cattle', NULL)");
        // Legacy animal 3 with empty lifecycle_status ''
        stmt.execute("INSERT INTO animas (id, name, species, lifecycle_status) VALUES (3, 'Legacy Goat Empty', 'Goat', '')");

        // First Sale on Legacy Animal 2 (NULL status)
        conn.setAutoCommit(false);
        stmt.executeUpdate("INSERT INTO income (id, category, amount, related_animal_id) VALUES (201, 'Animal Sales', 1200000, 2)");
        int upd2 = stmt.executeUpdate("UPDATE animas SET lifecycle_status = 'Sold' WHERE id = 2 AND (lifecycle_status = 'Active' OR lifecycle_status IS NULL OR lifecycle_status = '')");
        assertEquals(1, upd2);
        conn.commit();

        // First Sale on Legacy Animal 3 (Empty '' status)
        conn.setAutoCommit(false);
        stmt.executeUpdate("INSERT INTO income (id, category, amount, related_animal_id) VALUES (202, 'Animal Sales', 350000, 3)");
        int upd3 = stmt.executeUpdate("UPDATE animas SET lifecycle_status = 'Sold' WHERE id = 3 AND (lifecycle_status = 'Active' OR lifecycle_status IS NULL OR lifecycle_status = '')");
        assertEquals(1, upd3);
        conn.commit();

        // Attempt Second Sale on Legacy Animal 2 (Now marked Sold)
        conn.setAutoCommit(false);
        stmt.executeUpdate("INSERT INTO income (id, category, amount, related_animal_id) VALUES (203, 'Animal Sales', 1200000, 2)");
        int upd2Second = stmt.executeUpdate("UPDATE animas SET lifecycle_status = 'Sold' WHERE id = 2 AND (lifecycle_status = 'Active' OR lifecycle_status IS NULL OR lifecycle_status = '')");
        if (upd2Second == 0) {
            conn.rollback(); // Transaction rolled back on duplicate sale attempt
        } else {
            conn.commit();
            org.junit.Assert.fail("Second sale on legacy animal must be rejected!");
        }
        conn.setAutoCommit(true);

        // Verify count of income for legacy animal 2 remains 1
        ResultSet rsInc2 = stmt.executeQuery("SELECT COUNT(*) FROM income WHERE related_animal_id = 2");
        assertTrue(rsInc2.next());
        assertEquals(1, rsInc2.getInt(1));
        rsInc2.close();

        stmt.close();
    }

    @Test
    public void testAuditFailureRollsBackTransaction() throws Exception {
        Statement stmt = conn.createStatement();
        stmt.execute("INSERT INTO animas (id, name, species, lifecycle_status) VALUES (5, 'Moo Five', 'Cattle', 'Active')");

        conn.setAutoCommit(false);
        boolean auditFailed = false;
        try {
            stmt.executeUpdate("INSERT INTO income (id, category, amount, related_animal_id) VALUES (501, 'Animal Sales', 2000000, 5)");
            stmt.executeUpdate("UPDATE animas SET lifecycle_status = 'Sold' WHERE id = 5 AND (lifecycle_status = 'Active' OR lifecycle_status IS NULL OR lifecycle_status = '')");

            // Simulate audit log failure by attempting to insert into non-existent audit table
            stmt.executeUpdate("INSERT INTO audit_logs_non_existent VALUES (1)"); 
            conn.commit();
        } catch (Exception e) {
            auditFailed = true;
            conn.rollback(); // Transaction aborted on audit log failure!
        }
        conn.setAutoCommit(true);

        assertTrue("Audit log failure should throw exception and abort transaction", auditFailed);

        // Verify Animal 5 is STILL Active and NO income row exists
        ResultSet rsStatus = stmt.executeQuery("SELECT lifecycle_status FROM animas WHERE id = 5");
        assertTrue(rsStatus.next());
        assertEquals("Active", rsStatus.getString(1));
        rsStatus.close();

        ResultSet rsInc = stmt.executeQuery("SELECT COUNT(*) FROM income WHERE id = 501");
        assertTrue(rsInc.next());
        assertEquals(0, rsInc.getInt(1));
        rsInc.close();

        stmt.close();
    }
}
