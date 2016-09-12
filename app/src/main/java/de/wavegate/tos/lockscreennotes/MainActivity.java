package de.wavegate.tos.lockscreennotes;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;

import de.wavegate.tos.lockscreennotes.activity.EditNoteActivity;
import de.wavegate.tos.lockscreennotes.activity.NotesActivity;
import de.wavegate.tos.lockscreennotes.activity.SettingsActivity;
import de.wavegate.tos.lockscreennotes.data.Note;
import de.wavegate.tos.lockscreennotes.data.NoteAdapter;
import de.wavegate.tos.lockscreennotes.sql.DBAdapter;
import de.wavegate.tos.lockscreennotes.util.NotesNotificationManager;

public class MainActivity extends NotesActivity implements Observer {

	public static final String LOGTAG = "HomeScreenNotes";
	private DBAdapter databaseAdapter;
	private NoteAdapter noteAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(LOGTAG, "====== Home Screen Notes onCreate() ======");

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		ListView notesList = (ListView) findViewById(R.id.notes_list);

		databaseAdapter = new DBAdapter(this);
		databaseAdapter.open();
		databaseAdapter.addObserver(this);

		Log.i(LOGTAG, "Database opened.");

		noteAdapter = new NoteAdapter(this, R.layout.note_row, new ArrayList<Note>());
		notesList.setAdapter(noteAdapter);
		//notesList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
		//	@Override
		//	public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
		//		Log.i(LOGTAG, "Long click on Note-Item.");
		//		return false;
		//	}
		//});

		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		setShowNotifications(true);
		loadNotesFromDB();

		FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				addNewNote();
			}
		});
	}

	private void loadNotesFromDB() {
		noteAdapter.clear();
		Cursor cursor = databaseAdapter.getAllRows();

		Log.i(LOGTAG, "Refreshing displayed Notes. Found: " + cursor.getCount());
		ArrayList<Note> list = Note.getAllNotesFromDB(databaseAdapter);

		Collections.sort(list);
		for (Note n : list) noteAdapter.add(n);

		noteAdapter.notifyDataSetChanged();
	}

	private void saveNotesToDB() {
		ArrayList<Note> notes = new ArrayList<>();
		for (int i = 0; i < noteAdapter.getCount(); i++) notes.add(noteAdapter.getItem(i));

		boolean changed = false;
		Log.i(LOGTAG, "Saving known Notes. Found: " + notes.size());
		for (Note note : notes) {
			long id = note.getDatabaseID();
			String text = note.getText();
			int enabled = note.isEnabledSQL();
			long timestamp = note.getTimestamp();

			changed |= databaseAdapter.updateRow(id, text, enabled, timestamp);
		}

		Log.i(LOGTAG, "Saving finished. Changed? " + changed);
	}

	public void requestDeleteNote(final Note note) {
		Log.i(LOGTAG, "Request to delete note: '" + note.getText() + "'");
		new AlertDialog.Builder(this)
				.setTitle(R.string.delete_dialog_title)
				.setMessage(String.format(getString(R.string.delete_dialog_content), note.getTextPreview()))
				.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						deleteNote(note.getDatabaseID());
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

	public void requestEditNote(Note note) {
		setShowNotifications(false);

		Intent intent = new Intent(this, EditNoteActivity.class);
		intent.putExtra(EditNoteActivity.NOTE_ACTIVITY_NOTE_ID, note.getDatabaseID());
		startActivity(intent);
	}

	private void requestDeleteAll() {
		new AlertDialog.Builder(this)
				.setTitle(R.string.deleteall_dialog_title)
				.setMessage(R.string.deleteall_dialog_text)
				.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						for (Note n : Note.getAllNotesFromDB(databaseAdapter)) {
							deleteNote(n.getDatabaseID());
						}
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

	private void requestDisableAll() {
		for (int i =0;i<noteAdapter.getCount();i++){
			Note n = noteAdapter.getItem(i);
			if (n != null) {
				n.setEnabled(false);
			}
		}

		saveNotesToDB();
		noteAdapter.notifyDataSetChanged();
	}

	private void deleteNote(long id) {
		databaseAdapter.deleteRow(id);
	}

	@Override
	protected void onPause() {
		super.onPause();
		databaseAdapter.deleteObserver(this);
		saveNotesToDB();
		databaseAdapter.close();

		Log.i(LOGTAG, "Mainactivity: onPause() [Show notifications? " + isShowNotifications() + "]");
		if (isShowNotifications())
			new NotesNotificationManager(this).showNotifications();
	}

	@Override
	protected void onResume() {
		super.onResume();
		databaseAdapter.addObserver(this);
		databaseAdapter.open();
		loadNotesFromDB();

		Log.i(LOGTAG, "Mainactivity: onResume()");
		new NotesNotificationManager(this).hideNotifications();
	}

	public void addNewNote() {
		addNewNote("");
	}

	public void addNewNote(String text) {
		Note note = new Note(text, false, new Date().getTime());
		long id = databaseAdapter.insertRow(note.getText(), note.isEnabledSQL(), note.getTimestamp());
		note.setDatabaseID(id);

		requestEditNote(note);
	}

	@Override
	public void setShowNotifications(boolean showNotifications) {
		boolean b = isShowNotifications();
		super.setShowNotifications(showNotifications);

		Log.i(LOGTAG, "Mainactivity changed its ShowNotification Behavior: " + b + " -> " + isShowNotifications());
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		databaseAdapter.close();

		Log.i(LOGTAG, "Database closed.");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		switch (id) {
			case R.id.action_settings:
				setShowNotifications(false);
				Intent intent = new Intent(this, SettingsActivity.class);
				startActivity(intent);
				return true;
			case R.id.action_delete_all:
				requestDeleteAll();
				return true;

			case R.id.action_disable_all:
				requestDisableAll();
				return true;
		}


		return super.onOptionsItemSelected(item);
	}

	@Override
	public void update(Observable observable, Object o) {
		Log.i(LOGTAG, "MainActivity is triggered!");
		loadNotesFromDB();
	}
}
