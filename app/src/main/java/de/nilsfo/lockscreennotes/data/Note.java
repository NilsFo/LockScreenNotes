package de.nilsfo.lockscreennotes.data;

import android.database.Cursor;
import android.text.format.DateUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Date;

import de.nilsfo.lockscreennotes.sql.DBAdapter;
import de.nilsfo.lockscreennotes.util.NotesNotificationManager;
import timber.log.Timber;

/**
 * Created by Nils on 13.08.2016.
 */

public class Note implements Comparable<Note> {

	public static final int DEFAULT_PREVIEW_CHARS = 250;
	public static final long DEFAULT_TIMESTAMP = 0;

	private String text;
	private boolean enabled;
	private long timestamp;
	private long databaseID;

	public Note() {
		this("", false, DEFAULT_TIMESTAMP);
	}

	public Note(String text, boolean enabled, long timestamp) {
		this.text = text;
		this.enabled = enabled;
		this.timestamp = timestamp;
	}

	@Nullable
	public static Note getNoteFromDB(long id, DBAdapter adapter) {
		Cursor cursor = null;
		try {
			cursor = adapter.getRow(id);
		} catch (IllegalStateException e) {
			Timber.e(e);
		}
		if (cursor == null) {
			return null;
		}

		Note note = new Note();

		String text = cursor.getString(DBAdapter.COL_NOTE_TEXT);
		boolean enabled = cursor.getInt(DBAdapter.COL_NOTE_ENABLED) != 0;
		long time = cursor.getLong(DBAdapter.COL_TIMESTAMP);

		note.setText(text);
		note.setEnabled(enabled);
		note.setTimestamp(time);
		note.setDatabaseID(id);

		return note;
	}

	public static ArrayList<Note> getAllNotesFromDB(DBAdapter adapter) {
		Cursor cursor = adapter.getAllIDs();
		ArrayList<Note> list = new ArrayList<>();

		if (cursor.moveToFirst()) {
			do {
				long id = cursor.getLong(DBAdapter.COL_ROWID);
				Note note = Note.getNoteFromDB(id, adapter);

				if (note == null) {
					Timber.e("Cannot find a note with ID: " + id);
				} else {
					Timber.i("Adding note with ID: " + id);
					list.add(note);
				}
			} while (cursor.moveToNext());
		}

		return list;
	}

	public String getTextPreview(int characters) {
		String text = getText();
		text = text.replace("\n", " ");
		while (text.contains("  ")) text = text.replace("  ", " ");
		text = text.trim();

		if (characters > 0) {
			if (text.length() > characters) {
				text = text.trim().substring(0, characters);
				text = text.trim() + "...";
			}
		}

		return text;
	}

	@Override
	public String toString() {
		return "Note. ID: " + getDatabaseID() + ", Content: '" + getTextPreview(15) + "', timestamp: " + DateUtils.getRelativeTimeSpanString(getTimestamp(), new Date().getTime(), 0L, DateUtils.FORMAT_ABBREV_ALL);
	}

	public int isEnabledSQL() {
		if (isEnabled()) return 1;
		return 0;
	}

	@Override
	public int compareTo(@NonNull Note note) {
		return (note.getTimestampAsDate().compareTo(getTimestampAsDate()));
	}

	public String getTextPreview() {
		return getTextPreview(DEFAULT_PREVIEW_CHARS);
	}

	public long getDatabaseID() {
		return databaseID;
	}

	public void setDatabaseID(long databaseID) {
		this.databaseID = databaseID;
	}

	public int getNotificationID() {
		return (int) (getDatabaseID() % Integer.MAX_VALUE) + NotesNotificationManager.NOTES_NOTIFICATION_ID_OFFSET;
	}

	public Date getTimestampAsDate() {
		return new Date(getTimestamp());
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void setTimestamp(Date timestamp) {
		setTimestamp(timestamp.getTime());
	}
}
