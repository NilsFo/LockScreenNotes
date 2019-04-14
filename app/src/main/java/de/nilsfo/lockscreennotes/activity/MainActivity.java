package de.nilsfo.lockscreennotes.activity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.nilsfo.lockscreennotes.data.Note;
import de.nilsfo.lockscreennotes.data.RelativeTimeTextfieldContainer;
import de.nilsfo.lockscreennotes.data.content.browse.NoteContentBrowseDialogMail;
import de.nilsfo.lockscreennotes.data.content.browse.NoteContentBrowseDialogPhone;
import de.nilsfo.lockscreennotes.data.content.browse.NoteContentBrowseDialogURLs;
import de.nilsfo.lockscreennotes.io.FileManager;
import de.nilsfo.lockscreennotes.io.backups.BackupManager;
import de.nilsfo.lockscreennotes.sql.DBAdapter;
import de.nilsfo.lockscreennotes.util.NoteSharer;
import de.nilsfo.lockscreennotes.util.NotesNotificationManager;
import de.nilsfo.lockscreennotes.util.TimeUtils;
import de.nilsfo.lockscreennotes.util.VersionManager;
import de.nilsfo.lockscreennotes.view.NotesRecyclerAdapter;
import de.nilsfo.lsn.R;
import timber.log.Timber;

import static de.nilsfo.lockscreennotes.LockScreenNotes.PREFS_TAG;
import static de.nilsfo.lockscreennotes.LockScreenNotes.REQUEST_CODE_INTENT_EXTERNAL_SEARCH;
import static de.nilsfo.lockscreennotes.LockScreenNotes.REQUEST_CODE_PERMISSION_STORAGE;

public class MainActivity extends NotesActivity implements Observer, NotesRecyclerAdapter.NotesRecyclerAdapterListener {

	public static final int IMPORT_NOTE_PEWVIEW_SIZE = 35;
	public static final int ONE_SECOND_IN_MS = 1000;
	public static final String PREFS_HIDE_TUTORIAL = "prefs_hide_tutorial";
	public static final int DEFAULT_SNACKBAR_PREVIEW_WORD_COUNT = 15;
	public static final String PREFS_LAST_KNOWN_VERSION = PREFS_TAG + "last_known_version";

	private DBAdapter databaseAdapter;
	private NotesRecyclerAdapter noteRecyclerAdapter;
	private RecyclerView notesRecyclerView;
	private ScrollView tutorialView;
	private CheckBox tutorialDontShowAgainCB;
	private TextView nothingToDisplayLB;
	private ExecutorService executorService;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		tutorialView = (ScrollView) findViewById(R.id.tutorial_view);
		nothingToDisplayLB = (TextView) findViewById(R.id.nothing_to_display);

