package de.wavegate.tos.lockscreennotes.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.Date;

import de.wavegate.tos.lockscreennotes.R;
import de.wavegate.tos.lockscreennotes.data.Note;
import de.wavegate.tos.lockscreennotes.sql.DBAdapter;
import de.wavegate.tos.lockscreennotes.util.NotesNotificationManager;

import static de.wavegate.tos.lockscreennotes.MainActivity.LOGTAG;

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
		Log.i(LOGTAG, "I opened via intent. Action: '" + receivedAction + "' Type: '" + receivedType + "'");
		boolean error = true;

		if (receivedAction != null && receivedAction.equals(Intent.ACTION_SEND)) {
			if (receivedType != null && receivedType.startsWith("text/")) {
				String receivedText = receivedIntent.getStringExtra(Intent.EXTRA_TEXT);
				if (receivedText != null) {
					error = false;
					receivedIntent.setAction(Intent.ACTION_DEFAULT);
					//Toast.makeText(this, "I got a text intent: " + receivedText, Toast.LENGTH_LONG).show();
					addNewNote(receivedText);
				}
			}
		}

		if (error) {
			Toast.makeText(this, R.string.error_note_sharing_listener, Toast.LENGTH_LONG).show();
		}

		finish();
	}

	private void addNewNote(String receivedText) {
		DBAdapter databaseAdapter = new DBAdapter(this);
		databaseAdapter.open();

		Note note = new Note(receivedText, true, new Date().getTime());
		databaseAdapter.insertRow(note.getText(), note.isEnabledSQL(), note.getTimestamp());

		databaseAdapter.close();

		NotesNotificationManager notesNotificationManager = new NotesNotificationManager(this);
		notesNotificationManager.hideNotifications();
		notesNotificationManager.showNotifications();

		Toast.makeText(this, R.string.success_note_sharing_listener, Toast.LENGTH_LONG).show();
	}
}
