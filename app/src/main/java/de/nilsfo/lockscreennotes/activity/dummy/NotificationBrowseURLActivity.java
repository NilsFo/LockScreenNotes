package de.nilsfo.lockscreennotes.activity.dummy;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

import java.util.ArrayList;

import de.nilsfo.lockscreennotes.data.Note;
import de.nilsfo.lockscreennotes.sql.DBAdapter;
import de.nilsfo.lockscreennotes.util.NotesNotificationManager;
import de.nilsfo.lockscreennotes.util.URLUtils;
import de.nilsfo.lsn.R;
import timber.log.Timber;

public class NotificationBrowseURLActivity extends Activity {

	@Override
	protected void onStart() {
		super.onStart();

		Bundle extras = getIntent().getExtras();
		if (extras == null) {
			Timber.e("I just got a 'NoitificationBrowseURL' activity intent! But there were no extras! Could not perform any actions!");
			Toast.makeText(this, R.string.error_internal_error, Toast.LENGTH_LONG).show();
			return;
		}

		DBAdapter databaseAdapter = new DBAdapter(this);
		databaseAdapter.open();

		long notificationId = extras.getInt(NotesNotificationManager.INTENT_EXTRA_NOTE_ID);
		Note note = Note.getNoteFromDB(notificationId, databaseAdapter);

		if (note == null) {
			Timber.e("The note with the ID " + notificationId + " was not found in the Database!");
			Toast.makeText(this, R.string.error_internal_error, Toast.LENGTH_LONG).show();
			return;
		}

		String text = note.getText();
		databaseAdapter.close();

		Timber.i("Attempting to browse URL: " + text);
		URLUtils utils = new URLUtils(this);
		if (!utils.containsSingleURL(text)) {
			Timber.e("The note with the ID " + notificationId + " is not a single URL!");
			Toast.makeText(this, R.string.error_internal_error, Toast.LENGTH_LONG).show();
			return;
		}

		/*
		Debug code, to test if a Alert Dialog could finally be shown from within a notification!

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.lipsum);
		builder.setTitle(R.string.app_name);
		builder.setIcon(R.mipmap.ic_launcher);
		builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		builder.show();
		*/

		ArrayList<String> list = URLUtils.getURLRegexManager().findMatchesInText(text);
		utils.browseURL(list.get(0));

		finish();
	}
}
