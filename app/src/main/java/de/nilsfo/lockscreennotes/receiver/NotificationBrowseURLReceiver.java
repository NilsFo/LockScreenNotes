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
import de.nilsfo.lockscreennotes.util.URLUtils;
import de.nilsfo.lsn.R;
import timber.log.Timber;

/**
 * Created by Nils on 17.09.2016.
 */

@Deprecated
public class NotificationBrowseURLReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Bundle extras = intent.getExtras();
		if (extras == null) {
			Timber.e("I just got a 'NoitificationBrowseURL' intent! But there were no extras! Could not perform any actions!");
			Toast.makeText(context, R.string.error_internal_error, Toast.LENGTH_LONG).show();
			return;
		}

		DBAdapter databaseAdapter = new DBAdapter(context);
		databaseAdapter.open();

		long notificationId = extras.getInt(NotesNotificationManager.INTENT_EXTRA_NOTE_ID);
		Note note = Note.getNoteFromDB(notificationId, databaseAdapter);

		if (note == null) {
			Timber.e("The note with the ID " + notificationId + " was not found in the Database!");
			Toast.makeText(context, R.string.error_internal_error, Toast.LENGTH_LONG).show();
			return;
		}

		String text = note.getText();
		databaseAdapter.close();

		Timber.i("Attempting to browse URL: " + text);
		URLUtils utils = new URLUtils(context);
		if (!utils.containsSingleURL(text)) {
			Timber.e("The note with the ID " + notificationId + " is not a single URL!");
			Toast.makeText(context, R.string.error_internal_error, Toast.LENGTH_LONG).show();
			return;
		}

		ArrayList<String> list = URLUtils.getURLRegexManager().findMatchesInText(text);
		utils.browseURL(list.get(0));
	}
}
