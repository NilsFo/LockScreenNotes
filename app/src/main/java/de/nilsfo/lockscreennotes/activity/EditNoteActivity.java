package de.nilsfo.lockscreennotes.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Date;

import de.nilsfo.lsn.R;
import de.nilsfo.lockscreennotes.data.Note;
import de.nilsfo.lockscreennotes.data.font.FontAwesomeDrawableBuilder;
import de.nilsfo.lockscreennotes.sql.DBAdapter;
import de.nilsfo.lockscreennotes.util.NotesNotificationManager;
import timber.log.Timber;

public class EditNoteActivity extends NotesActivity {
	public static final String NOTE_ACTIVITY_NOTE_ID = "EditNoteActivity_note_id";
	public static final long ILLEGAL_NOTE_ID = -1;

	private Note myNote;
	private boolean canceled;
	private DBAdapter databaseAdapter;
	private EditText noteTF;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		canceled = false;
		setContentView(R.layout.activity_edit_note);
		ActionBar bar = getSupportActionBar();
		if (bar != null) bar.setDisplayHomeAsUpEnabled(true);

		SharedPreferences preferencses = PreferenceManager.getDefaultSharedPreferences(this);

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

		if (!preferencses.getBoolean("prefs_ignore_tutorial_autosave", false)) {
			Timber.i("Displaying the auto-save tutorial now.");

			Snackbar snackbar = Snackbar.make(noteTF, R.string.tutorial_autosave, Snackbar.LENGTH_INDEFINITE);
			snackbar.setAction(R.string.ok_got_it, new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Timber.i("Will not display this tutorial again.");
					PreferenceManager.getDefaultSharedPreferences(v.getContext()).edit().putBoolean("prefs_ignore_tutorial_autosave", true).apply();
				}
			});
			snackbar.setActionTextColor(getResources().getColor(R.color.colorPrimary));
			snackbar.show();
		}

		if (savedInstanceState == null) {
			actionMoveToBottom();
		}
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

	private void requestKeyBoard() {
		requestKeyBoard(noteTF);
	}

	public synchronized void requestKeyBoard(View view) {
		InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
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

		menu.findItem(R.id.action_move_to_bottom).setIcon(FontAwesomeDrawableBuilder.getOptionsIcon(this, R.string.fa_icon_down));
		menu.findItem(R.id.action_clear).setIcon(FontAwesomeDrawableBuilder.getOptionsIcon(this, R.string.fa_icon_clear));
		menu.findItem(R.id.action_share).setIcon(FontAwesomeDrawableBuilder.getOptionsIcon(this, R.string.fa_icon_share));
		menu.findItem(R.id.action_copy_note).setIcon(FontAwesomeDrawableBuilder.getOptionsIcon(this, R.string.fa_icon_copy));

		return true;
	}

	@Override
	protected void onPause() {
		super.onPause();
		Timber.i("EditNoteFrame: Paused");
		if (!canceled) saveNote();
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

		Timber.i("EditNoteFrame: Saving the note. Has something changed? " + changed);

		if (changed) {
			myNote.setText(text);
			myNote.setTimestamp(new Date());

			databaseAdapter.updateRow(myNote.getDatabaseID(), myNote.getText(), myNote.isEnabledSQL(), myNote.getTimestamp());
		}
	}

	public void actionCancelEdit() {
		new AlertDialog.Builder(this)
				.setTitle(R.string.action_cancel_edit)
				.setMessage(R.string.action_cancel_edit_confirm)
				.setPositiveButton(R.string.action_cancel_edit, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								canceled = true;
								finish();
							}
						});
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

			case R.id.action_cancel_edit:
				actionCancelEdit();
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
		Toast.makeText(this, R.string.error_internal_error, Toast.LENGTH_LONG).show();
	}

	@Override
	protected void onStop() {
		super.onStop();
		databaseAdapter.close();
	}
}
