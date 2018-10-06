package de.nilsfo.lockscreennotes.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import de.nilsfo.lockscreennotes.receiver.alarms.LSNAlarmManager;
import de.nilsfo.lockscreennotes.util.NotesNotificationManager;

/**
 * Created by Nils on 02.09.2016.
 */

public class BootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
			new NotesNotificationManager(context).showNoteNotifications();

			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
			LSNAlarmManager alarmManager = new LSNAlarmManager(context);
			if (preferences.getBoolean("pref_auto_backups_enabled", false)) {
				alarmManager.cancelNextAutoBackup();
				alarmManager.scheduleNextAutoBackup();
			}
		}
	}
}