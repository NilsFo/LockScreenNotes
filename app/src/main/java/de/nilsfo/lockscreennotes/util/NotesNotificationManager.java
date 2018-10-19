package de.nilsfo.lockscreennotes.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;

import de.nilsfo.lockscreennotes.LockScreenNotes;
import de.nilsfo.lockscreennotes.activity.EditNoteActivity;
import de.nilsfo.lockscreennotes.activity.MainActivity;
import de.nilsfo.lockscreennotes.activity.dummy.NotificationBrowseURLActivity;
import de.nilsfo.lockscreennotes.data.Note;
import de.nilsfo.lockscreennotes.io.backups.BackupManager;
import de.nilsfo.lockscreennotes.receiver.NotificationDeleteReceiver;
import de.nilsfo.lockscreennotes.receiver.NotificationDismissedReceiver;
import de.nilsfo.lockscreennotes.sql.DBAdapter;
import de.nilsfo.lsn.R;
import timber.log.Timber;

import static de.nilsfo.lockscreennotes.LockScreenNotes.REQUEST_CODE_INTENT_OPEN_APP;
import static de.nilsfo.lockscreennotes.activity.EditNoteActivity.EXTRA_NOTE_ACTIVITY_NOTE_ID;
import static de.nilsfo.lockscreennotes.util.NotificationChannelManager.CHANNEL_ID_AUTO_BACKUP_CHANNEL;

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

		Timber.i("Notes to display: " + Arrays.toString(notesList.toArray()));
		Collections.sort(notesList);
		if (sharedPreferences.getBoolean(PREFERENCE_REVERSE_ORDERING, false)) {
			Collections.reverse(notesList);
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

	public int getNoteNotificationCount() {
		return notesList.size();
	}

	public void showNoteNotifications() {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		Timber.i("NotesNotificationManager: Request to show notifications received!");

		if (!hasNotesNotifications()) {
			Timber.i("... but it was empty. Nothing to display.");
			return;
		}

		NotificationCompat.Builder builder = getNotesBuilder();

		String text = "";
		String bigtext = "";
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

			text = String.format(context.getString(R.string.notification_multiple_notes), String.valueOf(getNoteNotificationCount()));
			builder.setNumber(getNoteNotificationCount());
			bigtext = "";
			for (int i = 0; i < notesList.size(); i++) {
				bigtext += (i + 1) + ". " + notesList.get(i).getText() + "\n";
			}
			bigtext = bigtext.trim();
		}

		builder.setContentText(text);
		builder.setStyle(new NotificationCompat.BigTextStyle().bigText(bigtext));
		builder.setDeleteIntent(createOnNoteDismissIntent(INTENT_EXTRA_NOTE_ID_NONE));
		builder.addAction(R.drawable.baseline_notifications_off_black_24, context.getString(R.string.action_mark_disabled_all), createOnNoteDismissIntent(INTENT_EXTRA_NOTE_ID_NONE));
		builder.setCategory(NotificationCompat.CATEGORY_REMINDER);

		NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
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
			manager.notify(id, builder.build());
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

		ArrayList<Note> tempList = new ArrayList<Note>(notesList);
		Collections.reverse(tempList);
		for (int i = 0; i < tempList.size(); i++) {
			Note note = tempList.get(i);
			NotificationCompat.Builder builder = getNotesBuilder();
			String noteIndex = context.getString(R.string.note_notification_index, String.valueOf(tempList.size() - i));

			builder.setGroup(KEY_NOTIFICATION_GROUP_NOTES);
			builder.setContentTitle(noteIndex);
			builder.setContentText(note.getTextPreview());
			builder.setStyle(new NotificationCompat.BigTextStyle().bigText(note.getText()));
			builder.setWhen(note.getTimestamp());

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

	private void applyActionsToIndividualNote(NotificationCompat.Builder builder, Note note) {
		builder.setDeleteIntent(createOnNoteDismissIntent((int) note.getDatabaseID()));
		builder.addAction(R.drawable.baseline_notifications_off_black_24, context.getString(R.string.action_mark_disabled), createOnNoteDismissIntent((int) note.getDatabaseID()));

		URLUtils utils = new URLUtils(context);
		String text = note.getText();
		boolean includeBrowse = false;
		if (utils.containsSingleURL(text)) {
			includeBrowse = true;
			builder.addAction(R.drawable.baseline_open_in_browser_black_24, context.getString(R.string.action_browse), createOnNoteBrowseURLIntent((int) note.getDatabaseID()));
		}
		builder.setContentIntent(getIntentToNote(note));

		Timber.i("Setting up notification actions for note ID " + note.getDatabaseID() + " (" + note.getTextPreview() + "): OnClick: true. Disable: true. Browse: " + includeBrowse);
	}

	private PendingIntent createOnNoteDismissIntent(int notificationId) {
		Intent intent = new Intent(context, NotificationDismissedReceiver.class);
		intent.putExtra(INTENT_EXTRA_NOTE_ID, notificationId);
		Timber.i("Creating a dismiss intent. ID: " + notificationId);
		return PendingIntent.getBroadcast(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
	}

	private PendingIntent createOnNoteBrowseURLIntent(int notificationId) {
		Intent intent = new Intent(context, NotificationBrowseURLActivity.class);
		intent.putExtra(INTENT_EXTRA_NOTE_ID, notificationId);
		intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
		intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
		intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		Timber.i("Creating a URL intent. ID: " + notificationId);
		return PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
	}

	@Deprecated
	private PendingIntent createOnNoteDeleteIntent(int notificationId) {
		Intent intent = new Intent(context, NotificationDeleteReceiver.class);
		intent.putExtra(INTENT_EXTRA_NOTE_ID, notificationId);
		Timber.i("Creating a delete intent. ID: " + notificationId);
		return PendingIntent.getBroadcast(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
	}

	private NotificationCompat.Builder getNotesBuilder() {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
		//builder.setDefaults(Notification.DEFAULT_ALL);
		builder.setSmallIcon(R.drawable.notification_ticker_bar);
		builder.setTicker(context.getString(R.string.notes_notification_ticker));
		builder.setAutoCancel(true);
		builder.setOngoing(!sharedPreferences.getBoolean("prefs_dismissable_notes", false));

		int prioity = getNoteNotificationPriority();
		builder.setPriority(prioity);
		builder.setChannelId(channelManager.getNoteChannel(prioity));

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			builder.setColor(context.getColor(R.color.colorNotificationLight));
		} else {
			builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher));
			builder.setContentTitle(context.getString(R.string.app_name));
		}

		builder.setContentIntent(getIntentToMainActivity());
		return builder;
	}

	private NotificationCompat.Builder getGenericNotificationBuilder(String tickerMessage, boolean ongoing, int priority, String channelID) {
		NotificationCompat.Builder builder = getNotesBuilder();
		builder.setTicker(tickerMessage);
		builder.setOngoing(ongoing);
		builder.setPriority(priority);
		builder.setChannelId(channelID);
		return builder;
	}

	private PendingIntent getIntentToMainActivity() {
		Intent intent = new Intent(context, MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		return PendingIntent.getActivity(context, REQUEST_CODE_INTENT_OPEN_APP, intent, PendingIntent.FLAG_UPDATE_CURRENT);
	}

	private PendingIntent getIntentToNote(Note note) {
		Intent intent = new Intent(context, EditNoteActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		intent.putExtra(EXTRA_NOTE_ACTIVITY_NOTE_ID, note.getDatabaseID());
		intent.putExtra(EditNoteActivity.EXTRA_ACTIVITY_STANDALONE, true);

		int requestCode = (int) (REQUEST_CODE_INTENT_OPEN_APP_EDIT_NOTE_DYNAMIC_BASE + note.getDatabaseID());
		return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
	}

	public void displayNotificationAutomaticBackup(boolean success, String contentText) {
		BackupManager backupManager = new BackupManager(context);
		int backupFileCount = backupManager.findBackupFiles().size();

		String title;
		String extraText;
		if (success) {
			title = context.getString(R.string.notification_auto_backup_success);
			extraText = context.getString(R.string.notification_auto_backup_next_detail, contentText, String.valueOf(backupFileCount));
		} else {
			title = context.getString(R.string.notification_auto_backup_failed);
			extraText = contentText;
		}

		NotificationCompat.Builder builder = getGenericNotificationBuilder(title, false, NotificationCompat.PRIORITY_DEFAULT, CHANNEL_ID_AUTO_BACKUP_CHANNEL);
		builder.setContentTitle(title);
		builder.setShowWhen(true);
		builder.setWhen(new Date().getTime());
		builder.setContentText(contentText);
		builder.setNumber(backupFileCount);
		builder.setStyle(new NotificationCompat.BigTextStyle().bigText(extraText));

		NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		if (manager == null) {
			Timber.e("Failed to hide all notifications. No mananager available.");
			return;
		}
		manager.notify(NOTIFICATION_STATIC_ID_AUTOMATIC_BACKUP, builder.build());
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

	public void hideAllNotifications() {
		Timber.i("NotesNotificationManager: Request to hide notifications received!");
		NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		if (manager == null) {
			Timber.e("Failed to hide all notifications. No mananager available.");
			return;
		}

		manager.cancelAll();
	}
}
