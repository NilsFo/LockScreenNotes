package de.nilsfo.lockscreennotes.io.backups;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.nilsfo.lockscreennotes.data.Note;
import de.nilsfo.lockscreennotes.sql.DBAdapter;
import de.nilsfo.lockscreennotes.util.VersionManager;
import timber.log.Timber;

public abstract class NoteJSONUtils {

	public static final int VERSION_NOT_AVAILABLE = -1;

	public static final String JSONARG_META_COUNT = "count";
	public static final String JSONARG_META_VERSION = "version";
	public static final String JSONARG_META_TIMESTAMP = "timestamp";
	public static final String JSONARG_META_NOTES = "notes";

	public static final String JSONARG_NOTE_TEXT = "text";
	public static final String JSONARG_NOTE_ENABLED = "enabdled";
	public static final String JSONARG_NOTE_TIMESTAMP = "timestamp_edit";
	public static final String JSONARG_NOTE_DATABASEID = "database_id";

	public static JSONObject toJSON(Note note) throws JSONException {
		JSONObject convertedNote = new JSONObject();

		convertedNote.put(JSONARG_NOTE_TEXT, note.getText());
		convertedNote.put(JSONARG_NOTE_ENABLED, note.isEnabled());
		convertedNote.put(JSONARG_NOTE_TIMESTAMP, note.getTimestamp());
		convertedNote.put(JSONARG_NOTE_DATABASEID, note.getDatabaseID());

		return convertedNote;
	}

	public static JSONArray toJSON(Note... notes) throws JSONException {
		JSONArray array = new JSONArray();
		for (Note n : notes) array.put(toJSON(n));
		return array;
	}

	public static JSONArray toJSON(List<Note> notes) throws JSONException {
		Note[] array = new Note[notes.size()];
		for (int i = 0; i < notes.size(); i++) {
			array[i] = notes.get(i);
		}
		return toJSON(array);
	}

	public static JSONObject toJSON(Context context) throws JSONException {
		JSONObject data = new JSONObject();

		DBAdapter databaseAdapter = new DBAdapter(context);
		databaseAdapter.open();
		ArrayList<Note> notes = Note.getAllNotesFromDB(databaseAdapter);
		JSONArray array = toJSON(notes);
		databaseAdapter.close();

		data.put(JSONARG_META_NOTES, array);
		data.put(JSONARG_META_COUNT, notes.size());
		data.put(JSONARG_META_TIMESTAMP, new Date().getTime());
		data.put(JSONARG_META_VERSION, VersionManager.getCurrentVersion(context));

		return data;
	}

	public static ArrayList<Note> toNotes(JSONArray data, int dataVersion, int currentVersion) {
		ArrayList<Note> list = new ArrayList<>();

		for (int i = 0; i < data.length(); i++) {
			Note n = new Note();

			try {
				JSONObject o = data.getJSONObject(i);
				n.setText(o.getString(JSONARG_NOTE_TEXT));
				n.setEnabled(o.getBoolean(JSONARG_NOTE_ENABLED));
				n.setTimestamp(o.getLong(JSONARG_NOTE_TIMESTAMP));
			} catch (JSONException e) {
				e.printStackTrace();
				Timber.e(e);
				continue;
			}

			list.add(n);
		}

		return list;
	}

}
