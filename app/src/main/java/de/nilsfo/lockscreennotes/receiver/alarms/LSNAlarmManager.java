package de.nilsfo.lockscreennotes.receiver.alarms;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Calendar;

import de.nilsfo.lockscreennotes.receiver.LSNAutoBackupReceiver;
import de.nilsfo.lockscreennotes.util.TimeUtils;
import timber.log.Timber;

import static de.nilsfo.lockscreennotes.LockScreenNotes.REQUEST_CODE_INTENT_AUTO_BACKUP_ALARM;

public class LSNAlarmManager {

	public static final int AUTO_BACKUP_SCHEDULE_HOUR = 3;
	private Context context;
	private AlarmManager alarmManager;

	public LSNAlarmManager(Context context) {
		this.context = context;
		alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
	}

	public void cancelAll() {
		cancelNextAutoBackup();
	}

	public void cancelNextAutoBackup() {
		alarmManager.cancel(getAutoBackupIntent());
		Timber.i("Canceled Alarm: AutoBackup");
	}

	public void scheduleNextAutoBackup() {
		cancelAll();
		PendingIntent intent = getAutoBackupIntent();
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

		int days = Integer.valueOf(preferences.getString("pref_auto_backups_schedule_days", "3"));
		days = Math.max(days, 1);

		long triggerTimeDist = getTimeUntilNextNewAutoBackup();
		long triggerTime = System.currentTimeMillis() + triggerTimeDist;
		long interval = AlarmManager.INTERVAL_DAY * days;
		String formatedDebugTimestamp = new TimeUtils(context).formatAbsolute(triggerTime);

		Timber.i("Scheduling next alarm at " + triggerTime + ". That's: " + formatedDebugTimestamp);
		Timber.i("Repeating every " + interval + "ms. That should be every " + days + " day(s).");

		alarmManager.setInexactRepeating(AlarmManager.RTC, triggerTime, interval, intent);
	}

	private PendingIntent getAutoBackupIntent() {
		Intent myIntent = new Intent(context, LSNAutoBackupReceiver.class);
		myIntent.setAction(LSNAutoBackupReceiver.class.getCanonicalName());
		return PendingIntent.getBroadcast(context, REQUEST_CODE_INTENT_AUTO_BACKUP_ALARM, myIntent, PendingIntent.FLAG_CANCEL_CURRENT);
	}

	public long getTimeUntilNextNewAutoBackup() {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_MONTH, 1);
		c.set(Calendar.HOUR_OF_DAY, AUTO_BACKUP_SCHEDULE_HOUR);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		return (c.getTimeInMillis() - System.currentTimeMillis());
	}
}
