package de.nilsfo.lockscreennotes.util;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import de.nilsfo.lockscreennotes.data.Note;
import de.nilsfo.lockscreennotes.sql.DBAdapter;
import de.nilsfo.lsn.R;
import timber.log.Timber;

public class NoteSharer {

	public static final String INTENT_TYPE = "text/plain";
	private final Context context;

	public NoteSharer(Context context) {
		this.context = context;
	}

	public boolean share(Note note) {
		return share(note.getText());
	}

	public boolean share(long noteID) {
		DBAdapter adapter = new DBAdapter(context);
		adapter.open();
		boolean b = share(noteID, adapter);
		adapter.close();
		return b;
	}

	public boolean share(long noteID, DBAdapter adapter) {
		Note note = Note.getNoteFromDB(noteID, adapter);

		if (note == null) {
			Toast.makeText(context, R.string.error_internal_error, Toast.LENGTH_LONG).show();
			return false;
		} else return share(note);
	}

	public boolean share(String text) {
		Intent intent = new IntentUtils(context).getNoteShareIntent(text);
		try {
			context.startActivity(intent);
		} catch (Exception e) {
			Timber.e(e);
			return false;
		}
		return true;
	}

}
