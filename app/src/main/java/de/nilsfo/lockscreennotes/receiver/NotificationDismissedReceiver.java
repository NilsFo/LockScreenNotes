package de.nilsfo.lockscreennotes.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;

import de.nilsfo.lockscreennotes.data.Note;
import de.nilsfo.lockscreennotes.sql.DBAdapter;
import de.nilsfo.lockscreennotes.util.NotesNotificationManager;

/**
 * Created by Nils on 17.09.2016.
 */

public class NotificationDismissedReceiver extends BroadcastReceiver {


	@Override
	public void onReceive(Context context, Intent intent) {
		int notificationId = intent.getExtras().getInt(NotesNotificationManager.INTENT_EXTRA_NOTE_ID);

		//Toast.makeText(context,"Dismiss event listened: "+notificationId,Toast.LENGTH_LONG).show();
		DBAdapter databaseAdapter = new DBAdapter(context);
		databaseAdapter.open();

		ArrayList<Note> notes = new ArrayList<>();
		if (notificationId == NotesNotificationManager.INTENT_EXTRA_NOTE_ID_NONE) {
			notes = Note.getAllNotesFromDB(databaseAdapter);
		} else {
			notes.add(Note.getNoteFromDB(notificationId, databaseAdapter));
		}

		for (Note n : notes) {
			long id = n.getDatabaseID();
			databaseAdapter.updateRow(id, n.getText(), 0, n.getTimestamp());
		}

		databaseAdapter.close();
	}
}
