package de.wavegate.tos.lockscreennotes.data;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Date;

import de.wavegate.tos.lockscreennotes.sql.DBAdapter;

/**
 * Created by Nils on 13.08.2016.
 */

public class Note implements Comparable<Note> {

	public static final int DEFAULT_PREVIEW_CHARS = 250;
	private String text;
	private boolean enabled;
	private long timestamp;
	private long databaseID;

	public Note() {
		this("", false, 0);
	}

	public Note(String text, boolean enabled, long timestamp) {
		this.text = text;
		this.enabled = enabled;
		this.timestamp = timestamp;
	}

	@Nullable
	public static Note getNoteFromDB(long id, DBAdapter adapter) {
		Cursor cursor = adapter.getRow(id);
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
		Cursor cursor = adapter.getAllRows();
		ArrayList<Note> list = new ArrayList<>();

		if (cursor.moveToFirst()) {
			do {
				int id = cursor.getInt(DBAdapter.COL_ROWID);
				Note note = Note.getNoteFromDB(id, adapter);

				//Log.i(LOGTAG, "Adding note with ID: " + id);
				list.add(note);
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

	public String getTextPreview() {
		return getTextPreview(DEFAULT_PREVIEW_CHARS);
	}

	public long getDatabaseID() {
		return databaseID;
	}

	public void setDatabaseID(long databaseID) {
		this.databaseID = databaseID;
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

	public void setTimestamp(Date timestamp) {
		setTimestamp(timestamp.getTime());
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

	public int isEnabledSQL() {
		if (isEnabled()) return 1;
		return 0;
	}

	@Override
	public int compareTo(@NonNull Note note) {
		return (note.getTimestampAsDate().compareTo(getTimestampAsDate()));
	}
}
