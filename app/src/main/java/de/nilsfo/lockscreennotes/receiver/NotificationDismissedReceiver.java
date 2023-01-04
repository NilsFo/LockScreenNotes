package de.nilsfo.lockscreennotes.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import java.util.ArrayList;

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
			Timber.e("I just got a 'NotificationDismissed' intent! But there were no extras! Could not perform any actions!");
			Toast.makeText(context, R.string.error_internal_error, Toast.LENGTH_LONG).show();
			return;
		}

		long notificationId = NotesNotificationManager.INTENT_EXTRA_NOTE_ID_NONE;
		if (extras.containsKey(NotesNotificationManager.INTENT_EXTRA_NOTE_ID)) {
			notificationId = extras.getLong(NotesNotificationManager.INTENT_EXTRA_NOTE_ID);

			// #######################
			// TODO THIS IS A BIG ONE: THE NOTIFICATION ID ALWAYS RESOLVES TO THE ID OF THE FIRST NOTE WHY?
			// #######################
		} else {
			Timber.e("FAILED TO GET INTENT EXTRA FOR THE NOTIFICATION ID!!");
			Toast.makeText(context, R.string.error_internal_error, Toast.LENGTH_LONG).show();
			return;
		}

		// TODO REMOVE THIS LINE BELOW, but only if you have resolved the issue stated above
		// This is a backup case where all notifications will be disabled regardless
		notificationId = NotesNotificationManager.INTENT_EXTRA_NOTE_ID_NONE;

		// Opening the DB to fetch notes
		DBAdapter databaseAdapter = new DBAdapter(context);
		databaseAdapter.open();

		ArrayList<Note> notes = new ArrayList<>();
		if (notificationId == NotesNotificationManager.INTENT_EXTRA_NOTE_ID_NONE) {
			try {
				notes = Note.getAllNotesFromDB(databaseAdapter);
			} catch (Exception e) {
				Timber.e(e);
				Timber.e("Failed to get all notes from database!");
				Toast.makeText(context, R.string.error_internal_error, Toast.LENGTH_LONG).show();
			}
			Timber.i("That was no known ID, so just hide them all.");
		} else {
			try {
				notes.add(Note.getNoteFromDB(notificationId, databaseAdapter));
			} catch (Exception e) {
				Timber.e(e);
				Timber.e("Failed to get Note from Database with ID: " + notificationId);
				Toast.makeText(context, R.string.error_internal_error, Toast.LENGTH_LONG).show();
			}
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
