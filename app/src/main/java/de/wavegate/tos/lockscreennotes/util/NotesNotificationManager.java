package de.wavegate.tos.lockscreennotes.util;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import java.util.ArrayList;

import de.wavegate.tos.lockscreennotes.activity.EditNoteActivity;
import de.wavegate.tos.lockscreennotes.activity.MainActivity;
import de.wavegate.tos.lockscreennotes.R;
import de.wavegate.tos.lockscreennotes.data.Note;
import de.wavegate.tos.lockscreennotes.receiver.NotificationDismissedReceiver;
import de.wavegate.tos.lockscreennotes.sql.DBAdapter;
import timber.log.Timber;

/**
 * Created by Nils on 16.08.2016.
 */

public class NotesNotificationManager {

	public static final String PREFERENCE_LOW_PRIORITY_NOTE = "prefs_low_priority_note";
	public static final String INTENT_EXTRA_NOTE_ID = "de.wavegate.tos.lockscreennotes.notification_id";

	public static final int DEFAULT_NOTIFICATION_ID = 1;
	public static final int NOTE_PREVIEW_SIZE = -1;
	public static final int INTENT_EXTRA_NOTE_ID_NONE = -1;
	private Context context;
	private ArrayList<Note> notesList;

	public NotesNotificationManager(Context context) {
		this.context = context;

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

		NotificationCompat.Builder builder = getBuilder();

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
		}
		builder.setContentText(text);
		builder.setStyle(new NotificationCompat.BigTextStyle()
				.bigText(bigtext));
		builder.setDeleteIntent(getOnDismissIntent(INTENT_EXTRA_NOTE_ID_NONE));

		NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = builder.build();
		manager.notify(DEFAULT_NOTIFICATION_ID, notification);

		//Toast.makeText(context, "Notification created. Debug Code: " + new Random().nextInt(500), Toast.LENGTH_LONG).show();
	}

	private void displayMultipleNotifications() {
		NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		for (Note n : notesList) {
			String text = n.getTextPreview(NOTE_PREVIEW_SIZE);
			String bigtext = n.getText();

			NotificationCompat.Builder builder = getBuilder();
			builder.setContentText(text);
			builder.setStyle(new NotificationCompat.BigTextStyle()
					.bigText(bigtext));

			int id = (int) (n.getDatabaseID() % Integer.MAX_VALUE);
			builder.setDeleteIntent(getOnDismissIntent(id));
			manager.notify(id, builder.build());
		}

	}

	private PendingIntent getOnDismissIntent(int notificationId) {
		Intent intent = new Intent(context, NotificationDismissedReceiver.class);
		intent.putExtra(INTENT_EXTRA_NOTE_ID, notificationId);
		Timber.i("Creating a dismiss intent. ID: "+notificationId);
		return PendingIntent.getBroadcast(context, notificationId, intent, 0);
	}

	private NotificationCompat.Builder getBuilder() {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
		//builder.setDefaults(Notification.DEFAULT_ALL);
		builder.setSmallIcon(R.drawable.notification_ticker_bar);
		builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher));
		builder.setContentTitle(context.getString(R.string.app_name));
		builder.setTicker(context.getString(R.string.notification_ticker));
		builder.setAutoCancel(true);
		builder.setOngoing(!sharedPreferences.getBoolean("prefs_dismissable_notes", false));
		builder.setPriority(getNotificationPriority());

		builder.setContentIntent(getIntentToMainActivity());

		return builder;
	}

	private PendingIntent getIntentToMainActivity(){
		Intent intent = new Intent(context, MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		return PendingIntent.getActivity(context, 0, intent, 0);
	}

	@Deprecated
	private PendingIntent getIntentToNote(long id){
		Intent intent = new Intent(context, EditNoteActivity.class);
		intent.putExtra(EditNoteActivity.NOTE_ACTIVITY_NOTE_ID, id);
		intent.setFlags(PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);
		return PendingIntent.getActivity(context, 0, intent, 0);
	}

	public void hideNotifications() {
		Timber.i("NotesNotificationManager: Request to hide notifications received!");
		NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		manager.cancelAll();
	}

	private int getNotificationPriority() {
		if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREFERENCE_LOW_PRIORITY_NOTE, true)) {
			return NotificationCompat.PRIORITY_MIN;
		}
		return NotificationCompat.PRIORITY_DEFAULT;
	}
}
