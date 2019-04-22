package de.nilsfo.lockscreennotes.util;

import android.content.Context;
import android.content.Intent;

import de.nilsfo.lockscreennotes.data.Note;
import de.nilsfo.lockscreennotes.sql.DBAdapter;

public class NoteSharer {

	public static final String INTENT_TYPE = "text/plain";
	private Context context;

	public NoteSharer(Context context) {
		this.context = context;
	}

	public void share(Note note) {
		share(note.getText());
	}

	public void share(long noteID) {
		DBAdapter adapter = new DBAdapter(context);
		adapter.open();
		share(noteID, adapter);
		adapter.close();
	}

	public void share(long noteID, DBAdapter adapter) {
		share(Note.getNoteFromDB(noteID, adapter));
	}

	public void share(String text) {
		Intent intent = new IntentUtils(context).getNoteShareIntent(text);
		context.startActivity(intent);
	}

}
