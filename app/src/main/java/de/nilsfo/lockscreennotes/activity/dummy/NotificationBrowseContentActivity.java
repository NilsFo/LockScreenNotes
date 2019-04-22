package de.nilsfo.lockscreennotes.activity.dummy;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import java.util.ArrayList;

import de.nilsfo.lockscreennotes.data.Note;
import de.nilsfo.lockscreennotes.data.content.NoteContentAnalyzer;
import de.nilsfo.lockscreennotes.sql.DBAdapter;
import de.nilsfo.lockscreennotes.util.IntentUtils;
import de.nilsfo.lockscreennotes.util.NotesNotificationManager;
import de.nilsfo.lockscreennotes.util.URLUtils;
import de.nilsfo.lsn.R;
import timber.log.Timber;

import static de.nilsfo.lockscreennotes.LockScreenNotes.APP_TAG;

public class NotificationBrowseContentActivity extends Activity {

	public static final String INTENT_EXTRA_NOTE_CONTENT_TYPE = APP_TAG + "_intent_content_type";
	public static final int CONTENT_TYPE_URL = 0;
	public static final int CONTENT_TYPE_MAIL = 1;
	public static final int CONTENT_TYPE_PHONE_NUMBER = 2;

	private String noteText;
	private long noteID;
	private int contentType;
	private NoteContentAnalyzer contentAnalyzer;

	@Override
	protected void onStart() {
		super.onStart();
		Timber.i("Recieved a request to browse a note's content in external apps!");

		Bundle extras = getIntent().getExtras();
		if (extras == null) {
			Timber.e("I just got a 'NoitificationBrowseURL' activity intent! But there were no extras! Could not perform any actions!");
			Toast.makeText(this, R.string.error_internal_error, Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		DBAdapter databaseAdapter = new DBAdapter(this);
		databaseAdapter.open();

		noteID = extras.getLong(NotesNotificationManager.INTENT_EXTRA_NOTE_ID);
		contentType = extras.getInt(INTENT_EXTRA_NOTE_CONTENT_TYPE);
		Timber.i("The note's ID is: " + noteID + ". Content type: " + contentType + ". Fetching that from the DB now.");

		if (noteID <= 0) {
			Timber.e("The notification ID fetched [" + noteID + "] is illegal!");
			Toast.makeText(this, R.string.error_internal_error, Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		Note note = Note.getNoteFromDB(noteID, databaseAdapter);
		databaseAdapter.close();

		if (note == null) {
			Timber.e("The note with the ID " + noteID + " was not found in the Database!");
			Toast.makeText(this, R.string.error_internal_error, Toast.LENGTH_LONG).show();
			return;
		}
		noteText = note.getText();
		contentAnalyzer = new NoteContentAnalyzer(noteText);

		switch (contentType) {
			case CONTENT_TYPE_URL:
				handleURL();
				break;
			case CONTENT_TYPE_MAIL:
				handleMail();
				break;
			case CONTENT_TYPE_PHONE_NUMBER:
				handlePhoneNumber();
				break;
			default:
				Timber.e("Illegal content type: " + contentType + ". Can't process that.");
				Toast.makeText(this, R.string.error_internal_error, Toast.LENGTH_LONG).show();
				finish();
				break;
		}

		/* Felt obsolete. Might delete later.

		Timber.i("Attempting to browse URL: " + noteText);
		URLUtils utils = new URLUtils(this);
		if (!utils.containsSingleURL(noteText)) {
			Timber.e("The note with the ID " + noteID + " is not a single URL!");
			Toast.makeText(this, R.string.error_internal_error, Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		*/

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

		finish();
	}

	private void handleURL() {
		Timber.i("Requested to handle URL in note.");

		ArrayList<String> urls = contentAnalyzer.getURLs();
		Timber.i("Urls found in the note: " + urls);
		String url = urls.get(0);
		Timber.i("Browsing url: " + url);

		new URLUtils(this).browseURL(url);
	}

	private void handlePhoneNumber() {
		Timber.i("Requested to handle phone number in note.");

		ArrayList<String> numbers = contentAnalyzer.getPhoneNumbers();
		Timber.i("Phone numbers found in the note: " + numbers);
		String number = numbers.get(0);
		Timber.i("Browsing number: " + number);

		Intent intent = new IntentUtils(this).getPhoneNumberIntent(number);
		startActivity(intent);
	}

	private void handleMail() {
		Timber.i("Requested to handle mail in note.");

		ArrayList<String> mails = contentAnalyzer.getMails();
		Timber.i("Mail addresses found in the note: " + mails);
		String mail = mails.get(0);
		Timber.i("Sending mail to: " + mail);

		Intent intent = new IntentUtils(this).getMailIntent(mail,false);
		startActivity(intent);
	}

	public String getNoteText() {
		return noteText;
	}

	public long getNoteID() {
		return noteID;
	}

	public int getContentType() {
		return contentType;
	}
}
