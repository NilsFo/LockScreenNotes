// ------------------------------------ DBADapter.java ---------------------------------------------

// TODO: Change the package to match your project.
package de.wavegate.tos.lockscreennotes.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Observable;

import static de.wavegate.tos.lockscreennotes.activity.MainActivity.LOGTAG;


// TO USE:
// Change the package (at top) to match your project.
// Search for "TODO", and make the appropriate changes.
public class DBAdapter extends Observable {

	// DB Fields
	public static final String KEY_ROWID = "_id";
	public static final int COL_ROWID = 0;
	/*
	 * CHANGE 1:
	 */
	// TODO: Setup your fields here:
	public static final String KEY_NOTE_TEXT = "text";
	public static final String KEY_NOTE_ENABLED = "enabled";
	public static final String KEY_TIMESTAMP = "timestamp";

	// TODO: Setup your field numbers here (0 = KEY_ROWID, 1=...)
	public static final int COL_NOTE_TEXT = 1;
	public static final int COL_NOTE_ENABLED = 2;
	public static final int COL_TIMESTAMP = 3;

	public static final String[] ALL_KEYS = new String[]{KEY_ROWID, KEY_NOTE_TEXT, KEY_NOTE_ENABLED, KEY_TIMESTAMP};

	// DB info: it's name, and the table we are using (just one).
	public static final String DATABASE_NAME = "de.wavegate.tos.homescreennotes_db";
	public static final String DATABASE_TABLE = "Notes";
	// Track DB version if a new version of your app changes the format.
	public static final int DATABASE_VERSION = 1;

	/////////////////////////////////////////////////////////////////////
	//	Constants & Data
	/////////////////////////////////////////////////////////////////////
	// For logging:
	private static final String TAG = LOGTAG;

	private static final String DATABASE_CREATE_SQL =
			"create table " + DATABASE_TABLE
					+ " (" + KEY_ROWID + " integer primary key auto_increment, "
			/*
			 * CHANGE 2:
			 */
					// TODO: Place your fields here!
					// + KEY_{...} + " {type} not null"
					//	- Key is the column name you created above.
					//	- {type} is one of: text, integer, real, blob
					//		(http://www.sqlite.org/datatype3.html)
					//  - "not null" means it is a required field (must be given a value).
					// NOTE: All must be comma separated (end of line!) Last one must have NO comma!!
					+ KEY_NOTE_TEXT + " text not null, "
					+ KEY_NOTE_ENABLED + " integer not null, "
					+ KEY_TIMESTAMP + " integer not null"
					// Rest  of creation:
					+ ");";

	// Context of application who uses us.
	//private final Context context;

	private DatabaseHelper myDBHelper;
	private SQLiteDatabase db;

	/////////////////////////////////////////////////////////////////////
	//	Public methods:
	/////////////////////////////////////////////////////////////////////

	public DBAdapter(Context context) {
		myDBHelper = new DatabaseHelper(context);
	}

	// Open the database connection.
	public DBAdapter open() {
		db = myDBHelper.getWritableDatabase();
		return this;
	}

	// Close the database connection.
	public void close() {
		myDBHelper.close();
	}

	// Add a new set of values to the database.
	public long insertRow(String text, int enabled, long timestamp) {
		/*
		 * CHANGE 3:
		 */
		// TODO: Update data in the row with new fields.
		// TODO: Also change the function's arguments to be what you need!
		// Create row's data:
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_NOTE_TEXT, text);
		initialValues.put(KEY_NOTE_ENABLED, enabled);
		initialValues.put(KEY_TIMESTAMP, timestamp);
		// Insert it into the database.

		long l = db.insert(DATABASE_TABLE, null, initialValues);

		setChanged();
		notifyObservers();

		return l;
	}

	// Delete a row from the database, by rowId (primary key)
	public boolean deleteRow(long rowId) {
		String where = KEY_ROWID + "=" + rowId;
		boolean b = db.delete(DATABASE_TABLE, where, null) != 0;

		setChanged();
		notifyObservers();

		return b;
	}

	public void deleteAll() {
		Cursor c = getAllRows();
		long rowId = c.getColumnIndexOrThrow(KEY_ROWID);
		if (c.moveToFirst()) {
			do {
				deleteRow(c.getLong((int) rowId));
			} while (c.moveToNext());
		}
		c.close();

		setChanged();
		notifyObservers();
	}

	// Return all data in the database.
	public synchronized Cursor getAllRows() {
		String where = null;
		Cursor c = db.query(true, DATABASE_TABLE, ALL_KEYS,
				where, null, null, null, null, null);
		if (c != null) {
			c.moveToFirst();
		}
		return c;
	}

	// Get a specific row (by rowId)
	public Cursor getRow(long rowId) {
		String where = KEY_ROWID + "=" + rowId;
		Cursor c = db.query(true, DATABASE_TABLE, ALL_KEYS,
				where, null, null, null, null, null);
		if (c != null) {
			c.moveToFirst();
		}
		return c;
	}

	// Change an existing row to be equal to new data.
	public boolean updateRow(long rowId, String text, int enabled, long timestamp) {
		String where = KEY_ROWID + "=" + rowId;

		/*
		 * CHANGE 4:
		 */
		// TODO: Update data in the row with new fields.
		// TODO: Also change the function's arguments to be what you need!
		// Create row's data:
		ContentValues newValues = new ContentValues();
		newValues.put(KEY_NOTE_TEXT, text);
		newValues.put(KEY_NOTE_ENABLED, enabled);
		newValues.put(KEY_TIMESTAMP, timestamp);

		Cursor oldData = getRow(rowId);
		long oldTimestamp = oldData.getLong(COL_TIMESTAMP);
		//Log.i(LOGTAG, "Old data: " + oldData.getString(COL_NOTE_TEXT) + ", " + oldData.getInt(COL_NOTE_ENABLED) + ", " + oldData.getLong(COL_TIMESTAMP));
		//Log.i(LOGTAG, "New data: " + text + ", " + enabled + ", " + timestamp);

		// Insert it into the database.
		int i = db.update(DATABASE_TABLE, newValues, where, null);

		setChanged();
		notifyObservers();

		return oldTimestamp != timestamp;
	}

	/////////////////////////////////////////////////////////////////////
	//	Private Helper Classes:
	/////////////////////////////////////////////////////////////////////

	/**
	 * Private class which handles database creation and upgrading.
	 * Used to handle low-level database access.
	 */
	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase _db) {
			_db.execSQL(DATABASE_CREATE_SQL);
		}

		@Override
		public void onUpgrade(SQLiteDatabase _db, int oldVersion, int newVersion) {
			//Log.w(TAG, "Upgrading application's database from version " + oldVersion
			//		+ " to " + newVersion + ", which will destroy all old data!");

			// Destroy old database:
			_db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);

			// Recreate new database:
			onCreate(_db);
		}
	}
}
