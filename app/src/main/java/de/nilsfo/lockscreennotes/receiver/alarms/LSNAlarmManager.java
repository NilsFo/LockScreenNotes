package de.nilsfo.lockscreennotes.receiver.alarms;

import static de.nilsfo.lockscreennotes.LockScreenNotes.REQUEST_CODE_INTENT_AUTO_BACKUP_ALARM;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

import java.util.Calendar;

import de.nilsfo.lockscreennotes.receiver.LSNAutoBackupReceiver;
import de.nilsfo.lockscreennotes.util.TimeUtils;
import de.nilsfo.lsn.R;
import timber.log.Timber;

public class LSNAlarmManager {

	public static final int AUTO_BACKUP_SCHEDULE_HOUR = 3;
	private Context context;
	private AlarmManager alarmManager;

	public LSNAlarmManager(Context context) {
		this.context = context;
		alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
	}

	public boolean requestCancelAndReScheduleNextAutoBackup() {
		boolean nextBackupCanceled = cancelNextAutoBackup();
		if (nextBackupCanceled) {
			Timber.i("Next backup schedule canceled successfully.");
			boolean nextBackupScheduled = scheduleNextAutoBackup();

			if (nextBackupScheduled) {
				Timber.i("Next backup schedule created successfully.");
				return true;
			} else {
				Timber.e("Failed to set up next alarm!");
				Toast.makeText(context, R.string.error_failed_to_setup_backup_alarm, Toast.LENGTH_LONG).show();
			}
		} else {
			Timber.e("Failed to cancel next alarm!");
			Toast.makeText(context, R.string.error_failed_to_cancel_backup_alarm, Toast.LENGTH_LONG).show();
		}
		return false;
	}

	public boolean cancelNextAutoBackup() {
		// TODO red val
		PendingIntent pendingIntent = getAutoBackupIntent();
		if (pendingIntent != null) {
			alarmManager.cancel(pendingIntent);
			Timber.i("Canceled Alarm: AutoBackup");
			return true;
		}
		return false;
	}

	public boolean scheduleNextAutoBackup() {
		boolean nextBackupCanceled = cancelNextAutoBackup();
		if (!nextBackupCanceled) {
			Timber.e("Failed to cancel next alarm!");
			Toast.makeText(context, R.string.error_failed_to_cancel_backup_alarm, Toast.LENGTH_LONG).show();
			return false;
		}

		PendingIntent intent = getAutoBackupIntent();
		if (intent == null) {
			return false;
		}

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		int days = Integer.parseInt(preferences.getString("pref_auto_backups_schedule_days", "3"));
		days = Math.max(days, 1);

		long triggerTimeDist = getTimeUntilNextNewAutoBackup();
		long triggerTime = System.currentTimeMillis() + triggerTimeDist;
		long interval = AlarmManager.INTERVAL_DAY * days;
		String formattedDebugTimestamp = new TimeUtils(context).formatAbsolute(triggerTime);

		Timber.i("Scheduling next alarm at " + triggerTime + ". That's: " + formattedDebugTimestamp);
		Timber.i("Repeating every " + interval + "ms. That should be every " + days + " day(s).");

		alarmManager.setInexactRepeating(AlarmManager.RTC, triggerTime, interval, intent);
		return true;
	}

	private PendingIntent getAutoBackupIntent() {
		Intent myIntent = new Intent(context, LSNAutoBackupReceiver.class);
		myIntent.setAction(LSNAutoBackupReceiver.class.getCanonicalName());
		PendingIntent broadcast = null;

		try {
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
				broadcast = PendingIntent.getBroadcast(context, REQUEST_CODE_INTENT_AUTO_BACKUP_ALARM, myIntent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
			} else {
				broadcast = PendingIntent.getBroadcast(context, REQUEST_CODE_INTENT_AUTO_BACKUP_ALARM, myIntent, PendingIntent.FLAG_CANCEL_CURRENT);
			}
		} catch (IllegalArgumentException | IllegalStateException e) {
			Timber.e(e);
			Timber.e("Failed to create broadcast Intent: " + e);
		}
		return broadcast;
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
