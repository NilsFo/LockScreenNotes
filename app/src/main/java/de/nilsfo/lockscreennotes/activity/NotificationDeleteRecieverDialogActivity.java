package de.nilsfo.lockscreennotes.activity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;

import de.nilsfo.lockscreennotes.data.Note;
import de.nilsfo.lockscreennotes.sql.DBAdapter;
import de.nilsfo.lockscreennotes.util.NotesNotificationManager;
import de.nilsfo.lsn.R;
import timber.log.Timber;

@Deprecated
public class NotificationDeleteRecieverDialogActivity extends Activity {

	private ArrayList<Note> notes;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();

		DBAdapter databaseAdapter = new DBAdapter(this);
		databaseAdapter.open();

		long notificationId = extras.getInt(NotesNotificationManager.INTENT_EXTRA_NOTE_ID);
		notes = new ArrayList<>();

		if (notificationId == NotesNotificationManager.INTENT_EXTRA_NOTE_ID_NONE) {
			notes = Note.getAllNotesFromDB(databaseAdapter);
			Timber.i("That was no known ID, so just hide them all.");
		} else {
			Note note = Note.getNoteFromDB(notificationId, databaseAdapter);
			if (note != null) {
				notes.add(note);
				Timber.i("Found the right note with matching ID in the database.");
			} else {
				Timber.e("Failed to get note from DB with ID: " + notificationId);
			}
		}
		databaseAdapter.close();

		if (notes.isEmpty()) {
			// ERROR: Notes to be deleted are empty!
			Timber.e("Cannot delete notes! None found!");
			Toast.makeText(this, R.string.error_internal_error, Toast.LENGTH_LONG)
					.show();
			return;
		}

		String msgText;
		if (notes.size() == 1) {
			msgText = String.format(getString(R.string.delete_dialog_content), notes.get(0).getTextPreview());
		} else {
			msgText = String.format(getString(R.string.delete_dialog_content_multiple), String.valueOf(notes.size()));
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.delete_dialog_title);
		builder.setMessage(msgText);
		builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				performDelete();
			}
		});
		builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		builder.setIcon(R.mipmap.ic_launcher);
		AlertDialog alertDialog = builder.create();
		//alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
		alertDialog.show();
	}

	private void performDelete() {
		DBAdapter databaseAdapter = new DBAdapter(this);
		databaseAdapter.open();
		for (Note n : notes) {
			long id = n.getDatabaseID();
			databaseAdapter.deleteRow(id);
		}

		databaseAdapter.close();

		Timber.i("Updating notifications...");
		NotesNotificationManager manager = new NotesNotificationManager(this);
		manager.hideAllNotifications();
		manager.showNoteNotifications();
	}
}
