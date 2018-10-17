package de.nilsfo.lockscreennotes.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;

import de.nilsfo.lockscreennotes.data.Note;
import de.nilsfo.lockscreennotes.sql.DBAdapter;
import de.nilsfo.lockscreennotes.util.NotesNotificationManager;
import de.nilsfo.lsn.R;
import timber.log.Timber;

/**
 * Created by Nils on 17.09.2016.
 */

public class NotificationDismissedReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Bundle extras = intent.getExtras();
		if (extras == null) {
			Timber.e("I just got a 'NoitificationDismissed' intent! But there were no extras! Could not perform any actions!");
			Toast.makeText(context, R.string.error_internal_error, Toast.LENGTH_LONG).show();
			return;
		}

		DBAdapter databaseAdapter = new DBAdapter(context);
		databaseAdapter.open();

		long notificationId = extras.getInt(NotesNotificationManager.INTENT_EXTRA_NOTE_ID);
		ArrayList<Note> notes = new ArrayList<>();
		if (notificationId == NotesNotificationManager.INTENT_EXTRA_NOTE_ID_NONE) {
			notes = Note.getAllNotesFromDB(databaseAdapter);
			Timber.i("That was no known ID, so just hide them all.");
		} else {
			notes.add(Note.getNoteFromDB(notificationId, databaseAdapter));
			Timber.i("Found the right note with matching ID in the database.");
		}

		for (Note n : notes) {
			long id = n.getDatabaseID();
			databaseAdapter.updateRow(id, n.getText(), 0, n.getTimestamp());
		}

		databaseAdapter.close();

		Timber.i("Updating notifications...");
		NotesNotificationManager manager = new NotesNotificationManager(context);
		manager.hideAllNotifications();
		manager.showNoteNotifications();
	}
}
