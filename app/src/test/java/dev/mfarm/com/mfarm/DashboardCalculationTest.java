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
        Statement stmt = conn.createStatement();
        stmt.execute("CREATE TABLE animas (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, species TEXT, gender TEXT, lifecycle_status TEXT DEFAULT 'Active')");
        stmt.execute("CREATE TABLE livestock_batches (id INTEGER PRIMARY KEY AUTOINCREMENT, batch_id TEXT UNIQUE, species TEXT, current_quantity INTEGER, status TEXT DEFAULT 'Active')");
        stmt.execute("CREATE TABLE inventory (id INTEGER PRIMARY KEY AUTOINCREMENT, item_name TEXT, quantity REAL, min_quantity REAL)");
        stmt.execute("CREATE TABLE vaccinations (id INTEGER PRIMARY KEY AUTOINCREMENT, animal_id INTEGER, vaccine_name TEXT, scheduled_date TEXT, status TEXT)");
        stmt.execute("CREATE TABLE income (id INTEGER PRIMARY KEY AUTOINCREMENT, category TEXT, amount REAL, date TEXT, description TEXT)");
        stmt.execute("CREATE TABLE expenses (id INTEGER PRIMARY KEY AUTOINCREMENT, category TEXT, amount REAL, date TEXT, description TEXT)");
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
        stmt.execute("INSERT INTO livestock_batches (batch_id, species, current_quantity) VALUES ('BATCH-100', 'Poultry', 100)");

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
}
