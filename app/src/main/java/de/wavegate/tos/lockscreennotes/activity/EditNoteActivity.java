package de.wavegate.tos.lockscreennotes.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Date;

import de.wavegate.tos.lockscreennotes.R;
import de.wavegate.tos.lockscreennotes.data.Note;
import de.wavegate.tos.lockscreennotes.sql.DBAdapter;
import de.wavegate.tos.lockscreennotes.util.NotesNotificationManager;

import static de.wavegate.tos.lockscreennotes.MainActivity.LOGTAG;

public class EditNoteActivity extends NotesActivity {
	public static final String NOTE_ACTIVITY_NOTE_ID = "EditNoteActivity_note_id";
	public static final long ILLEGAL_NOTE_ID = -1;
	private Note myNote;

	private DBAdapter databaseAdapter;
	private EditText noteTF;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_edit_note);
		ActionBar bar =getSupportActionBar();
		if (bar!=null) bar.setDisplayHomeAsUpEnabled(true);

		long id = getIntent().getExtras().getLong(NOTE_ACTIVITY_NOTE_ID, ILLEGAL_NOTE_ID);
		if (id == ILLEGAL_NOTE_ID) {
			handleIllegalNote();
			return;
		}

		databaseAdapter = new DBAdapter(this);
		databaseAdapter.open();

		myNote = Note.getNoteFromDB(id, databaseAdapter);
		if (myNote == null) {
			handleIllegalNote();
			return;
		}

		setShowNotifications(true);
		noteTF = (EditText) findViewById(R.id.enditNoteFullscreenTF);
		noteTF.setText(myNote.getText());

		if (savedInstanceState == null) {
			actionMoveToBottom();
		}
		//InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		//imm.showSoftInput(noteTF, InputMethodManager.SHOW_IMPLICIT);
		requestKeyBoard();
	}

	private void actionClear() {
		new AlertDialog.Builder(this)
				.setTitle(R.string.action_clear)
				.setMessage(R.string.action_clear_confirm)
				.setPositiveButton(R.string.action_clear, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						noteTF.setText("");
						actionMoveToBottom();
					}
				})
				.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				})
				.setIcon(android.R.drawable.ic_dialog_alert)
				.show();
	}

	private void actionMoveToBottom() {
		noteTF.requestFocus();
		noteTF.setSelection(noteTF.getText().length());
		requestKeyBoard();
	}

	private void requestKeyBoard(){
		InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		imm.showSoftInput(noteTF, InputMethodManager.SHOW_IMPLICIT);
	}

	private void actionShare() {
		Intent sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_TEXT, noteTF.getText().toString());
		sendIntent.setType("text/plain");
		startActivity(Intent.createChooser(sendIntent, getResources().getString(R.string.share_using)));
		//startActivity(Intent.createChooser(sharingIntent, getResources().getString(R.string.share_using)));
	}

	private void actionCopyText() {
		String text = noteTF.getText().toString();

		ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
		ClipData clip = ClipData.newPlainText(getString(R.string.app_name), text);
		clipboard.setPrimaryClip(clip);

		Toast.makeText(this, R.string.action_copy_text_success, Toast.LENGTH_LONG).show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.edit_note_menu, menu);
		return true;
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.i(LOGTAG, "EditNoteFrame: Paused");
		saveNote();
		databaseAdapter.close();

		if (isShowNotifications())
			new NotesNotificationManager(this).showNotifications();
	}

	@Override
	protected void onResume() {
		super.onResume();
		databaseAdapter.open();
		new NotesNotificationManager(this).hideNotifications();
		setShowNotifications(true);
	}

	public void saveNote() {
		String oldText = myNote.getText();
		String text = noteTF.getText().toString();
		boolean changed = !oldText.equals(text);

		Log.i(LOGTAG, "EditNoteFrame: Saving the note. Has something changed? " + changed);

		if (changed) {

			myNote.setText(text);
			myNote.setTimestamp(new Date());

			databaseAdapter.updateRow(myNote.getDatabaseID(), myNote.getText(), myNote.isEnabledSQL(), myNote.getTimestamp());
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			// Respond to the action bar's Up/Home button
			case android.R.id.home:
				setShowNotifications(false);
				NavUtils.navigateUpFromSameTask(this);
				return true;

			case R.id.action_clear:
				actionClear();
				return true;

			case R.id.action_move_to_bottom:
				actionMoveToBottom();
				return true;

			case R.id.action_share:
				actionShare();
				return true;

			case R.id.action_copy_note:
				actionCopyText();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBackPressed() {
		setShowNotifications(false);
		super.onBackPressed();
	}

	private void handleIllegalNote() {
		//TODO handle
	}

	@Override
	protected void onStop() {
		super.onStop();
		databaseAdapter.close();
	}
}
