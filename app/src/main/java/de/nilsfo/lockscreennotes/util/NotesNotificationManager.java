package de.nilsfo.lockscreennotes.util;

import static de.nilsfo.lockscreennotes.LockScreenNotes.REQUEST_CODE_INTENT_OPEN_APP;
import static de.nilsfo.lockscreennotes.activity.EditNoteActivity.EXTRA_NOTE_ACTIVITY_NOTE_ID;
import static de.nilsfo.lockscreennotes.activity.dummy.NotificationBrowseContentActivity.CONTENT_TYPE_MAIL;
import static de.nilsfo.lockscreennotes.activity.dummy.NotificationBrowseContentActivity.CONTENT_TYPE_PHONE_NUMBER;
import static de.nilsfo.lockscreennotes.activity.dummy.NotificationBrowseContentActivity.CONTENT_TYPE_URL;
import static de.nilsfo.lockscreennotes.activity.dummy.NotificationBrowseContentActivity.INTENT_EXTRA_NOTE_CONTENT_TYPE;
import static de.nilsfo.lockscreennotes.util.NotificationChannelManager.CHANNEL_ID_AUTO_BACKUP_CHANNEL;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.preference.PreferenceManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;

import de.nilsfo.lockscreennotes.LockScreenNotes;
import de.nilsfo.lockscreennotes.activity.EditNoteActivity;
import de.nilsfo.lockscreennotes.activity.MainActivity;
import de.nilsfo.lockscreennotes.activity.dummy.NotificationBrowseContentActivity;
import de.nilsfo.lockscreennotes.data.Note;
import de.nilsfo.lockscreennotes.data.content.NoteContentAnalyzer;
import de.nilsfo.lockscreennotes.io.StoragePermissionManager;
import de.nilsfo.lockscreennotes.io.backups.BackupManager;
import de.nilsfo.lockscreennotes.receiver.NotificationDeleteReceiver;
import de.nilsfo.lockscreennotes.receiver.NotificationDismissedReceiver;
import de.nilsfo.lockscreennotes.sql.DBAdapter;
import de.nilsfo.lsn.R;
import timber.log.Timber;

/**
 * Created by Nils on 16.08.2016.
 */

public class NotesNotificationManager {

	public static final String KEY_NOTIFICATION_GROUP_NOTES = LockScreenNotes.APP_TAG + "key_notification_group";

	public static final int NOTIFICATION_STATIC_ID_AUTOMATIC_BACKUP = 1;
	public static final String PREFERENCE_LOW_PRIORITY_NOTE = "prefs_low_priority_note";
	public static final String PREFERENCE_HIGH_PRIORITY_NOTE = "prefs_high_priority_note";
	public static final String PREFERENCE_REVERSE_ORDERING = "prefs_reverse_displayed_notifications";
	public static final String INTENT_EXTRA_NOTE_ID = LockScreenNotes.APP_TAG + "notification_id";

	public static final int REQUEST_CODE_INTENT_OPEN_APP_DYNAMIC_BASE = LockScreenNotes.REQUEST_CODE_INTENT_OPEN_APP * 10000;
	public static final int REQUEST_CODE_INTENT_OPEN_APP_EDIT_NOTE_DYNAMIC_BASE = LockScreenNotes.REQUEST_CODE_INTENT_OPEN_APP_EDIT_NOTE * 10000;

	@Deprecated
	public static final String INTENT_EXTRA_DELETE = LockScreenNotes.APP_TAG + "delete_mode";

	/**
	 * Hint: All notes notification IDs start at this offset!
	 */
	public static final int NOTES_NOTIFICATION_ID_OFFSET = 100;

	public static final int DEFAULT_NOTIFICATION_ID = 100;
	public static final int NOTE_PREVIEW_SIZE = -1;
	public static final int INTENT_EXTRA_NOTE_ID_NONE = -1;
	private Context context;
	private ArrayList<Note> notesList;
	private boolean reversed;
	private NotificationChannelManager channelManager;

	public NotesNotificationManager(Context context) {
		this.context = context;

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		notesList = new ArrayList<>();
		DBAdapter databaseAdapter = new DBAdapter(context);
		databaseAdapter.open();

		Cursor cursor = databaseAdapter.getAllRows();
		if (cursor.moveToFirst()) {
			do {
				int id = cursor.getInt(DBAdapter.COL_ROWID);
				Note note = Note.getNoteFromDB(id, databaseAdapter);

				if (note != null && note.isEnabled()) {
					Timber.i("NotificatuonManager: Found an enabled note with: " + id);
					notesList.add(note);
				}
			} while (cursor.moveToNext());
		}
		databaseAdapter.close();

		Collections.sort(notesList);
		Timber.i("Sorted Notes to display: " + Arrays.toString(notesList.toArray()));

		reversed = sharedPreferences.getBoolean(PREFERENCE_REVERSE_ORDERING, false);
		Timber.i("Reversed notes preference: " + reversed);
		if (reversed) {
			Collections.reverse(notesList);
			Timber.i("Reversed note contents: " + Arrays.toString(notesList.toArray()));
		}

		Timber.i("Preparing Channels...");
		channelManager = new NotificationChannelManager(context);
		channelManager.requestSetUpChannels();
	}

