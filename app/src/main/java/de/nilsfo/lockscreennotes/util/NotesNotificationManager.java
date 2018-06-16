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

import de.nilsfo.lockscreennotes.LockScreenNotes;
import de.nilsfo.lockscreennotes.activity.EditNoteActivity;
import de.nilsfo.lockscreennotes.activity.MainActivity;
import de.nilsfo.lockscreennotes.data.Note;
import de.nilsfo.lockscreennotes.receiver.NotificationDismissedReceiver;
import de.nilsfo.lockscreennotes.sql.DBAdapter;
import de.nilsfo.lsn.R;
import timber.log.Timber;

/**
 * Created by Nils on 16.08.2016.
 */

public class NotesNotificationManager {

	public static final String PREFERENCE_LOW_PRIORITY_NOTE = "prefs_low_priority_note";
	public static final String PREFERENCE_HIGH_PRIORITY_NOTE = "prefs_high_priority_note";
	public static final String PREFERENCE_REVERSE_ORDERING = "prefs_reverse_displayed_notifications";
	public static final String INTENT_EXTRA_NOTE_ID = LockScreenNotes.APP_TAG + "notification_id";

	public static final int DEFAULT_NOTIFICATION_ID = 1;
	public static final int NOTE_PREVIEW_SIZE = -1;
	public static final int INTENT_EXTRA_NOTE_ID_NONE = -1;
	private Context context;
	private ArrayList<Note> notesList;

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
	}

	public boolean hasNotifications() {
		return !notesList.isEmpty();
	}

	public boolean hasOnlyOneNotification() {
		return getNotificationCount() == 1;
	}

	public int getNotificationCount() {
		return notesList.size();
	}

	//@Deprecated
	//public boolean isFamiliarActivityActive() {
	//	ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
	//	ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
	//
	//	Timber.i("componentName = " + cn);
	//	return true;
	//}

	public void showNotifications() {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		Timber.i("NotesNotificationManager: Request to show notifications received!");

		if (!hasNotifications()) {
			Timber.i("... but it was empty. Nothing to display.");
			return;
		}

		NotificationCompat.Builder builder = getNotesBuilder();

		String text;
		String bigtext;
		if (hasOnlyOneNotification()) {
			Note note = notesList.get(0);
			text = note.getTextPreview(NOTE_PREVIEW_SIZE);
			bigtext = note.getText();
		} else {
			if (sharedPreferences.getBoolean("prefs_seperate_notes", false)) {
				displayMultipleNotifications();
				return;
			}

			text = String.format(context.getString(R.string.notification_multiple_notes), String.valueOf(getNotificationCount()));
			builder.setNumber(getNotificationCount());
			bigtext = "";
			for (int i = 0; i < notesList.size(); i++) {
				bigtext += (i + 1) + ". " + notesList.get(i).getText() + "\n";
			}
			bigtext = bigtext.trim();

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && getNotificationPriority()==NotificationCompat.PRIORITY_MIN) {
				builder.setContentTitle(text);
			}
		}
		builder.setContentText(text);
		builder.setStyle(new NotificationCompat.BigTextStyle()
				.bigText(bigtext));
		builder.setDeleteIntent(getOnDismissIntent(INTENT_EXTRA_NOTE_ID_NONE));
		builder.setCategory(NotificationCompat.CATEGORY_REMINDER);

		NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = builder.build();
		if (manager != null) {
			manager.notify(DEFAULT_NOTIFICATION_ID, notification);
		}else{
			Timber.w("Could not display notification! Reason: Failed to get NotificationManager from SystemService!");
		}

		//Toast.makeText(context, "Notification created. Debug Code: " + new Random().nextInt(500), Toast.LENGTH_LONG).show();
	}

	private void displayMultipleNotifications() {
		NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		for (Note n : notesList) {
			String text = n.getTextPreview(NOTE_PREVIEW_SIZE);
			String bigtext = n.getText();

			NotificationCompat.Builder builder = getNotesBuilder();
			builder.setContentTitle(text);
			builder.setStyle(new NotificationCompat.BigTextStyle().bigText(bigtext));

			int id = (int) (n.getDatabaseID() % Integer.MAX_VALUE);
			builder.setDeleteIntent(getOnDismissIntent(id));
			manager.notify(id, builder.build());
		}
	}

	private PendingIntent getOnDismissIntent(int notificationId) {
		Intent intent = new Intent(context, NotificationDismissedReceiver.class);
		intent.putExtra(INTENT_EXTRA_NOTE_ID, notificationId);
		Timber.i("Creating a dismiss intent. ID: " + notificationId);
		return PendingIntent.getBroadcast(context, notificationId, intent, 0);
	}

	private NotificationCompat.Builder getNotesBuilder() {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
		//builder.setDefaults(Notification.DEFAULT_ALL);
		builder.setSmallIcon(R.drawable.notification_ticker_bar);
		builder.setTicker(context.getString(R.string.notification_ticker));
		builder.setAutoCancel(true);
		builder.setOngoing(!sharedPreferences.getBoolean("prefs_dismissable_notes", false));
		builder.setPriority(getNotificationPriority());
		builder.setShowWhen(false);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			builder.setColor(context.getColor(R.color.colorPrimaryDark));
		}else{
			builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher));
			builder.setContentTitle(context.getString(R.string.app_name));
		}

		builder.setContentIntent(getIntentToMainActivity());
		return builder;
	}

	private PendingIntent getIntentToMainActivity() {
		Intent intent = new Intent(context, MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		return PendingIntent.getActivity(context, 0, intent, 0);
	}

	//@Deprecated
	//private PendingIntent getIntentToNote(long id) {
	//	Intent intent = new Intent(context, EditNoteActivity.class);
	//	intent.putExtra(EditNoteActivity.NOTE_ACTIVITY_NOTE_ID, id);
	//	intent.setFlags(PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);
	//	return PendingIntent.getActivity(context, 0, intent, 0);
	//}

	public void hideNotifications() {
		Timber.i("NotesNotificationManager: Request to hide notifications received!");
		NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		manager.cancelAll();
	}

	private int getNotificationPriority() {
		int priority = NotificationCompat.PRIORITY_DEFAULT;
		if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREFERENCE_LOW_PRIORITY_NOTE, false)) {
			priority = NotificationCompat.PRIORITY_MIN;
		}
		if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREFERENCE_HIGH_PRIORITY_NOTE, false)) {
			priority = NotificationCompat.PRIORITY_MAX;
		}
		return priority;
	}
}