		tutorialDontShowAgainCB = (CheckBox) findViewById(R.id.tutorial_dont_show_again_cb);
		tutorialDontShowAgainCB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				onTutorialCBclicked(isChecked);
			}
		});

		databaseAdapter = new DBAdapter(this);
		databaseAdapter.open();
		databaseAdapter.addObserver(this);

		Timber.i("Database opened.");

		noteRecyclerAdapter = new NotesRecyclerAdapter(databaseAdapter, this);
		noteRecyclerAdapter.setListener(this);

		notesRecyclerView = (RecyclerView) findViewById(R.id.notes_recycler_view);
		notesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
		notesRecyclerView.setAdapter(noteRecyclerAdapter);

		setShowNotifications(true);
		loadNotesFromDB();
		setupRelativeDateUpdater();

		FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				onFABClicked();
			}
		});

		if (noteRecyclerAdapter.isEmpty()) {
			tutorialView.animate().alpha(1f).setDuration(2000);
		}

		//Version change check & changelog
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		int lastVer = prefs.getInt(PREFS_LAST_KNOWN_VERSION, 0);
		int currentVer = VersionManager.getCurrentVersion(this);
		if (lastVer != 0 && lastVer != currentVer && currentVer != VersionManager.CURRENT_VERSION_UNKNOWN) {
			VersionManager.onVersionChange(this, lastVer, currentVer);
		}

		FileManager fm = new FileManager(this);
		//fm.notifyDirectoryChange(fm.getInternalDir().getAbsolutePath());
		fm.notifyMediaScanner(fm.getInternalDir());

		prefs.edit().putInt(PREFS_LAST_KNOWN_VERSION, currentVer).apply();
		Timber.i("Application started. App version: " + currentVer);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		databaseAdapter.close();

		Timber.i("Database closed.");
	}

	private void onTutorialCBclicked(boolean isChecked) {
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
		editor.putBoolean(PREFS_HIDE_TUTORIAL, isChecked);
		Timber.i("Changed the 'dont show tutorial again' prefs: " + isChecked);
		editor.apply();
	}

	private void loadNotesFromDB() {
		noteRecyclerAdapter.refreshNotesList();
		updateTutorialView();
	}

	public void requestRecoverNote(Note note) {
		try {
			addNewNote(note);
			Toast.makeText(this, getString(R.string.info_note_recovered, getNoteSnackbarPreview(note)), Toast.LENGTH_LONG).show();
		} catch (Exception e) {
			Timber.e(e, "Failed to add note " + note.getTextPreview() + " to the database!");
			Toast.makeText(this, R.string.error_note_save_failed, Toast.LENGTH_LONG).show();
		}
	}

	public void requestDeleteNote(long databaseID) {
		final Note note = Note.getNoteFromDB(databaseID, databaseAdapter);
		Timber.i("Request to delete note: '" + note.getText() + "'");
		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("prefs_quick_delete", true)) {
			deleteNote(note.getDatabaseID());

			Snackbar snackbar = Snackbar.make(notesRecyclerView, getString(R.string.info_note_deleted, note.getTextPreview()), Snackbar.LENGTH_LONG);
			snackbar.setAction(R.string.action_undo, new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					requestRecoverNote(note);
				}
			});
			snackbar.setActionTextColor(getResources().getColor(R.color.colorPrimary));

			View snackbarView = snackbar.getView();
			TextView textView = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
			textView.setMaxLines(2);
			textView.setMinLines(2);
			textView.setEllipsize(TextUtils.TruncateAt.END);

			snackbar.show();
		} else {
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
					.setIcon(R.mipmap.ic_launcher)
					.show();
		}
	}

	public void requestEditNote(long noteDatabaseID) {
		setShowNotifications(false);

		Intent intent = new Intent(this, EditNoteActivity.class);
		intent.putExtra(EditNoteActivity.EXTRA_NOTE_ACTIVITY_NOTE_ID, noteDatabaseID);
		intent.putExtra(EditNoteActivity.EXTRA_ACTIVITY_STANDALONE, false);
		startActivity(intent);
	}

	private void requestDeleteAll() {
		if (Note.getAllNotesFromDB(databaseAdapter).isEmpty()) {
			Toast.makeText(this, R.string.error_no_note_to_delete, Toast.LENGTH_LONG).show();
			return;
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.deleteall_dialog_title);
		builder.setMessage(R.string.deleteall_dialog_text);
		builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				for (Note n : Note.getAllNotesFromDB(databaseAdapter)) {
					deleteNote(n.getDatabaseID());
				}
			}
		});
		builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		builder.setIcon(R.drawable.baseline_warning_black_48);
		builder.show();
	}

	private void requestDeleteAllDisabled() {
		int allCount = 0;
		int disabledCount = 0;
		final ArrayList<Note> disabledList = new ArrayList<>();
		for (int i = 0; i < noteRecyclerAdapter.getItemCount(); i++) {
			Note n = Note.getNoteFromDB(noteRecyclerAdapter.getItemAt(i), databaseAdapter);
			if (n != null) {
				allCount++;
				if (!n.isEnabled()) {
					disabledList.add(n);
					disabledCount++;
				}
			}
		}
		Timber.i("Disabling check. Count: " + disabledCount + " - ListSize: " + disabledList.size());

		if (disabledCount == 0 || disabledList.isEmpty()) {
			Toast.makeText(this, R.string.error_no_note_to_delete, Toast.LENGTH_LONG).show();
			return;
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.action_delete_all);
		builder.setMessage(getString(R.string.deleteall_disabled_dialog_text, disabledCount, allCount));
		builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				for (Note n : disabledList) {
					deleteNote(n.getDatabaseID());
				}
			}
		});
		builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		builder.setIcon(R.drawable.baseline_warning_black_48);
		builder.show();
	}

	private void requestDisableAll() {
		ArrayList<Note> notes = Note.getAllNotesFromDB(databaseAdapter);
		for (Note n : notes) {
			databaseAdapter.updateRow(n.getDatabaseID(), n.getText(), 0, n.getTimestamp());
		}
		noteRecyclerAdapter.refreshNotesList();
	}

	private void requestBackupMenu() {
		BackupManager bcm = new BackupManager(this);
		if (!bcm.hasExternalStoragePermission()) {
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSION_STORAGE);
			return;
		}

		String backupCount = "-";
		String lastBackup = "-";
		if (bcm.hasBackupsMade()) {
			backupCount = String.valueOf(bcm.findBackupFiles().size());
			long l = bcm.getLastestBackupFile().lastModified();
			lastBackup = new TimeUtils(this).formatDateAccordingToPreferences(l);
		}

		FileManager manager = new FileManager(this);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.action_backup);
		builder.setIcon(R.mipmap.ic_launcher);
		builder.setMessage(getString(R.string.action_backup_info, manager.getNoteBackupDir().getAbsolutePath(), backupCount, lastBackup));
		builder.setPositiveButton(R.string.action_create_backup, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				requestBackupExport();
			}
		});
		builder.setNeutralButton(R.string.action_import_backup, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				requestBackupImportMenu();
			}
		});
		builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		builder.show();
	}

	public void requestBackupExport() {
		BackupManager backupManager = new BackupManager(this);
		File backupFile;
		try {
			backupFile = backupManager.completeBackup();
		} catch (JSONException e) {
			e.printStackTrace();
			Timber.e(e);
			Toast.makeText(this, R.string.error_action_export, Toast.LENGTH_LONG).show();
			return;
		} catch (IOException e) {
			e.printStackTrace();
			Timber.e(e);
			Toast.makeText(this, R.string.error_action_export, Toast.LENGTH_LONG).show();
			return;
		}

		Timber.i("Everything saved! -> " + backupFile.getAbsolutePath());
		Toast.makeText(this, R.string.action_export_success, Toast.LENGTH_LONG).show();
	}

	public void requestBackupImportMenu() {
		BackupManager manager = new BackupManager(this);
		File f = manager.getLastestBackupFile();

		if (f == null) {
			Toast.makeText(this, R.string.error_no_backup, Toast.LENGTH_LONG).show();
			requestBackupImportExternal();
			return;
		}
		try {
			requestBackupImportMenuConfirmFile(f);
		} catch (Exception e) {
			e.printStackTrace();
			Timber.e(e);
			Toast.makeText(this, R.string.error_internal_error, Toast.LENGTH_LONG).show();
		}
	}

	public void requestBackupImportMenuConfirmFile(final File file) throws IOException, JSONException {
		BackupManager manager = new BackupManager(this);
		BackupManager.BackupMetaData metaData = manager.getMetaData(file);

		String versionWarning = "";
		if (VersionManager.getCurrentVersion(this) != metaData.getVersion()) {
			versionWarning = getString(R.string.warning_backup_version_difference);
		}
		String time = new TimeUtils(this).formatDateAccordingToPreferences(metaData.getTimestamp());
		String msg = getString(R.string.action_backup_import_confirm, file.getName(), time, metaData.getCount(), versionWarning);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.action_import_backup);
		builder.setIcon(R.mipmap.ic_launcher);
		builder.setMessage(msg.trim());
		builder.setPositiveButton(R.string.action_import_this, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				requestBackupImportMenuConfirmData(file);
			}
		});
		builder.setNeutralButton(R.string.action_different_file, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				requestBackupImportChoose();
			}
		});
		builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		builder.show();
	}

	public void requestBackupImportMenuConfirmData(File source) {
		BackupManager manager = new BackupManager(this);
		final ArrayList<Note> list;
		try {
			list = manager.readBackupFile(source);
		} catch (IOException e) {
			e.printStackTrace();
			Timber.e(e);
			Toast.makeText(this, R.string.error_internal_error, Toast.LENGTH_LONG).show();
			return;
		} catch (JSONException e) {
			e.printStackTrace();
			Timber.e(e);
			Toast.makeText(this, R.string.error_invalid_file_format, Toast.LENGTH_LONG).show();
			return;
		}

		Timber.i("Found " + list.size() + " notes in file: " + Arrays.toString(list.toArray()));
		int currentNoteCount = databaseAdapter.getAllRows().getCount();
		if (currentNoteCount == 0) {
			addNewNote(list);
			return;
		}

		String summary = "";
		for (Note n : list) {
			summary = "-" + n.getTextPreview(IMPORT_NOTE_PEWVIEW_SIZE) + "\n" + summary;
		}
		summary = summary.trim();

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.action_import_choose);
		builder.setIcon(R.mipmap.ic_launcher);
		builder.setMessage(getString(R.string.action_backup_import_replace_confirm, String.valueOf(currentNoteCount), String.valueOf(list.size()), summary));
		builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		builder.setPositiveButton(R.string.action_replace, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				databaseAdapter.deleteAll();
				addNewNote(list);
			}
		});
		builder.setNeutralButton(R.string.action_merge, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				addNewNote(list);
			}
		});
		builder.show();
	}

	public void requestBackupImportChoose() {
		BackupManager manager = new BackupManager(this);
		ArrayList<File> list = manager.findBackupFiles();
		switch (list.size()) {
			case 0:
				Toast.makeText(this, R.string.error_no_backup, Toast.LENGTH_LONG).show();
				requestBackupImportExternal();
				return;
			case 1:
				Toast.makeText(this, R.string.error_only_one_backup, Toast.LENGTH_LONG).show();
				requestBackupImportExternal();
				return;
		}

		ArrayList<String> filenames = new ArrayList<>();
		for (File f : list) {
			filenames.add(f.getName());
		}
		Collections.sort(filenames);
		Collections.reverse(filenames);
		final CharSequence[] sequences = new CharSequence[filenames.size()];
		for (int i = 0; i < filenames.size(); i++) {
			sequences[i] = filenames.get(i);
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.action_import_choose);
		builder.setIcon(R.mipmap.ic_launcher);
		builder.setSingleChoiceItems(sequences, 0, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				SparseBooleanArray sel = ((AlertDialog) dialog).getListView().getCheckedItemPositions();
				Timber.v("File(s) selection updated: " + sel);
			}
		});
		builder.setPositiveButton(R.string.action_select, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				SparseBooleanArray sel = ((AlertDialog) dialog).getListView().getCheckedItemPositions();
				CharSequence sequence = null;
				for (int i = 0; i < sequences.length; i++) {
					if (sel.get(i)) {
						sequence = sequences[i];
					}
				}
				Timber.i("On accept: " + sequence);
				File file = new File(new FileManager(MainActivity.this).getNoteBackupDir(), sequence.toString());
				Timber.i("Assumed backup File: " + file.getAbsolutePath() + " - Exists: " + file.exists());

				if (!file.exists()) {
					Timber.e("Failed to set up a selected file! This file does not exist: " + file.getAbsolutePath());
					Toast.makeText(MainActivity.this, R.string.error_internal_error, Toast.LENGTH_LONG).show();
					return;
				}

				try {
					requestBackupImportMenuConfirmFile(file);
				} catch (Exception e) {
					e.printStackTrace();
					Timber.e(e);
					Toast.makeText(MainActivity.this, R.string.error_internal_error, Toast.LENGTH_LONG).show();
				}
			}
		});
		builder.setNeutralButton(R.string.action_external_file, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				requestBackupImportExternal();
			}
		});
		builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		builder.show();
	}

	private void requestBackupImportExternal() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("*/*");
		intent.addCategory(Intent.CATEGORY_OPENABLE);

		try {
			startActivityForResult(
					Intent.createChooser(intent, getString(R.string.action_import_choose)), REQUEST_CODE_INTENT_EXTERNAL_SEARCH);
		} catch (android.content.ActivityNotFoundException ex) {
			Toast.makeText(this, R.string.error_no_file_manager, Toast.LENGTH_SHORT).show();
		}
	}

	private void deleteNote(long id) {
		databaseAdapter.deleteRow(id);    //we can request changes to the database here, and this object listens to changes and updates
	}

	private void updateTutorialView() {
		boolean prefs_hide = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(PREFS_HIDE_TUTORIAL, false);
		boolean tut = noteRecyclerAdapter.isEmpty();
		tutorialView.setVisibility(View.GONE);
		nothingToDisplayLB.setVisibility(View.GONE);
		notesRecyclerView.setVisibility(View.VISIBLE);
		Timber.i("Checking data for displaying the tutorial now. Empty? " + tut + " Preferences 'HIDE'? " + prefs_hide);
		if (tut) {
			notesRecyclerView.setVisibility(View.GONE);
			if (prefs_hide) {
				nothingToDisplayLB.setVisibility(View.VISIBLE);
			} else {
				tutorialView.setVisibility(View.VISIBLE);
			}
		}
	}

	private void setupRelativeDateUpdater() {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		if (!sharedPref.getBoolean("prefs_time_relative", true)) {
			executorService = null;
			return;
		}

		if (executorService != null) {
			executorService.shutdownNow();
		}

		executorService = Executors.newFixedThreadPool(1);
		executorService.execute(new Runnable() {
			@Override
			public void run() {
				try {
					while (!executorService.isShutdown()) {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								RelativeTimeTextfieldContainer container = RelativeTimeTextfieldContainer.getContainer();
								container.updateText(MainActivity.this);
							}
						});
						Thread.sleep(ONE_SECOND_IN_MS);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
					Timber.e(e, "Loop interrupted!");
				}
			}
		});
	}

	private void onFABClicked() {
		actionAddNewNote();
	}

	public void actionAddNewNote() {
		actionAddNewNote("");
	}

	public long addNewNote(Note note) {
		long id = databaseAdapter.insertRow(note.getText(), note.isEnabledSQL(), note.getTimestamp());
		note.setDatabaseID(id);
		return id;
	}

	public void addNewNote(Collection<Note> notes) {
		for (Note n : notes) {
			addNewNote(n);
		}
	}

	public void actionAddNewNote(String text) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		boolean enabled = sharedPreferences.getBoolean("prefs_auto_enable_new_notes", true);
		Note note = new Note(text, enabled, new Date().getTime());
		long id = addNewNote(note);
		requestEditNote(id);
	}

	private String getNoteSnackbarPreview(Note note) {
		double density = getResources().getDisplayMetrics().density;
		int words = (int) (DEFAULT_SNACKBAR_PREVIEW_WORD_COUNT * density);
		return note.getTextPreview(words);
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
			case R.id.action_delete_all_disabled:
				requestDeleteAllDisabled();
				return true;
			case R.id.action_backup:
				requestBackupMenu();
				return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Timber.i("Got an Activity result intent! Code: " + requestCode + " Data: " + data);
		switch (requestCode) {
			case REQUEST_CODE_INTENT_EXTERNAL_SEARCH:
				if (resultCode == RESULT_OK) {
					Timber.i("Haha, it was an file searcher result, all along!");
					Uri uri = data.getData();
					Timber.i("File Uri: " + uri.toString());
					String path = null;
					try {
						path = new FileManager(this).getPath(uri);
						Timber.i("File Path: " + path);
					} catch (URISyntaxException e) {
						e.printStackTrace();
						Timber.e(e);
						return;
					}
					try {
						requestBackupImportMenuConfirmFile(new File(path));
					} catch (Exception e) {
						e.printStackTrace();
						Timber.e(e);
						Toast.makeText(this, R.string.error_invalid_file_format, Toast.LENGTH_LONG).show();
					}
				}
				break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onPause() {
		super.onPause();
		databaseAdapter.deleteObserver(this);
		databaseAdapter.close();

		if (executorService != null) {
			executorService.shutdownNow();
		}

		Timber.i("Mainactivity: onPause() [Show notifications? " + isShowNotifications() + "]");
		if (isShowNotifications()) {
			new NotesNotificationManager(this).showNoteNotifications();
		}
	}

	@Override
	public void update(Observable observable, Object o) {
		Timber.i("MainActivity is triggered!");
		loadNotesFromDB();
	}

	@Override
	public void onCardNotePressed(long noteID) {
		Timber.i("Main: Note was pressed: " + noteID);
		requestEditNote(noteID);
	}

	@Override
	public void onCardNotePressedLong(long noteID) {
		Timber.i("Main: Note was long pressed: " + noteID);
	}

	@Override
	public void onCardNoteMenuPressed(long noteID, MenuItem item) {
		int itemID = item.getItemId();
		Note note = Note.getNoteFromDB(noteID, databaseAdapter);

		if (note == null) {
			Toast.makeText(this, R.string.error_internal_error, Toast.LENGTH_LONG).show();
			return;
		}

		Timber.i("Main: Note menu was pressed: " + note.getTextPreview() + ": '" + item.getTitle() + "'");
		switch (itemID) {
			case R.id.action_card_delete_note:
				requestDeleteNote(noteID);
				return;
			case R.id.action_card_share:
				new NoteSharer(this).share(note);
				return;
			case R.id.action_card_open_url:
				new NoteContentBrowseDialogURLs(this).displayDialog(note);
				return;
			case R.id.action_card_open_phone:
				new NoteContentBrowseDialogPhone(this).displayDialog(note);
				return;
			case R.id.action_card_open_mail:
				new NoteContentBrowseDialogMail(this).displayDialog(note);
				return;
			default:
		}
	}

	@Override
	public void onCardNoteToggleImagePressed(long noteID) {
		Note note = Note.getNoteFromDB(noteID, databaseAdapter);
		if (note == null) {
			Toast.makeText(this, R.string.error_internal_error, Toast.LENGTH_LONG).show();
			return;
		}

		boolean enabled = note.isEnabled();
		Timber.i("Toggling enabled status for note: " + note.getTextPreview() + ". Currently enabled: " + enabled);
		note.setEnabled(!enabled);

		databaseAdapter.updateRow(noteID, note.getText(), note.isEnabledSQL(), note.getTimestamp());
		noteRecyclerAdapter.refreshNotesList();
		Timber.i("Finished toggle status. Has the view updated?");
	}

	@Override
	public void setShowNotifications(boolean showNotifications) {
		boolean b = isShowNotifications();
		super.setShowNotifications(showNotifications);

		Timber.i("Mainactivity changed its ShowNotification Behavior: " + b + " -> " + isShowNotifications());
	}

	@Override
	protected void onResume() {
		super.onResume();
		databaseAdapter.addObserver(this);
		databaseAdapter.open();
		loadNotesFromDB();

		tutorialDontShowAgainCB.setChecked(PreferenceManager.getDefaultSharedPreferences(this).getBoolean(PREFS_HIDE_TUTORIAL, false));
		setupRelativeDateUpdater();

		Timber.i("Mainactivity: onResume()");
		new NotesNotificationManager(this).hideAllNotifications();

		noteRecyclerAdapter.refreshNotesList();
	}
}