	public boolean hasNotesNotifications() {
		return !notesList.isEmpty();
	}

	public boolean hasOnlyOneNoteNotification() {
		return getNoteNotificationCount() == 1;
	}

	public void showNoteNotifications() {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		Timber.i("NotesNotificationManager: Request to show notifications received!");

		if (!hasNotesNotifications()) {
			Timber.i("... but it was empty. Nothing to display.");
			return;
		}

		if (!hasUserPermissionToDisplayNotifications()) {
			String appName = context.getString(R.string.app_name);
			String s = context.getString(R.string.error_not_displaying_notifications_no_permissions, appName);
			Toast.makeText(context, s, Toast.LENGTH_LONG).show();
			return;
		}

		NotificationCompat.Builder builder = getNotesBuilder();
		String text = "";
		String bigText = "";
		if (hasOnlyOneNoteNotification()) {
			displayMultipleNoteNotifications();
			return;
		} else {
			if (sharedPreferences.getBoolean("prefs_seperate_notes", false)) {
				displayMultipleNoteNotifications();
				return;
			}

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
				displayAndroid7SummaryNoteNotifications();
				return;
			}

			//Version 6 or lower: Summarized, single notification that displays all notes in a single notification!
			String noteNotificationCount = String.valueOf(getNoteNotificationCount());
			text = String.format(context.getString(R.string.notification_multiple_notes), noteNotificationCount);
			builder.setNumber(getNoteNotificationCount());
			bigText = "";
			for (int i = 0; i < notesList.size(); i++) {
				bigText += (i + 1) + ". " + notesList.get(i).getText() + "\n";
			}
			bigText = bigText.trim();
		}

		builder.setContentText(text);
		builder.setStyle(new NotificationCompat.BigTextStyle().bigText(bigText));
		builder.setCategory(NotificationCompat.CATEGORY_REMINDER);

		PendingIntent dismissIntent = createOnNoteDismissIntent(INTENT_EXTRA_NOTE_ID_NONE);
		if (dismissIntent == null) {
			Toast.makeText(context, R.string.error_notification_pending_intent_failure, Toast.LENGTH_LONG).show();
		} else {
			builder.setDeleteIntent(dismissIntent);
			builder.addAction(R.drawable.baseline_notifications_off_black_24, context.getString(R.string.action_mark_disabled_all), createOnNoteDismissIntent(INTENT_EXTRA_NOTE_ID_NONE));
		}

