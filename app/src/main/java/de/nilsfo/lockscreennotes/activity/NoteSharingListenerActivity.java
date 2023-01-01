package de.nilsfo.lockscreennotes.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.util.Date;

import de.nilsfo.lockscreennotes.data.Note;
import de.nilsfo.lockscreennotes.sql.DBAdapter;
import de.nilsfo.lockscreennotes.util.NotesNotificationManager;
import de.nilsfo.lsn.R;
import timber.log.Timber;

/**
 * Created by Nils on 06.09.2016.
 */

public class NoteSharingListenerActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent receivedIntent = getIntent();

		//Handling intent
		String receivedAction = receivedIntent.getAction();
		String receivedType = receivedIntent.getType();
		Timber.i("I opened via intent. Action: '" + receivedAction + "' Type: '" + receivedType + "'");
		boolean intentError = true;
		boolean noteCreatedWithoutError = true;

		if (receivedAction != null && receivedAction.equals(Intent.ACTION_SEND)) {
			if (receivedType != null && receivedType.startsWith("text/")) {
				String receivedText = receivedIntent.getStringExtra(Intent.EXTRA_TEXT);
				if (receivedText != null) {
					intentError = false;
					receivedIntent.setAction(Intent.ACTION_DEFAULT);
					//Toast.makeText(this, "I got a text intent: " + receivedText, Toast.LENGTH_LONG).show();
					noteCreatedWithoutError = addNewNote(receivedText);
				}
			}
		}

		Timber.i("Note share intent received.");
		Timber.i("Intent error: " + intentError);
		Timber.i("Note creation and DB without error: " + noteCreatedWithoutError);

		if (intentError || !noteCreatedWithoutError) {
			Toast.makeText(this, R.string.error_note_sharing_listener, Toast.LENGTH_LONG).show();
		}

		finish();
	}

	private boolean addNewNote(String receivedText) {
		DBAdapter databaseAdapter = new DBAdapter(this);
		databaseAdapter.open();
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

		boolean enabled = sharedPreferences.getBoolean("prefs_auto_enable_new_notes", true);
		Note note = new Note(receivedText, enabled, new Date().getTime());

		boolean successfullyCreated = true;
		try {
			databaseAdapter.insertRow(note.getText(), note.isEnabledSQL(), note.getTimestamp());
		} catch (IllegalStateException e) {
			Timber.e(e);
			Timber.e("Failed to insert new Note '" + note.getText() + "'!");
			successfullyCreated = false;
		} finally {
			databaseAdapter.close();
		}

		NotesNotificationManager notesNotificationManager = new NotesNotificationManager(this);
		notesNotificationManager.hideAllNotifications();
		notesNotificationManager.showNoteNotifications();

		if (successfullyCreated) {
			Toast.makeText(this, R.string.success_note_sharing_listener, Toast.LENGTH_LONG).show();
			return true;
		} else {
			Toast.makeText(this, R.string.error_internal_error, Toast.LENGTH_LONG).show();
			return false;
		}
	}
}
