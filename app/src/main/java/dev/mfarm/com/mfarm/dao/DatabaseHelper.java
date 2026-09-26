package dev.mfarm.com.mfarm.dao;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import dev.mfarm.com.mfarm.MainActivity;

public class DatabaseHelper extends SQLiteOpenHelper {

	public static String DB_PATH;
	public static String DB_NAME = MainActivity.DB_NAME;
	public static final int DB_VERSION = 2;
	public static SQLiteDatabase database;
	public final Context context;
	public static final String ID_COLUMN = "_id";
	private static DatabaseHelper instance;

	public static synchronized DatabaseHelper getHelper(Context context) {
		if (instance == null)
			instance = new DatabaseHelper(context, DB_NAME);
		return instance;
	}

	public SQLiteDatabase getDb() {
		return database;
	}

	@SuppressLint("NewApi")
	public DatabaseHelper(Context context, String databaseName) {
		super(context, databaseName, null, DB_VERSION);
		this.context = context;
		File dbFile = resolveDbFile(context, databaseName);
		DB_PATH = dbFile.getParent() + "/";
		DB_NAME = databaseName;
		openDataBase();
	}

	private static File resolveDbFile(Context context, String databaseName) {
		File standard = context.getDatabasePath(databaseName);
		File parent = standard.getParentFile();
		if (parent != null && !parent.exists()) {
			parent.mkdirs();
		}
		File externalDir = context.getExternalFilesDir(null);
		File legacy = externalDir != null ? new File(externalDir, databaseName) : null;
		if (!standard.exists() && legacy != null && legacy.exists()) {
			copyFile(legacy, standard);
		}
		return standard;
	}

	private static void copyFile(File from, File to) {
		try {
			FileInputStream in = new FileInputStream(from);
			FileOutputStream out = new FileOutputStream(to);
			byte[] buffer = new byte[4096];
			int n;
			while ((n = in.read(buffer)) > 0) {
				out.write(buffer, 0, n);
			}
			out.close();
			in.close();
		} catch (IOException e) {
			Log.e("DatabaseHelper", "Could not migrate farm database", e);
		}
	}

	public void createDataBase() {
		boolean dbExist = checkDataBase();
		if (!dbExist) {
			try {
				copyDataBase();
			} catch (IOException e) {
				Log.e(this.getClass().toString(), "Copying error");
				throw new Error("Error copying database!");
			}
		} else {
			Log.i(this.getClass().toString(), "Database already exists");
		}
	}

	private boolean checkDataBase() {
		File dbFile = new File(DB_PATH + DB_NAME);
		return dbFile.exists() && dbFile.length() > 100;
	}

	private void copyDataBase() throws IOException {
		InputStream externalDbStream = context.getAssets().open(DB_NAME);
		String outFileName = DB_PATH + DB_NAME;
		OutputStream localDbStream = new FileOutputStream(outFileName);

		byte[] buffer = new byte[1024];
		int bytesRead;
		while ((bytesRead = externalDbStream.read(buffer)) > 0) {
			localDbStream.write(buffer, 0, bytesRead);
		}
		localDbStream.close();
		externalDbStream.close();
	}