		NotificationManager manager = getNotificationManagerService();
		Notification notification = builder.build();
		if (manager != null) {
			manager.notify(DEFAULT_NOTIFICATION_ID, notification);
		} else {
			Timber.w("Could not display notification! Reason: Failed to get NotificationManager from SystemService!");
		}
		//Toast.makeText(context, "Notification created. Debug Code: " + new Random().nextInt(500), Toast.LENGTH_LONG).show();
	}

	private void displayMultipleNoteNotifications() {
		NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		if (manager == null) {
			Timber.e("Wanted to display a notification, but no Manager is present!");
			return;
		}

		HashMap<Integer, NotificationCompat.Builder> map = generateSeparateNoteNotifications();
		for (int id : map.keySet()) {
			NotificationCompat.Builder builder = map.get(id);
			Timber.i("Attempting to display a notification via builder from ID " + id);

			if (builder != null) {
				manager.notify(id, builder.build());
			} else {
				Timber.w("There was no builder for ID " + id);
			}
		}
	}

	private HashMap<Integer, NotificationCompat.Builder> generateSeparateNoteNotifications() {
		HashMap<Integer, NotificationCompat.Builder> map = new HashMap<Integer, NotificationCompat.Builder>();

		ArrayList<Note> tempList = new ArrayList<>(notesList);
		Collections.reverse(tempList);
		for (int i = 0; i < tempList.size(); i++) {
			Note n = tempList.get(i);
			String text = n.getTextPreview(NOTE_PREVIEW_SIZE);
			String bigtext = n.getText();

			NotificationCompat.Builder builder = getNotesBuilder();
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
				builder.setContentTitle(text);
			} else {
				builder.setContentText(n.getTextPreview());
				builder.setContentTitle(context.getString(R.string.app_name));
			}

			NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle();
			style.bigText(bigtext);
			style.setBigContentTitle(context.getString(R.string.note_notification_index, String.valueOf(notesList.size() - i)));
			if (!hasOnlyOneNoteNotification()) {
				style.setSummaryText(context.getString(R.string.notification_multiple_notes, String.valueOf(notesList.size())));
			}
			builder.setStyle(style);
			builder.setNumber(notesList.size() - i);
			builder.setSortKey(String.valueOf(notesList.size() - i));
			builder.setWhen(n.getTimestamp());

			int id = n.getNotificationID();
			applyActionsToIndividualNote(builder, n);
			map.put(id, builder);
		}
		return map;
	}

	private void displayAndroid7SummaryNoteNotifications() {
		NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		if (manager == null) {
			Timber.e("Wanted to display Android7+ specific summary notifications. But it failed. No notification-manager available.");
			return;
		}

		ArrayList<Note> tempList = new ArrayList<>(notesList);
		Collections.reverse(tempList);
		for (int i = 0; i < tempList.size(); i++) {
			Note note = tempList.get(i);
			NotificationCompat.Builder builder = getNotesBuilder();
			String noteIndex = context.getString(R.string.note_notification_index, String.valueOf(tempList.size() - i));

			builder.setGroup(KEY_NOTIFICATION_GROUP_NOTES);
			builder.setContentTitle(noteIndex);
			builder.setContentText(note.getTextPreview());
			builder.setStyle(new NotificationCompat.BigTextStyle().bigText(note.getText()));

			if (!reversed) {
				builder.setWhen(note.getTimestamp());
			}

			applyActionsToIndividualNote(builder, note);
			manager.notify(note.getNotificationID(), builder.build());
		}

		NotificationCompat.Builder builder = getNotesBuilder();
		builder.setGroup(KEY_NOTIFICATION_GROUP_NOTES);
		builder.setGroupSummary(true);
		builder.setShowWhen(false);
		builder.setContentText(context.getString(R.string.app_name));
		if (!hasOnlyOneNoteNotification()) {
			builder.setSubText(context.getString(R.string.notification_multiple_notes, String.valueOf(notesList.size())));
		}

		Notification notification = builder.build();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			Timber.i("Notification channel info: " + notification.getChannelId() + " Importance: " + manager.getNotificationChannel(notification.getChannelId()).getImportance());
		}
		manager.notify(DEFAULT_NOTIFICATION_ID, notification);
	}

	private boolean applyActionsToIndividualNote(NotificationCompat.Builder builder, Note note) {
		if (note == null) {
			return false;
		}

		PendingIntent dismissIntent = createOnNoteDismissIntent(INTENT_EXTRA_NOTE_ID_NONE);
		if (dismissIntent == null) {
			Toast.makeText(context, R.string.error_notification_pending_intent_failure, Toast.LENGTH_LONG).show();
		} else {
			builder.setDeleteIntent(dismissIntent);
			builder.addAction(R.drawable.baseline_notifications_off_black_24, context.getString(R.string.action_mark_disabled_all), createOnNoteDismissIntent(INTENT_EXTRA_NOTE_ID_NONE));
		}

		Timber.i("Setting up notification actions for note ID " + note.getDatabaseID() + " (" + note.getTextPreview() + ").");
		PendingIntent intentToNote = getIntentToNote(note);
		if (intentToNote == null) {
			Toast.makeText(context, R.string.error_notification_pending_intent_failure, Toast.LENGTH_LONG).show();
		} else {
			builder.setContentIntent(intentToNote);
		}
		createOnNoteContentIntent(builder, note);

		return true;
	}

	private PendingIntent createOnNoteDismissIntent(int notificationId) {
		Intent intent = new Intent(context, NotificationDismissedReceiver.class);
		intent.putExtra(INTENT_EXTRA_NOTE_ID, notificationId);
		Timber.i("Creating a dismiss intent. ID: " + notificationId);

		PendingIntent broadcast = null;
		try {
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
				broadcast = PendingIntent.getBroadcast(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
			} else {
				broadcast = PendingIntent.getBroadcast(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
			}
		} catch (Exception e) {
			Timber.e(e);
			Timber.e("Failed to create on Dismissed event!");
			Toast.makeText(context, R.string.error_notification_pending_intent_failure, Toast.LENGTH_LONG).show();
		}
		return broadcast;
	}

	private void createOnNoteContentIntent(NotificationCompat.Builder builder, Note note) {
		//Applies a specific intent depending on the note's content to this
		NoteContentAnalyzer contentAnalyzer = new NoteContentAnalyzer(note);
		Timber.i("Checking if this note contains content to browse: " + note.getTextPreview());

		if (!contentAnalyzer.containsAnything()) {
			Timber.i("Nope, doesn't contain anything.");
			return;
		}

		Intent intent = new Intent(context, NotificationBrowseContentActivity.class);
		intent.putExtra(INTENT_EXTRA_NOTE_ID, note.getDatabaseID());
		intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
		intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
		intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

		int iconID;
		int textID;
		if (contentAnalyzer.containsEMail()) {
			intent.putExtra(INTENT_EXTRA_NOTE_CONTENT_TYPE, CONTENT_TYPE_MAIL);
			textID = R.string.action_write_mail;
			iconID = R.drawable.baseline_email_black_24;
			Timber.i("The note '" + note.getTextPreview() + "' got resisted as an email. Note ID: " + note.getDatabaseID());
		} else if (contentAnalyzer.containsPhoneNumber()) {
			intent.putExtra(INTENT_EXTRA_NOTE_CONTENT_TYPE, CONTENT_TYPE_PHONE_NUMBER);
			textID = R.string.action_dial;
			iconID = R.drawable.baseline_phone_black_24;
			Timber.i("The note '" + note.getTextPreview() + "' got resisted as a phone number. Note ID: " + note.getDatabaseID());
		} else if (contentAnalyzer.containsURL()) {
			intent.putExtra(INTENT_EXTRA_NOTE_CONTENT_TYPE, CONTENT_TYPE_URL);
			textID = R.string.action_browse;
			iconID = R.drawable.baseline_link_black_24;
			Timber.i("The note '" + note.getTextPreview() + "' got resisted as a link. Note ID: " + note.getDatabaseID());
		} else {
			Timber.e("A very hard and unexpected error occured while scanning a note's content and prepping a notification button for it! This should be reported somehow!");
			//TODO notify user or dev about this (automatically?)
			return;
		}

		PendingIntent pendingIntent = null;
		try {
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
				pendingIntent = PendingIntent.getActivity(context, (int) note.getDatabaseID(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
			} else {
				pendingIntent = PendingIntent.getActivity(context, (int) note.getDatabaseID(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
			}
		}catch (Exception e){
			Timber.e(e);
			Timber.e("Error! Could not set up note context specific intent!");
			Toast.makeText(context, R.string.error_notification_pending_intent_failure,Toast.LENGTH_LONG).show();
		}

		if (pendingIntent != null) {
			builder.addAction(iconID, context.getString(textID), pendingIntent);
		}
	}

	private NotificationCompat.Builder getGenericNotificationBuilder(String tickerMessage, boolean ongoing, int priority, String channelID) {
		NotificationCompat.Builder builder = getNotesBuilder();
		builder.setTicker(tickerMessage);
		builder.setOngoing(ongoing);
		builder.setPriority(priority);
		builder.setChannelId(channelID);
		return builder;
	}

	private PendingIntent getIntentToNote(Note note) {
		Intent intent = new Intent(context, EditNoteActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		intent.putExtra(EXTRA_NOTE_ACTIVITY_NOTE_ID, note.getDatabaseID());
		intent.putExtra(EditNoteActivity.EXTRA_ACTIVITY_STANDALONE, true);

		int requestCode = (int) (REQUEST_CODE_INTENT_OPEN_APP_EDIT_NOTE_DYNAMIC_BASE + note.getDatabaseID());
		PendingIntent pendingIntent = null;
		try {
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
				pendingIntent = PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
			} else {
				pendingIntent = PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
			}
		} catch (Exception e) {
			Timber.e(e);
			Timber.e("Error! Could not set up intent for Note!");
		}
		return pendingIntent;
	}

	public void displayNotificationAutomaticBackup(boolean success, String contentText) {
		BackupManager backupManager = new BackupManager(context);
		String fileCountText = context.getString(R.string.info_number_not_available);
		int backupFileCount = 0;
		try {
			backupFileCount = backupManager.findBackupFiles().size();
			fileCountText = String.valueOf(backupFileCount);
		} catch (StoragePermissionManager.InsufficientStoragePermissionException e) {
			e.printStackTrace();
			Timber.e(e);
		}

		String title;
		String extraText;
		if (success) {
			title = context.getString(R.string.notification_auto_backup_success);
			extraText = context.getString(R.string.notification_auto_backup_next_detail, contentText, fileCountText);
		} else {
			title = context.getString(R.string.notification_auto_backup_failed);
			extraText = contentText;
		}

		NotificationCompat.Builder builder = getGenericNotificationBuilder(title, false, NotificationCompat.PRIORITY_DEFAULT, CHANNEL_ID_AUTO_BACKUP_CHANNEL);
		builder.setContentTitle(title);
		builder.setShowWhen(true);
		builder.setWhen(new Date().getTime());
		builder.setContentText(contentText);
		if (backupFileCount > 0) builder.setNumber(backupFileCount);
		builder.setStyle(new NotificationCompat.BigTextStyle().bigText(extraText));

		NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		if (manager == null) {
			Timber.e("Failed to hide all notifications. No manager available.");
			return;
		}
		manager.notify(NOTIFICATION_STATIC_ID_AUTOMATIC_BACKUP, builder.build());
	}

	public void hideAllNotifications() {
		Timber.i("NotesNotificationManager: Request to hide notifications received!");
		NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		if (manager == null) {
			Timber.e("Failed to hide all notifications. No mananager available.");
			return;
		}

		manager.cancelAll();
	}

	public boolean isReversed() {
		return reversed;
	}

	public int getNoteNotificationCount() {
		return notesList.size();
	}

	private NotificationCompat.Builder getNotesBuilder() {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
		//builder.setDefaults(Notification.DEFAULT_ALL);
		builder.setSmallIcon(R.drawable.notification_ticker_bar);
		builder.setTicker(context.getString(R.string.notes_notification_ticker));
		builder.setAutoCancel(true);
		builder.setOngoing(!sharedPreferences.getBoolean("prefs_dismissable_notes", false));

		int priority = getNoteNotificationPriority();
		builder.setPriority(priority);
		builder.setChannelId(channelManager.getNoteChannel(priority));

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			builder.setColor(context.getColor(R.color.notification_accent));
		} else {
			builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher));
			builder.setContentTitle(context.getString(R.string.app_name));
		}

		PendingIntent pendingIntent = getIntentToMainActivity();
		if (pendingIntent != null) {
			builder.setContentIntent(pendingIntent);
		} else {
			Toast.makeText(context, R.string.error_notification_pending_intent_failure, Toast.LENGTH_LONG).show();
		}

		return builder;
	}

	private PendingIntent getIntentToMainActivity() {
		Intent intent = new Intent(context, MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		PendingIntent pendingIntent = null;
		try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				pendingIntent = PendingIntent.getActivity(context,
						REQUEST_CODE_INTENT_OPEN_APP, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
			} else {
				pendingIntent = PendingIntent.getActivity(context,
						REQUEST_CODE_INTENT_OPEN_APP, intent, PendingIntent.FLAG_UPDATE_CURRENT);
			}
		} catch (RuntimeException e) {
			Timber.e(e);
			Timber.e("Failed to set up intent for notification!");
		}
		return pendingIntent;
	}

	private int getNoteNotificationPriority() {
		int priority = NotificationCompat.PRIORITY_DEFAULT;
		if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREFERENCE_LOW_PRIORITY_NOTE, false)) {
			priority = NotificationCompat.PRIORITY_MIN;
		}
		if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREFERENCE_HIGH_PRIORITY_NOTE, false)) {
			priority = NotificationCompat.PRIORITY_MAX;
		}
		return priority;
	}

	public NotificationManager getNotificationManagerService() {
		NotificationManager manager = null;
		try {
			manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		} catch (Exception e) {
			Timber.w(e);
			Timber.w("Failed to create 'NotificationManager'. See above warning for stacktrace.");
			Toast.makeText(context, R.string.error_internal_error, Toast.LENGTH_LONG).show();
		}
		return manager;
	}

	public boolean hasUserPermissionToDisplayNotifications() {
		NotificationManager manager = getNotificationManagerService();
		if (manager == null) {
			return false;
		}

		boolean ret = true;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			ret &= manager.areNotificationsEnabled();
		}
		ret &= hasDeviceNotificationPermission();

		return ret;
	}

	public boolean shouldShowRequestPermissionRationale(Activity activity) {
		if (Build.VERSION.SDK_INT >= 33) {
			return ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS);
		}
		return false;
	}

	private boolean hasDeviceNotificationPermission() {
		if (Build.VERSION.SDK_INT >= 33) {
			return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
		}
		return true;
	}

	public void requestPermissionRationale(Activity activity) {
		if (Build.VERSION.SDK_INT >= 33) {
			ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
		} else {
			Toast.makeText(activity, R.string.error_internal_error, Toast.LENGTH_LONG).show();
		}
	}

}
