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
import android.util.Log;

import java.util.ArrayList;

import de.wavegate.tos.lockscreennotes.MainActivity;
import de.wavegate.tos.lockscreennotes.R;
import de.wavegate.tos.lockscreennotes.data.Note;
import de.wavegate.tos.lockscreennotes.sql.DBAdapter;

import static de.wavegate.tos.lockscreennotes.MainActivity.LOGTAG;

/**
 * Created by Nils on 16.08.2016.
 */

public class NotesNotificationManager {

	public static final String PRFERENCE_LOW_PRIORITY_NOTE = "prefs_low_priority_note";

	public static final int DEFAULT_NOTIFICATION_ID = 1;
	public static final int NOTE_PREVIEW_SIZE = 15;
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
					Log.i(LOGTAG, "NotificatuonManager: Found an enabled note with: " + id);
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

	public boolean isFamiliarActivityActive() {
		ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		ComponentName cn = am.getRunningTasks(1).get(0).topActivity;

		Log.i(LOGTAG, "componentName = " + cn);
		return true;
	}

	public void showNotifications() {
		isFamiliarActivityActive();
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		Log.i(LOGTAG, "NotesNotificationManager: Request to show notifications received!");

		if (!hasNotifications()) {
			Log.i(LOGTAG, "... but it was empty. Nothing to display.");
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
			if(sharedPreferences.getBoolean("prefs_seperate_notes",false)){
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

		NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = builder.build();
		manager.notify(DEFAULT_NOTIFICATION_ID, notification);

		//Toast.makeText(context, "Notification created. Debug Code: " + new Random().nextInt(500), Toast.LENGTH_LONG).show();
	}

	private void displayMultipleNotifications(){
		NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		for(Note n:notesList){
			String text = n.getTextPreview(NOTE_PREVIEW_SIZE);
			String bigtext = n.getText();

			NotificationCompat.Builder builder = getBuilder();
			builder.setContentText(text);
			builder.setStyle(new NotificationCompat.BigTextStyle()
					.bigText(bigtext));

			long id = n.getDatabaseID() % Integer.MAX_VALUE;
			manager.notify((int) id, builder.build());
		}

	}

	private NotificationCompat.Builder getBuilder() {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
		builder.setSmallIcon(R.drawable.notification_ticker_bar);
		builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher));
		builder.setContentTitle(context.getString(R.string.app_name));
		builder.setTicker(context.getString(R.string.notification_ticker));
		builder.setAutoCancel(true);
		builder.setOngoing(!sharedPreferences.getBoolean("prefs_dismissable_notes", false));
		builder.setPriority(getNotificationPriority());
		PendingIntent notificationIntent = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), 0);
		builder.setContentIntent(notificationIntent);

		return builder;
	}

	public void hideNotifications() {
		Log.i(LOGTAG, "NotesNotificationManager: Request to hide notifications received!");
		NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		manager.cancelAll();
	}

	private int getNotificationPriority() {
		if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PRFERENCE_LOW_PRIORITY_NOTE, true)) {
			return NotificationCompat.PRIORITY_MIN;
		}
		return NotificationCompat.PRIORITY_DEFAULT;
	}
}