	public SQLiteDatabase openDataBase() throws SQLException {
		String path = DB_PATH + DB_NAME;
		if (database == null || !database.isOpen()) {
			createDataBase();
			database = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READWRITE);
			ensureSchema(database);
		}
		return database;
	}

	public static void ensureSchema(SQLiteDatabase db) {
		if (db == null) return;

		db.execSQL("CREATE TABLE IF NOT EXISTS expenses (" +
				"id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				"category TEXT, " +
				"amount REAL, " +
				"date TEXT, " +
				"description TEXT)");

		db.execSQL("CREATE TABLE IF NOT EXISTS vaccinations (" +
				"id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				"animal_id INTEGER, " +
				"vaccine_name TEXT, " +
				"scheduled_date TEXT, " +
				"status TEXT, " +
				"remarks TEXT)");

		db.execSQL("CREATE TABLE IF NOT EXISTS inventory (" +
				"id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				"item_name TEXT, " +
				"category TEXT, " +
				"quantity REAL, " +
				"unit TEXT, " +
				"min_quantity REAL)");

		db.execSQL("CREATE TABLE IF NOT EXISTS inventory_transactions (" +
				"id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				"item_id INTEGER, " +
				"type TEXT, " +
				"quantity REAL, " +
				"date TEXT, " +
				"remarks TEXT)");

		db.execSQL("CREATE TABLE IF NOT EXISTS breeding_records (" +
				"id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				"animal_id INTEGER, " +
				"mating_date TEXT, " +
				"bull_id TEXT, " +
				"expected_birth_date TEXT, " +
				"status TEXT)");

		db.execSQL("CREATE TABLE IF NOT EXISTS vet_checks (" +
				"id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				"animal_id INTEGER, " +
				"check_type TEXT, " +
				"check_date TEXT, " +
				"remarks TEXT)");

		db.execSQL("CREATE TABLE IF NOT EXISTS farm_profile (" +
				"id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				"farm_name TEXT, " +
				"owner_name TEXT, " +
				"location TEXT, " +
				"phone TEXT, " +
				"reg_number TEXT, " +
				"farm_type TEXT, " +
				"farm_size TEXT, " +
				"currency_symbol TEXT DEFAULT '$', " +
				"units_system TEXT DEFAULT 'Metric', " +
				"logo_path TEXT, " +
				"notes TEXT)");

		db.execSQL("CREATE TABLE IF NOT EXISTS calving_records (" +
				"id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				"dam_id INTEGER, " +
				"sire_id INTEGER, " +
				"birth_date TEXT, " +
				"offspring_id INTEGER, " +
				"sex TEXT, " +
				"birth_weight REAL, " +
				"birth_condition TEXT, " +
				"survival_status TEXT, " +
				"notes TEXT)");

		db.execSQL("CREATE TABLE IF NOT EXISTS feed_types (" +
				"id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				"feed_name TEXT, " +
				"category TEXT, " +
				"unit TEXT, " +
				"min_stock_alert REAL)");

		db.execSQL("CREATE TABLE IF NOT EXISTS feed_consumption (" +
				"id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				"feed_id INTEGER, " +
				"date TEXT, " +
				"quantity REAL, " +
				"cost REAL, " +
				"group_or_animal_id TEXT, " +
				"notes TEXT)");

		db.execSQL("CREATE TABLE IF NOT EXISTS income (" +
				"id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				"date TEXT, " +
				"category TEXT, " +
				"description TEXT, " +
				"amount REAL, " +
				"payment_method TEXT, " +
				"buyer TEXT, " +
				"related_animal_id INTEGER, " +
				"notes TEXT)");

		db.execSQL("CREATE TABLE IF NOT EXISTS audit_logs (" +
				"id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				"timestamp INTEGER, " +
				"action TEXT, " +
				"module TEXT, " +
				"record_id INTEGER, " +
				"details TEXT)");

		// Safe column additions
		addColumnIfMissing(db, "animas", "photo_path", "TEXT");
		addColumnIfMissing(db, "animas", "tag_number", "TEXT");
		addColumnIfMissing(db, "animas", "species", "TEXT DEFAULT 'Cattle'");
		addColumnIfMissing(db, "animas", "lifecycle_status", "TEXT DEFAULT 'Active'");
		addColumnIfMissing(db, "animas", "repro_status", "TEXT DEFAULT 'Open'");
		addColumnIfMissing(db, "animas", "lactation_status", "TEXT DEFAULT 'Dry'");
		addColumnIfMissing(db, "animas", "health_status", "TEXT DEFAULT 'Healthy'");
		addColumnIfMissing(db, "animas", "location", "TEXT");
		addColumnIfMissing(db, "animas", "source", "TEXT");
		addColumnIfMissing(db, "animas", "purchase_date", "TEXT");
		addColumnIfMissing(db, "animas", "purchase_price", "REAL DEFAULT 0");
		addColumnIfMissing(db, "animas", "notes", "TEXT");

		addColumnIfMissing(db, "vaccinations", "batch_number", "TEXT");
		addColumnIfMissing(db, "vaccinations", "vet_name", "TEXT");
		addColumnIfMissing(db, "vaccinations", "dosage", "TEXT");
		addColumnIfMissing(db, "vaccinations", "next_due_date", "TEXT");

		addColumnIfMissing(db, "illness", "cost", "REAL DEFAULT 0");
		addColumnIfMissing(db, "illness", "vet_name", "TEXT");
		addColumnIfMissing(db, "illness", "outcome", "TEXT");
		addColumnIfMissing(db, "illness", "start_date", "TEXT");
		addColumnIfMissing(db, "illness", "end_date", "TEXT");

		addColumnIfMissing(db, "milk_production", "morning_yield", "REAL DEFAULT 0");
		addColumnIfMissing(db, "milk_production", "afternoon_yield", "REAL DEFAULT 0");
		addColumnIfMissing(db, "milk_production", "evening_yield", "REAL DEFAULT 0");

		addColumnIfMissing(db, "breeding_records", "service_type", "TEXT DEFAULT 'AI'");
		addColumnIfMissing(db, "breeding_records", "heat_date", "TEXT");
		addColumnIfMissing(db, "breeding_records", "pregnancy_confirmed", "INTEGER DEFAULT 0");

		addColumnIfMissing(db, "expenses", "payee", "TEXT");
		addColumnIfMissing(db, "expenses", "payment_method", "TEXT");
		addColumnIfMissing(db, "expenses", "related_animal_id", "INTEGER");

		try {
			dev.mfarm.com.mfarm.sync.FarmSyncSchema.install(db);
		} catch (Exception e) {
			Log.e("DatabaseHelper", "Farm sync schema install failed", e);
		}
	}

	private static void addColumnIfMissing(SQLiteDatabase db, String table, String column, String type) {
		if (!tableExists(db, table)) return;
		Cursor c = null;
		boolean found = false;
		try {
			c = db.rawQuery("PRAGMA table_info(" + table + ")", null);
			while (c.moveToNext()) {
				String colName = c.getString(1);
				if (column.equalsIgnoreCase(colName)) {
					found = true;
					break;
				}
			}
		} catch (Exception e) {
			Log.w("DatabaseHelper", "Pragma check failed for " + table, e);
		} finally {
			if (c != null) c.close();
		}

		if (!found) {
			try {
				db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
			} catch (Exception e) {
				Log.w("DatabaseHelper", "Could not add column " + column + " to " + table, e);
			}
		}
	}

	private static boolean tableExists(SQLiteDatabase db, String table) {
		Cursor c = null;
		try {
			c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", new String[]{table});
			return c.moveToFirst();
		} catch (Exception e) {
			return false;
		} finally {
			if (c != null) c.close();
		}
	}

	public static void logAudit(SQLiteDatabase db, String action, String module, long recordId, String details) {
		if (db == null || !db.isOpen()) return;
		try {
			ContentValues values = new ContentValues();
			values.put("timestamp", System.currentTimeMillis());
			values.put("action", action);
			values.put("module", module);
			values.put("record_id", recordId);
			values.put("details", details);
			db.insert("audit_logs", null, values);
		} catch (Exception e) {
			Log.w("DatabaseHelper", "Failed to write audit log", e);
		}
	}

	@Override
	public synchronized void close() {
		if (database != null) {
			database.close();
		}
		super.close();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		ensureSchema(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		ensureSchema(db);
	}
}
